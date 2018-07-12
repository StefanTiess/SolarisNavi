package com.example.stefantiess.solarisnavi;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.TilesOverlay;

import java.util.Locale;

public class MainActivity extends Activity {
    MapView map = null;
    IMapController mapController = null;
    Double zoomLevel = 18.0; //initial zoom level
    GeoPoint startPoint = new GeoPoint(52.366988, 13.643645);
    private static final int LOCATION_PERMISSION_CODE = 123;
    private boolean hasLocationPermission = false;
    private boolean viewIsCentered = true;
    Marker boatMarker = null;
    LocationHelper loc = new LocationHelper();
    GestureDetector scrollGestureDetector;
    TextView speedLabel = null;
    TextView directionLabel = null;
    TextView accuracyLabel = null;
    TextView sourceLabel = null;
    CoordinatorLayout containerView;
    CardView dashboardView;
    AccuracyOverlay accuracyOverlay = null;



    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //handle permissions first
        getLocationPermission();

        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        //inflate the Map View
        setContentView(R.layout.activity_main);

        //Configure the map and set up the location Service
        initializeMap();

        //add Buttons
        FloatingActionButton zoomInButton = findViewById(R.id.zoom_in);
        FloatingActionButton zoomOutButton = findViewById(R.id.zoom_out);
        FloatingActionButton centerViewButton = findViewById(R.id.center_on_position);

        // Set button Listeners
        zoomInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.zoomIn();
            }
        });
        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.zoomOut();
            }
        });
        centerViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewIsCentered = true;
                updateLocation();
            }
        });

        //Initialize Dashboard
         speedLabel = findViewById(R.id.speed_text_view);
         directionLabel = findViewById(R.id.direction_text_view);
         accuracyLabel = findViewById(R.id.accuracy_text_view);
         sourceLabel = findViewById(R.id.locationsource_text_view);

         containerView = findViewById(R.id.mainContainer);
         dashboardView =  findViewById(R.id.dashboardContainer);

         //now the ui should be initiated and follow the users location
    }


    /*  Update Location gets called whenever the location service receives
        a new position. It adapts the location marker to the new information,
        populates the Dashboard, and - if the user is not currently in
        scrolling mode - centers the map around the current location

     */
    public void updateLocation() {
        IMapController mapController = map.getController();

        /*
        if (accuracyOverlay == null) {
            accuracyOverlay = new AccuracyOverlay(loc.getCurrentPosition(), loc.getAccuracy());
            map.getOverlays().add(accuracyOverlay);
        }
        else     {
            accuracyOverlay.updateAccuracyOverlay(loc.getCurrentPosition(), loc.getAccuracy());
        }*/



        if (dashboardView.getVisibility() == View.GONE) {
            addDashboard();
        }
        //Update the boat markers position and direction
        if (boatMarker == null) {
            boatMarker = new Marker(map);
            boatMarker.setIcon(getDrawable(R.drawable.ic_boat_marker_20));
            boatMarker.setPosition(loc.getCurrentPosition());
            boatMarker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_CENTER);
            map.getOverlays().add(boatMarker);
        }
        else {
            boatMarker.setPosition(loc.getCurrentPosition());
            boatMarker.setRotation(loc.getDirection());
        }

        //

        //TODO insert scale circle for Accuracy around boat

        // Format the direction and append the Cardinal Direction (for example NE)
        String directionStr = String.format(Locale.getDefault(), "%.1f", loc.getDirection());
        directionStr += parseCardinalDirection(loc.getDirection());

        // Format the accuracy Information
        String accuracyStr = String.format(Locale.getDefault(), "%.1f", loc.getAccuracy());
        accuracyStr += "m";

        // Populate the Dashboard
        speedLabel.setText(String.format(Locale.getDefault(), "%.1f", loc.getSpeedInKmh()));
        accuracyLabel.setText(accuracyStr);
        if (loc.getAccuracy() > 10) {
            accuracyLabel.setTextColor(Color.parseColor("#660000"));
        }
        else if (loc.getAccuracy() > 5) {
            accuracyLabel.setTextColor(Color.parseColor("#ffa500"));
        }
        else if (loc.getAccuracy() > 0) {
            accuracyLabel.setTextColor(Color.parseColor("#005900"));
        }
        else accuracyLabel.setTextColor(Color.parseColor("#ffffff"));
        /*
        if (map.getZoomLevelDouble() < 20) {
            drawAccuracyCircle(loc.getAccuracy());
        }
        else drawAccuracyCircle(0);
        */
        directionLabel.setText(directionStr);
        sourceLabel.setText(loc.getLocationSource());

        if (viewIsCentered) {
            mapController.setCenter(loc.getCurrentPosition());
        }


    }

    private void initializeMap() {

        // Set up Open Sea Map Overlays
        MapTileProviderBasic mProvider = new MapTileProviderBasic(this);
        TilesOverlay seaMap = new TilesOverlay(mProvider, this);
        seaMap.setLoadingLineColor(Color.TRANSPARENT);
        seaMap.setLoadingBackgroundColor(Color.TRANSPARENT);
        seaMap.setLoadingDrawable(null);
        mProvider.setTileSource(TileSourceFactory.OPEN_SEAMAP);

        //Configure Base Map View
        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.HIKEBIKEMAP);
        map.setMaxZoomLevel(18.0);
        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);
        map.getOverlays().add(seaMap);
        mapController = map.getController();
        mapController.setZoom(zoomLevel); //max of SeaMap is 18, (min 3)

        // If a user touches the map for scrolling, deactive the follow mode
        // so it does not jump back to center whenever there is a new position
        map.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                viewIsCentered = false;
                FloatingActionButton centerViewButton = findViewById(R.id.center_on_position);
                centerViewButton.setBackgroundColor(Color.BLUE);
                return false;
            }



        });
        MapEventsReceiver er = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                Log.v("Maps Event Receiver", "single Tap");

                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                addPostionMarker(p);
                return false;
            }
        };
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(getBaseContext(), er);
        map.getOverlays().add(eventsOverlay);


        mapController.setCenter(startPoint);

    }

    //Ask for Location Permission, if necessary, and subscribe to location updates from the system
    private void getLocationPermission () {
        String[] permission = {Manifest.permission.ACCESS_FINE_LOCATION};
        if (ContextCompat.checkSelfPermission(getApplicationContext(), permission[0]) == PackageManager.PERMISSION_GRANTED) {
           hasLocationPermission = true;
            getCurrentLocation();
        }
        else { //If no permission is granted, ask for it
           ActivityCompat.requestPermissions(this ,permission, LOCATION_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
           hasLocationPermission = false;
        Log.v("Map Permission:", "Permission Results are in!");

        switch(requestCode) {
               case LOCATION_PERMISSION_CODE: {
                   if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                       hasLocationPermission = true;
                       Log.v("Map Permission:", "Permission have been granted!");

                       getCurrentLocation();
                   }
               }
           }

    }

    private void getCurrentLocation () {
       LocationManager locationManager = (LocationManager)  this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                loc.handleLocationUpdate(location);
                updateLocation();
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

// Register the listener with the Location Manager to receive location updates
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        } catch (SecurityException e) {
            Log.e("Security Exception", "Location Update Error: " + e.toString());
            getLocationPermission();
        }
        catch (NullPointerException e) {
            Log.e("Null Pointer Exception", "Location Update Error: " + e.toString());
        }
    }

    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onPause(){
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    public void drawAccuracyCircle(float radius) {

    }

    private String parseCardinalDirection(float d) {
        String directionStr = "";
        if (d >= 337.5 || d < 22.5) directionStr += "° N";
        else if (d >= 22.5 && d < 67.5) directionStr += "° NE";
        else if (d >= 67.5 && d < 112.5) directionStr += "° E";
        else if (d >= 112.5 && d < 157.5) directionStr += "° SE";
        else if (d >= 157.5 && d < 202.5) directionStr += "° S";
        else if (d >= 202.5 && d < 247.5) directionStr += "° SW";
        else if (d >= 247.5 && d < 292.5) directionStr += "° W";
        else if (d >= 292.5 && d < 337.5) directionStr += "° NW";
        return directionStr;
    }

    private void addPostionMarker(GeoPoint p) {
        Marker marker = new Marker(map);
        marker.setPosition(p);

        marker.setIcon(getDrawable(R.drawable.direction_arrow));
        marker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_CENTER);
        map.getOverlays().add(marker);
        map.invalidate();

    }

    private void addDashboard() {
        dashboardView.setVisibility(View.VISIBLE);
    }
}
