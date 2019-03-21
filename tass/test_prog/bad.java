

public class bad extends java.applet.Applet {

    public java.applet.AppletContext ac;

    public java.util.Hashtable foo(bad a, java.awt.im.spi.InputMethod b) 
    		throws java.security.acl.AclNotFoundException, // OK
		       java.awt.print.PrinterException  // bad
    {
	Object foo = null;
	java.nio.channels.ByteChannel ch;
	ch = (java.nio.channels.DatagramChannel) getSomething();
	System.out.println(ch.isOpen());
	java.awt.Graphics2D g = null;

	// This is OK.  In J2SE, it's defined in Grahpics2D, and in
	// PBP it's defined in Graphics, but that's not a problem.
	g.draw3DRect(0, 0, 10, 10, true);

	// This next ones are problems:  rotate doesn't
	// exist at all in PBP
	g.rotate(0.0);
	g.rotate(0.0, 0.0, 0.0);

	// This does exist
	g.setComposite(null);

	// This does not
	g.setPaint(null);

	System.out.println("vaild reference to field System.out");
	System.out.println("Invalid, but is inlined:  "
			   + java.awt.AWTEvent.WINDOW_STATE_EVENT_MASK);
	System.out.println("Invalid:  " + java.awt.Color.BLACK);

	// Bad interface impementation
	Object baz = new java.awt.datatransfer.ClipboardOwner() {
	    public void lostOwnership(java.awt.datatransfer.Clipboard cl, 
	    		              java.awt.datatransfer.Transferable t) {
	    }
	};

	// Bad class extension
	Object glorp = new java.awt.datatransfer.DataFlavor() {
	    public String toString() {
		return "glorp";
	    }
	};

	System.out.println("" + baz + glorp);

	// Put in a call to HashSet.contains(Object) that is statically
	// called by Set.contains(Object)
	java.util.Set hs = new java.util.HashSet();
	hs.contains(null);

	return null;
    }

    public Object getSomething() {
	return null;
    }

    public String callObjectToString(Object foo) {
	// This injects a of superclass reference for every class
	// that overrides toString
	return foo.toString();
    }

}
