package com.ds.googol.downloader;

import java.io.Serializable;

/**
 * The URLData class represents the data associated with a URL in the downloader application.
 * It implements the Serializable interface to support object serialization.
 */
public class URLData implements Serializable {

    public int downloaderId, messageId;
    public String pageURL = "", title = "", textCitation = "", tokens = "", links = "";

    /**
     * Default constructor for the URLData class.
     */
    public URLData() {

    }

    /**
     * Returns a string representation of the URLData object.
     * The string contains the values of each variable, separated by newlines.
     *
     * @return A string representation of the URLData object.
     */
    @Override
    public String toString() {
        return "type0\n" + downloaderId + "\n" + messageId + "\n" +
                pageURL + "\n" + title + "\n" + textCitation + "\n" + tokens + "\n" + links;
    }
}