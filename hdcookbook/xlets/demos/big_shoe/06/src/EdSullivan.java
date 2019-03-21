
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.util.Debug;

import java.util.Random;

/**
 * A director class for a minimal game.  This just manages moving
 * an image around, via a GRIN translator.
 **/

public class EdSullivan extends Director {

    public int score = 0;

    private int shoeX = 1000;
    private int shoeY = 900;
    private int shoeDX = 0;
    private int bushX = -1;     // -1 means "not showing"
    private Random rand = new Random();

    private InterpolatedModel shoeMover;
    private InterpolatedModel spinningShoeMover;
    private InterpolatedModel bushMover;
    private InterpolatedModel bushHitMover;

    public EdSullivan() {
    }

    public void initialize() {
        shoeMover = (InterpolatedModel) getFeature("F:Shoe.Mover");
        spinningShoeMover 
            = (InterpolatedModel) getFeature("F:SpinningShoe.Mover");
        bushMover = (InterpolatedModel) getFeature("F:Bush.Mover");
        bushHitMover = (InterpolatedModel) getFeature("F:BushHit.Mover");
    }

    public void heartbeat() {
        shoeX += shoeDX;
        if (shoeX < 100) {
            shoeX = 100;
        } else if (shoeX > 1820) {
            shoeX = 1820;
        }
        shoeMover.setField(Translator.X_FIELD, shoeX);
        shoeMover.setField(Translator.Y_FIELD, shoeY);
    }

    public void shoeLeft() {
        shoeDX = -30;
    }

    public void shoeRight() {
        shoeDX = 30;
    }

    public void shoeStop() {
        shoeDX = 0;
    }

    public void startFiringSequence() {
        spinningShoeMover.setField(Translator.X_FIELD, shoeX);
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
}
