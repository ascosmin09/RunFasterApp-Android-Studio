package com.example.runfasterapp;

import static android.provider.BaseColumns._ID;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class dbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME="runFasterDB.db";
    private static final int DATABASE_VERSION=9;

    public dbHelper(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static final String TABLE_RUNS = "runs";
    private static final String TABLE_ROUTE = "route";;
    private static final String KEY_ROUTE_TITLE = "routeTitle";
    private static final String KEY_COORDINATES ="coordinates";
    private static final String KEY_TIME="time";
    private static final String KEY_DISTANCE="distance";
    private static final String KEY_PACE="avg_pace";
    private static final String KEY_TITLE="title";
    private static final String KEY_LASTLOCATION="lastLocation";
    private static final String KEY_ROUTE_IS_CIRCULAR="circular_route_flag";
    private static final String[] FROMROUTES ={_ID,KEY_COORDINATES,KEY_DISTANCE,KEY_TITLE,KEY_ROUTE_IS_CIRCULAR,KEY_TIME};
    private static final String[] FROMRUNS ={_ID,KEY_TITLE,KEY_TIME,KEY_DISTANCE,KEY_PACE,KEY_LASTLOCATION,KEY_ROUTE_TITLE};

    private static final String CREATE_TABLE_RUNS = "CREATE TABLE "
            + TABLE_RUNS + "("
            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_TITLE + " TEXT,"
            + KEY_TIME + " TEXT,"
            + KEY_DISTANCE + " TEXT,"
            + KEY_PACE + " TEXT,"
            + KEY_LASTLOCATION + " TEXT,"
            + KEY_ROUTE_TITLE + " TEXT "
            + ")";

    private static final String CREATE_TABLE_ROUTE = "CREATE TABLE "
            + TABLE_ROUTE + "("
            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_COORDINATES + " TEXT,"
            + KEY_DISTANCE + " INTEGER,"
            + KEY_TITLE + " TEXT,"
            + KEY_ROUTE_IS_CIRCULAR + " INTEGER,"
            + KEY_TIME + " INTEGER "
            + ")";


    @Override
    public void onCreate(SQLiteDatabase dbHelper) {
        dbHelper.execSQL(CREATE_TABLE_ROUTE);
        dbHelper.execSQL(CREATE_TABLE_RUNS);

    }

    @Override
    public void onUpgrade(SQLiteDatabase dbHelper, int i, int i1) {
        dbHelper.execSQL("DROP TABLE IF EXISTS " + TABLE_RUNS);
        dbHelper.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUTE);
        onCreate(dbHelper);
    }


    //RUNS QUERY

    //creating(saving) a run in the db
    public void saveRunInDB(String title, String time, String distance,String pace,String lastLocation,String routeTitle) {
        try{
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(KEY_TITLE, title);
            values.put(KEY_TIME, time);
            values.put(KEY_DISTANCE, distance);
            values.put(KEY_PACE,pace);
            values.put(KEY_LASTLOCATION,lastLocation);
            values.put(KEY_ROUTE_TITLE,routeTitle);
            db.insertOrThrow(TABLE_RUNS,null,values);
        }
        catch (Exception ex){
        } finally {
            this.close();
        }

    }

    //simple query to get all info from runs table
    public Cursor getAllRuns() {
        SQLiteDatabase db=this.getReadableDatabase();
        return db.query(TABLE_RUNS,FROMRUNS,null,null,null,null, null);
    }

    //deleting a run from the db
    public void deleteRun(String title) {
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL("DELETE FROM "+TABLE_RUNS+" WHERE title='"+title+"'");
        db.close();
    }

    //ROUTES QUERY

    //creating(saving) a route in the db
    public void saveRouteInDB(String coordinates, int time, int distance,String title,int circularRouteFlag) {
        try{
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(KEY_COORDINATES, coordinates);
            values.put(KEY_TIME, time);
            values.put(KEY_DISTANCE,distance);
            values.put(KEY_TITLE,title);
            values.put(KEY_ROUTE_IS_CIRCULAR,circularRouteFlag);
            db.insertOrThrow(TABLE_ROUTE,null,values);
        }
        catch (Exception ex){
        } finally {
            this.close();
        }

    }

    //simple query to get all info from Routes table
    public Cursor getAllRoutes() {
            SQLiteDatabase db=this.getReadableDatabase();
            return db.query(TABLE_ROUTE,FROMROUTES,null,null,null,null, null);
    }


    //simple sqlite query to update an entrance in my table
    public void editRoute(String oldTitle,String newTitle, int time){
        SQLiteDatabase db = this.getReadableDatabase();
        String query="UPDATE "+TABLE_ROUTE+" SET "+ KEY_TITLE+" = '"+newTitle+"', "+
                KEY_TIME+" = '"+time
                +"' WHERE "+KEY_TITLE+" = '"+oldTitle+"'";
        db.execSQL(query);
        db.close();
    }

    //deleting a route from the db
    public void deleteRoute(String title) {
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL("DELETE FROM "+TABLE_ROUTE+" WHERE title='"+title+"'");
        db.close();
    }


}
