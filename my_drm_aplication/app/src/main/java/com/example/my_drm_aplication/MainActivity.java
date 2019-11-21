package com.example.my_drm_aplication;

//import android.os.Bundle;
//import androidx.appcompat.app.AppCompatActivity;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button nonDrmButton;
    private Button clearKeyButton;
    private Button playReadyButton;
    private Button widevineButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nonDrmButton = (Button) findViewById(R.id.button_nonDrm);
        clearKeyButton = (Button) findViewById(R.id.button_clearKey);
        widevineButton = (Button) findViewById(R.id.button_widevine);

        nonDrmButton.setOnClickListener(onClickListener);
        clearKeyButton.setOnClickListener(onClickListener);
        widevineButton.setOnClickListener(onClickListener);

    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String streamType = "DASH";

            Intent intent = new Intent(getApplicationContext(), DRMPlayer.class);
            intent.putExtra("streamType", streamType);

            switch (v.getId()) {
                case R.id.button_clearKey:
                    intent.putExtra("isDrm", true);
                    intent.putExtra("drmType", "ClearKey");
                    break;
                case R.id.button_widevine:
                    intent.putExtra("isDrm", true);
                    intent.putExtra("drmType", "WideVine");
                    break;
                case R.id.button_nonDrm:
                    intent.putExtra("isDrm", false);
                    intent.putExtra("drmType", "non");
                    break;
            }

            startActivity(intent);
        }
    };

}
