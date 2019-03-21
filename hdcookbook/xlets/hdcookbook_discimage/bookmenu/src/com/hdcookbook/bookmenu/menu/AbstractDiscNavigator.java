
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

import com.hdcookbook.grin.util.Debug;

import java.io.IOException;
import java.util.Enumeration;

import javax.media.Control;
import javax.media.Player;
import javax.media.Time;
import javax.media.Manager;
import javax.media.ControllerListener;
import javax.media.ControllerEvent;
import javax.media.RestartingEvent;
import javax.media.StartEvent;
import javax.media.StopEvent;
import javax.media.protocol.DataSource;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.SIManager;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceMediaHandler;
import javax.tv.service.selection.ServiceContextException;
import javax.tv.xlet.XletContext;

import org.davic.media.MediaLocator;
import org.davic.media.MediaTimePositionControl;
import org.havi.ui.HSound;

import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;
import org.dvb.application.CurrentServiceFilter;

import org.bluray.net.BDLocator;
import org.bluray.media.InvalidPlayListException;
import org.bluray.media.PlayListChangeControl;
import org.bluray.media.PlaybackControl;
import org.bluray.media.PlaybackListener;
import org.bluray.media.PlaybackMarkEvent;
import org.bluray.media.PlaybackPlayItemEvent;
import org.bluray.media.PrimaryAudioControl;
import org.bluray.media.PrimaryGainControl;
import org.bluray.media.StreamNotAvailableException;
import org.bluray.media.SubtitlingControl;
import org.bluray.ti.Title;
import org.bluray.ti.selection.TitleContext;

/**
 * Navigate the disc.  This is an abstract superclass for a
 * singleton is used by an xlet
 * to seek to different parts of the disc, change subtitles,
 * and stuff like that.  Basically, anything involving a locator.
 * <p>
 * This class assumes that when a title is selected, there is no
 * autoplay video.  After a title starts, the controlling Xlet should 
 * start video using gotoPlaylistInCurrentTitle().  In addition to
 * starting the video, this ensures that a Player under our control
 * has been created and is managing playback.  Title selection using
 * a ServiceContext can take that control away from our Player, so
 * going to a playlist ensures that it's restored.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/


public abstract class AbstractDiscNavigator 
                implements PlaybackListener, ControllerListener
{
    private XletContext xletContext;
    /**
     * The player for the main A/V content.  This will be set
     * as soon as we navigate to our first  playlist.
     **/
    private Player mainPlayer;
    private boolean weAreControlling = false;   // true while we control player
    private boolean playerIsStarted = false;    // Shadows player state
    private boolean waitingForStarted = false;  // handshake to avoid deadlock
    private PlayListChangeControl playlistControl;
    private MediaTimePositionControl timePositionControl;
    private PlaybackControl playbackControl;
    private SubtitlingControl subtitlingControl;
    private PrimaryAudioControl audioControl;
    private PrimaryGainControl gainControl;
    private TitleContext titleContext;

    private static SIManager siManager;

    /**
     * The playlist ID of the video that's currently playing.  We
     * rely on video not auto-starting, and instead being launched
     * by BD-J.  It's -1 if video hasn't been selected yet.
     **/
    protected int currentPlaylistID = -1;

    protected AbstractDiscNavigator(XletContext xletContext) {
        this.xletContext = xletContext;
    }

    /**
     * Convenience method to create a BD locator.  Returns
     * null on error.
     **/
    protected static BDLocator makeBDLocator(String ls) {
        if (Debug.LEVEL > 1) {
            Debug.println("Making BD locator " + ls);
        }
        try {
            return new BDLocator(ls);
        } catch (Exception ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
            }
            return null;
        }
    }

    /**
     * Convenience method to make a MediaLocator, given a BD locator string
     **/
    protected static MediaLocator makeMediaLocator(String ls) {
        return new MediaLocator(makeBDLocator(ls));
    }

    /**
     * Convenience method to make an HSound, given a BD locator string
     **/
    protected static HSound makeSound(String ls) {
        try {
            MediaLocator ml = makeMediaLocator(ls);
            HSound hs = new HSound();
            hs.load(ml.getURL());
            return hs;
        } catch (Throwable t) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(t);
                Debug.println();
                Debug.println("****  Failed to load sound " + ls + "  *****");
                Debug.println(t);
                Debug.println();
            }
            return null;
        }
    }

    /**
     * Convenience method to make Title, given a BD locator string
     **/
    protected static Title makeTitle(String ls) {
        BDLocator loc = makeBDLocator(ls);
        try {
            if (siManager == null) {
                siManager = SIManager.createInstance();
            }
            return (Title) siManager.getService(loc);
        } catch (InvalidLocatorException ignored) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ignored);
            }
            return null;
        } catch (SecurityException ex) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(ex);
                Debug.println();
                Debug.println("*** Permission denied for creating Title "+loc);
                Debug.println("*** Only signed xlets can do this.");
                Debug.println();
            }
            return null;
        }
    }


    /**
     * Select a title.  Once this is done, you should select a playlist
     * in that title.  If, instead, the title has autostart video,
     * then we won't have a JMF player monitoring that playback, so we
     * won't receive events.
     *
     * @see #gotoPlaylistInCurrentTitle(org.bluray.net.BDLocator)
     **/
    public synchronized void selectTitle(Title title) {
        if (titleContext == null) {
            try {
                ServiceContextFactory scf = ServiceContextFactory.getInstance();
                titleContext = (TitleContext)scf.getServiceContext(xletContext);
            } catch (ServiceContextException ignored) {
                if (Debug.LEVEL > 0) {
                    Debug.printStackTrace(ignored);
                }
                if (Debug.ASSERT) {
                    Debug.assertFail();
                }
            }
        }
        if (Debug.LEVEL > 0) {
            Debug.println("*** Changing title to " + title.getLocator()+" ***");
        }
        titleContext.start(title, false);
    }

    /**
     * Start playing a playlist.  This also establishes a JMF player
     * (if one hasn't been establised already), and starts listening
     * for various events.  This should be done shortly after
     * any title selection (including the initial, automatic title
     * selection).
     **/
    protected synchronized void gotoPlaylistInCurrentTitle(BDLocator loc) {

            //
            // Guard against an attempt to control player from two
            // threads simultaneously
            //
        while (weAreControlling) {
            if (Debug.LEVEL > 0) {
                Debug.println("Waiting for other thread to control player");
            }
            try {
                wait();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        if (Debug.LEVEL > 0) {
            Debug.println("Start controlling player at "
                                + System.currentTimeMillis());
        }
        if (Debug.LEVEL > 0) {
            Debug.println("*** Changing playlist to " + loc + " ***");
        }
        weAreControlling = true;
        try {
            if (mainPlayer == null) {
                try {
                    MediaLocator ml = new MediaLocator(loc);
                    mainPlayer  = Manager.createPlayer(ml);
                } catch (Exception ignored) {
                    if (Debug.LEVEL > 0) {
                        Debug.printStackTrace(ignored);
                    }
                    if (Debug.ASSERT) {
                        Debug.assertFail("Error creating player");
                    }
                }
                mainPlayer.addControllerListener(this);
                mainPlayer.prefetch();
                Control[] controls = mainPlayer.getControls();
                for (int i = 0; i < controls.length; i++) {
                    if (controls[i] instanceof PlayListChangeControl) {
                        playlistControl = (PlayListChangeControl) controls[i];
                    } else if (controls[i] instanceof PlaybackControl) {
                        playbackControl = (PlaybackControl) controls[i];
                    } else if (controls[i] instanceof SubtitlingControl) {
                        subtitlingControl = (SubtitlingControl) controls[i];
                    } else if (controls[i] instanceof PrimaryAudioControl) {
                        audioControl = (PrimaryAudioControl) controls[i];
                    } else if (controls[i] instanceof PrimaryGainControl) {
                        gainControl = (PrimaryGainControl) controls[i];
                    } else if (controls[i] instanceof MediaTimePositionControl){
                        timePositionControl = 
                                (MediaTimePositionControl)controls[i];
                    }
                }
                if (Debug.LEVEL > 1) {
                    Debug.println("Playback control:  " + playbackControl);
                    Debug.println("Playlist control:  " + playlistControl);
                    Debug.println("Subtitling control:  " + subtitlingControl);
                    Debug.println("Audio control:  " + audioControl);
                    Debug.println("Gain control:  " + gainControl);
                }
                if (Debug.ASSERT && 
                     (playbackControl == null || playlistControl == null
                      || audioControl == null || gainControl == null
                      || subtitlingControl == null 
                      || timePositionControl == null))
                {
                    Debug.assertFail("Missing control");
                }
                playbackControl.addPlaybackControlListener(this);
            } else {
                // We had already created the player, so we can use
                // org.bluray.media.PlayListChangeControl
                mainPlayer.stop();
                waitForStarted(false, 3000);
                try {
                    playlistControl.selectPlayList(loc);
                } catch (Exception ignored) {
                    if (Debug.LEVEL > 0) {
                        Debug.printStackTrace(ignored);
                    }
                }
            }
            mainPlayer.start();
            currentPlaylistID = loc.getPlayListId();
                // I'm not sure, but I think that selecting different streams
                // might not work if the player isn't started...
                //
                // Also, it's crucial that we release our lock for long
                // enough for the player state events to come in.
            if (Debug.LEVEL > 1) {
                Debug.println("Waiting for player to enter started state...");
            }
            waitForStarted(true, 3000);
            notifyAVStarted();
        } finally {
            weAreControlling = false;
            notifyAll();
            if (Debug.LEVEL > 0) {
                Debug.println("Done controlling player at " 
                                + System.currentTimeMillis());
            }
        }
    }


    /**
     * After a playlist is selected, this method is called to let
     * the subclass do any other setup, like subtitles or audio
     * stream.  When it is called, the player will already be
     * started, and the data member currentPlaylistID will be set.
     * <p>
     * This is called with the navigator lock held, so applications
     * should not do anything in this method that might cause deadlock.
     *
     * @see #currentPlaylistID
     **/
    protected abstract void notifyAVStarted();


    /**
     * Navigate to the given time in the video determined by the playlist
     * This can only be done after a playlist has been started.
     *
     * @see #gotoPlaylistInCurrentTitle(org.bluray.net.BDLocator)
     **/
    public synchronized void gotoMediaTime(BDLocator playlist, long mediaTime) {
        gotoPlaylistInCurrentTitle(playlist);
        timePositionControl.setMediaTimePosition(new Time(mediaTime));
    }

    /**
     * Select a subtitle stream.  This can only be done once a playlist
     * has been started.
     *
     * @see #gotoPlaylistInCurrentTitle(org.bluray.net.BDLocator)
     **/
    public synchronized void selectSubtitles(boolean on, int streamNum) {
        if (subtitlingControl != null) {
            if (Debug.LEVEL > 0) {
                Debug.println("Subtitles set " + on + ", " + streamNum);
            }
            subtitlingControl.setSubtitling(on);
            if (on) {
                try {
                    subtitlingControl.selectStreamNumber(streamNum);
                } catch (StreamNotAvailableException ignored) {
                    if (Debug.LEVEL > 0) {
                        Debug.println("*** Subtitles stream " + streamNum 
                                      + " not available.");
                    }
                }
            }
        }
    }

    /** 
     * Select the given audio stream.  Audio streams are numbered from 1
     * on a disc.  
     **/
    public synchronized void selectAudio(int streamNum) {
        if (gainControl != null && audioControl != null) {  
                        // They're set at same time
            if (Debug.LEVEL > 0) {
                Debug.println("Audio set to " + streamNum);
            }
            if (streamNum == 0) {
                // We could call gainControl.setMute(true);
                // However, this turns out to not be necessary with our
                // video, and it seems to trigger a player bug.
            } else {
                // Could undo mute with gainControl.setMute(false);
                try {
                    audioControl.selectStreamNumber(streamNum);
                } catch (StreamNotAvailableException ignored) {
                    if (Debug.LEVEL > 0) {
                        Debug.println("*** Audio stream " + streamNum 
                                      + " not available.");
                    }
                }
            } 
        }
    }

    /**
     * Get the current media time.
     * This can only be done after a playlist has been started.
     *
     * @return the current media time, or Long.MIN_VALUE if it can't
     *         be determined.
     *
     * @see #gotoPlaylistInCurrentTitle(org.bluray.net.BDLocator)
     **/
    public synchronized long getMediaTime() {
        if (timePositionControl == null) {
            // Should never happen
            return Long.MIN_VALUE;
        }
        Time t = timePositionControl.getMediaTimePosition();
        if (t == null) {
            return Long.MIN_VALUE;
        }
        return t.getNanoseconds();
    }

    /**
     * Destroy this disc navigator.  This should be called on
     * xlet termination.
     **/
    public synchronized void destroy() {
        if (playbackControl != null) {
            playbackControl.removePlaybackControlListener(this);
        }
        if (mainPlayer != null) {
            mainPlayer.removeControllerListener(this);
            mainPlayer.stop();          // MHP 11.7.1.2
        }
    }

    /**
     * Callback from PlaybackListener
     **/
    public void markReached(PlaybackMarkEvent event) {
        // We're not currently doing anything with these events
    }

    /**
     * Callback from PlaybackListener
     **/
    public void playItemReached(PlaybackPlayItemEvent event) {
        // We're not currently doing anything with these events
    }

    /**
     * Callback from ControllerListener
     **/
    public void controllerUpdate(ControllerEvent event) {
        boolean doNotifyStop = false;
        if (Debug.LEVEL > 1) {
            Debug.println("Received controller event " + event);
        }
        synchronized(this) {
            boolean oldPlayerState = playerIsStarted;

            if (event instanceof RestartingEvent) {
                // Restarting event is a subtype of StopEvent, and can
                // be generated for things like a rate change.  It's not
                // a StopEvent that we care about, because the player
                // is just going to Start again automatically, so we ignore
                // it.
                if (Debug.LEVEL > 1) {
                    Debug.println("Ignoring RestartingEvent");
                }
            } else if (event instanceof StartEvent) {
                if (Debug.LEVEL > 0) {
                    Debug.println("*** StartEvent at media time "
                          + ((StartEvent) event).getMediaTime().getSeconds());
                }
                playerIsStarted = true;
            } else if (event instanceof StopEvent) {
                if (Debug.LEVEL > 0) {
                    Debug.println("*** StopEvent at media time "
                          + ((StopEvent) event).getMediaTime().getSeconds());
                }
                playerIsStarted = false;
            }

            if (playerIsStarted != oldPlayerState) {
                doNotifyStop = !playerIsStarted && !weAreControlling;
                notifyAll();

                if (Debug.LEVEL > 0) {
                    Debug.println("*** Player is now " 
                                   + (playerIsStarted ? "started" : "stopped"));
                }

                //
                // Handshake with waitForStarted(), in case our video
                // is so short that it could stop almost immediately.
                //
                while (playerIsStarted && waitingForStarted) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        if (doNotifyStop) {
            notifyStop();       // With no locks held
        }
    }

    /**
     * Called to notify the xlet when the video spontaneously stops.
     * This is not called when the video is stopped because we stop it.
     *
     * @see #currentPlaylistID
     **/
    public abstract void notifyStop();


    //
    // Wait until the state of our player is the indicated state.
    // Return true if all went well, false if we time out.
    //
    private boolean waitForStarted(boolean wantStarted, long timeout) {
        long tm = 0;
        if (timeout > 0) {
            tm = System.currentTimeMillis();
        }
        for (;;) {
            synchronized(this) {
                waitingForStarted = wantStarted;        // matched in finally
                try {
                    if (wantStarted == playerIsStarted) {
                        return true;
                    }
                    if (!waitWithTimeout(this, tm, timeout)) {
                        if (Debug.LEVEL > 0) {
                            Thread.currentThread().dumpStack();
                            Debug.println();
                            Debug.println("***  WARNING:  Timed out waiting "
                                          + timeout + " ms "
                                          + "for player started " +wantStarted);
                            Debug.println();
                        }
                        return false;
                    }
                } finally {
                    if (waitingForStarted) {
                        waitingForStarted = false;
                        notifyAll();
                    }
                }
            }
        }
    }

    //
    // Wait a bit.  Return true if we got notified, false if we timed out
    // or were interrupted.  The monitor must already be held when this
    // method is called.
    //
    private boolean waitWithTimeout(Object monitor,long startTime, long timeout)
    {
        try {
            if (timeout <= 0) {
                monitor.wait();
                return true;
            } else {
                long t = timeout - (System.currentTimeMillis() - startTime);
                if (t <= 0) {
                    return false;
                }
                monitor.wait(t);
                t = timeout - (System.currentTimeMillis() - startTime);
                return t > 0;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
