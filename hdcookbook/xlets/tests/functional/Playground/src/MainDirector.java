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

import java.awt.Rectangle;
import java.io.IOException;

import java.awt.AlphaComposite;
import java.awt.Container;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.Random;
import java.util.HashMap;

import com.hdcookbook.grinxlet.GrinXlet;

import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.animator.AnimationClient; 
import com.hdcookbook.grin.animator.AnimationContext;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.features.Clipped;
import com.hdcookbook.grin.features.ImageSequence;
import com.hdcookbook.grin.features.Group;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.Fade;
import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.io.binary.GrinBinaryReader;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.ImageManager;

/** 
 * The main class for the Playground project
 */

public class MainDirector extends Director {

    private InterpolatedModel scaler;
    private InterpolatedModel boxPos;
    private Text myText;
    private Fade boxedStuffFade;
    private FixedImage replaceImageImage;
    private Text f_smText;
    private Arc f_smArc;
    private ImageSequence randomImageSequence;
    private Clipped randomImageSequenceClip;
    private static String[] replacementImages = new String[] {
                    "images/hat_plain.jpg", "images/pope.jpg",
                    "images/spoo2.png"
    };
    private ManagedImage replacementImage = null;
    private Random random;

    private float fadeGoal = 1.0f;
    private float fadeAlpha = 1.0f;
    private int smAngle;
    private int smDelay;
    private int imageNumber = 0;

    public MainDirector() {
    }

    public void restoreNormalMenu() {
        AnimationClient[] clients = new AnimationClient[] { getShow() };
        GrinXlet.getInstance().resetAnimationClients(clients);
    }

    public void putNewShowOnTopOfMenu(String segmentName) {
            // First we print out the old clients.  This is only done
            // as a minimal test of AnimationEngine.getAnimationClients()
        AnimationClient[] clients 
                = GrinXlet.getInstance().getAnimationClients();
        Debug.println();
        Debug.println("Old animation clients:");
        for (int i = 0; i < clients.length; i++) {
            Debug.println("    [" + i + "]:  " + clients[i]);
        }
        Debug.println();

            // Now we create a new Show object, and set it to the
            // first segment.  It's OK to call activateSegment() before
            // the animation engine initializes the show.
        Show newShow = null;
        try {
            GrinBinaryReader reader = 
               new GrinBinaryReader(AssetFinder.getURL(
                        "second_show.grn").openStream());
            newShow = new Show(null);
            reader.readShow(newShow);
        } catch (IOException e) {
            if (Debug.LEVEL > 0) {
                Debug.printStackTrace(e);
            }
            Debug.println("Error in reading the show file");
            return;
        }
        newShow.activateSegment(newShow.getSegment(segmentName));       

            // Finally, we get the animation engine to reset its list of
            // clients.  This won't take effect until the current frame of
            // animation is complete.
        clients = new AnimationClient[] { getShow(), newShow };
        GrinXlet.getInstance().resetAnimationClients(clients);
    }


    /**
     * This method is called from main_show.txt before each frame when the
     * S:ProgrammaticSceneGraphControl segment is showing.  This
     * method does most of the control of the show, but a little bit
     * of Java scripting is also done in-line in the show file, just
     * to show what that looks like.  Note, however, that the Director
     * is the natural place to store the state information you'll
     * need for whatever you're doing, so it's probably easier to just
     * call a method on your director most of the time.
     **/
    public void programmaticallyChageSceneGraph() {
        if (scaler == null) {
            scaler = (InterpolatedModel) getFeature("F:MainScaler");
            boxPos = (InterpolatedModel) getFeature("F:BoxedStuffPosition");
            myText = (Text) getFeature("F:EnterText");
            boxedStuffFade = (Fade) getFeature("F:BoxedStuffFade");
            replaceImageImage = (FixedImage) getFeature("F:ReplaceImage.Image");
            randomImageSequence = (ImageSequence) 
                                   getFeature("F:RandomImageSequence.Im");
            randomImageSequenceClip = (Clipped) 
                                   getFeature("F:RandomImageSequence");
            random = new Random();
        }

                // Change the image sequence around, and its clip

        if (random.nextInt(48) == 42) {
            imageNumber = random.nextInt(3);
        }
        randomImageSequence.setCurrentFrame(imageNumber);
        if (random.nextInt(20) == 5) {
            Rectangle r = new Rectangle();
            r.x = 1400;
            r.y = 200;
            r.width = 20 + random.nextInt(180);
            r.height = 20 + random.nextInt(180);
            randomImageSequenceClip.changeClipRegion(r);
        }

                // Mess around with the X,Y center of scaling, and the
                // scale factors.

        if (random.nextInt(88) == 42) {
            scaler.setField(scaler.SCALE_X_FIELD, 780 + random.nextInt(400));
        }
        if (random.nextInt(88) == 42) {
            scaler.setField(scaler.SCALE_Y_FIELD, 410 + random.nextInt(300));
        }
        if (random.nextInt(88) == 42) {
            scaler.setField(scaler.SCALE_X_FACTOR_FIELD, 
                                500 + random.nextInt(1000));
        }
        if (random.nextInt(88) == 42) {
            scaler.setField(scaler.SCALE_Y_FACTOR_FIELD, 
                                500 + random.nextInt(1000));
        }

                // Randomly change the translation of the boxed stuff

        if (random.nextInt(88) == 42) {
            boxPos.setField(Translator.X_FIELD, -600 + random.nextInt(700));
        }
        if (random.nextInt(88) == 42) {
            boxPos.setField(Translator.Y_FIELD, -300 + random.nextInt(400));
        }

                // Scramble the text in entertaining ways

        if (random.nextInt(50) == 21) {
            String[] result = new String[2];
            String[] src = { "Press", "enter", "to", "return" };
            result[0] = scrambleText(src, 4, result[0]);
            result[0] = scrambleText(src, 3, result[0]);
            result[1] = scrambleText(src, 2, result[1]);
            result[1] = scrambleText(src, 1, result[1]);
            myText.setText(result);
        }

                // Make the boxed stuff fade in and out, but mostly
                // in, and make it smooth.
        if (random.nextInt(100) == 42) {
            fadeGoal = random.nextFloat();
            fadeGoal = 1.0f - fadeGoal * fadeGoal;  // Bias toward visibility
        }
        fadeAlpha = 0.95f * fadeAlpha + 0.05f * fadeGoal;
        if (fadeAlpha < 0f) {
            fadeAlpha = 0f;
        } else if (fadeAlpha > 1f) {
            fadeAlpha = 1f;
        }
        AlphaComposite ac 
            = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha);
        boxedStuffFade.setAlpha(ac);
    }

    private String scrambleText(String[] src, int srcLen, String line) {
        int i = random.nextInt(srcLen);
        String tmp = src[i];
        src[i] = src[srcLen-1];
        src[srcLen-1] = null;
        if (line == null) {
            return tmp;
        } else {
            return line + " " + tmp;
        }
    }

    //
    // Called each frame to manage the random replacement of 
    // the image F:ReplaceImage.Image.  We only replace images from a
    // set of 3, but we load the new image and unload the old.  This way
    // the technique can be scaled to any number of images.
    //
    // You'll also note that the API works in terms of only the
    // upper-left hand corner, but in this method we keep the images
    // centered about the same point.  We just do the arithmetic ourselves.
    //
    public void replaceImage() {
        if (replacementImage == null) {
            if (random.nextInt(48) != 42) {     
                // replace each two seconds on average
                return;
            }
            int i = random.nextInt(replacementImages.length);
            String name = replacementImages[i];
            Debug.println("Replace image with " + name);
            replacementImage = ImageManager.getImage(name);
            replacementImage.prepare();
            replacementImage.startLoading(getShow().component);
            return;
        }
        if (replacementImage.isLoaded()) {
            ManagedImage old = replaceImageImage.getImage();
            Rectangle loc = replaceImageImage.getMutablePlacement();
            loc.x = loc.x + loc.width/2 - replacementImage.getWidth()/2;
            loc.y = loc.y + loc.height/2 - replacementImage.getHeight()/2;
            loc.width = replacementImage.getWidth();
            loc.height = replacementImage.getHeight();
            replaceImageImage.setImageSizeChanged();
            // loc is mutable, and retained by replaceImageImage, so setting
            // the fields of the Rectangle has the effect of moving the
            // FixedImage.

            replaceImageImage.replaceImage(replacementImage);

            // When we give the image to FixedImage, it increments the 
            // reference count and the prepare count of replacementImage.
            // (ManagedImage.addReference() and ManagedImage.prepare()).
            // Thus, we need to remove our prepare and our reference count.
            // This is precisely what needs to be done by 
            // stopImageReplacement(), too.
            stopImageReplacement();
        }
    }

    public void stopImageReplacement() {
        if (replacementImage != null) {
            replacementImage.unprepare();
            ImageManager.ungetImage(replacementImage);
            replacementImage = null;
        }
    }

    public void notifyDestroyed() {
        super.notifyDestroyed();
        stopImageReplacement();
    }

    //***********************
    //  Assembly clone test:
    //***********************

    private Assembly assemblyCloneTestClone;

    public void createAssemblyCloneTest() {
        Assembly a = (Assembly) getFeature("F:AC.Assembly");
        Group g = (Group) getFeature("F:AC.ClonedGroup");
            // performance here doesn't matter, so we just look it up
            // every time
        assemblyCloneTestClone = (Assembly) a.cloneSubgraph(new HashMap());
        Feature[] parts = new Feature[] { assemblyCloneTestClone };
        g.resetVisibleParts(parts);
    }
    
    public void destroyAssemblyCloneTest() {
        Group g = (Group) getFeature("F:AC.ClonedGroup");
            // performance here doesn't matter, so we just look it up
            // every time
        g.resetVisibleParts(new Feature[0]);
        assemblyCloneTestClone.destroyClonedSubgraph();
        assemblyCloneTestClone = null;
    }

    //***********************
    //  Slow Model test:
    //***********************

    public void initSlowModel() {
        if (f_smText == null) {
            f_smText = (Text) getFeature("F:SM.Text");
            f_smArc = (Arc) getFeature("F:SM.Arc");
            random = new Random();
        }
        smDelay = -3;
        smAngle = 360;
    }

    public void slowModelHeartbeat() {
        smAngle += 15;  // 15 degrees/frame * 24fps = 360 degrees/second
        if (smAngle > 360) {
            smAngle -= 360;
            if (smDelay % 5 == 0) {
                smDelay += 2;
            } else {
                smDelay += 3;
            }
            String msg = "Model delay:  " + smDelay + " ms";
            f_smText.setText(new String[] { msg });
        }
        f_smArc.setArcAngle(smAngle);
        if (smDelay > 0) {
            int ms = smDelay/4 + random.nextInt(smDelay + smDelay/2);
                    // That averages out to smDelay, but puts some
                    // variability in it, to better simulate real
                    // code
            long tm = System.currentTimeMillis() + ms;
            while (System.currentTimeMillis() < tm) {
            }
        }
    }

    public void runGotoAssemblyCloneTest(Show caller) {
        Command c = getNamedCommand("C:GotoAssemblyCloneTest");
            // In a real program, we'd probably do the lookup once in an
            // initialize method, and store it in an instance variable.
        getShow().runCommand(c);
            // We do queue the command, but since we're executing within
            // a command, it'll get run this cycle.  It would also be OK
            // to call c.execute(caller).
    }
    
    public void toggleImagePosition() {
        InterpolatedModel model = (InterpolatedModel) 
                getShow().getFeature("F:OffScreenImagePosition");
        int xPos = model.getField(Translator.X_FIELD);
        if (xPos == Translator.OFFSCREEN) {
            model.setField(Translator.X_FIELD, 0);
        } else {
            model.setField(Translator.X_FIELD, Translator.OFFSCREEN);
        }
    }
    
    public void toggleShowTopClip() {
        Debug.println("Toggling show's clip region");
        Assembly assembly = (Assembly)
                getShow().getFeature("F:MyShow");
        Feature[] features = assembly.getParts();
        if (assembly.getCurrentPart() == features[0]) {
            assembly.setCurrentFeature(features[1]);
        } else {
            assembly.setCurrentFeature(features[0]);
        }
    }


    //###################################################################
    //#     IMAGE CLONE TEST (Issue 143 regression test)                #
    //###################################################################

    FixedImage imageClone;

    public void cloneImageTestSetup() {
        FixedImage fi = (FixedImage) getFeature("F:IC.Image");
        imageClone = (FixedImage) fi.cloneSubgraph(new HashMap());
        Group g = (Group) getFeature("F:IC.Group");
        g.resetVisibleParts(new Feature[] { imageClone });
    }

    public void cloneImageTestDestroy() {
        Group g = (Group) getFeature("F:IC.Group");
        g.resetVisibleParts(null);
        imageClone.destroyClonedSubgraph();
    }
    

}
