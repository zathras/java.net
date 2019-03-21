
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.util.Debug;

import java.util.Random;

/**
 * A director class for a minimal game.  This just manages moving
 * an image around, via a GRIN translator.
 **/

public class EdSullivan extends Director {

    public int score = 0;

    private int handleX = 1000;
    private int shoeX = 1000;
    private int shoeY= 900;
    private int handleDX = 0;
    private int shoeDX = 0;
    private int bushX = -1;     // -1 means "not showing"
    private static int SPRING_K = 1500;  // mills, that is, 1.5
    private static int DRAG = 600;       // mills, that is, 0.6
    private Random rand = new Random();

    private InterpolatedModel shoeMover;
    private InterpolatedModel spinningShoeMover;
    private InterpolatedModel bushMover;
    private InterpolatedModel bushHitMover;
    private Text scoreText;

    public EdSullivan() {
    }

    public void initialize() {
        shoeMover = (InterpolatedModel) getFeature("F:Shoe.Mover");
        spinningShoeMover 
            = (InterpolatedModel) getFeature("F:SpinningShoe.Mover");
        bushMover = (InterpolatedModel) getFeature("F:Bush.Mover");
        bushHitMover = (InterpolatedModel) getFeature("F:BushHit.Mover");
        scoreText = (Text) getFeature("F:Score");
        reset();
    }

    public void reset() {
        score = 0;
    }

    public void heartbeat() {
        handleX += handleDX;
        if (handleX < 100) {
            handleX = 100;
        } else if (handleX > 1820) {
            handleX = 1820;
        }
        shoeDX += ((handleX - shoeX) * SPRING_K + 500) / 1000;
        shoeDX += ((handleDX - shoeDX) * DRAG + 500) / 1000;
        shoeX = shoeX + shoeDX;
        shoeMover.setField(Translator.X_FIELD, shoeX);
        shoeMover.setField(Translator.Y_FIELD, shoeY);
    }

    public void shoeLeft() {
        handleDX = -30;
    }

    public void shoeRight() {
        handleDX = 30;
    }

    public void shoeStop() {
        handleDX = 0;
    }

    public void startFiringSequence() {
        spinningShoeMover.setField(Translator.X_FIELD, shoeX);
        shoeStop();
    }

    public boolean bushAppears() {
        if (bushX != -1) {
            return false;       // He's showing, so he can't appear
        }
        if (rand.nextInt(3*24) == 42) {
            bushX = 150 + rand.nextInt(1920 - 300);
            bushMover.setField(Translator.X_FIELD, bushX);
            bushHitMover.setField(Translator.X_FIELD, bushX);
            return true;
        } else {
            return false;
        }
    }

    public void bushDisappears() {
        bushX = -1;
    }

    public boolean bushHit() {
        if (bushX == -1) {
            return false;
        }
        return Math.abs(bushX - shoeX) < 150;
    }

    public void setScore() {
        String s = "Score:  " + score;
        scoreText.setText(new String[] { s });
    }
}
