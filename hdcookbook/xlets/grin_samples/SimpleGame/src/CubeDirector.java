
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;

/**
 * A director class for a minimal game.  This just manages moving
 * an image around, via a GRIN translator.
 **/

public class CubeDirector extends Director {

    private InterpolatedModel cubePosition;
    private int cubeX;
    private int cubeY;
    public int cubeDX = 0;
    public int cubeDY = 0;

    public CubeDirector() {
    }

    /**
     * Initialize the director.  If you need to access GRIN features
     * or other scene graph elements, it's a good idea to look them
     * up once, during initialization, and then keep them in an instance
     * variable.  That's faster than looking them up every time.
     **/
    public void initialize() {
        cubePosition = (InterpolatedModel) getFeature("F:Cube.Position");
        cubeX = cubePosition.getField(Translator.X_FIELD);
        cubeY = cubePosition.getField(Translator.Y_FIELD);
    }

    /**
     * This method gets invoked once per frame while the game is playing.
     * A GRIN timer calls it, and it executes within the animation thread.  
     * When it's called, the scene graph is guaranteed
     * to be in a safe state for modification.
     **/
    public void heartbeat() {
        cubeX += cubeDX;
        cubeY += cubeDY;
        if (cubeX < 0) {
            cubeX = 0;
            cubeDX = 0;
        } else if (cubeX > 1920) {
            cubeX = 1920;
            cubeDX = 0;
        }
        if (cubeY < 0) {
            cubeY = 0;
            cubeDY = 0;
        } else if (cubeY > 1080) {
            cubeY = 1080;
            cubeDY = 0;
        }
        cubePosition.setField(Translator.X_FIELD, cubeX);
        cubePosition.setField(Translator.Y_FIELD, cubeY);
    }
  
}
