/*
 * TwitterApi.java
 *
 * Copyright (C) 2005-2008 Tommi Laukkanen
 * http://www.substanceofcode.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.substanceofcode.twitter;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.Vector;

import com.substanceofcode.twitter.model.Status;
import com.substanceofcode.utils.HttpUtil;
import com.substanceofcode.utils.StringUtil;

import com.sugree.twitter.JSONTwitterParser;
import com.sugree.twitter.TwitterException;
import com.sugree.utils.MultiPartFormOutputStream;

import com.hdcookbook.grin.util.Debug;

/**
 * TwitterApi
 *
 * @author Tommi Laukkanen (tlaukkanen at gmail dot com)
 */
public class TwitterApi {

        private String gateway;
        private String source;
    private String username;
    private String password;

    private static final String DEFAULT_TWITPIC_GATEWAY = "http://nest.onedd.net/text/";
    private static final String PUBLIC_TIMELINE_URL = "statuses/public_timeline.json";
    private static final String FRIENDS_TIMELINE_URL = "statuses/friends_timeline.json";
    private static final String USER_TIMELINE_URL = "statuses/user_timeline.json";
    private static final String REPLIES_TIMELINE_URL = "statuses/replies.json";
    private static final String STATUS_UPDATE_URL = "statuses/update.json";
        private static final String DIRECT_MESSAGES_URL = "direct_messages.json";
        private static final String FAVORITES_URL = "favorites.json";
        private static final String FAVORITES_CREATE_URL = "favorites/create/%d.json";
        private static final String FAVORITES_DESTROY_URL = "favorites/destroy/%d.json";
        private static final String TEST_URL = "help/test.json";
        private static final String SCHEDULE_DOWNTIME_URL = "help/schedule_downtime.json";
        private static final String PICTURE_POST_URL = "twitpic/api/uploadAndPost";

    /** Creates a new instance of TwitterApi */
    public TwitterApi(String source) {
                this.source = source;
                this.gateway = "http://twitter.com/";
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAlternateAuthentication(boolean flag) {
                HttpUtil.setAlternateAuthentication(flag);
    }

    public void setOptimizeBandwidth(boolean flag) {
                HttpUtil.setOptimizeBandwidth(flag);
    }

    /**
     * Request public timeline from Twitter API.
     * @return Vector containing StatusEntry items.
     */
    public Vector requestPublicTimeline(String sinceId) throws TwitterException {
                if (sinceId == null) {
                        sinceId = "";
                }
                if (sinceId.length() > 0) {
                        sinceId = "since_id="+StringUtil.urlEncode(sinceId);
                } 
        HttpUtil.setBasicAuthentication("", "");
        return requestTimeline(gateway+PUBLIC_TIMELINE_URL, sinceId);
    }    
    
    /**
     * Request public timeline from Twitter API.
     * @return Vector containing StatusEntry items.
     */
    public Vector requestFriendsTimeline(String since) throws TwitterException {
                if (since != null && since.length() > 0) {
                        since = "since="+StringUtil.urlEncode(since);
                }
            HttpUtil.setBasicAuthentication(username, password);
        return requestTimeline(gateway+FRIENDS_TIMELINE_URL, prepareParam(since));
    }    

    /**
     * Request public timeline from Twitter API.
     * @return Vector containing StatusEntry items.
     */
    public Vector requestUserTimeline(String since) throws TwitterException {
                if (since != null && since.length() > 0) {
                        since = "since="+StringUtil.urlEncode(since);
                }
            HttpUtil.setBasicAuthentication(username, password);
        return requestTimeline(gateway+USER_TIMELINE_URL, prepareParam(since));
    }    

    /**
     * Request responses timeline from Twitter API.{
     * @return Vector containing StatusEntry items.
     */
    public Vector requestRepliesTimeline(String since) throws TwitterException {
                if (since != null && since.length() > 0) {
                        since = "since="+StringUtil.urlEncode(since);
                }
            HttpUtil.setBasicAuthentication(username, password);
        return requestTimeline(gateway+REPLIES_TIMELINE_URL, prepareParam(since));
    }  
    
    /**
     * Request favorites timeline from Twitter API.{
     * @return Vector containing StatusEntry items.
     */
    public Vector requestFavoritesTimeline() throws TwitterException {
            HttpUtil.setBasicAuthentication(username, password);
        return requestTimeline(gateway+FAVORITES_URL, prepareParam(""));
    }  

        public Status createFavorite(String id) throws TwitterException {
            HttpUtil.setBasicAuthentication(username, password);
        return requestObject(gateway+FAVORITES_CREATE_URL, id);
        }
    
        public Status destroyFavorite(String id) throws TwitterException {
            HttpUtil.setBasicAuthentication(username, password);
        return requestObject(gateway+FAVORITES_DESTROY_URL, id);
        }
    
    public Status updateStatus(String status) throws TwitterException {
                String response = "";
        try {
            String query = "status="+StringUtil.urlEncode(status);
            HttpUtil.setBasicAuthentication(username, password);
            HttpUtil.setContentType("text/plain; charset=utf-8");
            response = HttpUtil.doPost(gateway+STATUS_UPDATE_URL, prepareParam(query));
        } catch(Exception ex) {
                        if (Debug.LEVEL > 0) {
                            Debug.printStackTrace(ex);
                        }
                        throw new TwitterException("update "+ex.toString());
        }
                return null;
        //return JSONTwitterParser.parseStatus(response);
    }

        public void postPicture(String status, byte[] picture, String mimeType) throws TwitterException {
                String gateway = this.gateway;
                String fileName = "jibjib.jpg";
                if (mimeType.indexOf("jpeg") >= 0 || mimeType.indexOf("jpeg") >= 0) {
                        fileName = "jibjib.jpg";
                } else if (mimeType.indexOf("png") >= 0) {
                        fileName = "jibjib.png";
                } else if (mimeType.indexOf("gif") >= 0) {
                        fileName = "jibjib.gif";
                }
                if (gateway.equals("http://twitter.com/")) {
                        gateway = DEFAULT_TWITPIC_GATEWAY;
                }

                try {
                        status = new String(status.getBytes("UTF-8"), "ISO-8859-1");
                } catch (UnsupportedEncodingException e) {
                }

                try {
                        String response = "";
                        String boundary = MultiPartFormOutputStream.createBoundary();
                        ByteArrayOutputStream data = new ByteArrayOutputStream();
                        MultiPartFormOutputStream out = new MultiPartFormOutputStream(data, boundary);
                        out.writeFile("media", mimeType, fileName, picture);
                        out.writeField("username", username);
                        out.writeField("password", password);
                        out.writeField("message", status);

            HttpUtil.setBasicAuthentication("", "");
            HttpUtil.setContentType(MultiPartFormOutputStream.getContentType(boundary));
            response = HttpUtil.doPost(gateway+PICTURE_POST_URL, data.toByteArray());
                } catch (Exception ex) {
            HttpUtil.setContentType(null);
                        if (Debug.LEVEL > 0) {
                            Debug.printStackTrace(ex);
                        }
                        throw new TwitterException("post "+ex.toString());
                }
        HttpUtil.setContentType(null);
        }

        private Status requestObject(String url, String id) throws TwitterException {
                String response = "";
                Status status = null;
                try {
                        url = StringUtil.replace(url, "%d", id);
                HttpUtil.setBasicAuthentication(username, password);
            HttpUtil.setContentType("text/plain; charset=utf-8");
                    response = HttpUtil.doPost(url, "");
                status = JSONTwitterParser.parseStatus(response);
                } catch(Exception ex) {
                        if (Debug.LEVEL > 0) {
                            Debug.printStackTrace(ex);
                        }
                        throw new TwitterException("request "+ex);
                }
                return status;
        }
    
    private Vector requestTimeline(String timelineUrl, String param) throws TwitterException {
        Vector entries = new Vector();
                HttpUtil.setContentType("text/plain; charset=utf-8");
        try {
            String response = HttpUtil.doGet(timelineUrl, param);
                        if (response.length() > 0) {
                    entries = JSONTwitterParser.parseStatuses(response);
                        }
        } catch (IOException ex) {
                        if (Debug.LEVEL > 0) {
                            Debug.printStackTrace(ex);
                        }
                        throw new TwitterException("request "+ex);
        } catch (Exception ex) {
                        if (Debug.LEVEL > 0) {
                            Debug.printStackTrace(ex);
                        }
                        throw new TwitterException("request "+ex);
        }
        return entries;        
    }
    
    private String prepareParam(String param) {
                String newParam = "";
                if (param.length() > 0) {
                        newParam = param+"&source="+source;
                } else {
                        newParam = "source="+source;
                }
                return newParam;
        }
    
}
