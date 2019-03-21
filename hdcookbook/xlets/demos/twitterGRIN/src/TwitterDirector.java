/*  
 * Copyright (c) 2009, Sun Microsystems, Inc.
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


import com.hdcookbook.grin.Director;

import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.util.ImageManager;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.NetworkManager;
import com.substanceofcode.twitter.model.Status;
import com.substanceofcode.twitter.TwitterApi;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

/**
 * This is the "director" of the Twitter app widget.  It manages the UI,
 * and performs its work inside the animation thread.
 **/
public class TwitterDirector extends Director {

    private static class TweetView {
        FixedImage      icon;
        Text            screenName;
        Text            createdTime;
        Text            tweet;
    }

    private ManagedImage blankProfilePicture;
    private ManagedImage defaultProfilePicture;
    private Text footer;
    private TweetView[] tweets;
    private FontMetrics fontMetrics;

    private ManagedImage[] tweetIcon = new ManagedImage[0];
    private Status[] tweetStatus = new Status[0];

    private TwitterPoll pendingCommand;
    private SimpleDateFormat dateFormat=new SimpleDateFormat("hh:mm a MMM dd");

    private int page = 1;
    private String[] emptyStringArray = new String[0];

    public  InterpolatedModel windowMover;

    TwitterApi twitter = new TwitterApi(null);  // Used by TwitterPoll
    Component component;

    public TwitterDirector() {
    }

    public void initialize() {
        {
            FixedImage fi = (FixedImage) getFeature("F:ProfileImage.Blank");
            blankProfilePicture = fi.getImage();
            ImageManager.getImage(blankProfilePicture);
            blankProfilePicture.prepare();
            fi = (FixedImage) getFeature("F:ProfileImage.Default");
            defaultProfilePicture = fi.getImage();
            ImageManager.getImage(defaultProfilePicture);
            defaultProfilePicture.prepare();
        windowMover = (InterpolatedModel) getFeature("F:Window.Mover");
        }
        tweets = new TweetView[4];
        for (int i = 0; i < tweets.length; i++) {
            TweetView v = new TweetView();
            tweets[i] = v;
            v.icon = (FixedImage) getFeature("F:ProfileImage." + (i+1));
            v.screenName = (Text) getFeature("F:ScreenName." + (i+1));
            v.createdTime = (Text) getFeature("F:CreatedTime." + (i+1));
            v.tweet = (Text) getFeature("F:Tweet." + (i+1));

            v.screenName.setText(emptyStringArray);
            v.createdTime.setText(emptyStringArray);
            v.tweet.setText(emptyStringArray);
        }

        footer = (Text) getFeature("F:Footer");
        fontMetrics = getShow().component.getFontMetrics(tweets[0].tweet.getFont());
        NetworkManager.start();
    }

    public void initializeProfileImages() {
        for (int i = 0; i < tweets.length; i++) {
            tweets[i].icon.replaceImage(blankProfilePicture);
        }
    }

    public void notifyDestroyed() {
        super.notifyDestroyed();
        if (blankProfilePicture != null) {
            blankProfilePicture.unprepare();
            ImageManager.ungetImage(blankProfilePicture);
        }
        if (defaultProfilePicture != null) {
            defaultProfilePicture.unprepare();
            ImageManager.ungetImage(defaultProfilePicture);
        }
        NetworkManager.shutdown();
        TwitterPoll poll = null;
        synchronized(this) {
            poll = pendingCommand;
        }
        if (poll != null) {
            poll.destroy();
        }
        if (tweetIcon != null) {
            for (int i = 0; i < tweetIcon.length; i++) {
                if (tweetIcon[i] != null) {
                    tweetIcon[i].unprepare();
                    ImageManager.ungetImage(tweetIcon[i]);
                }
            }
        }
    }

    public void pollTwitter() {
        NetworkManager.enqueue(new TwitterPoll(this));
    }

    public void pageNext() {
        page++;
        normalizePage();
        copyDataToUI();
    }

    public void pageBack() {
        page--;
        normalizePage();
        copyDataToUI();
    }

    private void normalizePage() {
        int max = 1 + (tweetStatus.length - 1) / tweets.length;
        if (page > max) {
            page = max;
        }
        if (page < 1) {
            page = 1;
        }
        footer.setText(new String[] { "Page: " + page });
    }

    //
    // Update the screen with a new set of tweets.  This happens in the
    // animation thread, and is called from TwitterPoll.  After this
    // completes, TwitterDirector takes over ownership of the
    // icons; it will eventually unprepare and unget them.
    //
    void updateScreen(Status[] messages, ManagedImage[] icons) {
        if (tweetIcon != null) {
            for (int i = 0; i < tweetIcon.length; i++) {
                if (tweetIcon[i] != null) {
                    tweetIcon[i].unprepare();
                    ImageManager.ungetImage(tweetIcon[i]);
                }
            }
        }
        tweetIcon = icons;
        tweetStatus = messages;
        normalizePage();
        copyDataToUI();
    }

    private void copyDataToUI() {
        int entry = (page - 1) * 4;
        for (int i = 0; i < tweets.length; i++) {
            TweetView tweet = tweets[i];
            if (entry >= tweetStatus.length) {
                tweet.screenName.setText(emptyStringArray);
                tweet.createdTime.setText(emptyStringArray);
                tweet.tweet.setText(emptyStringArray);
                tweet.icon.replaceImage(blankProfilePicture);
            } else {
                Status status = tweetStatus[entry];
                ManagedImage image = tweetIcon[entry];
                tweet.screenName.setText(new String[] { status.getScreenName() });
                String date = dateFormat.format(status.getDate());
                tweet.createdTime.setText(new String[] { date });
                String[] lines = new String[] { "", "", "", "" };
                StringTokenizer tok = new StringTokenizer(status.getText(), " ", true);
                int line = 0;
                int width = 0;
                while (tok.hasMoreTokens() && line < lines.length) {
                    String t = tok.nextToken();
                    int w = fontMetrics.stringWidth(t);
                    if (" ".equals(t) || width == 0 || width+w < 215) {
                        lines[line] = lines[line] + t;
                        width += w;
                    } else {
                        line++;
                        if (line < lines.length) {
                            lines[line] = t;
                            width = w;
                        }
                    }
                }
                tweet.tweet.setText(lines);
                if (image == null) {
                    tweet.icon.replaceImage(blankProfilePicture);
                } else {
                    tweet.icon.replaceImage(image);
                }
            }
            entry++;
        }
    }

    void setPendingCommand(TwitterPoll poll) {
        synchronized(this) {
            if (pendingCommand == null) {
                pendingCommand = poll;
                poll = null;
            }
        }
        if (poll != null) {
            // This should never happen in practice, but there's a theoretical
            // possibility of this as a race condition.
            poll.destroy();
        }
    }

    void unsetPendingCommand(TwitterPoll poll) {
        synchronized(this) {
            if (pendingCommand == poll) {
                pendingCommand = null;
            }
        }
    }

    public Dimension getPaneModeDimension() {
        return new Dimension(300, 140);
    }

    /**
     * Move this show to the absolute coordinate of the screen
     *
     * @param x
     * @param y
     */
    public void moveWindow(int x, int y) {
        int xPos = windowMover.getField(Translator.X_FIELD);
        int yPos = windowMover.getField(Translator.Y_FIELD);
        windowMover.setField(Translator.X_FIELD, x);
        windowMover.setField(Translator.Y_FIELD, y);
    }

}
