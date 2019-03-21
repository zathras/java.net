
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

package com.hdcookbook.gunbunny;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.MediaTracker;

import org.dvb.ui.FontNotAvailableException;

import com.hdcookbook.gunbunny.util.ImageUtil;
import com.hdcookbook.gunbunny.util.Debug;

/**
 * This is the main class that sets up and manages the game's state.
 * 
 * @author Bill Foote
 */
public class Game {

    private static int gameDuration = 24 * 60;  // game duration in frames
    private int gameStartFrame;

    private final static int STATE_INITIAL = 0;
    private final static int STATE_PLAYING = 1;
    private final static int STATE_GAME_OVER = 2;
    private int state = STATE_INITIAL;
    
    private GunBunnyXlet xlet;

    private Bunny bunny;
    private TurtleTrooperSquad squad;
    private TurtleSaucer saucer;

    private ImageSprite gameTitle;
    private TextSprite gameMessage;
    private ImageSprite gameOverTitle;
    private TextSprite gameOverMessage;
    private TextSprite timeSprite;
    private TextSprite scoreSprite;

    private boolean needsClear = false;
    private int lastFrame = 0;

    public Game() {
    }

    public void initialize(GunBunnyXlet xlet) throws InterruptedException {
        this.xlet = xlet;

        if (Debug.LEVEL > 0) {
            Debug.println("Loading assets...");
        }
        MediaTracker tracker = new MediaTracker(xlet);
        Image turtleSaucerBlam = ImageUtil.getImage(
                        "images/turtle_saucer_blam.png", tracker);
        Image turtleSaucer = ImageUtil.getImage(
                        "images/turtle_saucer.png", tracker);
        Image turtleTrooper = ImageUtil.getImage(
                        "images/turtle_trooper.png", tracker);
        Image turtleTrooperBlam = ImageUtil.getImage(
                        "images/turtle_trooper_blam.png", tracker);
        Image bunnyImg = ImageUtil.getImage(
                        "images/bunny_00.png", tracker);
        Image carrotBullet = ImageUtil.getImage(
                        "images/carrot_bullet_01.png", tracker);
        Image gameTitleImg = ImageUtil.getImage(
                        "images/text_title.png", tracker);
        Image gameOverTitleImg = ImageUtil.getImage(
                        "images/text_title_gameover.png", tracker);
        tracker.waitForAll();   // Might throw InterruptedException
        if (Debug.LEVEL > 0) {
            Debug.println("Assets loaded.");
        }
            
        squad = new TurtleTrooperSquad(turtleTrooper, turtleTrooperBlam);
        squad.assemble();
        saucer = new TurtleSaucer(turtleSaucer, turtleSaucerBlam);
        bunny = new Bunny(bunnyImg, carrotBullet, squad, saucer);            

        gameTitle = new ImageSprite(gameTitleImg);
        gameTitle.initPositionCentered(0, 0, xlet.width, xlet.height);
        Font lisa;
        try {
            lisa = xlet.fontFactory.createFont("Lisa", Font.PLAIN, 64);
        } catch (Exception ex) {
            // Shouldn't happen, unless we're built with a bad font.
            lisa = new Font("SansSerif", Font.PLAIN, 60);
        }
        Color messageColor = new Color(240, 0, 0);
        gameMessage = new TextSprite("Hit Enter to begin!", lisa, messageColor,
                                     xlet, 600, 870);
        gameOverTitle = new ImageSprite(gameOverTitleImg);
        gameOverTitle.initPositionCentered(0, 0, xlet.width, xlet.height);
        gameOverMessage = new TextSprite(
                "Hit Enter to go to main menu.",
                lisa, messageColor, xlet, 300, 910);
        timeSprite = new TextSprite("", lisa, messageColor, xlet, 20, 10);
        scoreSprite = new TextSprite("", lisa, messageColor, xlet, 1410, 10);
    }

    /**
     * Advance the game's model to the given animation frame.
     **/
    public void advanceToFrame(int frame) {
        int numFrames = frame - lastFrame;
        lastFrame = frame;
        switch(state) {
            case STATE_INITIAL:
                gameTitle.nextFrame();
                gameMessage.nextFrame();
                break;
            case STATE_PLAYING: {
                int framesLeft = gameDuration - (frame - gameStartFrame);
                if (framesLeft > 0) {
                    bunny.nextFrame(numFrames);
                    squad.nextFrame(numFrames);
                    saucer.nextFrame(numFrames);
                    timeSprite.nextFrame("Time: " + (framesLeft / 24));
                    int score = bunny.trooperHits * 50
                                + bunny.saucerHits * 350;
                    scoreSprite.nextFrame("Score: " + score);
                    break;
                } else {
                    state = STATE_GAME_OVER;
                    needsClear = true;
                    // fall through to case STATE_GAME_OVER
                }
            }
            case STATE_GAME_OVER:
                gameOverTitle.nextFrame();
                gameOverMessage.nextFrame();
                break;
        }
    }

    /**
     * Paint the current state of the game
     **/
    public void paintFrame(Graphics2D g, boolean paintAll, Animator animator) {
        if (needsClear && animator != null) {
            paintAll = true;
            g.setColor(ImageUtil.colorTransparent);
            Rectangle r = animator.getPosition();
            g.fillRect(0, 0, r.width, r.height);
            needsClear = false;
        }
        switch(state) {
            case STATE_INITIAL:
                gameTitle.paintFrame(g, paintAll, animator);
                gameMessage.paintFrame(g, paintAll, animator);
                break;
            case STATE_PLAYING:
                bunny.paintFrame(g, paintAll, animator);
                squad.paintFrame(g, paintAll, animator);
                saucer.paintFrame(g, paintAll, animator);
                timeSprite.paintFrame(g, paintAll, animator);
                scoreSprite.paintFrame(g, paintAll, animator);
                break;
            case STATE_GAME_OVER:
                gameOverTitle.paintFrame(g, paintAll, animator);
                gameOverMessage.paintFrame(g, paintAll, animator);
                break;
        }
    }

    private void resetGameTime(int currFrame) {
        gameStartFrame = currFrame;
    }

    /**
     * Called by the xlet to end the game
     **/
    
    public synchronized void handleEnd() {
        state = STATE_GAME_OVER;
    }
    
    /**
     * Called by the xlet when the enter/OK key is pressed
     **/
    public void handleEnter() {
        boolean destroy = false;
        synchronized(this) {
            switch(state) {
                case STATE_INITIAL:
                    state = STATE_PLAYING;
                    needsClear = true;
                    resetGameTime(xlet.frame);
                    break;
                case STATE_PLAYING:
                    bunny.fire();
                    break;
                case STATE_GAME_OVER:
                    destroy = true;
                    break;
            }
        }
        if (destroy) {
            xlet.destroySelf();
        }
    }

    /** 
     * Called by the xlet when the right arrow key is pressed
     **/
    public synchronized void handleRight() {
        if (state == STATE_PLAYING) {
            bunny.setXSpeed(16);
        }
    }

    /** 
     * Called by the xlet when the left arrow key is pressed
     **/
    public synchronized void handleLeft() {
        if (state == STATE_PLAYING) {
            bunny.setXSpeed(-16);
        }
    }

    /** 
     * Called by the xlet when the up arrow key is pressed
     **/
    public synchronized void handleUp() {
        if (state == STATE_PLAYING) {
            bunny.fire();
        }
    }

    /** 
     * Called by the xlet when the down arrow key is pressed
     **/
    public synchronized void handleDown() {
        if (state == STATE_PLAYING) {
            bunny.setXSpeed(0);
        }
    }
}
