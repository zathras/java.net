
/*  
 * Copyright (c) 2008, Sun Microsystems, Inc.
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

package com.hdcookbook.grinbunny;

import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.features.Group;
import com.hdcookbook.grin.fontstrip.FontStripText;
import com.hdcookbook.grinxlet.GrinXlet;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.Profile;

import java.util.HashMap;
import java.util.Random;

/**
 * This is the director class for the Gun Bunny game.  We chose to put all
 * of the game logic in the director class.  The media control logic that
 * runs the starfield video is located in the xlet class.  By doing this
 * separation, we're able to run the entire game in GrinView on desktop
 * JDK, which makes debugging and experimentation much easier.
 * <p>
 * GrinBunnyDirector is tighly coupled to grinbunny_show.txt.  The director
 * consists almost entirely of public methods that are called from a
 * show using the java_command construct.  This means that they happen within
 * the animator thread when the show is in the command execution phase of
 * the animation loop.  For this reason, these methods
 * can safely operate directly on the GRIN scene graph, rather than by posting
 * commands to modify it.  Of course, the director could also change the
 * scene graph by posting commands; that would be slightly less efficient
 * and would defer the state change until slightly later in the same frame
 * of animation.
 * 
 * @author Bill Foote
 */
public class GrinBunnyDirector extends Director {

    private final static String PROFILE_IP_ADDRESS = null;
        // If you want to use the profiler,set this to the
        // IP address on your LAN of the PC where you'll collect
        // the profiling data.  You can use "127.0.0.1" to profile
        // from GrinView on the same PC.
        // cf. https://hdcookbook.dev.java.net/profiler.html

    private static int GAME_DURATION_FRAMES = 24 * 60;
    private static int CARROT_SPEED = 14;
    private static int BUNNY_SPEED = 16;
    private static int SAUCER_SPEED = 15;
    private static int SAUCER_STAGE_LEFT = -202;
    private static int SAUCER_STAGE_RIGHT = 1896;

    private int framesLeft;     // # of frames left in game
    private int lastTimeLeft;   // Last value of remaining time
    private int score;          // Current score
    private int lastScore;      // Last score displayed
    private int bunnyXSpeed;
    private int bunnyX;
    private int saucerXSpeed;
    private int saucerX;
    private Random random = new Random();

    private Trooper[] troopers;  // Our troopers.  The first is taken from
                                 // the show file, and the rest are cloned.
    private int[] trooperX = new int[]
                        { 162, 364, 566, 768, 970, 1172, 1374, 1576 };
    private Group troopersGroup; // The group that holds them
    private FontStripText scoreMessage;
    private FontStripText timeMessage;
    private Arc timeLeftCircle;
    private InterpolatedModel bunnyPos;
    private Assembly carrotAssembly;
    private Feature carrotFiringState;
    private Feature carrotEmptyState;
    private int carrotX;
    private int carrotY;
    private InterpolatedModel carrotPos;
    private Assembly trooperRestore;
    private Feature trooperRestoreCountdown;
    private Assembly saucerAssembly;
    private Feature saucerShowingState;
    private Feature saucerBlamState;
    private Feature saucerEmptyState;
    private InterpolatedModel saucerPos;
    private Segment gameOverSegment;
    boolean gameRunning = false;        // Used by grinbunny_show

    private byte[] profileMessageTurtle;
    private byte[] profileMessageSaucer;

    private int xScale;
    private int yScale;

        // This static class gives us a convenient place to stash references
        // to the GRIN nodes that make up a trooper.  A trooper is one of
        // the eight turtles along the top of the screen.
    private static class Trooper {
        Feature top;            // The top node in the trooper scene graph
        InterpolatedModel pos;  // What we manipulate to move it
        Assembly assembly;      // Controls the visual state of the trooper
        Feature blamState;      // What we set it to to make it go "blam"
        Feature showingState;
    }

    public GrinBunnyDirector() {
    }

    /**
     * Called by a java_command in the show to initialize the game
     **/
    public void initializeGame() {
        if (Debug.PROFILE && PROFILE_IP_ADDRESS != null) {
            Profile.initProfiler(2008, PROFILE_IP_ADDRESS);
        }
        xScale = getShow().getXScale();
        yScale = getShow().getYScale();
        if (Debug.ASSERT && troopers != null) {
            Debug.assertFail();
        }
        Trooper firstTrooper = new Trooper();
        firstTrooper.top = getFeature("F:TurtleTrooper");
        firstTrooper.pos 
            = (InterpolatedModel) getFeature("F:TurtleTrooper.Pos");
        firstTrooper.assembly 
            = (Assembly) getFeature("F:TurtleTrooper.Assembly");
        firstTrooper.blamState = getPart(firstTrooper.assembly, "blam");
        firstTrooper.showingState = getPart(firstTrooper.assembly, "showing");

        troopers = new Trooper[trooperX.length];
        troopers[0] = firstTrooper;
        for (int i = 1; i < troopers.length; i++) {
            HashMap clones = new HashMap();
            Trooper t = new Trooper();
            t.top = firstTrooper.top.cloneSubgraph(clones);
                // We clone the trooper subgraph, then for all the nodes
                // within that cloned subgraph, we look up the named features
                // using the original named feature as key.
            t.pos = (InterpolatedModel) clones.get(firstTrooper.pos);
            t.assembly = (Assembly) clones.get(firstTrooper.assembly);
            t.blamState = (Feature) clones.get(firstTrooper.blamState);
            t.showingState = (Feature) clones.get(firstTrooper.showingState);
            troopers[i] = t;
        }
        Feature[] groupMembers = new Feature[troopers.length];
        for (int i = 0; i < troopers.length; i++) {
            troopers[i].pos.setField(Translator.X_FIELD, 
                                     Show.scale(trooperX[i], xScale));
            groupMembers[i] = troopers[i].top;
        }

        troopersGroup = (Group) getFeature("F:TurtleTroopers");
        troopersGroup.resetVisibleParts(groupMembers);

        scoreMessage = (FontStripText) getFeature("F:ScoreMessage");
        timeMessage = (FontStripText) getFeature("F:TimeMessage");
        timeLeftCircle = (Arc) getFeature("F:TimeLeftCircle");
        bunnyPos = (InterpolatedModel) getFeature("F:Bunny.Pos");
        carrotAssembly = (Assembly) getFeature("F:Carrot.Assembly");
        carrotFiringState = getPart(carrotAssembly, "firing");
        carrotEmptyState = getPart(carrotAssembly, "empty");
        carrotPos = (InterpolatedModel) getFeature("F:Carrot.Pos");
        trooperRestore = (Assembly) getFeature("F:TrooperRestore");
        trooperRestoreCountdown = getPart(trooperRestore, "countdown");
    
        saucerAssembly = (Assembly) getFeature("F:TurtleSaucer.Assembly");
        saucerShowingState = getPart(saucerAssembly, "showing");
        saucerBlamState = getPart(saucerAssembly, "blam");
        saucerEmptyState = getPart(saucerAssembly, "empty");
        saucerPos = (InterpolatedModel) getFeature("F:TurtleSaucer.Pos");

        gameOverSegment = getSegment("S:GameOver");
        if (Debug.PROFILE) {
            profileMessageTurtle = Profile.makeMessage(
                                        "Turtle hit, score now XXXXX");
            profileMessageSaucer = Profile.makeMessage(
                                        "Saucer hit, score now XXXXX");
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void notifyDestroyed() {
        if (troopersGroup != null) {
            troopersGroup.resetVisibleParts(null);
        }
        // trooper[0] wasn't cloned by us, so it shouldn't be destroyed by us
        if (troopers != null) {
            for (int i = 1; i < troopers.length; i++) {
                if (troopers[i].top != null) {
                    troopers[i].top.destroyClonedSubgraph();
                }
            }
        }
        if (Debug.PROFILE && PROFILE_IP_ADDRESS != null) {
            Profile.doneProfiling();
        }
    }

    //
    // Update our show nodes' visual state to reflect the current state
    // of the world.
    //
    private void updateShow() {
        if (score != lastScore) {
            String[] s = scoreMessage.getText();
            s[0] = "Score:  " + score;
            scoreMessage.setText(s);
            lastScore = score;
        }
        int timeLeft = (framesLeft + 23) / 24;
        if (timeLeft != lastTimeLeft) {
            String[] s = timeMessage.getText();
            int minutes = timeLeft / 60;
            timeLeft -= minutes * 60;
            String t = "Time: " + minutes + ":";
            if (timeLeft < 10) {
                t += "0";
            }
            t += timeLeft;
            s[0] = t;
            timeMessage.setText(s);
            lastTimeLeft = timeLeft;
        }
        int arcAngle = (framesLeft * 360) / GAME_DURATION_FRAMES;
        timeLeftCircle.setArcAngle(arcAngle);
        bunnyPos.setField(Translator.X_FIELD, 
                          Show.scale(bunnyX, xScale));
    }

    /**
     * Called by a java_command in the show to start the game
     **/
    public void startGame() {
        framesLeft = GAME_DURATION_FRAMES;
        lastTimeLeft = Integer.MIN_VALUE;
        score = 0;
        lastScore = Integer.MIN_VALUE;
        restoreTroopers();
        carrotAssembly.setCurrentFeature(carrotEmptyState);
        saucerAssembly.setCurrentFeature(saucerEmptyState);
        updateShow();
        bunnyXSpeed = 0;
        bunnyX = 1000;
    }

    /**
     * Called by a java_command in the show before every frame
     **/
    public void heartbeat() {
        framesLeft--;
        if (framesLeft < 0) {
            getShow().activateSegment(gameOverSegment);
            return;
        }

        // 
        // Move the bunny
        //
        bunnyX += bunnyXSpeed;
        if (bunnyX < 100) {
            bunnyX = 100;
        } else if (bunnyX > 1820) {
            bunnyX = 1820;
        }

        //
        // If the saucer isn't showing, roll the dice and maybe start it
        // off.  If it is showing and hasn't just blown up, move it,
        // and see if we've hit it.
        //
        Feature saucerState = saucerAssembly.getCurrentPart();
        if (saucerState == saucerEmptyState) {
            if (random.nextInt(24 * 5) == 7)  {
                // Make a saucer every 5 seconds or so
                if (random.nextInt(2) == 0) {
                    saucerX  = SAUCER_STAGE_LEFT;
                    saucerXSpeed = SAUCER_SPEED;
                } else {
                    saucerX  = SAUCER_STAGE_RIGHT;
                    saucerXSpeed = -SAUCER_SPEED;
                }
                saucerPos.setField(Translator.X_FIELD, 
                                   Show.scale(saucerX, xScale));
                saucerAssembly.setCurrentFeature(saucerShowingState);
                saucerState = saucerShowingState;
            }
        } else if (saucerState == saucerShowingState
                    || saucerState == saucerBlamState)
        {
            saucerX += saucerXSpeed;
            saucerPos.setField(Translator.X_FIELD, 
                                Show.scale(saucerX, xScale));
            if (saucerX < SAUCER_STAGE_LEFT || saucerX > SAUCER_STAGE_RIGHT) {
                saucerAssembly.setCurrentFeature(saucerEmptyState);
                saucerState = saucerEmptyState;
            }
        }

        //
        // Collision detection.  I got the numbers here by positioning
        // the carrot on the screen, and visually checking where it runs
        // into things.  This is pretty easy to do, by overriding the
        // arrow keys to just move the carrot one pixel at a time, with
        // the game time set long.
        //

        //
        // If we're firing...
        //
        if (carrotAssembly.getCurrentPart() == carrotFiringState) {
            carrotY -= CARROT_SPEED;
            carrotPos.setField(Translator.Y_FIELD, 
                                Show.scale(carrotY, yScale));
            boolean hit = false;

            //
            // Check for trooper hit
            //
            for (int i = 0; !hit && i < troopers.length; i++) {
                Trooper t = troopers[i];
                int xT = trooperX[i];
                hit = t.assembly.getCurrentPart() == t.showingState
                      && -505 <= carrotY && carrotY <= -436
                      && 9 <= (carrotX - xT) && (carrotX - xT) <= 61;
                if (hit) {
                    score += 50;
                    t.assembly.setCurrentFeature(t.blamState);
                        // The show has a timer that will move it to
                        // empty 10 frames later
                    if (Debug.PROFILE) {
                        setScore(profileMessageTurtle, score);
                        Profile.sendMessage(profileMessageTurtle);
                    }
                }
            }

            //
            // Check to see if all troopers have now been hit, and if so,
            // start a timer to restore them.
            //
            if (hit) {
                boolean all = true;
                for (int i = 0; all && i < troopers.length; i++) {
                    Trooper t = troopers[i];
                    all = t.assembly.getCurrentPart() != t.showingState;
                }
                if (all) {
                    trooperRestore.setCurrentFeature(trooperRestoreCountdown);
                        // 24 frames later, it will call restoreTroopers()
                        // for us.
                }
            }

            //
            // Check to see if we've hit the saucer
            //
            if (!hit && saucerState == saucerShowingState) {
                hit = -673 <= carrotY && carrotY <= -554 
                     && 31 <= (carrotX - saucerX) && (carrotX - saucerX) <= 199;
                if (hit) {
                    score += 350;
                    saucerAssembly.setCurrentFeature(saucerBlamState);
                    if (Debug.PROFILE) {
                        setScore(profileMessageSaucer, score);
                        Profile.sendMessage(profileMessageSaucer);
                    }
                }
            }

            //
            // Check for carrot off top
            //
            if (!hit) {
                if (carrotY <= -710) {
                    hit = true;
                }
            }

            //
            // Restore carrot to firable if we hit something
            //
            if (hit) {
                carrotAssembly.setCurrentFeature(carrotEmptyState);
            }
        }
        updateShow();
    }

    //
    // Stuff the decimal score into the end of profileMessage
    //
    private void setScore(byte[] profileMessage, int score) {
        if (Debug.PROFILE) {
            for (int i = 1; i <= 5; i++) {
                profileMessage[profileMessage.length-i] 
                    = (byte) ('0' + score % 10);
                score /= 10;
            }
        }
    }

    /**
     * Restore the troopers to the visible state.  This is called from
     * a java_command in the show, and from startShow() (which is itself
     * part of a java_command).
     **/
    public void restoreTroopers() {
        for (int i = 0; i < troopers.length; i++) {
            troopers[i].assembly.setCurrentFeature(troopers[i].showingState);
        }
    }

    /**
     * Called by a java_command in the show when the user asks the 
     * bunny to move left
     **/
    public void moveBunnyLeft() {
        bunnyXSpeed = -BUNNY_SPEED;
    }

    /**
     * Called by a java_command in the show when the user asks 
     * the bunny to move right.
     **/
    public void moveBunnyRight() {
        bunnyXSpeed = BUNNY_SPEED;
    }

    /**
     * Called by a java_command in the show when the user asks the bunny to stop
     **/
    public void stopBunny() {
        bunnyXSpeed = 0;
    }


    /**
     * Called by a java_command in the show when the user asks the bunny to fire
     **/
    public void fire() {
        if (carrotAssembly.getCurrentPart() == carrotFiringState) {
            // We're already firing, so ignore.
            return;
        }
        carrotAssembly.setCurrentFeature(carrotFiringState);
        carrotX = bunnyX;
        carrotY = CARROT_SPEED;
                // This will become 0 when heartbeat() gets called,
                // which will happen later in this same frame.
        carrotPos.setField(Translator.X_FIELD, 
                                Show.scale(carrotX, xScale));
        carrotPos.setField(Translator.Y_FIELD, 
                                Show.scale(carrotY, yScale));
    }

}
