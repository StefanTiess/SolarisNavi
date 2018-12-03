package com.example.stefantiess.solarisnavi;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.modules.SqlTileWriter;
import org.osmdroid.tileprovider.modules.SqliteArchiveTileWriter;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;

public class MapDownloader {
    private CacheManager mgr = null;
    private MapView mMapView = null;
    private BoundingBox boundingBox = null;
    private GeoPoint center = null;
    private String outputName = "";
    private SqliteArchiveTileWriter mWriter = null;
    private SqlTileWriter sqlTileWriter = null;


    public MapDownloader(MapView mapView, SqliteArchiveTileWriter writer) {
        mWriter = writer;
        sqlTileWriter = new SqlTileWriter();
        mgr = new CacheManager(mapView, sqlTileWriter);
        }

    public void cancelAllJobs () {
        mgr.cancelAllJobs();
    }

    public void downloadMap(final Context context, BoundingBox boundingBox, Integer minZoom) {
        try {
            Toast.makeText(context, "Starting Download of Offline Tile Cache", Toast.LENGTH_SHORT).show();

            final int totalTiles = mgr.possibleTilesInArea(boundingBox, minZoom,18);
            Toast.makeText(context,totalTiles + " Tiles in Total", Toast.LENGTH_SHORT);

            mgr.downloadAreaAsyncNoUI(context, boundingBox, minZoom, 18, new CacheManager.CacheManagerCallback() {
                @Override
                public void onTaskComplete() {
                    Toast.makeText(context, "Download Done!", Toast.LENGTH_SHORT).show();
                    if (mWriter!=null)
                        mWriter.onDetach();
                }

                @Override
                public void updateProgress(int progress, int currentZoomLevel, int zoomMin, int zoomMax) {
                    Log.v("Map Downloader", "Progess: " + String.valueOf(progress) + " of " + String.valueOf(totalTiles));

                }

                @Override
                public void downloadStarted() {
                    Log.v("Map Downloader", "Download Started");

                }

                @Override
                public void setPossibleTilesInArea(int total) {

                }

                @Override
                public void onTaskFailed(int errors) {
                    Toast.makeText(context, "Download Failed!", Toast.LENGTH_SHORT).show();
                    if (mWriter!=null)
                        mWriter.onDetach();


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
