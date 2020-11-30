package com.biotronics.remotezap;

import android.os.AsyncTask;
//0 - send therapy mode. 1 - status check mode
public class DeviceDataSendAsync extends AsyncTask<Void, Integer, Void> {

    //Not sure whether Service context is danger as warning claim
    private MyDeviceCommunicationService devCom;
    private String[] data;
    private int mode;
    private int[] eventStart;
    private int[] eventEnd;

    //package private
    DeviceDataSendAsync(MyDeviceCommunicationService org, String[] cmdToSend,
                        int mode, int[] eventStart, int[] eventEnd) {
        devCom = org;
        data = cmdToSend;
        this.mode = mode;

        this.eventStart = eventStart;
        this.eventEnd = eventEnd;
    }

    @Override
    protected void onPreExecute() {
        if(eventStart != null) {
            for (int event : eventStart) {
                devCom.sendCbCode(event);
            }
        }
    }



    @Override
    protected Void doInBackground(Void... voids) {
        final int SHORT_WAIT = 200;
        final int MID_WAIT = 500;
        final int LONG_WAIT = 2000;
        if(mode == 0) {
            devCom.sendConsoleCmd("rm\n");
            waitTime(SHORT_WAIT);
            devCom.sendConsoleCmd("mem\n");
            waitTime(SHORT_WAIT);
            int i = 1;
            for (String cmd : data) {
                if (cmd.isEmpty() || cmd.equals(" ")){ continue;}

                boolean isEnd = cmd.matches(".*@.*");
                if (isEnd) {
                    devCom.sendConsoleCmd("@\n");
                    break;
                }
                devCom.sendConsoleCmd(cmd + "\n");

                if ((i % 8) == 0)//flush every 8 transmissions
                {
                    devCom.sendConsoleCmd("@\n");
                    waitTime(MID_WAIT);
                    devCom.sendConsoleCmd("mem @\n");
                    waitTime(MID_WAIT);
                }
                i++;
            }//for
            devCom.sendConsoleCmd("beep 800\n");
        }

        if (mode == 1)
        {//Status check
            String[] results;
            int exitCounter = 0;
            do
            {
                devCom.sendConsoleCmd("ls\n");
                waitTime(MID_WAIT);
                results = devCom.lastDataReceived.split("\n");
                if (results.length > 1)
                {
                    if (results[0].startsWith("#") || results[1].startsWith("#"))
                        break;
                }
                exitCounter++;
            } while (exitCounter < 5);
            /*!results[0].startsWith("#") ||*
            TODO: result[0] usually contains prevously sent ls command.
            Another loop would be necessary to check first few results if they start with #
             */


            devCom.checkCurrentTherapy();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if(eventEnd != null){
            for (int event: eventEnd) {
                devCom.sendCbCode(event);
            }
        }

        if (mode == 0)
        {//New therapy installed
            int[] startingEv = {MyDeviceCommunicationService.LOAD_START};
            int[] endingEv = {MyDeviceCommunicationService.LOAD_END,
                    MyDeviceCommunicationService.UPDATE_DEVICE_STATUS};

            new DeviceDataSendAsync(devCom, data, 1,
                    startingEv, endingEv).execute(); //UPDATE THP NAME
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    private void waitTime(int ms)
    {
        try{
            Thread.sleep(ms);
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        }
    }
}
