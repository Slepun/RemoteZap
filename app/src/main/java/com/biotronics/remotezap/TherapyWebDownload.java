package com.biotronics.remotezap;

import android.os.AsyncTask;
import android.view.View;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static com.biotronics.remotezap.WebParser.HTML_MODE;
import static com.biotronics.remotezap.WebParser.TEXT_MODE;

//LIBRARIES excluded in gradle
//Parsing data from website, results in weird behavior while sending therapy to device
//Probably data contains some unpredicted signs from HTML.
//Web download can be done while data format will be easier to parse
public class TherapyWebDownload extends AsyncTask<Void, Void, Void> {

    private final String extensionUrl = "?page=";
    private final String thpDivNodeUrlsQr = "div.node__links";
    private final String thpUrlsQr = "a[href]";
    private final String thpDivContentQr = "div.node__container";
    private Integer numOfPages;
    String mbaseUrl, lastPageTitle, clnScript, clnDevice, crntDevTherapyQr;//DUMMY
//    lastPageTitle = getResources().getString(R.string.lastPageTitle);
//    mbaseUrl = getResources().getString(R.string.webSiteUrl);
//    clnDevice = getResources().getString(R.string.cleanDevice);
//    clnScript = getResources().getString(R.string.cleanScript);

    TherapyManagementActivity thpMgmActivity;

    public TherapyWebDownload(TherapyManagementActivity therapyManagementActivity) {
        thpMgmActivity = therapyManagementActivity;
    }

    @Override
    protected void onPreExecute() {
        thpMgmActivity.progressBar.setVisibility(View.VISIBLE);
        numOfPages = new Integer(0);
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        String baseUrl = mbaseUrl;
        WebParser webParser = new WebParser();
        Document doc = webParser.connectToWebsite(baseUrl);
        numOfPages = webParser.getLastPageNumber(doc, lastPageTitle);

        Integer counter = 0;
        do{
            //TODO handle null returns
            Elements siteTherapyUrlsHtml = webParser.getWebsiteLinks(doc, thpDivNodeUrlsQr);
            siteTherapyUrlsHtml = siteTherapyUrlsHtml.select(thpUrlsQr);
            for( Element therapyUrl : siteTherapyUrlsHtml)//Walk through all therpies on this site
            {
                String visitUrl = webParser.getElementAttr(therapyUrl, "href");//Get URL
                doc = webParser.connectToWebsite(visitUrl);
                Element therapyDiv = doc.select(thpDivContentQr).first();//Get website body
                Therapy singleThp = getTherapyData(therapyDiv, webParser);
                cleanUpThpData(singleThp, webParser);
                thpMgmActivity.therapies.add(singleThp);
            }
            // Escape early if cancel() is called
            if (isCancelled()) break;

            counter++;
            doc = webParser.connectToWebsite(baseUrl+ extensionUrl + counter.toString());
        }while(counter<=numOfPages);
        return null;
    }

//    protected void onProgressUpdate(Integer... progress) {
//        //setProgressPercent(progress[0]);
//    }

    @Override
    protected void onPostExecute(Void result) {
        thpMgmActivity.progressBar.setVisibility(View.GONE);
        updateThpFile();
        thpMgmActivity.recycle_adapter.notifyDataSetChanged();
    }

    private Therapy getTherapyData(Element siteBody, WebParser parser)
    {
        String title, author, date, device, descripition, script;
        title = parser.getElementValue(siteBody, "h1[class=node__title]", TEXT_MODE);
        author = parser.getElementValue(siteBody, "span[class=node__author]", TEXT_MODE);
        date = parser.getElementValue(siteBody, "span[class=node__pubdate]", TEXT_MODE);
        device = parser.getElementValue(siteBody, "div[class*=field field-node--field-urzadzenie]", TEXT_MODE);
        descripition = parser.getElementValue(siteBody, "div[class*=ield-name-body field-type-text-with-summary]", TEXT_MODE);
        script  = parser.getElementValue(siteBody, "div[class*=field-name-field-skrypt]", HTML_MODE);
        script = parser.removeHtmlTag(script);
        String scriptArray[] = script.split("\\s*\n\\s*");//One command per row
        scriptArray[0].replaceFirst(clnScript, "");
        boolean isCurrent = isCurrentThp(script);
        Therapy thpObj = new Therapy(title, author, date, device, descripition, scriptArray, isCurrent);
        return thpObj;
    }

    private boolean cleanUpThpData(Therapy thpObj, WebParser parser)
    {
        boolean fRetVal = false;
        String dev = thpObj.getDevice();
        //String script = thpObj.getScript();
        if (!dev.isEmpty())
        {
            dev = dev.replaceFirst(clnDevice, "");
            thpObj.setDevice(dev);
            fRetVal = true;
        }
        return fRetVal;
    }

    private String br2nl(String html)
    {
        if (!html.isEmpty())
        {
            html = html.replaceAll("<br>|<p>|\\n", "\n");
        }
        return html;
    }

    private String removeRedundantNl(String text)
    {
        if (!text.isEmpty())
        {
            text = text.replaceAll("\\n\\n\\s\\n\\s", "");
            text = text.replaceAll("\\n\\n\\s\\n|\\n\\s*?\\n+?", "\n");
        }
        return text;
    }

    private boolean isCurrentThp(String crntScript)
    {
        return crntScript.matches(crntDevTherapyQr);
    }

    private boolean updateThpFile()
    {
        boolean fRetVal = false;
        //TODO compare if there are any new therapies, if yes, make update
        fRetVal = thpMgmActivity.fio.saveThpObjsToFile(thpMgmActivity.getApplicationContext(),
                thpMgmActivity.THP_DB_NEW,
                thpMgmActivity.therapies);
        fRetVal &= thpMgmActivity.fio.saveThpObjsToFile(thpMgmActivity.getApplicationContext(),
                thpMgmActivity.THP_DB,//TODO THB_DB is now JSON file which will be overwriten
                thpMgmActivity.therapies);
        return fRetVal;
    }
}
