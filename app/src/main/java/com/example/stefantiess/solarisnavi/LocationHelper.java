package com.example.stefantiess.solarisnavi;


import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;

public class LocationHelper {
    private float mAccuracy;
    private float mDirection;
    private double mSpeedInKmh;
    private double mAverageSpeed;
    private String locationSource;
    private String updateMessage = "";
    private GeoPoint mCurrentPosition = new GeoPoint(0.0, 0.0);
    private ArrayList<Location> lastLocations = new ArrayList<Location>();
    //TODO improve returned results by making use of best guess




    public LocationHelper() {
    }

    public void handleLocationUpdate (Location location) {

        //verify if the current location is worth keeping, or adding noise
        if (isBetterLocation(location, lastLocations)) {
            lastLocations.add(location);

        }
        //keep 5 last Locations
        if (lastLocations.size()>5) {
            lastLocations.remove(0);
        }
        // After checking the locations array, the best location is always the last one.
        location = lastLocations.get(lastLocations.size() - 1);
        locationSource = location.getProvider();
        mAccuracy = location.getAccuracy();
        mDirection = location.getBearing();
        mSpeedInKmh = (location.getSpeed() * 3.6);

        mCurrentPosition = new GeoPoint(location);
        updateMessage = "Source: " + locationSource + ", Speed: " + mSpeedInKmh + "km/h, Direction: " + mDirection + "°, Accuracy: " + mAccuracy + "m.";
        double speedsum = 0;
        for (Location loc:lastLocations) {
            speedsum += loc.getSpeed();
        }
        mAverageSpeed = speedsum / lastLocations.size();



    }

    public float getAccuracy() {
        return mAccuracy;
    }

    public float getDirection() {
        return mDirection;
    }

    public double getSpeedInKmh() {
        return mSpeedInKmh;
    }

    public double getAverageSpeedInMeterPerSecond() {

        return mAverageSpeed;
    }

    public String getLocationSource() {
        return locationSource;
    }

    public String getUpdateMessage() {
        return updateMessage;
    }

    public GeoPoint getCurrentPosition() {
        return mCurrentPosition;
    }

    private boolean isBetterLocation(Location newLoc, ArrayList<Location> oldLocs) {
        if (oldLocs.size() == 0) {return true;}
        else {
            Location lastLoc = oldLocs.get(oldLocs.size()-1);

            // compare if last location is still better than new one
            // Assumptions:
            // If the accuracy is better or same, the newer position is better.
            // if the accuracy of the new one is worse, then it should be scraped except
            // when the old speed drove the position already out of the accuracy.
            // ie: if the time since the last location multiplied by the last speed in
            // meters per second is larger than than the new accuracy, the new location must still be better.
            float distance = newLoc.distanceTo(lastLoc);
            float accuracy = newLoc.getAccuracy();
            long timeElapsed = newLoc.getElapsedRealtimeNanos() - lastLoc.getElapsedRealtimeNanos();
            // if accuracy is better, than the location is better
            if (accuracy <= lastLoc.getAccuracy()) { return true;}
            // If the accurracy is very close though, keep it for the sake of update frequency
            if (accuracy < 8.0) {return true;}
            //if there has been a gps position in the last 5 seconds, scrape incoming "network" positions since they lack accuracy
            else if ((newLoc.getProvider().equals("network")) && (lastLoc.getProvider().equals("gps")) && (timeElapsed < 5000)) {
                return false;}

            // if the distance between locations is greater than the accuracy, the new one is still closer to the true loc

            //else if (distance > accuracy) {return true;}
            // if the old location is older than 5 seconds, then keep the new one anyway
            else if (timeElapsed > 5000) {return true;}

            // else the old location is still better than the new one
            else return false;






        }


    }
}
