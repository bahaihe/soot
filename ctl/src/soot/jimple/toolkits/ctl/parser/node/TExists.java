/* This file was generated by SableCC (http://www.sablecc.org/). */

package soot.jimple.toolkits.ctl.parser.node;

import soot.jimple.toolkits.ctl.parser.analysis.*;

@SuppressWarnings("nls")
public final class TExists extends Token
{
    public TExists()
    {
        super.setText("E");
    }

    public TExists(int line, int pos)
    {
        super.setText("E");
        setLine(line);
        setPos(pos);
    }

    @Override
    public Object clone()
    {
      return new TExists(getLine(), getPos());
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseTExists(this);
    }

    @Override
    public void setText(@SuppressWarnings("unused") String text)
    {
        throw new RuntimeException("Cannot change TExists text.");
    }
}