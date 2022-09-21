package com.example.runfasterapp;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class viewReports extends AppCompatActivity {
    private final dbHelper db=new dbHelper(this);
    private List<Integer> times=new ArrayList<>();
    private List<Double> distances=new ArrayList<>();
    private List<Double> paces=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewreports);
        TextView distanceRanReports=(TextView) findViewById(R.id.totalRunningDistance);
        TextView timeRanReports=(TextView) findViewById(R.id.totalTime);
        TextView longestDistanceReports=(TextView) findViewById(R.id.longestDistance);
        TextView shortestDistanceReports=(TextView) findViewById(R.id.shortestDistance);
        TextView longestTimeReports=(TextView) findViewById(R.id.longestTime);
        TextView shortestTimeReports=(TextView) findViewById(R.id.shortestTime);
        TextView averagePaceReports=(TextView) findViewById(R.id.averagePace);
        TextView fastestPaceReports=(TextView) findViewById(R.id.fastestPace);
        TextView slowestPaceReports=(TextView) findViewById(R.id.slowestPace);

        Cursor cursor = db.getAllRuns();
        try{
            while(cursor.moveToNext()){
                times.add(Integer.parseInt(cursor.getString(2)));
                distances.add(Double.parseDouble(cursor.getString(3)));
                paces.add(Double.parseDouble(cursor.getString(4)));
            }
            cursor.close();
        }finally {
            db.close();
        }

        longestDistanceReports.setText(String.valueOf(Collections.max(distances))+" meters");
        shortestDistanceReports.setText(String.valueOf(Collections.min(distances))+" meters");
        longestTimeReports.setText(Collections.max(times) / 60 + " min " + Collections.max(times) % 60 + " sec ");
        shortestTimeReports.setText(Collections.min(times) / 60 + " min " + Collections.min(times) % 60 + " sec ");
        String formated=String.format("%.2f", Collections.max(paces));
        fastestPaceReports.setText(formated+" m/s");
        formated=String.format("%.2f", Collections.min(paces));
        slowestPaceReports.setText(formated+" m/s");
        int totalTime=0;
        double totalDistace=0;
        double totalPace=0;
        for(int i=0;i<times.size();i++){
            totalTime+=times.get(i);
            totalDistace+=distances.get(i);
            totalPace+=paces.get(i);
        }
        formated=String.format("%.2f",totalDistace/1000);
        distanceRanReports.setText(formated+" km");
        formated=String.format("%.2f",totalPace/times.size());
        averagePaceReports.setText(formated+" m/s");
        int hours = totalTime / 3600;
        int minutes = (totalTime % 3600) / 60;
        int seconds = totalTime % 60;
        timeRanReports.setText(hours+" h "+minutes+" min "+seconds+"s");

    }

    @Override
    public void onBackPressed() {
        this.finish();
    }
}
