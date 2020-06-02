package com.biotronics.remotezap;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import static com.biotronics.remotezap.MainMenu.CLICK_TIME_BREAK;

public class About extends AppCompatActivity {

    @SuppressWarnings("FieldCanBeLocal")
    private long mLastClickTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
    }

    public void onClickAboutButtonLicense(View view) {
            if (SystemClock.elapsedRealtime() - mLastClickTime > CLICK_TIME_BREAK){
                Intent intentThp = new Intent(this, License.class);
                startActivity(intentThp);
            }
    }
}
