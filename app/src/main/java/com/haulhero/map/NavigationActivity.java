package com.haulhero.map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.Resources;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.location.LocationDataSource;
import com.esri.arcgisruntime.location.RouteTrackerLocationDataSource;
import com.esri.arcgisruntime.location.SimulatedLocationDataSource;
import com.esri.arcgisruntime.location.SimulationParameters;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.navigation.DestinationStatus;
import com.esri.arcgisruntime.navigation.ReroutingParameters;
import com.esri.arcgisruntime.navigation.RouteTracker;
import com.esri.arcgisruntime.navigation.TrackingStatus;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;
import com.google.android.material.navigation.NavigationView;

import android.speech.tts.TextToSpeech;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class NavigationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private MapView mapView;
    private RouteTracker routeTracker;
    double point1Lat, point1Long, point2Lat, point2Long, velocity = 0.00;
    float a, speed;

    private Button mRecenterButton;
    private TextToSpeech mTextToSpeech;
    private boolean mIsTextToSpeechInitialized = false;

    DrawerLayout drawer1, drawer2;
    ImageButton imgBtn, img, addBtn, removeBtn;
    Button placeHolder1;
    TextView speedView;
    ConstraintLayout layout;

    Graphic ahead, traveled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        drawer1 = findViewById(R.id.drawer1);
        drawer2 = findViewById(R.id.drawer2);
        imgBtn = findViewById(R.id.img_btn);
        img = findViewById(R.id.image1111);

        placeHolder1 = findViewById(R.id.placeHolder1);
        layout = findViewById(R.id.speed_view_layout);
        addBtn = findViewById(R.id.add_btn);
        removeBtn = findViewById(R.id.remove_btn);
        speedView = findViewById(R.id.speeds_view);

        a = 1;
        speed = 0;

        onLayoutClick();

        point1Lat = 0;
        point1Long = 0;

        mapView = findViewById(R.id.mapView);

        ArcGISRuntimeEnvironment.setApiKey(getString(R.string.api_key));

        String portalItemId = "6d1a7513381e4048b9eda3d73fb60c57";
        Portal portal = new Portal("https://siqapxbs1j9b6pu2.maps.arcgis.com/");
        PortalItem portalItem = new PortalItem(portal, portalItemId);

        long layerId = 0;
        FeatureLayer layer = new FeatureLayer(portalItem, layerId);

        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_STREETS);
        map.getOperationalLayers().add(layer);
        mapView.setMap(map);

        portalItem.addDoneLoadingListener(() -> {
            if (portalItem.getLoadStatus() != LoadStatus.LOADED) {
                Toast.makeText(NavigationActivity.this, "Not Loaded", Toast.LENGTH_SHORT).show();
                Log.e("TAD", "Not Loaded");
            } else {
//                Log.e("TAD", portalItem.getTitle());
//                Log.e("TAD", portalItem.toJson());
            }
        });

        if (ContextCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(NavigationActivity.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 100);
        }

        Intent intent = getIntent();
        point2Lat = Double.parseDouble(intent.getStringExtra("Point1Lat"));
        point2Long = Double.parseDouble(intent.getStringExtra("Point1Long"));
        /// my current location
        LocationDisplay locationDisplay1 = mapView.getLocationDisplay();
        locationDisplay1.setAutoPanMode(LocationDisplay.AutoPanMode.NAVIGATION);
        locationDisplay1.startAsync();
        locationDisplay1.addLocationChangedListener(locationChangedEvent -> {
            point1Lat = locationChangedEvent.getLocation().getPosition().getY();
            point1Long = locationChangedEvent.getLocation().getPosition().getX();

            velocity = locationChangedEvent.getLocation().getVelocity();
            Formatter formatter = new Formatter();
            formatter.format("%.2f", velocity);

            speedView.setText(formatter.toString());


            ////extra
        });


//        point2Lat = 23.9372164;
//        point2Long = 90.2713285;

        GraphicsOverlay graphicsOverlay = new GraphicsOverlay();
        mapView.getGraphicsOverlays().add(graphicsOverlay);

        mapView.getGraphicsOverlays().get(0).getGraphics().clear();
        RouteTask routeTask = new RouteTask(this, getString(R.string.route_api));
        ListenableFuture<RouteParameters> routeParametersListenableFuture = routeTask.createDefaultParametersAsync();

        routeParametersListenableFuture.addDoneListener(() -> {
            try {
                RouteParameters routeParameters = routeParametersListenableFuture.get();
                routeParameters.setStops(getStops(point1Lat, point1Long, point2Lat, point2Long));
                routeParameters.setReturnDirections(true);
                routeParameters.setReturnStops(true);
                routeParameters.setReturnRoutes(true);

                ListenableFuture<RouteResult> routeResultListenableFuture = routeTask.solveRouteAsync(routeParameters);
                routeParametersListenableFuture.addDoneListener(() -> {
                    try {
                        RouteResult routeResult = routeResultListenableFuture.get();
                        Polyline routeGeometry = routeResult.getRoutes().get(0).getRouteGeometry();

                        Graphic routeGraphic = new Graphic(routeGeometry,
                                new SimpleLineSymbol(SimpleLineSymbol.Style.DASH, Color.MAGENTA, 5f));
                        mapView.getGraphicsOverlays().get(0).getGraphics().add(routeGraphic);
                        mapView.setViewpointAsync(new Viewpoint(routeGeometry.getExtent()));
                        if (velocity > 0)
                            startNavigation(routeTask, routeParameters, routeResult);
                    } catch (InterruptedException | ExecutionException e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (InterruptedException | ExecutionException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // recenter button
        mRecenterButton = findViewById(R.id.recenterButton);
        mRecenterButton.setVisibility(View.GONE);
        mRecenterButton.setOnClickListener(v -> {
            mapView.getLocationDisplay().setAutoPanMode(LocationDisplay.AutoPanMode.NAVIGATION);
            mRecenterButton.setEnabled(false);
            mRecenterButton.setVisibility(View.GONE);
        });

        // initialize text-to-speech to replay navigation voice guidance
        mTextToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                mTextToSpeech.setLanguage(Resources.getSystem().getConfiguration().locale);
                mIsTextToSpeechInitialized = true;
            }
        });
    }

    private static List<Stop> getStops(double point1Lat, double point1Long, double point2Lat, double point2Long) {
        List<Stop> stops = new ArrayList<>(2);

        Point point3 = new Point(point1Long, point1Lat, SpatialReferences.getWgs84());
        Point point4 = new Point(point2Long, point2Lat, SpatialReferences.getWgs84());

        Stop stop1 = new Stop(point3);
        Stop stop2 = new Stop(point4);

        stops.add(stop1);
        stops.add(stop2);
        return stops;
    }

    private void startNavigation(RouteTask routeTask, RouteParameters routeParameters, RouteResult routeResult) {
        mapView.getGraphicsOverlays().get(0).getGraphics().clear();

        Polyline routeGeometry = routeResult.getRoutes().get(0).getRouteGeometry();

        Graphic ahead = new Graphic(routeGeometry, new SimpleLineSymbol(SimpleLineSymbol.Style.DASH, Color.MAGENTA, 5f));
        mapView.getGraphicsOverlays().get(0).getGraphics().add(ahead);

        Graphic traveled = new Graphic(routeGeometry, new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 5f));
        mapView.getGraphicsOverlays().get(0).getGraphics().add(traveled);

        LocationDisplay locationDisplay = mapView.getLocationDisplay();
        SimulatedLocationDataSource dataSource = new SimulatedLocationDataSource();
        SimulationParameters simulationParameters = new SimulationParameters(Calendar.getInstance(), velocity, 5, 5);
        dataSource.setLocations(routeGeometry, simulationParameters);

        routeTracker = new RouteTracker(getApplicationContext(), routeResult, 0, true);
        ReroutingParameters reroutingParameters = new ReroutingParameters(routeTask, routeParameters);
        routeTracker.enableReroutingAsync(reroutingParameters);

        RouteTrackerLocationDataSource routeTrackerLocationDataSource = new RouteTrackerLocationDataSource(routeTracker, dataSource);
        locationDisplay.setLocationDataSource(routeTrackerLocationDataSource);
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.NAVIGATION);


        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (a <= 3) {
                    a *= 1.5;
                    double gg = mapView.getMapScale();
                    mapView.setViewpointScaleAsync(gg / a);
                }
            }
        });

        removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (a >= 1) {
                    a /= 1.5;
                    double gg = mapView.getMapScale();
                    mapView.setViewpointScaleAsync(a * gg);
                }
            }
        });

        locationDisplay.addAutoPanModeChangedListener(autoPanModeChangedEvent -> {
            mRecenterButton.setEnabled(true);
            mRecenterButton.setVisibility(View.VISIBLE);
        });

        locationDisplay.addLocationChangedListener(locationChangedEvent -> {
            TrackingStatus trackingStatus = routeTracker.getTrackingStatus();
            ahead.setGeometry(trackingStatus.getRouteProgress().getRemainingGeometry());
//            routeTracker.trackLocationAsync(locationChangedEvent.location);
            traveled.setGeometry(trackingStatus.getRouteProgress().getTraversedGeometry());

            if (trackingStatus.getDestinationStatus() == DestinationStatus.REACHED) {
                if (routeTracker.getTrackingStatus().getRemainingDestinationCount() > 1) {
                    routeTracker.switchToNextDestinationAsync();
                } else {
                    Toast.makeText(this, "Arrived at the final destination.", Toast.LENGTH_LONG).show();
                }
            }

            TextView nextDirectionTextView = findViewById(R.id.nextLocationTxt);
            TextView nextDirectionDistanceTextView = findViewById(R.id.nextLocationDistance);
            ImageView nextDirectionImg = findViewById(R.id.direction);

            routeTracker.addNewVoiceGuidanceListener(newVoiceGuidanceEvent -> {
                // use Android's text to speech to speak the voice guidance
                speakVoiceGuidance(newVoiceGuidanceEvent.getVoiceGuidance().getText());
                String directionAndText = newVoiceGuidanceEvent.getVoiceGuidance().getText();


                String directionDistance = "";
                String directionText = "";
                String directionImgName = "";

                if (directionAndText.contains(",")) {
                    String[] directionAndTextA = directionAndText.split(",");
                    directionDistance = directionAndTextA[0];
                    directionText = directionAndTextA[1];
                    if (directionText.contains("left")) {
                        nextDirectionImg.setImageResource(R.drawable.ic_moving_left);
                    } else if (directionText.contains("right")) {
                        nextDirectionImg.setImageResource(R.drawable.ic_moving);
                    }
                } else {
                    directionText = directionAndText;
                    if (directionText.contains("left")) {
                        nextDirectionImg.setImageResource(R.drawable.ic_moving_left);
                    } else if (directionText.contains("right")) {
                        nextDirectionImg.setImageResource(R.drawable.ic_moving);
                    }
                }

                nextDirectionTextView
                        .setText(directionText);
                nextDirectionDistanceTextView
                        .setText(directionDistance);
            });
        });


        locationDisplay.startAsync();
        Toast.makeText(this, "Navigating to the first stop.", Toast.LENGTH_LONG).show();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.dispose();
    }

    /**
     * Uses Android's text to speak to say the latest voice guidance from the RouteTracker out loud.
     */
    private void speakVoiceGuidance(String voiceGuidanceText) {
        if (mIsTextToSpeechInitialized && !mTextToSpeech.isSpeaking()) {
            mTextToSpeech.speak(voiceGuidanceText, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    void onLayoutClick() {
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                placeHolder1.setVisibility(View.VISIBLE);
                placeHolder1.setBackgroundColor(getColor(R.color.orange));

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            synchronized (this) {
                                wait(10000);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        placeHolder1.setVisibility(View.GONE);
                                        placeHolder1.setBackgroundColor(getColor(R.color.transparent));
                                    }
                                });
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        super.run();
                    }
                };
                thread.start();
            }
        });

        imgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (drawer1.isDrawerOpen(GravityCompat.START)) {
                    drawer1.closeDrawer(GravityCompat.START);
                } else
                    drawer1.openDrawer(GravityCompat.START);
            }
        });

        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (drawer2.isDrawerOpen(GravityCompat.END)) {
                    drawer2.closeDrawer(GravityCompat.END);
                } else
                    drawer2.openDrawer(GravityCompat.END);
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }
}