package com.example.tania.activityrectesi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;

public class IntroActivity extends AppCompatActivity {

    //intro animation
    private static int SHOW_TIME=3000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent startApp = new Intent(IntroActivity.this, MainActivity.class);
                startActivity(startApp);
            }
        },SHOW_TIME);
    }
}
