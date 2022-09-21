package com.example.runfasterapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.PolyUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class startRunning extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private static final int LOCATION_PERMISSION_CODE = 101;
    private final dbHelper db = new dbHelper(this);
    private List<LatLng> allPoints = new ArrayList<LatLng>();
    private List<Marker> allMarkers = new ArrayList<Marker>();
    private ArrayList<Polyline> polylines = new ArrayList<>();
    private int circularRouteFlag = 0;
    private LocationManager locationManager;
    private long time = 0;
    private CountDownTimer timerToComplete;
    private CountDownTimer timerToStartRunning;
    private LocationListener locationListener;
    private int startPointIndices = 0;
    private double dynamicDistance=0.0;
    private int distance = 0;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isUserOnRoute=false;
    private double requiredSpeed;
    private double distanceCovered;
    private LatLng lastPointOnMap;
    private int secondsCounter=0;
    private double timeRemaining=0.0;
    private long millisInSecGlobal=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startrunning);
        if (isLocationPermissionGranted()) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.mapStartRunning);
            mapFragment.getMapAsync(this);
        } else {
            requestLocationPermission();
        }

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            map.setMyLocationEnabled(true);

        //getting the info from the intent(the title of the route so i can identify and display it on the map)
        Intent intent = getIntent();
        String routeTitle = intent.getStringExtra("routeTitle");
        TextView titleStartRunning = (TextView) findViewById(R.id.titleStartRunning);
        TextView distanceStartRunning = (TextView) findViewById(R.id.distanceStartRunning);
        TextView timerStartRunning = (TextView) findViewById(R.id.timeStartRunning);
        Button startRunning = (Button) findViewById(R.id.startRunning);
        Button stopRunning = (Button) findViewById(R.id.stopRunning);
        TextView infoStartRunning = (TextView) findViewById(R.id.infoStartRunning);
        titleStartRunning.setText(routeTitle);


        String coordinates = "";
        Cursor cursor = db.getAllRoutes();
        try {
            while (cursor.moveToNext())
                if (cursor.getString(3).compareTo(routeTitle) == 0) {
                    time = cursor.getInt(5);
                    distance = cursor.getInt(2);
                    coordinates = cursor.getString(1);
                    circularRouteFlag = cursor.getInt(4);
                }
            cursor.close();
        } finally {
            db.close();
        }

        distanceStartRunning.setText(String.valueOf(distance) + "m");
        requiredSpeed= (double) distance /time;
        //recreating the route
        List<String> coordinatesRaw = new ArrayList<>();
        Pattern regex = Pattern.compile("\\((.*?)\\)");
        Matcher regexMatcher = regex.matcher(coordinates);

        while (regexMatcher.find()) {//Finds Matching Pattern in String
            coordinatesRaw.add(regexMatcher.group(1));//Fetching Group from String
        }

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

        timerStartRunning.setText(time / 60 + " min " + time % 60 + " sec ");

        //creating a location manager to get current location
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location currentLocation) {
                double currentLat = currentLocation.getLatitude();
                double currentLng = currentLocation.getLongitude();
                if (circularRouteFlag == 1) {
                    for (int i = 0; i < allPoints.size(); i++) {
                        // comparing the markers with the current location in order to activate the start button
                        if (distance(allPoints.get(i).latitude, allPoints.get(i).longitude, currentLat, currentLng) < 0.01) {
                            infoStartRunning.setVisibility(View.INVISIBLE);
                            startRunning.setVisibility(View.VISIBLE);
                            startPointIndices = i;
                            break;
                        } else {
                            infoStartRunning.setVisibility(View.VISIBLE);
                            startRunning.setVisibility(View.INVISIBLE);
                        }
                    }
                } else {
                    if (distance(allPoints.get(allPoints.size() - 1).latitude, allPoints.get(allPoints.size() - 1).longitude, currentLat, currentLng) < 0.01) {
                        infoStartRunning.setVisibility(View.INVISIBLE);
                        startRunning.setVisibility(View.VISIBLE);
                        startPointIndices = allPoints.size() - 1;
                    }
                    if (distance(allPoints.get(0).latitude, allPoints.get(0).longitude, currentLat, currentLng) < 0.01) {
                        infoStartRunning.setVisibility(View.INVISIBLE);
                        startRunning.setVisibility(View.VISIBLE);
                        startPointIndices = 0;
                    }

                }
            }
        };
        //requesting constant updates on the user's location
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1.0f, locationListener);

        //functionality for when the user taps "Start running"
        startRunning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //stopping the location listener that looked to activate the "start button"
                locationManager.removeUpdates(locationListener);

                //making the "stop running button" visible
                stopRunning.setVisibility(View.VISIBLE);
                //making the "start running button" invisible
                startRunning.setVisibility(View.INVISIBLE);
                //stopping the screen from locking so I don't lose the location
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);



                AlertDialog alertDialog = new AlertDialog.Builder(com.example.runfasterapp.startRunning.this).create();
                //countdown start timer
                timerToStartRunning = new CountDownTimer(11000, 1000) {
                    int countDown = 10;

                    public void onTick(long millisUntilFinished) {
                        alertDialog.setMessage("Start running in: " + String.valueOf(countDown));
                        alertDialog.show();
                        countDown--;
                    }

                    public void onFinish() {
                        //when the countdown is finished we cancel the dialog box and start monitoring the user's progress
                        alertDialog.dismiss();
                        //timer to complete the run
                        timerToComplete = new CountDownTimer(time * 1000, 1000) {

                            public void onTick(long millisUntilFinished) {
                                secondsCounter++;
                                //converting the time from milliseconds to min and seconds and displaying it at every tick
                                long min = millisUntilFinished / (60 * 1000) % 60;
                                long sec = millisUntilFinished / 1000 % 60;
                                millisInSecGlobal=millisUntilFinished/1000;
                                timerStartRunning.setText(min + " min " + sec + " sec ");
                                //checking if the user completed the run
                                fusedLocationClient = LocationServices.getFusedLocationProviderClient(startRunning.this);

                                if (ActivityCompat.checkSelfPermission(startRunning.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(startRunning.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    fusedLocationClient.getLastLocation()
                                            .addOnSuccessListener(com.example.runfasterapp.startRunning.this, new OnSuccessListener<Location>() {
                                                @RequiresApi(api = Build.VERSION_CODES.O)
                                                @Override
                                                public void onSuccess(Location location) {
                                                    if (location != null) {
                                                        LatLng locationAsLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                                        if(circularRouteFlag==1){
                                                            if(distance(allPoints.get(startPointIndices).latitude,allPoints.get(startPointIndices).longitude,locationAsLatLng.latitude,locationAsLatLng.longitude)<0.005 && distanceCovered>25) {
                                                                AlertDialog alertDialog = new AlertDialog.Builder(com.example.runfasterapp.startRunning.this).create();
                                                                alertDialog.setMessage("Run complete under the pre-set time, well done! Your run has been saved.");
                                                                alertDialog.show();
                                                                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                                v.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
                                                                v.vibrate(2000);
                                                                long min = millisUntilFinished / (60 * 1000) % 60;
                                                                long sec = millisUntilFinished / 1000 % 60;
                                                                timerStartRunning.setText(min + " min " + sec + " sec ");
                                                                String dateForTitle=String.valueOf(System.currentTimeMillis());
                                                                SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss");
                                                                String dateString = formatter.format(new Date(Long.parseLong(dateForTitle)));
                                                                db.saveRunInDB(dateString,String.valueOf(time-(millisUntilFinished/1000)),String.valueOf(distance),String.valueOf((double)distance/(double)(time-(millisUntilFinished/1000))),String.valueOf(allPoints.get(0)),titleStartRunning.getText().toString());
                                                                timerToComplete.cancel();
                                                                //REMEMBER TO REMOVE THIS FLAG WHEN THE STOP BUTTON IS PRESSED
                                                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                                            }
                                                        }
                                                        else{
                                                            if(startPointIndices==0)
                                                            {
                                                                if(distance(allPoints.get(allPoints.size()-1).latitude,allPoints.get(allPoints.size()-1).longitude,locationAsLatLng.latitude,locationAsLatLng.longitude)<0.005 && distanceCovered>25) {
                                                                    AlertDialog alertDialog = new AlertDialog.Builder(com.example.runfasterapp.startRunning.this).create();
                                                                    alertDialog.setMessage("Run complete under the pre-set time, well done! Your run has been saved.");
                                                                    alertDialog.show();
                                                                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                                    v.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
                                                                    v.vibrate(2000);
                                                                    long min = millisUntilFinished / (60 * 1000) % 60;
                                                                    long sec = millisUntilFinished / 1000 % 60;
                                                                    timerStartRunning.setText(min + " min " + sec + " sec ");
                                                                    String dateForTitle=String.valueOf(System.currentTimeMillis());
                                                                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss"   );
                                                                    String dateString = formatter.format(new Date(Long.parseLong(dateForTitle)));
                                                                    db.saveRunInDB(dateString,String.valueOf(time-(millisUntilFinished/1000)),String.valueOf(distance),String.valueOf((double)distance/(double)(time-(millisUntilFinished/1000))),String.valueOf(allPoints.get(allPoints.size()-1)),titleStartRunning.getText().toString());
                                                                    timerToComplete.cancel();
                                                                    //REMEMBER TO REMOVE THIS FLAG WHEN THE STOP BUTTON IS PRESSED
                                                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                                                }
                                                            }
                                                            else {
                                                                if(distance(allPoints.get(0).latitude,allPoints.get(0).longitude,locationAsLatLng.latitude,locationAsLatLng.longitude)<0.005 && distanceCovered>25) {
                                                                    AlertDialog alertDialog = new AlertDialog.Builder(com.example.runfasterapp.startRunning.this).create();
                                                                    alertDialog.setMessage("Run complete under the pre-set time, well done! Your run has been saved.");
                                                                    alertDialog.show();
                                                                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                                    v.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
                                                                    v.vibrate(2000);
                                                                    long min = millisUntilFinished / (60 * 1000) % 60;
                                                                    long sec = millisUntilFinished / 1000 % 60;
                                                                    timerStartRunning.setText(min + " min " + sec + " sec ");
                                                                    String dateForTitle=String.valueOf(System.currentTimeMillis());
                                                                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss"   );
                                                                    String dateString = formatter.format(new Date(Long.parseLong(dateForTitle)));
                                                                    db.saveRunInDB(dateString,String.valueOf(time-(millisUntilFinished/1000)),String.valueOf(distance),String.valueOf((double)distance/(double)(time-(millisUntilFinished/1000))),String.valueOf(allPoints.get(allPoints.size()-1)),titleStartRunning.getText().toString());
                                                                    timerToComplete.cancel();
                                                                    //REMEMBER TO REMOVE THIS FLAG WHEN THE STOP BUTTON IS PRESSED
                                                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                                                }
                                                            }
                                                        }
                                                        }
                                                }
                                            });
                                }
                                else{Toast.makeText(getApplicationContext(), "Location permissions required!", Toast.LENGTH_SHORT).show();}

                                if(secondsCounter==5){
                                    secondsCounter=0;
                                    //getting the current location in order calculate the user's velocity
                                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(startRunning.this);

                                    if (ActivityCompat.checkSelfPermission(startRunning.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(startRunning.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        fusedLocationClient.getLastLocation()
                                                .addOnSuccessListener(com.example.runfasterapp.startRunning.this, new OnSuccessListener<Location>() {
                                                    @RequiresApi(api = Build.VERSION_CODES.O)
                                                    @Override
                                                    public void onSuccess(Location location) {
                                                        if (location != null) {
                                                            LatLng locationAsLatLng=new LatLng(location.getLatitude(),location.getLongitude());
                                                            isUserOnRoute=false;
                                                            //after getting the user's location I check if the user is still on path or not
                                                            for(int i=0;i<polylines.size();i++)
                                                                if(PolyUtil.isLocationOnPath(locationAsLatLng,polylines.get(i).getPoints(),false,25))isUserOnRoute=true;
                                                            //if the user is on the correct path, I check if the velocity is equal or bigger than the one required and transmit the signal
                                                            if(isUserOnRoute){
                                                                //getting the distance that the user has covered
                                                                if(distanceCovered==0.0){
                                                                    distanceCovered=calculateDistanceDynamically(allPoints.get(0),locationAsLatLng);
                                                                    lastPointOnMap=new LatLng(location.getLatitude(),location.getLongitude());
                                                                }
                                                                else if(distance(lastPointOnMap.latitude,lastPointOnMap.longitude,locationAsLatLng.latitude,locationAsLatLng.longitude)>0.005){
                                                                    distanceCovered+=calculateDistanceDynamically(lastPointOnMap,locationAsLatLng);
                                                                    lastPointOnMap=new LatLng(location.getLatitude(),location.getLongitude());
                                                                    timeRemaining=millisUntilFinished/1000.0;
                                                                    double currentSpeed=distanceCovered/(time-timeRemaining);
                                                                    if(currentSpeed>=requiredSpeed){
                                                                        //sending a sound and a vibration
                                                                        MediaPlayer mp;
                                                                        mp = MediaPlayer.create(com.example.runfasterapp.startRunning.this, R.raw.beep);
                                                                        mp.setAudioAttributes(new AudioAttributes.Builder()
                                                                                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                                                                                .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                                                                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                                                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                                                                .build());
                                                                        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                                                            @Override
                                                                            public void onCompletion(MediaPlayer mp) {
                                                                                mp.reset();
                                                                                mp.release();
                                                                            }
                                                                        });
                                                                        mp.start();
                                                                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                                                                        v.vibrate(500);

                                                                    }
                                                                }
                                                            }
                                                            else{
                                                                //if user is not on path I inform him that the run is cancelled and reset everything
                                                                cancel();
                                                                distanceCovered=0.0;
                                                                timerStartRunning.setText(time / 60 + " min " + time % 60 + " sec ");
                                                                stopRunning.setVisibility(View.INVISIBLE);
                                                                infoStartRunning.setVisibility(View.VISIBLE);
                                                                if (ActivityCompat.checkSelfPermission(startRunning.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(startRunning.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                                                    //requesting constant updates on the user's location so it will listen for the activation of start button again
                                                                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1.0f, locationListener);
                                                                    AlertDialog alertDialog = new AlertDialog.Builder(com.example.runfasterapp.startRunning.this).create();
                                                                    alertDialog.setMessage("You didn't respect the route by more than 100 meters, so the recorded run has stopped.");
                                                                    alertDialog.show();
                                                                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                                    v.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
                                                                    v.vibrate(2000);
                                                                }
                                                            }
                                                        }
                                                    }
                                                });
                                    }
                                    else{Toast.makeText(getApplicationContext(), "Location permissions required!", Toast.LENGTH_SHORT).show();}

                                }
                            }
                            public void onFinish() {
                                timerStartRunning.setText("No time left!");
                                AlertDialog alertDialog = new AlertDialog.Builder(com.example.runfasterapp.startRunning.this).create();
                                alertDialog.setMessage("The time you set for this run ended, but the details of the run have been saved.");
                                alertDialog.show();
                                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    v.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
                                }
                                v.vibrate(2000);
                                String dateForTitle=String.valueOf(System.currentTimeMillis());
                                SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss"   );
                                String dateString = formatter.format(new Date(Long.parseLong(dateForTitle)));
                                stopRunning.setVisibility(View.INVISIBLE);
                                //REMEMBER TO REMOVE THIS FLAG WHEN THE STOP BUTTON IS PRESSED
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                db.saveRunInDB(dateString,String.valueOf(time),String.valueOf(distanceCovered),String.valueOf(distanceCovered/time),String.valueOf(lastPointOnMap),titleStartRunning.getText().toString());
                            }

                        }.start();
                    }
                }.start();
            }
        });
        stopRunning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog alertDialog = new AlertDialog.Builder(com.example.runfasterapp.startRunning.this).create();
                alertDialog.setMessage("The run was stopped and saved!");
                alertDialog.show();
                String dateForTitle=String.valueOf(System.currentTimeMillis());
                SimpleDateFormat formatter =new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss ");
                String dateString = formatter.format(new Date(Long.parseLong(dateForTitle)));
                timerToComplete.cancel();
                stopRunning.setVisibility(View.INVISIBLE);
                db.saveRunInDB(dateString,String.valueOf(time-millisInSecGlobal),String.valueOf(distanceCovered),String.valueOf(distanceCovered/(time-timeRemaining)),String.valueOf(lastPointOnMap),titleStartRunning.getText().toString());
            }
        });
        }



    // calculates the distance between two locations
    private double distance(double lat1, double lng1, double lat2, double lng2) {

        double earthRadius = 3958.75;

        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);

        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);

        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return earthRadius * c;
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

    //simple method to calculate the distance left every 3 seconds to see if the user is moving fast enough or not
    private double calculateDistanceDynamically(LatLng A,LatLng B){
        String url=getUrl(A,B);
        TaskRequestDistance taskRequestDistance = new TaskRequestDistance();
        taskRequestDistance.execute(url);
        return dynamicDistance;
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
            dynamicDistance=Double.parseDouble(distance);
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
