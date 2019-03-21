
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.util.Debug;

/**
 * A director class for a minimal game.  This just manages moving
 * an image around, via a GRIN translator.
 **/

public class EdSullivan extends Director {

    private int shoeX = 1000;
    private int shoeY = 900;
    private int shoeDX = 0;

    private InterpolatedModel shoeMover;

    public EdSullivan() {
    }

    public void initialize() {
        shoeMover = (InterpolatedModel) getFeature("F:Shoe.Mover");
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

}
