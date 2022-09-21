package com.example.runfasterapp;

import android.app.Activity;
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

public class viewPastRuns extends AppCompatActivity implements  AdapterView.OnItemClickListener{
    private final dbHelper db=new dbHelper(this);
    List<String> titles=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpastruns);
        ListView listView = (ListView) findViewById(R.id.listPastRuns);
        setView(listView,titles);
        listView.setOnItemClickListener(this::onItemClick);
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, viewPastRunsRunSelected.class);
        intent.putExtra("runTitle",titles.get(position) );
        startActivity(intent);
        this.finish();
    }

    //method to fill my view(the listview) with the required data from the database
    private void setView(ListView list, List<String> titles){
        Cursor cursor = db.getAllRuns();
        try{
            while(cursor.moveToNext())
                titles.add(cursor.getString(1));
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
