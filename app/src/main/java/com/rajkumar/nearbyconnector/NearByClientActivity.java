package com.rajkumar.nearbyconnector;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.rajkumar.nearbytest.R;

public class NearByClientActivity extends AppCompatActivity {
    private static final String TAG = "NearByClientActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
