import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.StringTokenizer;

public class Downloader {
    public static void main(String args[]) {
        // Todo: 1 fetch the url from url queue

        // todo: 2 

        // String url = args[0];
        String url = "https://en.wikipedia.org/wiki/Main_Page";
        try {
            int countTokens = 0;
            Document doc = Jsoup.connect(url).get();
            StringTokenizer tokens = new StringTokenizer(doc.text());

            while (tokens.hasMoreElements() && countTokens++ < 100){

                System.out.println(tokens.nextToken().toLowerCase());
            }
            
            Elements links = doc.select("a[href]");
            for (Element link : links)
                System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}