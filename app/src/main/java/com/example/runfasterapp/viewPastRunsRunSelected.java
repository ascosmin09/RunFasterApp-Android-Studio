package com.example.runfasterapp;

import static java.lang.Math.round;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class viewPastRunsRunSelected extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private static final int LOCATION_PERMISSION_CODE = 101;
    private final dbHelper db=new dbHelper(this);
    private List<LatLng> allPoints = new ArrayList<LatLng>();
    private List<Marker> allMarkers = new ArrayList<Marker>();
    private ArrayList<Polyline> polylines=new ArrayList<>();
    private int circularRouteFlag=0;
    private String runTitle;
    private String routeTitlePastRunSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpastrunsrunselected);
        if(isLocationPermissionGranted()){
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.mapPastRun);
            mapFragment.getMapAsync(this);
        }
        else{
            requestLocationPermission();
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            map.setMyLocationEnabled(true);
        Intent intent=getIntent();

        TextView distanceRanView = (TextView) findViewById(R.id.distancePastRun);
        TextView timeRanView = (TextView) findViewById(R.id.timePastRun);
        TextView paceView = (TextView) findViewById(R.id.averagePacePastRun);

        runTitle=intent.getStringExtra("runTitle");
        int timeRan=0;
        double distanceRan=0.0;
        double pace=0.0;
        String lastLocationMarker = null;

        //retrieving the data about this run from the db
        Cursor cursor = db.getAllRuns();
        try{
            while(cursor.moveToNext())
                if(cursor.getString(1).compareTo(runTitle)==0) {
                    timeRan=Integer.parseInt(cursor.getString(2));
                    distanceRan=Double.parseDouble(cursor.getString(3));
                    pace=Double.parseDouble(cursor.getString(4));
                    lastLocationMarker=cursor.getString(5);
                    routeTitlePastRunSelected=cursor.getString(6);
                }
            cursor.close();
        }finally {
            db.close();
        }

        distanceRanView.setText(String.valueOf(distanceRan)+" meters");
        timeRanView.setText(timeRan / 60 + " min " + timeRan % 60 + " sec ");
        String formatedPace=String.format("%.2f", pace);
        paceView.setText(formatedPace+" m/s");


        //retrieving the route that was ran onto and displaying it on the map
        String coordinates = "";
        int distanceFromRoute=0;
        Cursor cursor1 = db.getAllRoutes();
        try {
            while (cursor1.moveToNext())
                if (cursor1.getString(3).compareTo(routeTitlePastRunSelected) == 0) {
                    coordinates = cursor1.getString(1);
                    circularRouteFlag = cursor1.getInt(4);
                    distanceFromRoute=cursor1.getInt(2);
                }
            cursor1.close();
        } finally {
            db.close();
        }
        //recreating the route
        List<String> coordinatesRaw = new ArrayList<>();
        Pattern regex = Pattern.compile("\\((.*?)\\)");
        Matcher regexMatcher = regex.matcher(coordinates);

        while (regexMatcher.find()) {//Finds Matching Pattern in String
            coordinatesRaw.add(regexMatcher.group(1));//Fetching Group from String
        }

        String lastPoint=lastLocationMarker.substring(10,lastLocationMarker.length()-1);
        List<String> splittedLastPoint = Arrays.asList(lastPoint.split(","));

        LatLng lastPointLatLng=new LatLng(Double.parseDouble(splittedLastPoint.get(0)),Double.parseDouble(splittedLastPoint.get(1)));
        for (int i = 0; i < coordinatesRaw.size(); i++) {
            String[] latLng = coordinatesRaw.get(i).split(",");
            allPoints.add(new LatLng(Double.parseDouble(latLng[0]), Double.parseDouble(latLng[1])));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(allPoints.get(0), 15f));
            //creating a marker on the map and saving it so I can delete it later if needed
            allMarkers.add(map.addMarker(new MarkerOptions().position(allPoints.get(i))));
            //if there is more than one marker I fetch the route between the markers from the maps api
            if (allMarkers.size() > 1) {
                String url = getUrl(allPoints.get(allMarkers.size() - 2), allPoints.get(allMarkers.size() - 1));
                TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                taskRequestDirections.execute(url);
            }
        }
        if (circularRouteFlag == 1) {
            String url = getUrl(allPoints.get(allPoints.size() - 1), allPoints.get(0));
            TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
            taskRequestDirections.execute(url);
        }

        if(distanceFromRoute!=distanceRan)map.addMarker(new MarkerOptions().position(lastPointLatLng).title("Here is where you stopped or the time ran out :)").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

    }

    //creating the url for the api request. this url is for the directions API
    private String getUrl(LatLng origin, LatLng destination) {
        //origin for the route
        String originString = "origin=" + origin.latitude + "," + origin.longitude;
        //destination
        String destinationString = "destination=" + destination.latitude + "," + destination.longitude;
        //mode(it can be driving, walking, biking)
        String mode = "mode=" + "walking";
        //formating the string for web service
        String parameters = originString + "&" + destinationString + "&" + mode;
        //type of the output format
        String output = "json";
        //building the url
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
    }


    //making a get request and obtaining the walking directions between the markers by using directions API
    private String requestDirection(String url) throws IOException {
        String responseString = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;
        try {
            URL url1 = new URL(url);
            httpURLConnection = (HttpURLConnection) url1.openConnection();
            httpURLConnection.connect();
            inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuilder stringBuffer = new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);

            }

            responseString = stringBuffer.toString();
            bufferedReader.close();
            inputStreamReader.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            httpURLConnection.disconnect();
        }
        //returning the json as a string
        return responseString;
    }

    public class TaskRequestDirections extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String responseString = "";
            try {
                responseString = requestDirection(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            TaskParser taskParser = new TaskParser();
            taskParser.execute(s);

        }
    }

    public class TaskParser extends AsyncTask<String, Void, List<List<HashMap<String, String>>>> {
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject = null;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jsonObject = new JSONObject((strings[0]));
                directionsJSONParser directionsJSONParser = new directionsJSONParser();
                routes = directionsJSONParser.parse(jsonObject);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            super.onPostExecute(lists);

            ArrayList points = null;
            PolylineOptions polylineOptions = null;
            for (List<HashMap<String, String>> path : lists) {
                points = new ArrayList();
                polylineOptions = new PolylineOptions();
                for (HashMap<String, String> point : path) {
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));

                    points.add(new LatLng(lat, lng));
                }
                polylineOptions.addAll(points);
                polylineOptions.width(15);
                polylineOptions.color(Color.BLUE);
                polylineOptions.geodesic(true);
            }
            if (polylineOptions != null) {

                polylines.add(map.addPolyline(polylineOptions));

            } else {
                Toast.makeText(getApplicationContext(), "Directions not found!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isLocationPermissionGranted(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            return true;
        else {
            return false;
        }
    }

    private void requestLocationPermission(){
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_PERMISSION_CODE);
    }
    @Override
    public void onBackPressed() {
        this.finish();
    }
}
