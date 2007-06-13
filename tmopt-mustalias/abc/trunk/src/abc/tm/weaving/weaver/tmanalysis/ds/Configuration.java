/* abc - The AspectBench Compiler
 * Copyright (C) 2006 Eric Bodden
 *
 * This compiler is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This compiler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this compiler, in the file LESSER-GPL;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package abc.tm.weaving.weaver.tmanalysis.ds;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import abc.tm.weaving.aspectinfo.TraceMatch;
import abc.tm.weaving.matching.SMEdge;
import abc.tm.weaving.matching.SMNode;
import abc.tm.weaving.matching.State;
import abc.tm.weaving.matching.TMStateMachine;
import abc.tm.weaving.weaver.tmanalysis.mustalias.TMFlowAnalysis;
import abc.tm.weaving.weaver.tmanalysis.util.SymbolShadow;

/**
 * An abstract state machine configuration. It holds a mapping from states to
 * Constraints.
 *
 * @author Eric Bodden
 */
public class Configuration implements Cloneable {

	/**
	 * a most-recently used cache to cache equal configurations; the idea is that equality checks
	 * are faster if performed on "interned" instances
	 * @see #intern()
	 * @see String#intern()
	 */
	protected static Map configToUniqueConfig = new HashMap();//new MemoryStableMRUCache("config-intern",10*1024*1024,false);
	
	public static void reset() {
		configToUniqueConfig.clear();
	}


	/** The mapping from states to constraints. */
	protected HashMap stateToConstraint;
	
	/** Statistical iteration counter. */
	public static int iterationCount;

	protected final TraceMatch tm;

	protected final TMFlowAnalysis flowAnalysis;
	
	/**
	 * Creates a new configuration holding a mapping for the given states and registering active
	 * shadows with the given analysis.
	 * @param stateIter an iterator over {@link SMNode}s of the associated tracematch state machine
	 * @param callback the analysis to call back in the case an active shadow is found
	 */
	public Configuration(TMFlowAnalysis flowAnalysis) {
		this.flowAnalysis = flowAnalysis;
		this.tm = flowAnalysis.getTracematch();
		stateToConstraint = new HashMap();
		iterationCount = 0;

		//associate each initial state with a TRUE constraint and all other states with a FALSE constraint
		Iterator<State> stateIter = tm.getStateMachine().getStateIterator();
		while(stateIter.hasNext()) {
			SMNode state = (SMNode) stateIter.next();
			Constraint constraint = state.isInitialNode() ? Constraint.TRUE : Constraint.FALSE; 
			stateToConstraint.put(state, constraint);
		}
	}
	
	/**
	 * Returns the successor configuration of this configuration under edge.
	 * Processes all currently active threads which are registered.
	 * @param edge and {@link SMVariableEdge} of the program graph
	 * @return the successor configuration under edge
	 */
	public Configuration doTransition(SymbolShadow shadow) {
		//the skip-copy has to be initialized as a copy of this configuration
		Configuration skip = (Configuration) clone();
		//the tmp-copy needs to be initialized to false on all states,
		//(we initialize it to true for initial states but that does not matter
		//because they are all the time true anyway)
		Configuration tmp = getCopyResetToInitial();
		
		//get the current symbol name
		final String symbolName = shadow.getSymbolName();
		//and thee variable binding
		final Map bindings = shadow.getTmFormalToAdviceLocal();
		//the shadow id
		final String shadowId = shadow.getUniqueShadowId();
		//all variables of the state machine
		final TMStateMachine sm = (TMStateMachine) tm.getStateMachine();
		final Collection allVariables =
			Collections.unmodifiableCollection(tm.getVariableOrder(symbolName));

		
		//for all transitions in the state machine
		for (Iterator transIter = sm.getEdgeIterator(); transIter.hasNext();) {
			SMEdge transition = (SMEdge) transIter.next();
			
			//if the labels coincide
			if(transition.getLabel().equals(symbolName)) {

				//statistics
				iterationCount++;
				
				
				if(transition.isSkipEdge()) {
					//if we have a skip transition
					assert transition.getSource()==transition.getTarget(); //must be a loop
					
					//get the state of this skip loop
					SMNode skipState = transition.getTarget();
					assert !skipState.isFinalNode(); 		   //only nonfinal nodes should have skip edges
					assert getStates().contains(skipState);    //assert consistency
					
					//get the old constraint at the state
					Constraint oldConstraint = skip.getConstraintFor(skipState);
					
					//add negative bindings
					Constraint newConstraint = oldConstraint.addNegativeBindingsForSymbol(
							allVariables,
							skipState,
							bindings,
							shadowId
					);
					
					//store the result at the original (=target) state
					skip.stateToConstraint.put(skipState, newConstraint);
				} else {
					//a "normal" transition					 
					
					//get constraint at source state
					Constraint oldConstraint = getConstraintFor(transition.getSource());
					
					//add bindings
					Constraint newConstraint = oldConstraint.addBindingsForSymbol(
							allVariables, 
							transition.getTarget(), 
							bindings, 
							shadowId,
							flowAnalysis
					); 

					//put the new constraint on the target state
					//via a disjoint update
					tmp.disjointUpdateFor(transition.getTarget(), newConstraint);
				}
			}
		}

		//disjointly merge the constraints of tmp and skip
		tmp = tmp.getJoinWith(skip);
		//cleanup the resulting configuration
		tmp.cleanup();
		//return an interned version of the result
		return tmp.intern();
	}	
	
	/**
	 * Merges the constraint disjoiuntly with the one currently associated with the state,
	 * updating this constraint of state.
	 * @param state any state in {@link #getStates()}
	 * @param constraint the constraint to merge
	 */
	public void disjointUpdateFor(SMNode state, Constraint constraint) {
		assert getStates().contains(state);		
		Constraint currConstraint = (Constraint) stateToConstraint.get(state);		
		stateToConstraint.put(state, currConstraint.or(constraint));
	}

	/**
	 * Joins this configuration with the other one and returns the result.
	 * This implies a disjoint update of all associated constraints and a merge
	 * of the associated thread edges. 
	 * @param other another configuration 
	 * @return the joined configuration
	 */
	public Configuration getJoinWith(Configuration other) {
		assert other.getStates().equals(getStates());
		
		Configuration clone = (Configuration) clone();
		for (Iterator stateIter = getStates().iterator(); stateIter.hasNext();) {
			SMNode state = (SMNode) stateIter.next();
			clone.disjointUpdateFor(state, other.getConstraintFor(state));
		}
		return clone;
	}

	/**
	 * Returns a copy of this configuration but with all constraints reset
	 * to the ones of the initial configuration.
	 * @return a configuration where each state <i>s</i> is mapped to <code>{@link Constraint#TRUE}</code>
	 * if it is initial and {@link Constraint#FALSE} otherwise.
	 */
	public Configuration getCopyResetToInitial() {
		Configuration copy = (Configuration) clone();
		for (Iterator iter = copy.stateToConstraint.entrySet().iterator(); iter.hasNext();) {
			Entry entry = (Entry) iter.next();
			SMNode state = (SMNode) entry.getKey();			
			entry.setValue(state.isInitialNode() ? Constraint.TRUE : Constraint.FALSE);
		}		
		return copy;
	}
	
	/**
	 * Cleans up this configuration.
	 * @see Constraint#cleanup()
	 */
	public void cleanup() {
		for (Iterator constIter = stateToConstraint.values().iterator(); constIter.hasNext();) {
			Constraint c = (Constraint) constIter.next();
			c.cleanup();
		}
	}
	
	/**
	 * Interns the configuration, i.e. returns a (usually) unique equal instance for it.
	 * @return a unique instance that is equal to this 
	 */
	protected Configuration intern() {
		Configuration cached = (Configuration) configToUniqueConfig.get(this);
		if(cached==null) {
			cached = this;
			configToUniqueConfig.put(this, this);
		}
		return cached;
	}

	/**
	 * Returns the state set of this configuration.
	 * @return
	 */
	public Set getStates() {
		return new HashSet(stateToConstraint.keySet()); 
	}
	
	/**
	 * Returns the constraint currently assosiated with the state. 
	 * @param state any state from {@link #getStates()}
	 * @return the constraint currently associated with this state
	 */
	public Constraint getConstraintFor(SMNode state) {
		assert getStates().contains(state);
		return (Constraint) stateToConstraint.get(state);
	}
	
	/**
	 * @return the number of disjuncts in this configuration
	 */
	public int size() {
		int res = 0;
		for (Iterator constIter = stateToConstraint.values().iterator(); constIter.hasNext();) {
			Constraint constr = (Constraint) constIter.next();
			res += constr.size();
		}
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		SMNode[] sorted = new SMNode[stateToConstraint.size()];
		//sort all states
		for (Iterator stateIter = stateToConstraint.keySet().iterator(); stateIter.hasNext();) {
			SMNode state = (SMNode) stateIter.next();
			sorted[state.getNumber()] = state;
		}

		String res = "[\n";
		for (int i = 0; i < sorted.length; i++) {
			SMNode state = sorted[i];
			res += "\t" + state.getNumber() + " -> " + stateToConstraint.get(state) + "\n";			
		}
		res += "]\n";

		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected Object clone() {
		Configuration clone;
		try {
			clone = (Configuration) super.clone();
			clone.stateToConstraint = (HashMap) stateToConstraint.clone();
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((stateToConstraint == null) ? 0 : stateToConstraint
						.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Configuration other = (Configuration) obj;
		if (stateToConstraint == null) {
			if (other.stateToConstraint != null)
				return false;
		} else if (!stateToConstraint.equals(other.stateToConstraint))
			return false;
		assert this.tm.equals(other.tm);
		return true;
	}

}
