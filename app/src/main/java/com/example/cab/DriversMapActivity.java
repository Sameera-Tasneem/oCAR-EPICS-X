package com.example.cab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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

import java.util.List;

public class DriversMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;

    private Button LogoutDriverButton;
    private Button SettingsDriverButton;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Boolean currentLogoutDriverStatus=false;
    private DatabaseReference AssignedCustomerRef,AssignedCustomerPickUpRef;
    private String DriverID,CustomerID="";
    Marker PickUpMarker;
    private ValueEventListener AssignedCustomerPickUpRefListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drivers_map2);


        LogoutDriverButton =(Button) findViewById(R.id.driver_logout_btn);
        SettingsDriverButton= (Button) findViewById(R.id.driver_settings_btn);

        mAuth=FirebaseAuth.getInstance();
        currentUser=mAuth.getCurrentUser();
        DriverID = mAuth.getCurrentUser().getUid();


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        SettingsDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent=new Intent(DriversMapActivity.this,SettingsActivity.class);
                intent.putExtra("type","Drivers");
                startActivity(intent);
            }
        });


        LogoutDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                currentLogoutDriverStatus=true;
                DisconnectTheDriver();
                mAuth.signOut();

                LogoutDriver();
            }
        });
            GetAssignedCustomerRequest();

    }

    private void GetAssignedCustomerRequest()
    {
        AssignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(DriverID)
                .child("CustomerReqID");
        AssignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
                    CustomerID = dataSnapshot.getValue().toString();

                    GetAssignedCustomerPickUpLocation();
                }
                else
                {
                    CustomerID="";

                    if (PickUpMarker != null){
                        PickUpMarker.remove();
                    }

                    if (AssignedCustomerPickUpRefListener != null)
                    {
                        AssignedCustomerPickUpRef.removeEventListener(AssignedCustomerPickUpRefListener);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void GetAssignedCustomerPickUpLocation()
    {
        AssignedCustomerPickUpRef = FirebaseDatabase.getInstance().getReference().child("Customer Requests")
                .child(CustomerID).child("l");

        AssignedCustomerPickUpRefListener= AssignedCustomerPickUpRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists()){
                    List<Object> customerLocationMap = (List<Object>) dataSnapshot.getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;


                    if(customerLocationMap.get(0)!=null)
                    {
                        LocationLat =Double.parseDouble(customerLocationMap.get(0).toString());
                    }
                    if(customerLocationMap.get(1)!=null)
                    {
                        LocationLng =Double.parseDouble(customerLocationMap.get(1).toString());
                    }
                    LatLng DriverLatLng = new LatLng(LocationLat,LocationLng);
                    PickUpMarker= mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Customer Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }



    @Override
    public void onMapReady(GoogleMap googleMap)
    {
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
        if (getApplicationContext()!=null)
        {
            lastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference DriverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");

            GeoFire geoFireAvailability = new GeoFire(DriverAvailabilityRef);

            DatabaseReference DriverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
            GeoFire geoFireWorking = new GeoFire(DriverWorkingRef);

            switch (CustomerID)
            {
                case "":
                    geoFireWorking.removeLocation(userID);
                    geoFireAvailability.setLocation(userID,new GeoLocation(location.getLatitude(),location.getLongitude()));
                    break;

                default:
                    geoFireAvailability.removeLocation(userID);
                    geoFireWorking.setLocation(userID,new GeoLocation(location.getLatitude(),location.getLongitude()));
                    break;

            }


        }


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

        if(!currentLogoutDriverStatus)
        {
            DisconnectTheDriver();
        }

    }

    private void DisconnectTheDriver()
    {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference DriverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");

        GeoFire geoFire = new GeoFire(DriverAvailabilityRef);
        geoFire.removeLocation(userID);
    }
    private void LogoutDriver()
    {
        Intent welcomeIntent= new Intent(DriversMapActivity.this,WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }


}
