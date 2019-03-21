
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.features.Group;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.util.AssetFinder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

public class ScrollList {

    public static final int MAX_NUM_OF_FILMS = 50;

    private InterpolatedModel scroller;
    private Assembly filmScrollingAssembly;
    private Feature  scrollUp;
    private Feature  scrollDown;
    private Feature  notScrolling;
    private Assembly upArrowImageAssembly;
    private Feature  upDisabled;
    private Feature  upShown;
    private Feature  upSelected;
    private Assembly downArrowImageAssembly;
    private Feature  downDisabled;
    private Feature  downShown;
    private Feature  downSelected;

    private FilmDescription originalFilmDescription; // the orignal copy to
                                                     // clone.
    private FilmDescription[] filmDescriptions; // FilmDescrition[0] would be
                                                // the original.
    private Group filmDescriptionsGroup; // The group which contains all the
                                         // FilmDescriptions
    private int currentYForFilms;
    private int maximumYForFilms;
    private final static int SCROLL_CONSTANT = 175;

    private Director d;

    public ScrollList(Director director) {

        this.d = director;

        scroller = (InterpolatedModel) d.getFeature("F:FilmDetailsScrollCoords");
        filmScrollingAssembly = (Assembly) d.getFeature("F:FilmNamesScrollerAssembly");
        scrollUp = filmScrollingAssembly.findPart("up");
        scrollDown = filmScrollingAssembly.findPart("down");
        notScrolling = filmScrollingAssembly.findPart("default");
        
        upArrowImageAssembly = (Assembly) d.getFeature("F:ScrollList.UpArrow");
        upDisabled = upArrowImageAssembly.findPart("disabled");
        upShown    = upArrowImageAssembly.findPart("default");
        upSelected = upArrowImageAssembly.findPart("selected");        
        downArrowImageAssembly = (Assembly) d.getFeature("F:ScrollList.DownArrow");
        downDisabled = downArrowImageAssembly.findPart("disabled");
        downShown    = downArrowImageAssembly.findPart("default");
        downSelected = downArrowImageAssembly.findPart("selected");

        // Cloning FilmDiscription items.  
        originalFilmDescription = new FilmDescription();
        originalFilmDescription.top = (Feature) d.getFeature("F:FilmDescription");
        originalFilmDescription.pos = (InterpolatedModel) d.getFeature("F:FilmDescription.Pos");
        originalFilmDescription.bullet = (FixedImage) d.getFeature("F:ScrollList.Bullet");
        originalFilmDescription.description = (Text) d.getFeature("F:ScrollList.FilmName");

        filmDescriptionsGroup = (Group) d.getFeature("F:FilmDetailsGroup");

        cloneFilmDescriptions(MAX_NUM_OF_FILMS);
    }

    /**
     * Clone and populate the film information elements. The entry of each
     * element will be filled at the later time dynamically.
     */
    private void cloneFilmDescriptions(int size) {
        filmDescriptions = new FilmDescription[size];
        filmDescriptions[0] = originalFilmDescription;
        for (int i = 1; i < filmDescriptions.length; i++) {
            HashMap clones = new HashMap();
            FilmDescription film = new FilmDescription();
            film.top = originalFilmDescription.top.cloneSubgraph(clones);
            // We clone the filmDescription subgraph, then for all the nodes
            // within that cloned subgraph, we look up the named features
            // using the original named feature as key.
            film.bullet = (FixedImage) clones
                    .get(originalFilmDescription.bullet);
            film.description = (Text) clones
                    .get(originalFilmDescription.description);
            film.pos = (InterpolatedModel) clones
                    .get(originalFilmDescription.pos);
            filmDescriptions[i] = film;
        }
    }

    public void populateListItems(String[] films) {

        Feature[] groupMembers = new Feature[films.length];
        
        int yPos = 0;
        String filmName;
        for (int i = 0; i < films.length; i++) {
            filmDescriptions[i].pos.setField(Translator.Y_FIELD, yPos);
            filmName = films[i].trim();
            if (filmName.length() < 32) { // TODO: Better to use FontMetrics
                filmDescriptions[i].description.setText(new String[]{filmName});
                yPos += 35;
            } else {
                // Need to split the description into multiple lines
                int beginIndex = 0;
                Vector vector = new Vector(4);
                while (filmName.length() > 32) {
                    int endIndex = filmName.lastIndexOf(" ", 32);
                    vector.add(filmName.substring(beginIndex, endIndex));
                    beginIndex = endIndex + 1;
                    filmName = filmName.substring(beginIndex).trim();
                }
                if (filmName.length() > 0) {  // add the last string, unless it's empty
                   vector.add(filmName);
                }
                String[] filmNameList = (String[]) vector.toArray(new String[]{});
                filmDescriptions[i].description.setText(filmNameList);
                yPos += (35 * filmNameList.length);  
            }
            groupMembers[i] = filmDescriptions[i].top;
        }
        maximumYForFilms = yPos;

        filmDescriptionsGroup.resetVisibleParts(groupMembers);
        
        Feature upArrow = canScrollUp() ? this.upShown : this.upDisabled ;
        this.upArrowImageAssembly.setCurrentFeature(upArrow);
        Feature downArrow = canScrollDown() ? this.downShown : this.downDisabled ;
        this.downArrowImageAssembly.setCurrentFeature(downArrow);
        restoreScrolling();
    }

    /**
     * Invoked by GRIN when the down arrow key is pressed for the film listing.
     * Check what we're displaying now and the number of available film names
     * for display, and if there is more film to display, then initiate
     * scrolling.
     */
    public void startScrollDown() {

        if (this.filmScrollingAssembly.getCurrentPart() != notScrolling) {
            // In the middle of the scrolling - do nothing
            return;
        }
            
        if (canScrollDown()) {
            this.filmScrollingAssembly.setCurrentFeature(scrollDown);
            this.downArrowImageAssembly.setCurrentFeature(downSelected);
            currentYForFilms -= SCROLL_CONSTANT;
        }
    }

    /**
     * Invoked by GRIN when the up arrow key is pressed for the film listing.
     * Check what we're displaying now and the number of available film names
     * for display, and if there is more film to display, then initiate
     * scrolling.
     */
    public void startScrollUp() {
      
        if (this.filmScrollingAssembly.getCurrentPart() != notScrolling) {
            // In the middle of the scrolling - do nothing
            return;
        }
           
        if (canScrollUp()) {
            this.filmScrollingAssembly.setCurrentFeature(scrollUp);
            this.upArrowImageAssembly.setCurrentFeature(upSelected);      ;
            currentYForFilms += SCROLL_CONSTANT;
        }
    }

    /**
     * Invoked by GRIN to scroll up the text area containing list of film names
     * that a selected Contributor has taken a part of.
     */
    public void finishScrollUp() {
        scroller.setField(Translator.Y_FIELD, currentYForFilms);  
        
        if (!canScrollUp()) {
           this.upArrowImageAssembly.setCurrentFeature(upDisabled);       
        }
        this.downArrowImageAssembly.setCurrentFeature(this.downShown); 
    }

    /**
     * Invoked by GRIN to scroll down the text area containing list of film
     * names that a selected Contributor has taken a part of.
     */
    public void finishScrollDown() {
        scroller.setField(Translator.Y_FIELD, currentYForFilms);  
  
        if (!canScrollDown()) {
           this.downArrowImageAssembly.setCurrentFeature(downDisabled);       
        }
        this.upArrowImageAssembly.setCurrentFeature(this.upShown); 
    }

    public void restoreScrolling() {
        currentYForFilms = 0;
        scroller.setField(Translator.Y_FIELD, currentYForFilms); 
    }
    
    public void destroy() {
        
        if (filmDescriptionsGroup != null) {
            filmDescriptionsGroup.resetVisibleParts(null);
        }
        // filmDescriptions[0] wasn't cloned by us, so it shouldn't be destroyed by us
        if (filmDescriptions != null) {
            for (int i = 1; i < filmDescriptions.length; i++) {
                if (filmDescriptions[i].top != null) {
                    filmDescriptions[i].top.destroyClonedSubgraph();
                }
            }
        }
    }
    
    private boolean canScrollUp() {
        // Note that the currentYForFilms is used for translation, so
        // the values only grow negative.  The number increases
        // when scrolling up, and decreases when scrolling down.
        return (currentYForFilms < 0);
    }
    
    private boolean canScrollDown() {
        // Note that the currentYForFilms is used for translation, so
        // the values only grow negative.  The number increases
        // when scrolling up, and decreases when scrolling down.
        return (-currentYForFilms+SCROLL_CONSTANT < maximumYForFilms);
    }

    synchronized public void println(String message) {
        System.out.println(message);
    }

    private static class FilmDescription {
        Feature top;
        FixedImage bullet;
        Text description;
        InterpolatedModel pos;
    }
}
