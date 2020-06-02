package com.biotronics.remotezap;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.IOException;

//LIBRARIES excluded in gradle
//Parsing data from website, results in weird behavior while sending therapy to device
//Probably data contains some unpredicted signs from HTML.
//Web download can be done while data format will be easier to parse
public class WebParser {

    public static int TEXT_MODE = 1;
    public static int HTML_MODE = 2;
    public WebParser() {
    }

    public Document connectToWebsite(String webSiteUrl) {
        Document html = null;
        try {
            html = Jsoup.connect(webSiteUrl).get();
        } catch (IOException e) {
            Log.e("Web", "Unable to connect");
        }
        finally {
            return html;
        }
    }

    /*Method to find last node value in a href.                  *
     * i.e "a href?=page=2". will return 2                       *
     * Input query should exactly hit Last link title description*/
    public int getLastPageNumber(Document doc, String query)
    {
        int numOfPages = 0;
        Elements lastPageLinks = doc.select(query);

        Element lastPageLink = lastPageLinks.get(0);//Only one result expected
        String lastpageStr = new String();
        lastpageStr = lastPageLink.attr("href");//assign "a href" value
        char last = lastpageStr.charAt(lastpageStr.length() - 1);
        numOfPages = Character.getNumericValue(last);

        return numOfPages;
    }

    public Elements getWebsiteLinks(Document doc, String query)
    {
        Elements returnElements = null;
        returnElements = doc.select( query );
        return returnElements;
    }

    public String getElementAttr(Element htmlUrl, String attrName)
    {
        String urlString = "";
        urlString = htmlUrl.attr(attrName);

        return urlString;
    }

    //mode 1 - normal text, mode 2 - html
    public String getElementValue(Element content, String attrName, int mode)
    {
        String result = "";
        if (mode == TEXT_MODE) {
            result = content.select(attrName).text();

        }
        if (mode == HTML_MODE) {
            result = content.select(attrName).html();//TODO split html
        }

        return result;
    }

    public String removeHtmlTag (String html)
    {
        return Jsoup.clean(html, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
    }
}
