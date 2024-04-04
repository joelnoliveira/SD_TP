package downloader;

import java.io.Serializable;

/**
 * Represents data associated with a URL.
 */
public class URLData implements Serializable {

    public int downloaderId;
    public int messageId;
    public String pageURL;
    public String title;
    public String textCitation;
    public String tokens;
    public String links;

    /**
     * Default constructor for URLData.
     */
    public URLData() {

    }

    /**
     * Returns a string representation of the URLData object.
     *
     * @return A string representation of the URLData object.
     */
    @Override
    public String toString() {
        // Return each variable on a separate line
        return "type0\n" +
                downloaderId + "\n" +
                messageId + "\n" +
                pageURL + "\n" +
                title + "\n" +
                textCitation + "\n" +
                tokens + "\n" +
                links;
    }
}
