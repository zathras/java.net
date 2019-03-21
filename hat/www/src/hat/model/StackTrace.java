
/*
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/, and in the file LICENSE.html in the
 * doc directory.
 * 
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun. Portions created by Bill Foote and others
 * at Javasoft/Sun are Copyright (C) 1997-2004. All Rights Reserved.
 * 
 * In addition to the formal license, I ask that you don't
 * change the history or donations files without permission.
 * 
 */

package hat.model;

/**
 *
 * @version     1.4, 03/06/98
 * @author      Bill Foote
 */


/**
 * Represents a stack trace, that is, an ordered collection of stack frames.
 */

public class StackTrace {

    private StackFrame[] frames;

    public StackTrace(StackFrame[] frames) {
	this.frames = frames;
    }

    /**
     * @param depth.  The minimum reasonable depth is 1.
     *
     * @return a (possibly new) StackTrace that is limited to depth.
     */
    public StackTrace traceForDepth(int depth) {
	if (depth >= frames.length) {
	    return this;
	} else {
	    StackFrame[] f = new StackFrame[depth];
	    System.arraycopy(frames, 0, f, 0, depth);
	    return new StackTrace(f);
	}
    }

    public void resolve(Snapshot snapshot) {
	for (int i = 0; i < frames.length; i++) {
	    frames[i].resolve(snapshot);
	}
    }

    public StackFrame[] getFrames() {
	return frames;
    }
}
