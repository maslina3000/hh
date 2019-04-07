package com.example.here;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapGesture;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.mapping.MapState;
import com.here.android.mpa.mapping.PositionIndicator;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.GeocodeRequest;
import com.here.android.mpa.search.GeocodeRequest2;
import com.here.android.mpa.search.GeocodeResult;
import com.here.android.mpa.search.ResultListener;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapActivity extends AppCompatActivity
implements MapGesture.OnGestureListener, PositioningManager.OnPositionChangedListener {
        final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        Map map;
        MapFragment mapFragment;
        PositioningManager mPositioningManager;

        GeoPosition currentPosition = null;
        MapMarker currentLocationMarker = null;
        MapMarker destinationMarker = null;
        SharedPreferences sPref;
        BroadcastReceiver newDriverBroadcastReceiver;
        BroadcastReceiver newPassengerBroadcastReceiver;
        BroadcastReceiver driverInfoBroadcastReceiver;
        BroadcastReceiver connectedBroadcastReceiver;
        MapRoute mapRoute;
        Context context;
        Button btFindCar;
        EditText etFindPlace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

    checkPermissions();
}

    int first = 1;
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                initialize();
                break;
        }
    }
    private void initialize() {
        mapFragment = (MapFragment)
                getFragmentManager().findFragmentById(R.id.mapfragment);
        context = this;
        etFindPlace = findViewById(R.id.editText3);
        etFindPlace.setOnKeyListener(new View.OnKeyListener()
        {
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if(event.getAction() == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_ENTER))
                {
                    geoCodeRequest(etFindPlace.getText().toString());
                    return true;
                }
                return false;
            }
        });
        btFindCar = findViewById(R.id.button2);
        btFindCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*  findCar();*/
                //WiFiClient.getDriver(String.valueOf(3000));
                Gson g = new Gson();
                String json = g.toJson(mapRoute);
                /*   clearRoute();*/
/*                MapRoute m = g.fromJson(json, MapRoute.class);
                map.addMapObject(m);*/
            }
        });
        sPref = getPreferences(MODE_PRIVATE);
        if(sPref.contains("IP")) {
            String ip = sPref.getString("IP", "");
          /*  WiFiClient = new WiFiClient(this, ip);
            Toast.makeText(this, ip, Toast.LENGTH_SHORT).show();*/
        }
        /*  connectToWiFiNetwork();*/
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();
                    // Set the map zoom level to the average between min and max (no animation)
                    map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
                    mapFragment.getMapGesture().addOnGestureListener(MapActivity.this);
                    mPositioningManager = PositioningManager.getInstance();
                    mPositioningManager.addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(
                            MapActivity.this));
                    // start position updates, accepting GPS, network or indoor positions
                    if (mPositioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK)) {
                        //mapFragment.getPositionIndicator().setVisible(true);


                        Toast.makeText(MapActivity.this, "PositioningManager.start", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MapActivity.this, "PositioningManager.start: failed, exiting", Toast.LENGTH_LONG).show();
                        finish();
                    }
                } else {
                    Log.e("ERROR", "Cannot initialize MapFragment (" + error.toString() + ")");
                }
            }
        });
    }
    private void geoCodeRequest(String query) {
        if(currentPosition == null) {
            return;
        }
        GeocodeRequest2 geocodeRequest = new GeocodeRequest2(query);
        geocodeRequest.setSearchArea(currentPosition.getCoordinate(), 5000);
        geocodeRequest.execute(new ResultListener<List<GeocodeResult>>() {
            @Override
            public void onCompleted(List<GeocodeResult> results, ErrorCode errorCode) {
                if (errorCode == ErrorCode.NONE) {
                    /*
                     * From the result object, we retrieve the location and its coordinate and
                     * display to the screen. Please refer to HERE Android SDK doc for other
                     * supported APIs.
                     */
                    //TODO: обработать все пришедшие результаты
                    /*StringBuilder sb = new StringBuilder();
                    for (GeocodeResult result : results) {
                        sb.append(result.getLocation().getCoordinate().toString());
                        sb.append("\n");
                    }*/
                    if(results.isEmpty()){
                        //  Toast.makeText(context, "Адрес не найден", Toast.LENGTH_LONG).show();
                        return;
                    }
                    GeocodeResult result = results.get(0);
                    Image marker_img = new Image();
                    try {
                        marker_img.setImageResource(R.drawable.marker);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    /*  clearRoute();*/
                    destinationMarker = new MapMarker(result.getLocation().getCoordinate(), marker_img);
                    // add a MapMarker to current active map.
                    map.addMapObject(destinationMarker);

                    /*     createRoute(result.getLocation().getCoordinate());*/
                    // Toast.makeText(context, sb.toString(), Toast.LENGTH_SHORT).show();
                } else {
                    //  Toast.makeText(context, "Ошибка распознавания адреса:" + errorCode, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createRoute(GeoCoordinate destination) {
        if(currentPosition == null) {
            Toast.makeText(context, "Текущее местоположение не определено", Toast.LENGTH_LONG).show();
            return;
        }
        if(destination == null) {
            return;
        }
        RouteManager rm = new RouteManager();

        RoutePlan routePlan = new RoutePlan();
        routePlan.addWaypoint(currentPosition.getCoordinate());
        routePlan.addWaypoint(destination);

        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);

        routePlan.setRouteOptions(routeOptions);

        rm.calculateRoute(routePlan, new RouteListener());
    }
private class RouteListener implements RouteManager.Listener {

    // Method defined in Listener
    public void onProgress(int percentage) {
        // Display a message indicating calculation progress
    }

    @Override
    // Method defined in Listener
    public void onCalculateRouteFinished(RouteManager.Error error, List<RouteResult> routeResult) {
        // If the route was calculated successfully
        if (error == RouteManager.Error.NONE) {
            // Render the route on the map
            mapRoute = new MapRoute(routeResult.get(0).getRoute());
            map.addMapObject(mapRoute);
        }
        else {
            // Display a message indicating route calculation failure
        }
    }
}
    private void clearRoute() {
        if(destinationMarker != null) {
            map.removeMapObject(destinationMarker);
            destinationMarker = null;
        }
        if(mapRoute != null) {
            map.removeMapObject(mapRoute);
            mapRoute = null;
        }
    }

    @Override
    public void onPositionUpdated(PositioningManager.LocationMethod locationMethod, GeoPosition geoPosition, boolean b) {
        if(currentPosition == null) {
            currentPosition = geoPosition;
        }
        else {

        }
        GeoCoordinate geoCoordinate = geoPosition.getCoordinate();
        if(first == 1) {
            map.setCenter(geoCoordinate,
                    Map.Animation.NONE);
            first++;
        }

        Image marker_img = new Image();
        try {
            marker_img.setImageResource(R.drawable.circle);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(currentLocationMarker != null) {
            map.removeMapObject(currentLocationMarker);
        }
        // create a MapMarker centered at current location with png image.
        currentLocationMarker = new MapMarker(currentPosition.getCoordinate(), marker_img);
        currentLocationMarker.setTitle("Я");
        // add a MapMarker to current active map.
        map.addMapObject(currentLocationMarker);
    }


    @Override
    public void onPositionFixChanged(PositioningManager.LocationMethod locationMethod, PositioningManager.LocationStatus locationStatus) {

    }

    @Override
    public void onPanStart() {

    }

    @Override
    public void onPanEnd() {

    }

    @Override
    public void onMultiFingerManipulationStart() {

    }

    @Override
    public void onMultiFingerManipulationEnd() {

    }

    @Override
    public boolean onMapObjectsSelected(List<ViewObject> list) {
        for (ViewObject viewObject : list) {
            if (viewObject.getBaseType() == ViewObject.Type.USER_OBJECT) {
                MapObject mapObject = (MapObject) viewObject;

                if (mapObject.getType() == MapObject.Type.MARKER) {

                    MapMarker window_marker = ((MapMarker) mapObject);
                    Toast.makeText(context, window_marker.getTitle(), Toast.LENGTH_SHORT).show();
                    // CustomDialogClass cdd=new CustomDialogClass(Values.this);
                    // cdd.show();
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onTapEvent(PointF pointF) {


        Log.d("G_",pointF.toString());
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(PointF pointF) {
        Image marker_img = new Image();
        try {
            marker_img.setImageResource(R.drawable.circle1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        GeoCoordinate geoCoordinate=map.pixelToGeo(pointF);
        MapMarker testMarker = new MapMarker(geoCoordinate,marker_img);
        testMarker.setDescription("rr");
        testMarker.setTitle("Покормите кота!");
// add a MapMarker to current active map.
        map.addMapObject(testMarker);


        Log.d("TUL",pointF.toString());
        ShowDialog();
        return false;
    }
    public void ShowDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
        builder.setTitle("Важное сообщение!")
                .setMessage("Покормите кота!")
                .setIcon(R.drawable.circle1)
                .setCancelable(false)
                .setNegativeButton("ОК, иду на кухню",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onPinchLocked() {

    }

    @Override
    public boolean onPinchZoomEvent(float v, PointF pointF) {
        return false;
    }

    @Override
    public void onRotateLocked() {

    }

    @Override
    public boolean onRotateEvent(float v) {
        return false;
    }

    @Override
    public boolean onTiltEvent(float v) {
        return false;
    }

    @Override
    public boolean onLongPressEvent(PointF pointF) {
        return false;
    }

    @Override
    public void onLongPressRelease() {

    }

    @Override
    public boolean onTwoFingerTapEvent(PointF pointF) {
        return false;
    }

    // void intend claasпш
    public void newTODO(View view)
    {
        Intent intent = new Intent(this, ToDolist.class);
        startActivity(intent);
    }
}