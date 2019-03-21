package com.sugree.twitter;

public class TwitterException extends Exception {
        public TwitterException(Exception e) {
                super(e.toString());
        }

        public TwitterException(String text) {
                super(text);
        }
}
