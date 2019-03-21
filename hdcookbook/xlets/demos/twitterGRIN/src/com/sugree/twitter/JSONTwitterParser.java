package com.sugree.twitter;

import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import com.substanceofcode.utils.StringUtil;
import com.substanceofcode.twitter.model.Status;

import com.sugree.utils.DateUtil;
import com.sugree.twitter.TwitterException;

import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.JsonIO;

public class JSONTwitterParser {
        public static Vector parseStatuses(String payload) throws TwitterException {
                Vector statuses = new Vector();

                try {
                        Object[] json = (Object[]) JsonIO.readJSON(new StringReader(payload));
                        for (int i = 0; i < json.length; i++) {
                                HashMap status = (HashMap) json[i];
                                HashMap user = (HashMap) status.get("user");
                                String screenName = StringUtil.decodeEntities(
                                                                                (String) user.get("screen_name"));
                                long id = Long.parseLong(status.get("id").toString());
                                                // id comes from twitter as a string, not a long!
                                String text = StringUtil.decodeEntities((String) status.get("text"));
                                Date createAt 
                                        = DateUtil.parseDate((String) status.get("created_at"));
                                String source = StringUtil.removeHtml((String) status.get("source"));
                                boolean favorited = Boolean.TRUE.equals(status.get("favorited"));
                                String profileImageURL = (String) user.get("profile_image_url");
                                statuses.addElement(new Status(id, screenName, text, createAt, source, favorited, profileImageURL));
                        }
                } catch (Exception e) {
                        if (Debug.LEVEL > 0) {
                            Debug.printStackTrace(e);
                        }
                        throw new TwitterException(e);
                }
                return statuses;
        }

        public static Status parseStatus(String payload) throws TwitterException {
                Status s = null;
                try {
                        HashMap status = (HashMap) JsonIO.readJSON(new StringReader(payload));
                        HashMap user = (HashMap) status.get("user");

                        String screenName = StringUtil.decodeEntities(
                                                                                (String) user.get("screen_name"));
                        long id = Long.parseLong(status.get("id").toString());
                                        // id comes from twitter as a string, not a long!
                        String text = StringUtil.decodeEntities((String) status.get("text"));
                        Date createAt = DateUtil.parseDate((String) status.get("created_at"));
                        String source = StringUtil.removeHtml((String) status.get("source"));
                        boolean favorited = Boolean.TRUE.equals(status.get("favorited"));
                        String profileImageURL = (String) user.get("profile_image_url");
                        s = new Status(id, screenName, text, createAt, source, favorited, profileImageURL);
                } catch (Exception e) {
                        if (Debug.LEVEL > 0) {
                            Debug.printStackTrace(e);
                        }
                        throw new TwitterException(e);
                }
                return s;
        }

        public static String parse400(String payload) throws TwitterException {
                try {
                        HashMap map = (HashMap) JsonIO.readJSON(new StringReader(payload));
                        return "" + map.get("error");
                } catch (Exception e) {
                        return payload;
                }
        }
}
