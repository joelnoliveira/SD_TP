package downloader;

import java.io.Serializable;

public class URLData implements Serializable{

    public int messageId;
    public String pageURL, title, textCitation, tokens, links;

    public URLData() {

    }
}