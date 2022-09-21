package com.example.runfasterapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class viewRoutesRouteSelected extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private static final int LOCATION_PERMISSION_CODE = 101;
    private final dbHelper db=new dbHelper(this);
    private List<LatLng> allPoints = new ArrayList<LatLng>();
    private List<Marker> allMarkers = new ArrayList<Marker>();
    private ArrayList<Polyline> polylines=new ArrayList<>();
    private int circularRouteFlag=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewroutesrouteselected);
        if(isLocationPermissionGranted()){
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.mapViewRouteSelected);
            assert mapFragment != null;
            mapFragment.getMapAsync(this);
        }
        else{
            requestLocationPermission();
        }

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        map = googleMap;
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            map.setMyLocationEnabled(true);
        Intent intent=getIntent();
        String routeTitle=intent.getStringExtra("routeTitle");
        EditText minutesViewRouteSelected = (EditText) findViewById(R.id.minutesViewRouteSelected);
        EditText secondsViewRouteSelected = (EditText) findViewById(R.id.secondsViewRouteSelected);
        EditText routeTitleViewRouteSelected = (EditText) findViewById(R.id.titleViewRouteSelected);
        TextView distanceViewRouteSelected = (TextView) findViewById(R.id.distanceViewRouteSelected);
        routeTitleViewRouteSelected.setText(routeTitle);

        int time=0;
        int distance=0;
        String coordinates="";
        Cursor cursor = db.getAllRoutes();
        try{
            while(cursor.moveToNext())
                if(cursor.getString(3).compareTo(routeTitle)==0) {
                    time=cursor.getInt(5);
                    distance=cursor.getInt(2);
                    coordinates=cursor.getString(1);
                    circularRouteFlag=cursor.getInt(4);
                }
            cursor.close();
        }finally {
            db.close();
        }

        minutesViewRouteSelected.setText(String.valueOf(time/60));
        secondsViewRouteSelected.setText(String.valueOf(time%60));
        distanceViewRouteSelected.setText(String.valueOf(distance)+"m");

        List<String> coordinatesRaw = new ArrayList<>();
        Pattern regex = Pattern.compile("\\((.*?)\\)");
        Matcher regexMatcher = regex.matcher(coordinates);

        while (regexMatcher.find()) {//Finds Matching Pattern in String
            coordinatesRaw.add(regexMatcher.group(1));//Fetching Group from String
        }

        for(int i=0;i<coordinatesRaw.size();i++){
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
        if(circularRouteFlag==1){
            String url = getUrl(allPoints.get(allPoints.size()-1), allPoints.get(0));
            TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
            taskRequestDirections.execute(url);
        }

        //editing the route from the db and reopening the "View routes" page
        Button saveRoute = (Button) findViewById(R.id.saveViewRouteSelected);
        saveRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean fieldsCompleted=true;
                String minutesText=minutesViewRouteSelected.getText().toString();
                if(minutesText.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please enter a value for minutes(It can be 0 if you're fast enough :) )", Toast.LENGTH_SHORT).show();
                    fieldsCompleted=false;
                }
                String secondsText=secondsViewRouteSelected.getText().toString();
                if(secondsText.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please enter a value for seconds(It can be 0)", Toast.LENGTH_SHORT).show();
                    fieldsCompleted=false;
                }

                String titleText=routeTitleViewRouteSelected.getText().toString();
                if(titleText.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please enter a title!", Toast.LENGTH_SHORT).show();
                    fieldsCompleted=false;
                }
                String coordinates="";
                Cursor cursor = db.getAllRoutes();
                try{
                    while(cursor.moveToNext())
                        if(cursor.getString(3).compareTo(titleText)==0) {
                            Toast.makeText(getApplicationContext(), "There's already a route with this title! Please enter a different one.", Toast.LENGTH_SHORT).show();
                            fieldsCompleted=false;
                        }
                    cursor.close();
                }finally {
                    db.close();}


                if(minutesText.compareTo("0")==0&&secondsText.compareTo("0")==0){
                    Toast.makeText(getApplicationContext(), "The time must be bigger than 0 minutes and 0 seconds.", Toast.LENGTH_SHORT).show();
                    fieldsCompleted=false;
                }

                if(fieldsCompleted){
                    if(allMarkers.size()==polylines.size())circularRouteFlag=1;
                    db.editRoute(routeTitle,routeTitleViewRouteSelected.getText().toString(),getTimeViewRouteRouteSelected());
                    Intent intent = new Intent(viewRoutesRouteSelected.this, MainActivity.class);
                    startActivity(intent);
                    Toast.makeText(getApplicationContext(), "The route has been edited!", Toast.LENGTH_SHORT).show();
                }


            }
        });


        //deleting the route from the db and reopening the "View routes" page
        Button deleteRoute = (Button) findViewById(R.id.deleteViewRouteSelected);
        deleteRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(viewRoutesRouteSelected.this);
                builder.setCancelable(true);
                builder.setMessage("Are you sure you want to delete this route?");
                builder.setPositiveButton("Confirm",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                db.deleteRoute(routeTitle);
                                Intent intent = new Intent(viewRoutesRouteSelected.this, MainActivity.class);
                                startActivity(intent);
                                Toast.makeText(getApplicationContext(), "The route has been deleted!", Toast.LENGTH_SHORT).show();
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    public int getTimeViewRouteRouteSelected(){
        EditText minutesViewRouteSelected = (EditText) findViewById(R.id.minutesViewRouteSelected);
        EditText secondsViewRouteSelected = (EditText) findViewById(R.id.secondsViewRouteSelected);
        return Integer.parseInt(minutesViewRouteSelected.getText().toString())*60+Integer.parseInt(secondsViewRouteSelected.getText().toString());
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

    //checking if location is permitted
    private boolean isLocationPermissionGranted(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            return true;
        else {
            return false;
        }
    }

    //requesting permission to use location
    private void requestLocationPermission(){
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_PERMISSION_CODE);
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }
}
