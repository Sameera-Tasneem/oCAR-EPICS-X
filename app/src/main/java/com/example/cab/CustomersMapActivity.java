package com.example.cab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,com.google.android.gms.location.LocationListener
 {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    Marker DriverMarker, PickUpMarker;
    private DatabaseReference CustomerDatabaseRef;
    private DatabaseReference DriverAvailableRef;
    private DatabaseReference DriversRef;
    private DatabaseReference DriverLocationRef;


   private Button CustomerLogoutButton;
   private Button CustomerCallServiceButton;
   private String customerID;
   private Button SettingsButton;
   private LatLng CustomerPickUpLocation;
   private int radius = 1;
   private Boolean driverFound=false, requestType=false;
   private String driverFoundID;
   GeoQuery geoQuery;

   private ValueEventListener DriverLocationRefListener;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);

        mAuth=FirebaseAuth.getInstance();
        currentUser=mAuth.getCurrentUser();
        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customer Requests");
        DriverAvailableRef = FirebaseDatabase.getInstance().getReference().child("Drivers/Garages Available");
        DriverLocationRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");

        CustomerLogoutButton = (Button) findViewById(R.id.customer_logout_btn);
        CustomerCallServiceButton = (Button) findViewById(R.id.customer_call_service_btn);
        SettingsButton = (Button) findViewById(R.id.customer_settings_btn);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        SettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent=new Intent(CustomersMapActivity.this,SettingsActivity.class);
                intent.putExtra("type","Customers");
                startActivity(intent);
            }
        });

        CustomerLogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                mAuth.signOut();
                LogoutCustomer();
            }
        });


        CustomerCallServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (requestType)
                {
                    requestType = false;
                    geoQuery.removeAllListeners();
                    DriverLocationRef.removeEventListener(DriverLocationRefListener);

                    if (driverFound != null )
                    {
                        DriversRef= FirebaseDatabase.getInstance().getReference()
                                .child("Users").child("Drivers").child(driverFoundID).child("CustomerReqID");
                        DriversRef.removeValue();
                        driverFoundID = null;

                    }
                    driverFound=false;
                    radius=1;

                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    GeoFire geoFire=new GeoFire(CustomerDatabaseRef);
                    geoFire.removeLocation(customerID);

                    if (PickUpMarker != null)
                    {
                    PickUpMarker.remove();
                    }

                    if (DriverMarker != null)
                    {
                        DriverMarker.remove();
                    }

                    CustomerCallServiceButton.setText("Call a service");
                }


                else
                {
                    requestType=true;
                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    GeoFire geoFire=new GeoFire(CustomerDatabaseRef);
                    geoFire.setLocation(customerID,new GeoLocation(lastLocation.getLatitude(),lastLocation.getLongitude()));

                    CustomerPickUpLocation= new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(CustomerPickUpLocation).title("My Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                    CustomerCallServiceButton.setText("Getting your Service/Driver.....");
                    GetClosestService();
                }



            }
        });
    }

     private void GetClosestService()
     {
         GeoFire geoFire=new GeoFire(DriverAvailableRef);
         GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerPickUpLocation.latitude,CustomerPickUpLocation.longitude),radius);
         geoQuery.removeAllListeners();


         geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
             @Override
             public void onKeyEntered(String key, GeoLocation location)
             {
                if (!driverFound && requestType)
                {
                    driverFound = true;
                    driverFoundID = key;

                    DriversRef =FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerReqID",customerID);
                    DriversRef.updateChildren(driverMap);



                    GettingDriverLocation();
                    CustomerCallServiceButton.setText("Looking for garage/driver Location....");
                }

             }

             @Override
             public void onKeyExited(String key) {

             }

             @Override
             public void onKeyMoved(String key, GeoLocation location) {

             }

             @Override
             public void onGeoQueryReady()
             {
                 if (!driverFound)
                 {
                     radius = radius + 1;
                     GetClosestService();
                 }
             }

             @Override
             public void onGeoQueryError(DatabaseError error) {

             }
         });
     }

     private void GettingDriverLocation()
     {
        DriverLocationRefListener= DriverLocationRef.child(driverFoundID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                        if (dataSnapshot.exists() && requestType) {
                            List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;
                            CustomerCallServiceButton.setText("Garage/Driver Found");

                            if(driverLocationMap.get(0)!=null)
                            {
                                LocationLat =Double.parseDouble(driverLocationMap.get(0).toString());
                            }
                            if(driverLocationMap.get(1)!=null)
                            {
                                LocationLng =Double.parseDouble(driverLocationMap.get(1).toString());
                            }

                            LatLng DriverLatLng = new LatLng(LocationLat,LocationLng);
                            if (DriverMarker != null)
                            {
                                DriverMarker.remove();
                            }

                                Location location1=new Location("");
                                location1.setLatitude(CustomerPickUpLocation.latitude);
                                location1.setLongitude(CustomerPickUpLocation.longitude);


                                Location location2=new Location("");
                                location2.setLatitude(DriverLatLng.latitude);
                                location2.setLongitude(DriverLatLng.longitude);

                                float Distance = location1.distanceTo(location2);

                                if (Distance < 90)
                                {
                                    CustomerCallServiceButton.setText("Driver Arrived");
                                }
                                else
                                {
                                    CustomerCallServiceButton.setText("Driver/Garage Found:"+String.valueOf(Distance));
                                }


                            DriverMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Your Driver/Garage is here").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));


                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
     }


     @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap = googleMap;
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,locationRequest,this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location)
    {
        lastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
    }
    protected synchronized void buildGoogleApiClient()
    {
        googleApiClient =new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();
    }
    @Override
    protected void onStop()
    {
        super.onStop();

    }

     private void LogoutCustomer()
     {
         Intent welcomeIntent= new Intent(CustomersMapActivity.this,WelcomeActivity.class);
         welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
         startActivity(welcomeIntent);
         finish();
     }
}
