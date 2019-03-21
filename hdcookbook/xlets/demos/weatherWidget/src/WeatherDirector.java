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
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.commands.ActivatePartCommand;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.features.Group;
import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.util.ImageManager;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.NetworkManager;

import java.awt.Component;
import java.awt.FontMetrics;
import java.lang.Math;
import java.net.URL;

/**
 * A director class that populates the UI with the latest weather
 * data. 
 * The WeatherMan class gets the latest weather information from
 * the web. This class updates the UI and takes care of the
 * interactivity. For eg: when a new zip code is entered, it places
 * a new WeatherMan request for the NetworkManager to get the weather
 * feed for the respective city. 
 *
 */

public class WeatherDirector extends Director {

    private NetworkManager manager;
    private WeatherMan pendingCommand;
    private Component component;
    private ManagedImage weatherImages[] = new ManagedImage[3];
    private ManagedImage blankWeatherImage;
    private FixedImage weatherIcon;
    private FeedData weatherUpdate;
    private Text location;
    private Text condition;
    private Text day;
    private Text temp;
    private Text zipCode;
    private Assembly windSelector;
    private Group displayWind;
    private Feature noWind;
    private String[] emptyStringArray = new String[0];
    private int nextDay = 0;
    private StringBuffer zipBuf;

    public WeatherDirector() {
    }

    public void initialize() {
        weatherIcon = (FixedImage) getFeature("F:WeatherImage");
        weatherImages[0] = weatherIcon.getImage();
        blankWeatherImage = ImageManager.getImage("images/blank_weather.png");  
        blankWeatherImage.prepare();
        blankWeatherImage.load(getShow().component);
        location = (Text) getFeature("F:Location");
        condition = (Text) getFeature("F:Condition");
        day = (Text) getFeature("F:Day");
        temp = (Text) getFeature("F:Temp");
        windSelector = (Assembly) getFeature("F:WindSelector");
        Feature[] features = windSelector.getParts();
        displayWind = (Group) features[0];
        noWind = features[1];
        zipCode = (Text) getFeature("F:ZipCode");
        String[] zip = zipCode.getText();
        zipBuf = new StringBuffer(zip[0]);
        NetworkManager.start();
    }

    public void pollWeather() {
        NetworkManager.enqueue(new WeatherMan(this));
    }

    public void notifyDestroyed() {
        super.notifyDestroyed();
        for (int i = 0; i < weatherImages.length; i++) {
            if (weatherImages[i] != null) {
                weatherImages[i].unprepare();
                ImageManager.ungetImage(weatherImages[i]);
            }
        }
        if (blankWeatherImage != null) {
            blankWeatherImage.unprepare();
            ImageManager.ungetImage(blankWeatherImage);
        }
        NetworkManager.shutdown();
        WeatherMan man = null;
        synchronized (this) {
            man = pendingCommand;
        }
        if (man != null) {
            man.destroy();
        }
        if (weatherIcon != null) {
            ManagedImage image = weatherIcon.getImage();
            image.unprepare();
            ImageManager.ungetImage(image);
        }
    }

    void updateScreen(ManagedImage newImages[], FeedData fd) {
        for (int i = 0; i < weatherImages.length; i++) {
            if (weatherImages[i] != null) {
                weatherImages[i].unprepare();
                ImageManager.ungetImage(weatherImages[i]);
            }
        }
        weatherImages = newImages;
        weatherUpdate = fd;
        updateStatus();
    }

    void updateStatus() {
        Debug.println("Updating the status");

        if ((weatherImages == null) ||
                (weatherUpdate == null)) {
            weatherIcon.replaceImage(blankWeatherImage);
            location.setText(emptyStringArray);
            day.setText(emptyStringArray);
            condition.setText(emptyStringArray);
            temp.setText(emptyStringArray);
            getShow().runCommand(new ActivatePartCommand(
                    getShow(), windSelector, noWind));
            return;
        }

        location.setText(new String[]{weatherUpdate.location});
        ManagedImage downloadedImage = weatherImages[nextDay];
        if (downloadedImage == null) {
            weatherIcon.replaceImage(blankWeatherImage);
        } else {
            weatherIcon.replaceImage(downloadedImage);
        }
        if (nextDay == 0) { // present day
            day.setText(new String[]{weatherUpdate.day});
            condition.setText(new String[]{weatherUpdate.condition});
            temp.setText(new String[]{weatherUpdate.temp});

            // Wind Display
            int angle = 0, startAngle = 0;
            try {
                angle = Integer.parseInt(weatherUpdate.direction);
            } catch (NumberFormatException e) {
                Debug.printStackTrace(e);
            }
            int displayAngle = 15;
            if (angle < displayAngle) {
                startAngle = 360 - (displayAngle - angle);
            } else {
                startAngle = angle - displayAngle;
            }
            angle = displayAngle;

            // update the wind UI
            Feature[] windGroupParts = displayWind.getParts();
            ((Arc) windGroupParts[2]).setStartAngle(startAngle);
            ((Arc) windGroupParts[2]).setArcAngle(angle);
            ((Text) windGroupParts[3]).setText(new String[]{weatherUpdate.speed});
            getShow().runCommand(new ActivatePartCommand(getShow(),
                    windSelector, displayWind));
        } else {
            // erase wind UI
            getShow().runCommand(new ActivatePartCommand(
                    getShow(), windSelector, noWind));
            if (nextDay == 1) {
                day.setText(new String[]{weatherUpdate.day1});
                condition.setText(new String[]{
                            weatherUpdate.day1Condition});
                temp.setText(new String[]{
                            (weatherUpdate.day1High + " | " +
                            weatherUpdate.day1Low)});
            } else if (nextDay == 2) {
                day.setText(new String[]{weatherUpdate.day2});
                condition.setText(new String[]{
                            weatherUpdate.day2Condition});
                temp.setText(new String[]{
                            (weatherUpdate.day2High + " | " +
                            weatherUpdate.day2Low)});
            }
        }
    }

    void dayNext() {
        nextDay = (nextDay + 1) % 3;
        updateStatus();
    }

    void zipEntered(char key) {
        if (zipBuf.length() < 5) {  // accepts only zip length of 5
            zipBuf.append(key);
            zipCode.setText(new String[] { zipBuf.toString()}); 
        }
    }

    void zipErased() {
        if (zipBuf.length() > 0) {
            zipBuf = zipBuf.deleteCharAt((zipBuf.length() - 1));
            zipCode.setText(new String[] { zipBuf.toString()}); 
        }
    }

    void zipDone() {
        WeatherMan.setZip(zipBuf.toString());
        //zipBuf.setLength(0); // editing is over;clear the zip code in the UI
        //zipCode.setText(new String[] { zipBuf.toString()});   
        pollWeather();
    }
        
        
    void setPendingCommand(WeatherMan man) {
        synchronized (this) {
            if (pendingCommand == null) {
                pendingCommand = man;
                man = null;
            }
        }
        if (man != null) {
            // This should never happen in practice, but there's a theoretical
            // possibility of this as a race condition.
            man.destroy();
        }
    }

    void unsetPendingCommand(WeatherMan man) {
        synchronized (this) {
            if (pendingCommand == man) {
                pendingCommand = null;
            }
        }
    }


}
