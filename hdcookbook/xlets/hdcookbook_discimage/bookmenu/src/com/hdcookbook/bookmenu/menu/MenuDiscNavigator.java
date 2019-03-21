
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

import javax.media.Control;
import javax.media.Player;
import javax.media.Manager;
import javax.media.protocol.DataSource;
import javax.tv.xlet.XletContext;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceMediaHandler;
import javax.tv.service.selection.ServiceContextException;

import org.davic.media.MediaLocator;

import org.dvb.application.AppProxy;
import org.havi.ui.HSound;

import org.bluray.net.BDLocator;
import org.bluray.media.PlayListChangeControl;
import org.bluray.media.PlaybackControl;
import org.bluray.media.PlaybackListener;
import org.bluray.media.PlaybackMarkEvent;
import org.bluray.media.PlaybackPlayItemEvent;
import org.bluray.media.InvalidPlayListException;

/**
 * Navigate the disc.  This singleton is used by the xlet
 * to seek to different parts of the disc, change subtitles,
 * and stuff like that.  Basically, anything involving a locator
 * goes through here.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class MenuDiscNavigator extends AbstractDiscNavigator {

    private MenuXlet xlet;
    private int currentSubtitleStream = 0;      // For the feature video
    private int currentAudioStream = 1;         // For the feature video
    private boolean showIsLoaded = false;

    /**
     * The playlist entry for the background video that is shown during
     * the main menu.  This video loops automatically.
     **/
    public BDLocator menuVideoStartPL = makeBDLocator("bd://1.PLAYLIST:00000");

    private int menuVideoPL_ID = menuVideoStartPL.getPlayListId();

    /**
     * The playlist entry for the main feature video.
     **/
    public BDLocator movieVideoStartPL = makeBDLocator("bd://1.PLAYLIST:00001");

    private int movieVideoPL_ID = movieVideoStartPL.getPlayListId();

    /**
     * The playlist entry for the bonus feature video.
     **/
    public BDLocator bonusVideoStartPL = makeBDLocator("bd://1.PLAYLIST:00002");

    /**
     * The playlist entry for blank video
     **/
    public BDLocator blankVideo = makeBDLocator("bd://1.PLAYLIST:00004");
    private int blankVideoPL_ID = blankVideo.getPlayListId();

    /**
     * PL for the scenes
     **/
    public BDLocator[] sceneVideoStartPL = new BDLocator[] {
            makeBDLocator("bd://1.PLAYLIST:00001.MARK:00002"),
            makeBDLocator("bd://1.PLAYLIST:00001.MARK:00003"),
            makeBDLocator("bd://1.PLAYLIST:00001.MARK:00004"),
            makeBDLocator("bd://1.PLAYLIST:00001.MARK:00005"),
            makeBDLocator("bd://1.PLAYLIST:00001.MARK:00006")
    };

    /**
     * The sound effect when a menu entry is selected
     **/
    public HSound selectSound = makeSound("bd://SOUND:07");

    /**
     * The sound effect when a menu entry is activated
     **/
    public HSound activateSound = makeSound("bd://SOUND:02");

    public MenuDiscNavigator(MenuXlet xlet) {
        super(xlet.context);
        this.xlet = xlet;
    }

    /**
     * Initialize this navigator.  Called on xlet startup.
     **/
    public void init() {
    }

    /**
     * Start playing a playlist, or a playlist mark
     **/
    public void startVideoAt(BDLocator playlist) {
        if (playlist == null) {
            gotoPlaylistInCurrentTitle(blankVideo);  // this clip is very short
        } else {
            gotoPlaylistInCurrentTitle(playlist);
        }
    }

    /**
     * Called to notify the xlet when the video spontaneously stops.
     * This is not called when the video is stopped because we stop it.
     * If we're in a place where video is supposed to loop, this will
     * re-start it.  Note, however, that for seamless looping, it works
     * better to just repeat the video segment over and over in the
     * playlist, up to a length of an hour or more.  That way, the xlet
     * only has to loop the video once an hour, rather than once every
     * 30 seconds (for example).  When the xlet does the loop, it has
     * to stop and re-start the video no mattter what, which leads
     * to an objectionable pause on some players.
     **/
    public void notifyStop() {
        synchronized(this) {
            if (!showIsLoaded) {
                return;         // Show will bring us to video soon
            }
        }
        if (Debug.LEVEL > 0) { 
            Debug.println("notifyStop, currentPlaylistID is " 
                            + currentPlaylistID);
        }
        if (currentPlaylistID == menuVideoPL_ID) {
            gotoPlaylistInCurrentTitle(menuVideoStartPL);
        } else if (currentPlaylistID == blankVideoPL_ID) {
            // Do nothing - let it stay stopped
        } else {
            // In all other cases, we go back to the main menu state.  Note
            // that show.getSegment and show.activateSegment
            // were written such that they don't take out any
            // global locks, so it's OK for us to call them, even
            // though we know the navigator lock is held.
            //
            // We don't need to select any video, because the transition
            // from S:Loading to S:MenuRollout contains a "BOOK:PlayVideo menu"
            // command.
            xlet.show.activateSegment(xlet.show.getSegment("S:Loading"));
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
     **/
    protected void notifyAVStarted() {
        synchronized(this) {
            if (!showIsLoaded) {
                return;         // Show will change everything soon
            }
        }
        if (currentPlaylistID == movieVideoPL_ID) {
            selectSubtitleStream(currentSubtitleStream);
            selectAudio(currentAudioStream);
        } else if (currentPlaylistID == menuVideoPL_ID) {
                // Mute audio for the menu
            selectAudio(0);
        }
    }

    /** 
     * Called by the show to tell us that it's up and running
     **/
    public void notifyShowLoaded() {
        synchronized(this) {
            showIsLoaded = true;
            // Nothing waits on this, so no need to notifyAll()
        }
    }

    /**
     * Select an audio stream in the main video.
     **/
    public void selectAudioStream(int streamNumber) {
        currentAudioStream = streamNumber;
        selectAudio(streamNumber);
    }


    /** 
     * Select a subtitle stream in the main video.
     *
     * @param   streamNumber   The stream number, or 0 for no subtitles
     **/
    public void selectSubtitleStream(int streamNumber) {
        currentSubtitleStream = streamNumber;
        boolean on = streamNumber > 0;
        selectSubtitles(on, streamNumber);
    }

    /**
     * Play a sound-effects sound
     **/
    public void playSound(HSound sound) {
        if (sound == null) {
            if (Debug.LEVEL > 0) {
                Debug.println("Attempt to play null sound.");
            }
            return;
        }
        sound.play();
    }

    /**
     * Destroy this navigator.  Called on xlet termination.
     **/
    public synchronized void destroy() {
        super.destroy();
        selectSound.dispose();
        activateSound.dispose();
    }

}
