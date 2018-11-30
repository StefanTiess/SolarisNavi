package com.example.stefantiess.solarisnavi;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.modules.SqliteArchiveTileWriter;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;

public class MapDownloader {
    private CacheManager mgr = null;
    private SqliteArchiveTileWriter writer = null;
    private MapView mMapView = null;
    private BoundingBox boundingBox = null;
    private GeoPoint center = null;
    private String outputName = "";


    public MapDownloader(MapView mapView) {
          mgr = new CacheManager(mapView);
              }

    public void cancelAllJobs () {
        mgr.cancelAllJobs();
    }

    public void downloadMap(final Context context, int radius, GeoPoint center) {
        try {
            Toast.makeText(context, "Starting Download of Offline Tile Cache", Toast.LENGTH_SHORT).show();
            Double north = center.destinationPoint(radius, 0).getLatitude();
            Double east = center.destinationPoint(radius, 90).getLongitude();
            Double south = center.destinationPoint(radius, 180).getLatitude();
            Double west = center.destinationPoint(radius, 270).getLongitude();
            boundingBox = new BoundingBox(north, east, south, west);
            final int totalTiles = mgr.possibleTilesInArea(boundingBox, 17,18);


            mgr.downloadAreaAsyncNoUI(context, boundingBox, 17, 18, new CacheManager.CacheManagerCallback() {
                @Override
                public void onTaskComplete() {
                    Toast.makeText(context, "Download Done!", Toast.LENGTH_SHORT).show();
                    if (writer!=null)
                        writer.onDetach();
                }

                @Override
                public void updateProgress(int progress, int currentZoomLevel, int zoomMin, int zoomMax) {
                    Log.v("Map Downloader", "Progess: " + String.valueOf(progress) + " of " + String.valueOf(totalTiles));
                }

                @Override
                public void downloadStarted() {

                }

                @Override
                public void setPossibleTilesInArea(int total) {

                }

                @Override
                public void onTaskFailed(int errors) {
                    Toast.makeText(context, "Download Failed!", Toast.LENGTH_SHORT).show();
                    if (writer!=null)
                        writer.onDetach();


                }
            });
        }catch (Exception e) {
            e.printStackTrace();
        }


    }


    public long getCurrentCacheUsage() {
        return mgr.currentCacheUsage();

    }

    public long getCacheCapacity() {
        return mgr.cacheCapacity();

    }


}
