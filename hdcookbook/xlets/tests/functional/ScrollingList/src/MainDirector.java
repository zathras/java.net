// The main director for the ScrollableList show.
// Note that the actual scrolling logistic is encapsulated in ScrollList.java.

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

public class MainDirector extends Director {

    public ScrollList scrollList;

    // The test string array to populate the scrollable list with.
    private static String[] films = new String[] {
       "Citizen Kane",
       "Casablanca",
       "The Godfathe",
       "Gone with the Wind",
       "Lawrence of Arabia",
       "The Wizard of Oz",
       "The Graduate",
       "On the Waterfront",
       "Schindler's List",
       "Singin' in the Rain",
       "It's a Wonderful Life",
       "Sunset Blvd",
       "The Bridge on the River Kwa",
       "Some Like It Hot",
       "Star Wars Episode IV: A New Hope",
       "All About Ev",
       "The African Queen",
       "Psych",
       "Chinatown",
       "One Flew Over the Cuckoo's Nest ",
       "The Grapes of Wrath",
       "2001: A Space Odyssey",
       "The Maltese Falcon",
       "Raging Bull",
       "E.T. the Extra-Terrestrial",
       "Dr. Strangelove",
       "Bonnie and Clyde",
       "Apocalypse Now",
       "Mr. Smith Goes to Washington"
    } ;
    
    public MainDirector() {}

    /**
     * Initialize the director. If you need to access GRIN features or other
     * scene graph elements, it's a good idea to look them up once, during
     * initialization, and then keep them in an instance variable. That's faster
     * than looking them up every time.
     **/
    public void initialize() {
         scrollList = new ScrollList(this);
         scrollList.populateListItems(films);
    }

    public void notifyDestroyed() {
         scrollList.destroy();
    }
}
