package downloader;

import java.io.Serializable;

public class URLData implements Serializable{

    public int downloaderId, messageId;
    public String pageURL = "", title = "", textCitation = "", tokens = "", links = "";

    public URLData() {

    }

    @Override
    public String toString() {
        // return one each variable per line

        return "type0\n" + downloaderId + "\n" + messageId + "\n" + 
         pageURL + "\n" + title + "\n" + textCitation + "\n" + tokens + "\n" + links;
    }
}