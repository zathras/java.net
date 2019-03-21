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

import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.ImageManager;
import com.hdcookbook.grin.util.ManagedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;

/**
 * This class can be called as a WeatherPoll instead.
 * It's similar to TwitterPoll under ../../twitterGrin/src/TwitterPoll.java
 * This class contains a thread that fetches the yahoo weather feed
 * by sending a URL request to the yahoo webservice. It also fetches the
 * weather image for the current weather and the weather forecast ahead
 * of time. The feed data is parsed and stored. 
 * It then places itself in the command queue for UI update.
 */

public class WeatherMan extends Command implements Runnable {

    private WeatherDirector director;
    private ManagedImage icons[] = new ManagedImage[3];
    private URL weatherURL;
    private boolean destroyed = false;
    private static String zip = "94103";
    private String weatherRsc = "http://weather.yahooapis.com/forecastrss";

    // This URL points to a widget size png image.
    private String imageRsc =
            "http://l.yimg.com/us.yimg.com/i/us/nws/weather/gr/";
    private FeedData fd;

    public WeatherMan(WeatherDirector dir) {
        super(dir.getShow());
        this.director = dir;
        weatherRsc += "?p=" + zip;
        try {
            weatherURL = new URL(weatherRsc);
        } catch (MalformedURLException ignored) {
            Debug.printStackTrace(ignored);
        }
    }

    public static void setZip(String zipcode) {
        zip = zipcode;
    }

    public void run() {
        if (Debug.LEVEL > 0) {
            Debug.println("Polling server...");
        }
        int images = 0;
        try {
            HttpURLConnection conn = (HttpURLConnection) weatherURL.
                                        openConnection();
            int resp = conn.getResponseCode();
            if (resp != 200) {
                Debug.println("Got an error HTTP response code:" + resp);
            }
            this.fd = XMLParser.parse(conn.getInputStream());

            URL url;
            if (fd != null) {

               // select the image based on whether it's a day or night
                if (fd.isDayTime) {
                    url = new URL(imageRsc + fd.imageCode + "d.png");
                } else {
                    url = new URL(imageRsc + fd.imageCode + "n.png");
                }
                this.icons[images++] = ImageManager.getImage(url);

                // forecast day1's image
                url = new URL(imageRsc + fd.day1Code + "d.png");
                this.icons[images++] = ImageManager.getImage(url);

                // forecast day2's image
                url = new URL(imageRsc + fd.day2Code + "d.png");
                this.icons[images++] = ImageManager.getImage(url);
            }
        } catch (MalformedURLException ignored) {
            Debug.printStackTrace(ignored);
            icons[images] = null;
        } catch (IOException e) {
            Debug.printStackTrace(e);
            icons[images] = null;
        }
        for (int i = 0; i < 3; i++) {
            if (icons[i] != null) {
                icons[i].prepare();
                icons[i].load(director.getShow().component);
            }
        }

        director.setPendingCommand(this);
        show.runCommand(this);
    }

    /**
     * Update the screen. This happens in the animation thread.
     **/
    public void execute() {
        if (destroyed) {
            return;
        }
        director.unsetPendingCommand(this);
        if (Debug.LEVEL > 0) {
            Debug.println("Updating screen...");
        }
        director.updateScreen(icons, fd);
    }

    public void destroy() {
        destroyed = true;
        if (icons == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            icons[i].unprepare();
            ImageManager.ungetImage(icons[i]);
        }
    }
}

