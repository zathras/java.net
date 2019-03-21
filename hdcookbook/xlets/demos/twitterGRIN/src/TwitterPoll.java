/*  
 * Copyright (c) 2009, Sun Microsystems, Inc.
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


import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.ImageManager;
import com.hdcookbook.grin.util.ManagedImage;

import com.substanceofcode.twitter.TwitterApi;
import com.substanceofcode.twitter.model.Status;
import com.sugree.twitter.TwitterException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

/**
 * This object is used to represent a request to poll the twitter server.
 * The information is gathered from the network on the networking thread
 * (in the run() method), then delivered to the GRIN show as a GRIN command
 * on the animation thread (in the execute() method).
 **/
class TwitterPoll extends Command implements Runnable {

    private TwitterDirector director;

    TwitterPoll(TwitterDirector director) {
        super(director.getShow());
        this.director = director;
    }

    private ManagedImage[] icons;
    private Status[] tweets;
    private boolean destroyed = false;
        // Access to destroyed doesn't need to be synchronized -- see
        // the comments in destroy()

    /**
     * Run the networking task.  This happens in the networking thread.
     **/
    public void run() {
        if (Debug.LEVEL > 0) {
            Debug.println("Polling server...");
        }
        Vector v;
        try {
            v = director.twitter.requestPublicTimeline(null);
        } catch (TwitterException ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
                Debug.println();
                Debug.println("*** Twitter message fails to load:  " + ex);
                Debug.println();
            }
            return;
        }
        icons = new ManagedImage[v.size()];
        tweets = new Status[v.size()];
        for (int i = 0; i < v.size(); i++) {
            if (Thread.interrupted()) {
                // Note that NetworkManager calls Thread.interrupt() for us
                // when it wants us to terminate.  
                //
                // For added robustness, we could add a synchronized check
                // of NetworkManager.destroyed here too - that would provide
                // some robustness against buggy libraries that catch
                // InterruptedException without re-posting the
                // Thread.interrupt().  
                destroy();
                return;
            }
            tweets[i] = (Status) v.elementAt(i);
            try {
                URL url = new URL(tweets[i].getProfileImageURL());
                icons[i] = ImageManager.getImage(url);
            } catch (MalformedURLException ignored) {
                icons[i] = null;
            }
            if (icons[i] != null) {
                icons[i].prepare();
                icons[i].load(director.getShow().component);
            }
        }
        director.setPendingCommand(this);
        show.runCommand(this);  // To update the UI in the animation thread
    }

    /**
     * Update the screen.  This happens in the animation thread.
     **/
    public void execute() {
        if (destroyed) {
            return;
        }
        director.unsetPendingCommand(this);
        if (Debug.LEVEL > 0) {
            Debug.println("Updating screen...");
        }
        director.updateScreen(tweets, icons);
    }

    /**
     * Cleanup the state held this command.  This is used when we're
     * exiting, and the show shut down during network activity.  It either
     * gets called before the show command is queued, or it gets called from
     * the animation thread after it is queued.  Either is safe.  Essentially,
     * a TwitterPoll is active in either the network thread, or in the
     * animation thread, but never in both simultaneously.  When it
     * switches from one thread to the other, there's a sinchronization
     * point (via the enqueue/dequeue).
     **/
    public void destroy() {
        destroyed = true;
        if (icons == null) {
            return;
        }
        for (int i = 0; i < icons.length; i++) {
            ManagedImage im = icons[i];
            if (im != null) {
                im.unprepare();
                ImageManager.ungetImage(im);
            }
        }
    }

}


