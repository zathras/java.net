
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.IOException;

public class Oval extends Feature implements Node {
    
    int x;
    int y;
    int w;
    int h;
    Color color;
    boolean isActivated;
    private DrawRecord drawRecord = new DrawRecord();
    
    /** Creates a new instance of Oval */
    public Oval(Show show) {
        super(show);
    }

    /**
     * {@inheritDoc}
     **/
    protected void setActivateMode(boolean mode) {
        //
        // This is synchronized to only occur within model updates.
        //
        isActivated = mode;
    }

    /**
     * {@inheritDoc}
     **/
    protected int setSetupMode(boolean mode) {
        return 0;
    }

    /**
     * {@inheritDoc}
     **/
    public void doSomeSetup() {
    }

    /**
     * {@inheritDoc}
     **/
    public boolean needsMoreSetup() {
        return false;
    }

    /**
     * {@inheritDoc}
     **/
    public void nextFrame() {
    }

    /**
     * {@inheritDoc}
     **/
    public void markDisplayAreasChanged() {
        drawRecord.setChanged();
    }

    /**
     * {@inheritDoc}
     **/
    public void addDisplayAreas(RenderContext context) {
        drawRecord.setArea(x, y, w, h);
        drawRecord.setSemiTransparent();
        context.addArea(drawRecord);
    }

    /**
     * {@inheritDoc}
     **/
    public void paintFrame(Graphics2D gr) {
        if (!isActivated) {
            return;
        }
        gr.setColor(color);
        gr.fillOval(x, y, w, h);
    }
    
    public void destroy() {}
    
    public int getX() { return x; }
    
    public int getY() { return y; }
    
    public int getWidth() { return w; }
    
    public int getHeight() { return h; }
    
    public Color getColor() { return color; }
    
    public void initialize() {}

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {
        in.readSuperClassData(this);
        this.x = in.readInt();
        this.y = in.readInt();
        this.w = in.readInt();
        this.h = in.readInt();
        this.color = in.readColor();
    }
    
}
