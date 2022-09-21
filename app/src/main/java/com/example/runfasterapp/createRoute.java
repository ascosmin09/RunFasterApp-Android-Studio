package com.example.runfasterapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

public class createRoute extends AppCompatActivity implements OnMapReadyCallback {


    private GoogleMap map;
    private static final int LOCATION_PERMISSION_CODE = 101;
    private List<LatLng> allPoints = new ArrayList<LatLng>();
    private List<Marker> allMarkers = new ArrayList<Marker>();
    private List<Integer> distances = new ArrayList<Integer>();
    private int totalDistance=0;
    private int circularRouteFlag=0;
    private ArrayList<Polyline> polylines=new ArrayList<>();
    boolean mapAccessController=true;
    private dbHelper db=new dbHelper(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_createroute);
        if (isLocationPermissionGranted()) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.mapCreateRoute);
            mapFragment.getMapAsync(this);
        } else {
            requestLocationPermission();
        }
    }

    //manipulating the map when it becomes available
    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMaxZoomPreference(17.0f);
        TextView distanceTextView = (TextView) findViewById(R.id.distanceCreateRoute);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            map.setMyLocationEnabled(true);
        //when the user taps on the map I place a marker so I have the coordinates to create the route
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                //i let the user add markers as long as the only one that has been repeated is the first one(in order to create a circular route)
                if(mapAccessController){
                    //adding all points in order to save them to the database
                    allPoints.add(latLng);
                    //creating a marker on the map and saving it so I can delete it later if needed
                    allMarkers.add(map.addMarker(new MarkerOptions().position(latLng).title(String.valueOf(allMarkers.size()))));
                    //if there is more than one marker I fetch the route between the markers from the maps api
                    if (allMarkers.size() > 1) {
                        String url = getUrl(allPoints.get(allMarkers.size() - 2), allPoints.get(allMarkers.size() - 1));
                        TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                        taskRequestDirections.execute(url);
                        TaskRequestDistance taskRequestDistance = new TaskRequestDistance();
                        taskRequestDistance.execute(url);

                    }
                }
                else Toast.makeText(getApplicationContext(), "Seems that you have created a circular route. If you want to edit it click undo or reset.", Toast.LENGTH_SHORT).show();
            }
        });

        //linking some ui with variables so i can control them
        EditText minutesCreateRoute = (EditText) findViewById(R.id.minutesCreateRoute);
        EditText secondsCreateRoute = (EditText) findViewById(R.id.secondsCreateRoute);
        EditText titleCreateRoute = (EditText) findViewById(R.id.titleCreateRoute);
        Button saveCreateRoute= (Button) findViewById(R.id.saveRoute);
        //if the user clicks on the save button I save everything in the database(if all the fields are filled with correct values)
        saveCreateRoute.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                boolean fieldsCompleted=true;
                String minutesText=minutesCreateRoute.getText().toString();
                if(minutesText.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please enter a value for minutes(It can be 0 if you're fast enough :) )", Toast.LENGTH_SHORT).show();
                    fieldsCompleted=false;
                }
                String secondsText=secondsCreateRoute.getText().toString();
                if(secondsText.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please enter a value for seconds(It can be 0)", Toast.LENGTH_SHORT).show();
                    fieldsCompleted=false;
                }

                String titleText=titleCreateRoute.getText().toString();
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

                if(allMarkers.size()<2){
                    Toast.makeText(getApplicationContext(), "A route has to be created with a minimum of two markers!", Toast.LENGTH_SHORT).show();
                    fieldsCompleted=false;
                }
                if(fieldsCompleted){
                    if(allMarkers.size()==polylines.size())circularRouteFlag=1;
                    db.saveRouteInDB(getCoordinatesCreateRoute(),getTimeCreateRoute(),getDistanceCreateRoute(),getTitleCreateRoute(),circularRouteFlag);
                    map.clear();
                    allMarkers.clear();
                    totalDistance=0;
                    distances.clear();
                    distanceTextView.setText("");
                    allPoints.clear();
                    polylines.clear();
                    secondsCreateRoute.setText("");
                    minutesCreateRoute.setText("");
                    titleCreateRoute.setText("");
                    mapAccessController=true;
                    Toast.makeText(getApplicationContext(), "The route has been saved!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        Button resetCreateRoute= (Button) findViewById(R.id.resetCreateRoute);
        //when the user taps on reset, i delete everything from the map and reset all fields
        resetCreateRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            map.clear();
            allMarkers.clear();
            totalDistance=0;
            distances.clear();
            distanceTextView.setText("");
            allPoints.clear();
            polylines.clear();
            titleCreateRoute.setText("");
            mapAccessController=true;
            }
        });

        Button undoCreateRoute= (Button) findViewById(R.id.undoCreateRoute);
        //when the user pressed undo I update the map and my data
        undoCreateRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(allMarkers.size()==1){
                    allMarkers.get(0).remove();
                    allMarkers.remove(allMarkers.size()-1);
                    allPoints.remove(allPoints.size()-1);
                }
                if(allMarkers.size()>1){
                    if(allMarkers.size()==polylines.size()){
                        totalDistance=0;
                        distances.remove(distances.size()-1);
                        if(distances.size()>0)
                            for(int i=0;i<distances.size();i++){
                                totalDistance+=distances.get(i);
                            }
                        distanceTextView.setText(totalDistance + "m");
                        polylines.get(polylines.size()-1).remove();
                        polylines.remove(polylines.size()-1);
                        mapAccessController=true;
                    }
                    else{
                        allMarkers.get(allMarkers.size()-1).remove();
                        allMarkers.remove(allMarkers.size()-1);
                        totalDistance=0;
                        distances.remove(distances.size()-1);
                        if(distances.size()>0)
                            for(int i=0;i<distances.size();i++){
                                totalDistance+=distances.get(i);
                            }
                        distanceTextView.setText(totalDistance +"m");
                        allPoints.remove(allPoints.size()-1);
                        polylines.get(polylines.size()-1).remove();
                        polylines.remove(polylines.size()-1);
                    }

                }

            }
        });

        //identifying if the marker the user clicked on is the first marker, in order to complete a cirular route
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                    if(!mapAccessController)Toast.makeText(getApplicationContext(), "If you want to edit this circular route press undo or reset.", Toast.LENGTH_SHORT).show();
                    else if (marker.equals(allMarkers.get(0))) {
                        String url = getUrl(allPoints.get(allMarkers.size() - 1), allPoints.get(0));
                        TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                        taskRequestDirections.execute(url);
                        TaskRequestDistance taskRequestDistance = new TaskRequestDistance();
                        taskRequestDistance.execute(url);
                        totalDistance=0;
                        for(int j=0;j<distances.size();j++){
                            totalDistance+=distances.get(j);
                        }
                        distanceTextView.setText(String.valueOf(totalDistance)+"m");
                        mapAccessController=false;
                    }
                    else Toast.makeText(getApplicationContext(), "The only marker that can be repeated is the first marker, in order to create a circular route.", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
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
    public class TaskRequestDistance extends AsyncTask<String, Void, String> {
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
            TaskParserDistance taskParser = new TaskParserDistance();
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

    public class TaskParserDistance extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... strings) {
            JSONObject jsonObject = null;
            String distance = "";
            try {
                jsonObject = new JSONObject((strings[0]));
                directionsJSONParser directionsJSONParser = new directionsJSONParser();
                distance = directionsJSONParser.distance(jsonObject);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return distance;
        }

        @Override
        protected void onPostExecute(String distance) {
            super.onPostExecute(distance);
            if(distances.isEmpty()){
                totalDistance=Integer.parseInt(distance);
                TextView distanceTextView = (TextView) findViewById(R.id.distanceCreateRoute);
                distanceTextView.setText(distance+"m");
            }
            if (distance != null) {
                distances.add(Integer.parseInt(distance));
                TextView distanceTextView = (TextView) findViewById(R.id.distanceCreateRoute);
                totalDistance=0;
                for(int i=0;i<distances.size();i++){
                    totalDistance+=distances.get(i);
                }
                distanceTextView.setText(totalDistance+"m");
            }
        }
    }

    private boolean isLocationPermissionGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            return true;
        else {
            return false;
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
    }

    public String getCoordinatesCreateRoute(){
        StringBuilder coordinates=new StringBuilder();
        for(int i=0;i<allPoints.size();i++){
            coordinates.append(allPoints.get(i));
        }
        return coordinates.toString();
    }

    public int getTimeCreateRoute(){
        EditText minutesCreateRoute = (EditText) findViewById(R.id.minutesCreateRoute);
        EditText secondsCreateRoute = (EditText) findViewById(R.id.secondsCreateRoute);
        return Integer.parseInt(minutesCreateRoute.getText().toString())*60+Integer.parseInt(secondsCreateRoute.getText().toString());
    }

    public String getTitleCreateRoute(){
        EditText titleCreateRoute = (EditText) findViewById(R.id.titleCreateRoute);
        return titleCreateRoute.getText().toString();
    }

    public int getDistanceCreateRoute(){
        return totalDistance;
    }
    @Override
    public void onBackPressed() {
        this.finish();
    }

}

