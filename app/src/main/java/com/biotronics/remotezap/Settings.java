package com.biotronics.remotezap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.TextView;

public class Settings extends AppCompatActivity {

    protected static final String USB_ARDUINO_VID = "USB_ARDUINO_VID";
    protected static int ARDUINO_NANO_VID = 0x1A86;

    private EditText editText_arduinoUsbVID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        //noinspection RedundantCast
        editText_arduinoUsbVID = (EditText) findViewById(R.id.editText_usbVID);
        editText_arduinoUsbVID.addTextChangedListener(new TextValidator(editText_arduinoUsbVID) {
            @Override
            public void validate(TextView textView, String text) {
                if(text.length() == 4)
                {
                    SavePreferences();
                }
                else
                {
                    textView.setError("4 digit number in HEX");
                }
            }
        });
        LoadPreferences();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void SavePreferences(){
        SharedPreferences sharedPreferences = getSharedPreferences("SettingsShdPf", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String dataString = editText_arduinoUsbVID.getText().toString();
        Integer vidVal = Integer.decode("0x"+dataString);
        editor.putInt(USB_ARDUINO_VID, vidVal);
        editor.apply();
    }

    public void LoadPreferences(){
        SharedPreferences sharedPreferences = getSharedPreferences("SettingsShdPf", MODE_PRIVATE);
        int usbVID = sharedPreferences.getInt(USB_ARDUINO_VID, ARDUINO_NANO_VID);
        editText_arduinoUsbVID.setText(Integer.toHexString(usbVID));
        //ARDUINO_NANO_VID = usbVID;
    }

//    public static void loadVID()
//    {
//        SharedPreferences sharedPreferences = getSharedPreferences("SettingsShdPf", MODE_PRIVATE);
//        int usbVID = sharedPreferences.getInt(USB_ARDUINO_VID, ARDUINO_NANO_VID);
//        ARDUINO_NANO_VID = usbVID;
//    }
}
