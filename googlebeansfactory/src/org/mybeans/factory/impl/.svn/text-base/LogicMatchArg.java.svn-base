/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import org.mybeans.factory.MatchArg;

public class LogicMatchArg extends MatchArg {
	private MatchOp    op;
	private MatchArg[] constraints;

    public LogicMatchArg(MatchOp op, MatchArg...constraints) {
    	this.op = op;
    	this.constraints = constraints.clone();
    }

    public MatchArg[] getArgs() { return constraints.clone(); }
    public MatchOp    getOp()   { return op;                  }
}
