package com.biotronics.remotezap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.biotronics.remotezap.TherapyManagementActivity.JSON_ARRAY;

public class FileOperationsClass {

    FileOperationsClass() {
    }

    List<Therapy> loadJsonObjects(String json) {
        //noinspection Convert2Diamond
        List<Therapy> therapies = new ArrayList<Therapy>(50);
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray m_jArray = obj.getJSONArray(JSON_ARRAY);//"therapy"
            for(int i=0; i< m_jArray.length(); i++)
            {
                JSONObject j_obj = m_jArray.getJSONObject(i);

                String title, author, date, device, desc, scr;
                title = j_obj.getString("title");
                author = j_obj.getString("author");
                date = j_obj.getString("date");
                device = j_obj.getString("device");
                desc = j_obj.getString("description");
                scr = j_obj.getString("script");
                String[] thpScript = scr.split("\\s*\n\\s*");//One command per row
                Therapy thp = new Therapy(title, author, date, device, desc, thpScript, false);
                therapies.add(thp);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return therapies;
    }

    JSONArray loadJsonArray(String jsonArr)
    {
        JSONArray jArray = null;
        try {
            JSONObject obj = new JSONObject(jsonArr);
            jArray = obj.getJSONArray("therapy");
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return jArray;
    }

    JSONObject loadJsonObject(String jsonObj)
    {
        JSONObject jObj = null;
        try {
            jObj = new JSONObject(jsonObj);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return jObj;
    }

    String loadFileToString(String path, String fileName)
    {
        File updateFile = new File(path, fileName);
        String thpObj = "";

        if (updateFile.exists())
        {
            try {
                FileInputStream fis = new FileInputStream(updateFile);//context.openFileInput(fileName);
                DataInputStream dis = new DataInputStream(fis);
                BufferedReader br = new BufferedReader(new InputStreamReader(dis));
                String line = "";

                while ((line = br.readLine()) != null) {
                    thpObj += line;
                }
                dis.close();
                fis.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return thpObj;
    }

    boolean isStoragePermissionGranted(Context context) {
        String TAG = "Storage Permission";
        Activity activity = (Activity) context;
        if (Build.VERSION.SDK_INT >= 23) {
            if (activity.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }

    @SuppressWarnings("SameParameterValue")
    void saveFileExternal(Context context, String root, String dir, String therapy, String therapiesFileName) {
        if (isStoragePermissionGranted(context)) { // check or ask permission
            File myDir = new File(root, dir);
            if (!myDir.exists()) {
                myDir.mkdirs();
            }
            File file = new File(myDir, therapiesFileName);
            try {
                file.delete();
                file.createNewFile(); // if file already exists will do nothing
                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter outputStreamWriter =
                    new OutputStreamWriter(fos);
                outputStreamWriter.write(therapy);
                outputStreamWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @SuppressWarnings("SameParameterValue")
    void populateInitTherapyData(Context context, String fileName)
    {
        File updateFile = new File(context.getFilesDir(), fileName);
        try {
            if (!updateFile.exists())
            {
                Resources res = context.getResources();
                InputStream in_s = res.openRawResource(R.raw.therapies);
                OutputStream out_s = context.openFileOutput(fileName, Context.MODE_PRIVATE);

                byte[] b = new byte[in_s.available()];
                in_s.read(b);
                out_s.write(b);
                in_s.close();
                out_s.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    List<Therapy> loadThpObjsFromFile(Context context, String fileName)
    {
        File updateFile = new File(context.getFilesDir(), fileName);

        //noinspection Convert2Diamond
        List<Therapy> therapies = new ArrayList<Therapy>(50);
        if (updateFile.exists())
        {
            try {
                FileInputStream fis = context.openFileInput(fileName);
                ObjectInputStream ois = new ObjectInputStream(fis);
                therapies = (ArrayList<Therapy>) ois.readObject();
                ois.close();
                fis.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return therapies;
    }

    boolean saveThpObjsToFile(Context context, String fileName, List<Therapy> therapies) {
        boolean fRetVal = false;
        FileOutputStream outputStream;
        File updateFile = new File(context.getFilesDir(), fileName);
        try {
            if (updateFile.exists()){ updateFile.delete(); }

            outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(outputStream);
            oos.writeObject(therapies);

            oos.close();
            outputStream.close();
            fRetVal = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fRetVal;
    }

    @SuppressWarnings("SameParameterValue")
    boolean fileExist(String root, String dir, String fileName, boolean dontCrate)
    {
        File dirFolder = new File(root,dir);
        boolean fRetVal = dirFolder.exists();
        fRetVal |= dontCrate;
        if(!fRetVal) {
            try {
                dirFolder.mkdirs();
                if (!dirFolder.exists()) {
                    dirFolder.mkdir();
                }
                File file = new File(root+dir, fileName);
                file.createNewFile();
                fRetVal = true;
            } catch (IOException io) {
                io.printStackTrace();
            }
        }
        return fRetVal;
    }

}
