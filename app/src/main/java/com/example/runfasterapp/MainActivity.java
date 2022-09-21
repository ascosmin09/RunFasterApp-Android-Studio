package com.example.runfasterapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button createRoute= (Button) findViewById(R.id.createRoute);
        Button startRunningSelectRoute= (Button) findViewById(R.id.startRunning);
        Button viewPastRuns= (Button) findViewById(R.id.pastRuns);
        Button viewRoutes= (Button) findViewById(R.id.viewRoutes);
        Button viewReports= (Button) findViewById(R.id.viewReports);


        //set the intent for when is tapped
        createRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,createRoute.class);
                startActivity(intent);
            }
        });

        //set the intent for when is tapped
        startRunningSelectRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,startRunningSelectRoute.class);
                startActivity(intent);
            }
        });

        //set the intent for when is tapped
        viewPastRuns.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,viewPastRuns.class);
                startActivity(intent);
            }
        });

        //set the intent for when is tapped
        viewRoutes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,viewRoutes.class);
                startActivity(intent);
            }
        });

        //set the intent for when is tapped
        viewReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,viewReports.class);
                startActivity(intent);
            }
        });
    }
}