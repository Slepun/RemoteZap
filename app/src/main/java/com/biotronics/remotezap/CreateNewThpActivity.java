package com.biotronics.remotezap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.biotronics.remotezap.TherapyManagementActivity.JSON_ARRAY;
import static com.biotronics.remotezap.TherapyManagementActivity.USER_DIR;
import static com.biotronics.remotezap.TherapyManagementActivity.USER_ROOT_DIR;
import static com.biotronics.remotezap.TherapyManagementActivity.USER_THP_FILE;

public class CreateNewThpActivity extends AppCompatActivity {

    private static final int TITLE = 0;
    private static final int SCRIPT_NM = 1;
    private static final int SCRIPT = 2;
    private static final int DEVICES = 3;
    private static final int DESC = 4;
    private EditText et_title, et_scriptName, et_script, et_desc;
    //freepemf, multizap, zapper-plus
    private CheckBox[] cb_devices;
    private Button et_addBtn;
    //title, script name, script, device checkbox, description
    private boolean[] isDataValid = {false, false, false, false, false};

    @SuppressWarnings("RedundantCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_thp);

        et_title = (EditText) findViewById(R.id.crNt_edittext_title);
        et_title.addTextChangedListener(new TextValidator(et_title) {
            @Override
            public void validate(TextView textView, String text) {
                if(text.length() > 1)
                {
                    isDataValid[TITLE] = true;
                }
                else
                {
                    isDataValid[TITLE] = false;
                    textView.setError("At least 2 characters");
                }
                validateAddButton();
            }
        });

        et_scriptName = (EditText) findViewById(R.id.crNt_edittext_scrname);
        et_scriptName.addTextChangedListener(new TextValidator(et_scriptName) {
            @Override
            public void validate(TextView textView, String text) {
                if(text.startsWith("#") && text.length() > 1)
                {
                    isDataValid[SCRIPT_NM] = true;
                }
                else
                {
                    isDataValid[SCRIPT_NM] = false;
                    textView.setError("Start with #, at least 2 characters");
                }
                validateAddButton();
            }
        });

        et_desc = (EditText) findViewById(R.id.crNt_edittext_descbody);
        et_desc.addTextChangedListener(new TextValidator(et_desc) {
            @Override
            public void validate(TextView textView, String text) {
                if(text.length() > 20)
                {
                    isDataValid[DESC] = true;
                }
                else
                {
                    isDataValid[DESC] = false;
                    textView.setError("At least 20 characters");
                }
                validateAddButton();
            }
        });

        et_script = (EditText) findViewById(R.id.crNt_edittext_scrbody);
        et_script.addTextChangedListener(new TextValidator(et_script) {
            @Override
            public void validate(TextView textView, String text) {
                String[] text_split = text.split("\n");
                int lineCounter = 0;
                boolean isValid = true;

                if(text.isEmpty()) {
                    textView.setError("Empty line");
                    isValid = false;
                }
                for (String line:text_split)
                {
                    lineCounter++;
                    if(line.length() > 55){
                        isValid = false;
                        textView.setError("Line too long (Max 55). Line:"+ lineCounter);
                        break;
                    }
                    //SKIP CHECKING LINE IF IT's COMMENT
                    if(line.startsWith("#")) {continue;}

                    //ALLOW ONLY 4 WORDS PER LINE
                    if(countChars(line, ' ') > 2)
                    {
                        isValid = false;
                        textView.setError("Too many command arguments in line: "+ lineCounter);
                        break;
                    }
                }
                isDataValid[SCRIPT] = isValid;
                validateAddButton();
            }
        });

        cb_devices = new CheckBox[3];
        cb_devices[0] = (CheckBox) findViewById(R.id.crNt_checkbox_freepemf);
        cb_devices[1] = (CheckBox) findViewById(R.id.crNt_checkbox_multizap);
        cb_devices[2] = (CheckBox) findViewById(R.id.crNt_checkbox_zapperp);
        et_addBtn = (Button) findViewById(R.id.crNt_buttonAdd);
        et_addBtn.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FileOperationsClass foc = new FileOperationsClass();
        //noinspection PointlessBooleanExpression
        if (foc.isStoragePermissionGranted(this) == false){
            Intent returnIntent = new Intent();
            setResult(RESULT_CANCELED, returnIntent);
        }
    }

    public void onCheckboxDevClicked(View view) {
        isDataValid[DEVICES] = false;
        for (CheckBox cb : cb_devices)
        {
            if (cb.isChecked())
            {
                isDataValid[DEVICES] = true;
                break;
            }
        }
        validateAddButton();
    }

    public void crNt_btn_onClickAdd(View view) {
        Therapy tp = new Therapy();
        String[] newThp = new String[6];
        dataToStringAr(newThp);
        tp.setJson_tp(newThp);
        String jsonString = tp.getJson_tp().toString();
        boolean fResult = addTherapyToUserFile(jsonString);

        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        if(!fResult)
        {
            setResult(RESULT_CANCELED, returnIntent);
        }
        finish();
    }

    private void validateAddButton()
    {
        boolean isValid = true;
        for (boolean state:isDataValid) {
            isValid &= state;
        }

        //noinspection PointlessBooleanExpression
        if(isValid == true)
        {
            et_addBtn.setEnabled(true);
        }
        else
        {
            et_addBtn.setEnabled(false);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private int countChars(String line, char ch)
    {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }

    private String getCheckboxData()
    {
        String strOut = "";
        if(cb_devices[0].isChecked()) {strOut += Therapy.devFreePEMF + " ";}
        if(cb_devices[1].isChecked()) {strOut += Therapy.devMultiZap + " ";}
        if(cb_devices[2].isChecked()) {strOut += Therapy.devZapperP + " ";}
        strOut += Therapy.userTherapies;
        return strOut;
    }

    private void dataToStringAr(String[] ar_out)
    {
        ar_out[0] = et_title.getText().toString();
        ar_out[1] = "user";
        ar_out[2] = "2018-02-06 18:09";
        ar_out[3] = getCheckboxData();
        ar_out[4] = et_desc.getText().toString();
        ar_out[5] = et_scriptName.getText().toString() + "\n" + et_script.getText().toString();
    }

    private boolean addTherapyToUserFile(String userThpJson) {
        FileOperationsClass foc = new FileOperationsClass();
        String root = USER_ROOT_DIR;
        String dir = USER_DIR;
        boolean fRetVal = false;
        if (foc.isStoragePermissionGranted(this)) {
            if (foc.fileExist(root, dir, USER_THP_FILE, false)) {
                String allThp = foc.loadFileToString(root + dir, USER_THP_FILE);
                JSONArray jArray = foc.loadJsonArray(allThp);
                if(jArray == null)
                {
                    jArray = new JSONArray();
                }
                JSONObject jObj = foc.loadJsonObject(userThpJson);
                //noinspection ConstantConditions
                if (jObj != null && jArray != null) {
                    jArray.put(jObj);
                    try {
                        JSONObject finalobject = new JSONObject();
                        finalobject.put(JSON_ARRAY, jArray);
                        allThp = finalobject.toString(2);
                        fRetVal = true;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    foc.saveFileExternal(this, root, dir, allThp, USER_THP_FILE);
                }
            }
        }
        return fRetVal;
    }
}
