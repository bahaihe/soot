package soot.jimple.paddle;

import soot.jimple.paddle.queue.*;
import soot.jimple.paddle.bdddomains.*;
import soot.*;

public class BDDObjSensVirtualContextManager extends AbsVirtualContextManager {
    BDDObjSensVirtualContextManager(Rctxt_var_obj_srcm_stmt_kind_tgtm in, Qsrcc_srcm_stmt_kind_tgtc_tgtm out) {
        super(in, out);
    }
    
    public boolean update() {
        final jedd.internal.RelationContainer newOut =
          new jedd.internal.RelationContainer(new jedd.Attribute[] { srcc.v(), srcm.v(), stmt.v(), kind.v(), tgtm.v(), tgtc.v() },
                                              new jedd.PhysicalDomain[] { C1.v(), MS.v(), ST.v(), KD.v(), MT.v(), H1.v() },
                                              ("<soot.jimple.paddle.bdddomains.srcc:soot.jimple.paddle.bdddo" +
                                               "mains.C1, soot.jimple.paddle.bdddomains.srcm:soot.jimple.pad" +
                                               "dle.bdddomains.MS, soot.jimple.paddle.bdddomains.stmt:soot.j" +
                                               "imple.paddle.bdddomains.ST, soot.jimple.paddle.bdddomains.ki" +
                                               "nd:soot.jimple.paddle.bdddomains.KD, soot.jimple.paddle.bddd" +
                                               "omains.tgtm:soot.jimple.paddle.bdddomains.MT, soot.jimple.pa" +
                                               "ddle.bdddomains.tgtc:soot.jimple.paddle.bdddomains.H1> newOu" +
                                               "t = jedd.internal.Jedd.v().project(in.get(), new jedd.Physic" +
                                               "alDomain[...]); at /home/research/ccl/olhota/olhotak/soot-tr" +
                                               "unk/src/soot/jimple/paddle/BDDObjSensVirtualContextManager.j" +
                                               "edd:35,45-51"),
                                              jedd.internal.Jedd.v().project(in.get(),
                                                                             new jedd.PhysicalDomain[] { V1.v() }));
        out.add(new jedd.internal.RelationContainer(new jedd.Attribute[] { kind.v(), srcm.v(), stmt.v(), srcc.v(), tgtc.v(), tgtm.v() },
                                                    new jedd.PhysicalDomain[] { KD.v(), MS.v(), ST.v(), C1.v(), C2.v(), MT.v() },
                                                    ("out.add(jedd.internal.Jedd.v().replace(newOut, new jedd.Phys" +
                                                     "icalDomain[...], new jedd.PhysicalDomain[...])) at /home/res" +
                                                     "earch/ccl/olhota/olhotak/soot-trunk/src/soot/jimple/paddle/B" +
                                                     "DDObjSensVirtualContextManager.jedd:37,8-11"),
                                                    jedd.internal.Jedd.v().replace(newOut,
                                                                                   new jedd.PhysicalDomain[] { H1.v() },
                                                                                   new jedd.PhysicalDomain[] { C2.v() })));
        return !jedd.internal.Jedd.v().equals(jedd.internal.Jedd.v().read(newOut), jedd.internal.Jedd.v().falseBDD());
    }
}