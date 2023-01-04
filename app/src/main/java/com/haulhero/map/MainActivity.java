package com.haulhero.map;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.tasks.geocode.SuggestResult;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private final String COLUMN_NAME_ADDRESS = "address";
    String sourceAddr = "", point1Lat, point1Long, point2Lat, point2Long;
    private final String[] mColumnNames = {BaseColumns._ID, COLUMN_NAME_ADDRESS};

    private SearchView mAddressSourceView;// mAddressDestinationView;
    private LocatorTask mLocatorTask;
    private GeocodeParameters mAddressGeocodeParameters;
    TextView sourceLatLang;// destinationLatLang;
    Button navigateBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        startActivity(new Intent(MainActivity.this, NavigationActivity.class));

        ArcGISRuntimeEnvironment.setApiKey(getString(R.string.api_key));

//        mAddressDestinationView = findViewById(R.id.addressDestinationView);
//        mAddressDestinationView.setIconified(false);
//        mAddressDestinationView.setFocusable(false);
//        mAddressDestinationView.setQueryHint("Search Destination");

        mAddressSourceView = findViewById(R.id.addressSourceView);
        mAddressSourceView.setIconified(false);
        mAddressSourceView.setFocusable(false);
        mAddressSourceView.setQueryHint("Search Source");

        sourceLatLang = findViewById(R.id.sourceLatLang);
//        destinationLatLang = findViewById(R.id.destinationLatLang);
        navigateBtn = findViewById(R.id.navigateBtn);

        mLocatorTask = new LocatorTask("https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer");

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 100);
        }

        CheckGPSStatus();

//        setupAddressSearchView();

        navigateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!point1Lat.isEmpty() && !point1Long.isEmpty()) {
                    Intent intent = new Intent(MainActivity.this, NavigationActivity.class);
                    intent.putExtra("Point1Lat", point1Lat);
                    intent.putExtra("Point1Long", point1Long);
                    startActivity(intent);
                } else {
                    Log.e("TAG", "calling");
                }
            }
        });
    }

    private void CheckGPSStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsStatus) {
            buildAlertMessageNoGps();
        }
        setupAddressSearchView();
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        finish();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void setupAddressSearchView() {
        mAddressGeocodeParameters = new GeocodeParameters();

        mAddressGeocodeParameters.getResultAttributeNames().add("PlaceName");
        mAddressGeocodeParameters.getResultAttributeNames().add("Place_addr");

        mAddressGeocodeParameters.setMaxResults(1);

        mAddressSourceView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                geoCodeTypedAddress(query, "source");
                mAddressSourceView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!newText.equals("")) {
                    final ListenableFuture<List<SuggestResult>> suggestionsFuture = mLocatorTask.suggestAsync(newText);
                    suggestionsFuture.addDoneListener(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                List<SuggestResult> suggestResults = suggestionsFuture.get();
                                MatrixCursor suggestionsCursor = new MatrixCursor(mColumnNames);
                                int key = 0;

                                for (SuggestResult result : suggestResults) {
                                    suggestionsCursor.addRow(new Object[]{key++, result.getLabel()});
                                }

                                String[] cols = new String[]{COLUMN_NAME_ADDRESS};
                                int[] to = new int[]{R.id.suggestion_address};
                                final SimpleCursorAdapter suggestionAdapter = new SimpleCursorAdapter(MainActivity.this,
                                        R.layout.find_address_suggestion, suggestionsCursor, cols, to, 0);
                                mAddressSourceView.setSuggestionsAdapter(suggestionAdapter);
                                mAddressSourceView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                                    @Override
                                    public boolean onSuggestionSelect(int position) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onSuggestionClick(int position) {
                                        MatrixCursor selectedRow = (MatrixCursor) suggestionAdapter.getItem(position);
                                        int selectedCursorIndex = selectedRow.getColumnIndex(COLUMN_NAME_ADDRESS);
                                        String address = selectedRow.getString(selectedCursorIndex);
                                        mAddressSourceView.setQuery(address, true);
                                        return true;
                                    }
                                });
                            } catch (Exception e) {
                                Log.e("TAG", "Geocode suggestion error: " + e.getMessage());
                            }
                        }
                    });
                }
                return true;
            }
        });

//        mAddressDestinationView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                geoCodeTypedAddress(query, "destination");
//                mAddressDestinationView.clearFocus();
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                if (!newText.equals("")) {
//                    final ListenableFuture<List<SuggestResult>> suggestionsFuture = mLocatorTask.suggestAsync(newText);
//                    suggestionsFuture.addDoneListener(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                List<SuggestResult> suggestResults = suggestionsFuture.get();
//                                MatrixCursor suggestionsCursor = new MatrixCursor(mColumnNames);
//                                int key = 0;
//
//                                for (SuggestResult result : suggestResults) {
//                                    suggestionsCursor.addRow(new Object[]{key++, result.getLabel()});
//                                }
//
//                                String[] col = new String[]{COLUMN_NAME_ADDRESS};
//                                int[] too = new int[]{R.id.suggestion_address};
//                                final SimpleCursorAdapter suggestion = new SimpleCursorAdapter(MainActivity.this,
//                                        R.layout.find_address_suggestion, suggestionsCursor, col, too, 0);
//                                mAddressDestinationView.setSuggestionsAdapter(suggestion);
//                                mAddressDestinationView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
//                                    @Override
//                                    public boolean onSuggestionSelect(int position) {
//                                        return false;
//                                    }
//
//                                    @Override
//                                    public boolean onSuggestionClick(int position) {
//                                        MatrixCursor selectedRow = (MatrixCursor) suggestion.getItem(position);
//                                        int selectedCursorIndex = selectedRow.getColumnIndex(COLUMN_NAME_ADDRESS);
//                                        String address = selectedRow.getString(selectedCursorIndex);
//                                        mAddressDestinationView.setQuery(address, true);
//                                        return true;
//                                    }
//                                });
//                            } catch (Exception e) {
//                                Log.e("TAG", "Geocode suggestion error: " + e.getMessage());
//                            }
//                        }
//                    });
//                }
//                return true;
//            }
//        });

    }

    private void geoCodeTypedAddress(final String address, String route) {
        if (address != null) {
            Log.e("TAG", "called");
            mLocatorTask.addDoneLoadingListener(new Runnable() {
                @Override
                public void run() {
                    if (mLocatorTask.getLoadStatus() == LoadStatus.LOADED) {
                        Log.e("TAG", "called1");
                        final ListenableFuture<List<GeocodeResult>> geocodeResultListenableFuture = mLocatorTask
                                .geocodeAsync(address, mAddressGeocodeParameters);
                        geocodeResultListenableFuture.addDoneListener(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    List<GeocodeResult> geocodeResults = geocodeResultListenableFuture.get();
                                    if (geocodeResults.size() > 0) {
                                        if (Objects.equals(route, "source")) {
                                            point1Lat = String.valueOf(geocodeResults.get(0).getDisplayLocation().getY());
                                            point1Long = String.valueOf(geocodeResults.get(0).getDisplayLocation().getX());
                                            sourceAddr = "Source Location:\nLatitude: " + point1Lat +
                                                    "\nLongitude: " + point1Long;
                                            sourceLatLang.setText(sourceAddr);
                                        }
//                                        else {
//                                            point2Lat = String.valueOf(geocodeResults.get(0).getDisplayLocation().getY());
//                                            point2Long = String.valueOf(geocodeResults.get(0).getDisplayLocation().getX());
//                                            destinationAddr = "Source Location:\nLatitude: " + point1Lat +
//                                                    "\nLongitude: " + point2Long;
//                                            destinationLatLang.setText(destinationAddr);
//                                        }
                                        if (!sourceAddr.isEmpty()) {
                                            navigateBtn.setVisibility(View.VISIBLE);
                                        }
                                    } else {
                                        Log.e("TAG", "called6");
                                        Toast.makeText(getApplicationContext(), "location_not_found " + address,
                                                Toast.LENGTH_LONG).show();
                                    }
                                } catch (InterruptedException | ExecutionException e) {
                                    Log.e("TAG", "Geocode error: " + e.getMessage());
                                    Toast.makeText(getApplicationContext(), "geo_locate_error", Toast.LENGTH_LONG)
                                            .show();
                                }
                            }
                        });
                    } else {
                        Log.i("TAG", "Trying to reload locator task");
                        mLocatorTask.retryLoadAsync();
                    }
                }
            });
            mLocatorTask.loadAsync();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}