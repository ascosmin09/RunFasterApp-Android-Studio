package com.example.runfasterapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class startRunningSelectRoute extends AppCompatActivity implements  AdapterView.OnItemClickListener {
    private final dbHelper db=new dbHelper(this);
    List<String> titles=new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startrunningselectroute);
        setContentView(R.layout.activity_viewroutes);
        ListView listView = (ListView) findViewById(R.id.listViewRoutes);

        setView(listView,titles);
        listView.setOnItemClickListener(this);
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(startRunningSelectRoute.this, startRunning.class);
        intent.putExtra("routeTitle",titles.get(position) );
        startActivity(intent);
        this.finish();
    }

    //method to fill my view(the listview) with the required data from the database
    private void setView(ListView list, List<String> titles){
        Cursor cursor = db.getAllRoutes();
        try{
            while(cursor.moveToNext())
                titles.add(cursor.getString(3));
            cursor.close();
        }finally {
            db.close();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles);
        list.setAdapter(adapter);
    }
    @Override
    public void onBackPressed() {
        this.finish();
    }
}




