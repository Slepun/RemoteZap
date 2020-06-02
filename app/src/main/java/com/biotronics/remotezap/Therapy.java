package com.biotronics.remotezap;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

public class Therapy implements Serializable {

    public Therapy() {
        devId = new Integer[3];
        thpId = 0;
        title = new String("");
        author = new String("");
        date = new String("");
        device = new String("");
        description = new String("");
        script = new String[1];
        isCurrent = false;
        viewExpanded = false;
        json_tp = new JSONObject();
    }

    public Therapy(String title, String author, String date, String device, String description,
                   String[] script, boolean isCurrent) {
        thpId = 0;
        this.title = title;
        this.author = author;
        this.date = date;
        this.device = device;
        this.description = description;
        this.script = script;
        this.isCurrent = isCurrent;
        viewExpanded = false;
        devId = new Integer[MAX_NUM_OF_DEV];
        setDevId();
        json_tp = new JSONObject();
    }

    int getThpId() {
        return thpId;
    }

    void setThpId(int thpId) {
        this.thpId = thpId;
    }

    boolean isExpanded() {
        return viewExpanded;
    }

    void setExpanded(boolean viewExpanded) {
        this.viewExpanded = viewExpanded;
    }

    private void setDevId()
    {
        if (!device.isEmpty())
        {
            String regex =  "(.*)" + devFreePEMF +  "(.*)";
            devId[0] = device.matches(regex) ? devIdValue.get(devFreePEMF) : 0;
            regex =  "(.*)" + devMultiZap +  "(.*)";
            devId[1] = device.matches(regex) ? devIdValue.get(devMultiZap) : 0;
            regex =  "(.*)" + devZapperP +  "(.*)";
            devId[2] = device.matches(regex) ? devIdValue.get(devZapperP) : 0;
            regex =  "(.*)" + userTherapies +  "(.*)";
            devId[3] = device.matches(regex) ? devIdValue.get(userTherapies) : 0;
        }
    }

    @SuppressWarnings("unused")
    Integer[] getDevId() {
        return devId;
    }

    boolean containsId(int id)
    {
        boolean fRetVal = false;
        for (int i = 0; i < MAX_NUM_OF_DEV; i++)
        {
            if (id == devId[i])
            {
                fRetVal = true;
                break;
            }
        }
        return fRetVal;
    }

    @SuppressWarnings("unused")
    String printAll()
    {
        return  title + "\n" + author + "\n" + date + "\n" + device + "\n" + description + "\n" +
                Arrays.toString(script) + "\n" + devId[0].toString() + "\n" + "__end__" + "\n";
    }

    String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    @SuppressWarnings("unused")
    String getAuthor() {
        return author;
    }

    @SuppressWarnings("unused")
    void setAuthor(String author) {
        this.author = author;
    }

    @SuppressWarnings("unused")
    String getDate() {
        return date;
    }

    @SuppressWarnings("unused")
    void setDate(String date) {
        this.date = date;
    }

    String getDevice() {
        return device;
    }

    void setDevice(String device) {
        this.device = device;
    }

    String getDescription() {
        return description;
    }

    @SuppressWarnings("unused")
    void setDescription(String description) {
        this.description = description;
    }

    String[] getScript() {
        return script;
    }

    @SuppressWarnings("unused")
    boolean isCurrent() {
        return isCurrent;
    }

    @SuppressWarnings("unused")
    void setCurrent(boolean current) {
        isCurrent = current;
    }

    JSONObject getJson_tp() {
        return json_tp;
    }

    void setJson_tp(String[] a_thpData) {
        try {
            json_tp.put("title", a_thpData[0]);
            json_tp.put("author", a_thpData[1]);
            json_tp.put("date", a_thpData[2]);
            json_tp.put("device", a_thpData[3]);
            json_tp.put("description", a_thpData[4]);
            json_tp.put("script", a_thpData[5]);
        }catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private int thpId;
    private String title;
    private String author;
    private String date;
    private String device;
    private Integer[] devId;
    private String description;
    private String[] script;
    private boolean isCurrent; // Is current loaded in EEPROM
    private boolean viewExpanded;
    private JSONObject json_tp;

    final static String devFreePEMF = "freePEMF";
    final static String devMultiZap = "multiZAP";
    final static String devZapperP  = "zapper-plus";
    final static String userTherapies  = "user";
    final static int MAX_NUM_OF_DEV  = 4;
    private static final long serialVersionUID = 6319381028264959890L;
    private static HashMap<String, Integer> devIdValue;
    static {
        devIdValue = new HashMap<>();
        devIdValue.put(devFreePEMF, 1);
        devIdValue.put(devMultiZap, 2);
        devIdValue.put(devZapperP, 3);
        devIdValue.put(userTherapies, 4);
    }
}
