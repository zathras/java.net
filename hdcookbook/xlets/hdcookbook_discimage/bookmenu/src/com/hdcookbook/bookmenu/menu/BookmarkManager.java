
/*  
 * Copyright (c) 2007, Sun Microsystems, Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of Sun Microsystems nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 *  Note:  In order to comply with the binary form redistribution 
 *         requirement in the above license, the licensee may include 
 *         a URL reference to a copy of the required copyright notice, 
 *         the list of conditions and the disclaimer in a human readable 
 *         file with the binary form of the code that is subject to the
 *         above license.  For example, such file could be put on a 
 *         Blu-ray disc containing the binary form of the code or could 
 *         be put in a JAR file that is broadcast via a digital television 
 *         broadcast medium.  In any event, you must include in any end 
 *         user licenses governing any code that includes the code subject 
 *         to the above license (in source and/or binary form) a disclaimer 
 *         that is at least as protective of Sun as the disclaimers in the 
 *         above license.
 * 
 *         A copy of the required copyright notice, the list of conditions and
 *         the disclaimer will be maintained at 
 *         https://hdcookbook.dev.java.net/misc/license.html .
 *         Thus, licensees may comply with the binary form redistribution
 *         requirement with a text file that contains the following text:
 * 
 *             A copy of the license(s) governing this code is located
 *             at https://hdcookbook.dev.java.net/misc/license.html
 */

package com.hdcookbook.bookmenu.menu;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.commands.ActivatePartCommand;
import com.hdcookbook.grin.commands.SetVisualRCStateCommand;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.input.VisualRCHandler;
import com.hdcookbook.grin.util.Debug;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * This class manages all of the state associated with the
 * user-settable bookmarks, and it updates the UI to reflect
 * that state.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class BookmarkManager {

    private MenuXlet xlet;
    private Command deleteHelpMessageOn;
    private Command deleteHelpMessageOff;
    private Command scenesNormal;
    private Command scenesSelected;
    private Command[] menuStateNormal = new Command[5];
    private Command[] menuStateSelected = new Command[5];
    private Command[] showNumBookmarks = new Command[6];  
                                // [0] is the help message
    private Segment bookmarkSnapSegment;
    private Command[] setHandlerState = new Command[6];
                                // [0] is for scenes
    private Text[] bookmarkText = new Text[5];
    private long[] bookmarks = { Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE,
                                 Long.MIN_VALUE, Long.MIN_VALUE };
    private int numBookmarks = 0;  // Number of set entries in bookmarks
    private int currBookmark;      // Currently selected bookmark, 0 is "none"
    private boolean bookmarksChanged = false;

    /**
     * Create the bookmark manager.
     **/
    public BookmarkManager(MenuXlet xlet) {
        this.xlet = xlet;
    }

    /**
     * Initialize the bookmark manager.  Called on xlet startup.
     **/
    public void init() {
        Assembly a;
        a = (Assembly) getFeature("F:BookmarksMenu.DeleteHelpMessage.State");
        deleteHelpMessageOn = getPartSelectCommand(a, "on");
        deleteHelpMessageOff = getPartSelectCommand(a, "off");
        a = (Assembly) getFeature("F:BookmarksMenu.Scenes.State");
        scenesNormal = getPartSelectCommand(a, "normal");
        scenesSelected = getPartSelectCommand(a, "selected");
        for (int i = 0; i < menuStateNormal.length; i++) {
            a = (Assembly) getFeature("F:BookmarksMenu." + (i+1) + ".State");
            menuStateNormal[i] = getPartSelectCommand(a, "normal");
            menuStateSelected[i] = getPartSelectCommand(a, "selected");
        }
        a = (Assembly) getFeature("F:BookmarksMenu.NumBookmarks.Assembly");
        showNumBookmarks[0] = getPartSelectCommand(a, "help");
        for (int i = 1; i < showNumBookmarks.length; i++) {
            showNumBookmarks[i] = getPartSelectCommand(a, Integer.toString(i));
        }
        bookmarkSnapSegment = xlet.show.getSegment("S:BookmarkSnap");
        if (Debug.ASSERT && bookmarkSnapSegment == null) {
            Debug.assertFail();
        }
        VisualRCHandler h 
            = (VisualRCHandler) xlet.show.getRCHandler("H:BookmarksMenu");
        int s = h.lookupState("scenes");
        Show show = xlet.director.getShow();
        setHandlerState[0] = new SetVisualRCStateCommand(show, false, s, h, false);
        for (int i = 1; i < setHandlerState.length; i++)  {
            s = h.lookupState(Integer.toString(i));
            setHandlerState[i] = new SetVisualRCStateCommand(show, false, s, h,false);
        }
        for (int i = 0; i < bookmarkText.length; i++) {
            bookmarkText[i] 
                = (Text) getFeature("F:BookmarksMenu." + (i+1) + ".Text");
        }
        readBookmarks();
    }

    private Feature getFeature(String name) {
        Feature f = xlet.show.getFeature(name);
        if (Debug.ASSERT) {
            if (f == null) {
                Debug.assertFail();
            }
        }
        return f;
    }

    private Command getPartSelectCommand(Assembly assembly, String partName) {
        Feature f= ((Assembly) assembly).findPart(partName);
        if (Debug.ASSERT) {
            if (f == null) {
                Debug.assertFail();
            }
        }
        return new ActivatePartCommand(xlet.director.getShow(), assembly, f);
    }

    private String bookmarksFileName() {
        return System.getProperty("dvb.persistent.root")
               + "/" + xlet.context.getXletProperty("dvb.org.id")
               + "/" + xlet.context.getXletProperty("dvb.app.id")
               + "/bookmarks.dat";
    }

    //
    // Read the bookmarks from persistent storage
    //
    private void readBookmarks() {
        bookmarksChanged = false;
        FileInputStream is = null;
        try {
            String nm = bookmarksFileName();
            if (Debug.LEVEL > 0) {
                Debug.println("Reading bookmarks from " + nm);
            }
            is = new FileInputStream(nm);
            DataInputStream dis 
                = new DataInputStream(new BufferedInputStream(is));
            int newNum = dis.readInt();
            if (newNum < 0 || newNum > bookmarks.length) {
                if (Debug.LEVEL > 0) {
                    Debug.println("**** Corrupt bookmark data ****");
                }
            } else {
                long[] newBookmarks = new long[bookmarks.length];
                for (int i = 0; i < newBookmarks.length; i++) {
                    newBookmarks[i] = dis.readLong();
                }
                numBookmarks = newNum;
                bookmarks = newBookmarks;
            }
            dis.close();
            is = null;
        } catch (IOException ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
            }
        } catch (SecurityException ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
                Debug.println();
                Debug.println("***  No permission to read bookmarks ***");
                Debug.println();
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    //
    // Write the bookmarks to persistent storage.  We can be nice to players
    // by minimizing how often this is called, because persistent storage
    // might be implemented in flash, which can have a limited lifespan.
    // It should be safe for us to wait until xlet termination to do this.
    //
    private void writeBookmarks() {
        FileOutputStream os = null;
        try {
            String nm = bookmarksFileName();
            if (Debug.LEVEL > 0) {
                Debug.println("Writing bookmarks to " + nm);
            }
            os = new FileOutputStream(nm);
            DataOutputStream dos = new DataOutputStream(
                                       new BufferedOutputStream(os));
            dos.writeInt(numBookmarks);
            for (int i = 0; i < bookmarks.length; i++) {
                dos.writeLong(bookmarks[i]);
            }
            dos.close();
            os = null;
        } catch (SecurityException ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
                Debug.println();
                Debug.println("***  No permission to write bookmarks ***");
                Debug.println();
            }
        } catch (IOException ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
            }
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Throwable ignored) {
                }
            }
        }
        bookmarksChanged = false;
    }


    /** 
     * This is called from the BOOK:BookmarkUI command.  The scenes
     * assembly (F:BookmarksMenu.Scenes.State) is always set to normal;
     * the script sets it to activated when appropriate.
     *
     * @param bookmarkNum  Number of bookmark to select, 0 means "scenes",
     *                     -1 means "none".
     *                     If higher than the current number of bookmarks,
     *                     it's adjusted downward.
     *
     * @return the media time of the given bookmark, or
     *         Long.MIN_VALUE.
     **/
    public synchronized long updateUI(int bookmarkNum) {
        if (bookmarkNum > numBookmarks) {
            bookmarkNum = numBookmarks;
        }
        boolean scenesSel = bookmarkNum == 0;
        if (bookmarkNum == -1) {
            bookmarkNum = 0;
        }
        this.currBookmark = bookmarkNum;
        xlet.show.runCommand(showNumBookmarks[numBookmarks]);
        if (scenesSel) {
            xlet.show.runCommand(scenesSelected);
        } else {
            xlet.show.runCommand(scenesNormal);
        }
        for (int i = 0; i < numBookmarks; i++) {
            if ((i+1) == bookmarkNum) {
                xlet.show.runCommand(menuStateSelected[i]);
            } else {
                xlet.show.runCommand(menuStateNormal[i]);
            }
        }
        if (numBookmarks == 0 || bookmarkNum == 0) {
            xlet.show.runCommand(deleteHelpMessageOff);
        } else {
            xlet.show.runCommand(deleteHelpMessageOn);
        }
        xlet.show.runCommand(setHandlerState[bookmarkNum]);
        if (bookmarkNum == 0) {
            return Long.MIN_VALUE;
        } else {
            return bookmarks[bookmarkNum-1];
        }
    }

    /**
     * This is called from the BOOK:MakeBookmark command.  It happens
     * while video is playing (segment S:VideoPlaying and some others).
     * To give user feedback, it goes to segment S:BookmarkSnap
     **/
    public void makeBookmark() {
        long tm = xlet.navigator.getMediaTime();
        if (tm == Long.MIN_VALUE) {
            // Couldn't determine time; give up.
            return;
        }
        bookmarksChanged = true;
        if (numBookmarks >= bookmarks.length) {
            for (int i = 1; i < bookmarks.length; i++) {
                bookmarks[i-1] = bookmarks[i];
                setBookmarkText(i-1);
            }
        } else {
            numBookmarks++;
        }
        bookmarks[numBookmarks - 1] = tm;
        setBookmarkText(numBookmarks - 1);
        xlet.show.activateSegment(bookmarkSnapSegment);
    }

    /**
     * This is called from the BOOK:DeleteBookmark command.  It is called
     * while the bookmarks menu is up.
     **/
    public void deleteCurrentBookmark() {
        if (currBookmark == 0) {
            return;
        }
        if (Debug .ASSERT && numBookmarks <= 0) {
            Debug.assertFail();
        }
        bookmarksChanged = true;
        for (int i = (currBookmark - 1); i < bookmarks.length-1; i++) {
            bookmarks[i] = bookmarks[i+1];
            setBookmarkText(i);
        }
        numBookmarks--;
        updateUI(currBookmark);
    }

    private final static long NANO = 1000000000;

    private void setBookmarkText(int b) {
        long tm = bookmarks[b];
        long h = tm / (NANO * 60L * 60L);
        tm = tm - h * NANO * 60L * 60L;
        long m = tm / (NANO * 60L);
        tm = tm - m * NANO * 60L;
        long s = tm / NANO;
        tm = tm - s * NANO;
        long f = tm / (NANO / 24L);
        String str = twoDigits(h) + ":" + twoDigits(m) + ":" + twoDigits(s)
                     + ":" + twoDigits(f);
        bookmarkText[b].setText(new String[] { str });
    }

    private String twoDigits(long num) {
        if (num < 0 || num > 99) {
            return "--";
        } else {
            return "" + oneDigit(num / 10) + oneDigit(num % 10);
        }
    }

    private char oneDigit(long num) {
        if (num < 0 || num > 9) {
            return '-';
        } else {
            return ((char) (((int) '0') + num));
        }
    }

    /**
     * Destroy the bookmark manager.  This is called on xlet
     * destruction.  We wait to write the bookmarks out until
     * the xlet is destroyed, because it's antisocial to write
     * to persistent storage too often.  Persistent storage
     * is often implemented as flash memory, which supports a
     * finite number of writes.
     **/
    public synchronized void destroy() {
        if (bookmarksChanged) {
            writeBookmarks();
        }
    }
}
