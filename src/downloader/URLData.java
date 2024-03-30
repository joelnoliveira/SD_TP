package downloader;

import java.io.Serializable;
import java.util.List;

public class URLData implements Serializable{

    public int messageId;
    public String pageURL, title, textCitation;
    public List<String> words, links;

    public URLData(int messageId, String pageURL, String title, String textCitation, List<String> words, List<String> links) {
        this.messageId = messageId;
        this.pageURL = pageURL;
        this.title = title;
        this.textCitation = textCitation;
        this.words = words;
        this.links = links;
    }
}