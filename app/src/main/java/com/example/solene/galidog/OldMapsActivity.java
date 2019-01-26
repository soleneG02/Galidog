package com.example.solene.galidog;



import android.graphics.Color;
import android.os.CountDownTimer;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.sqrt;

public class OldMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Button btnStartPath;
    private ArrayList<CommandeVocale> listeCommandes;
    private ArrayList<Point> listePoints;
    private Marker marker;
    private PolylineOptions dessin = new PolylineOptions().width(9).color(Color.BLUE);
    private Polyline dessinTrajet;
    private ArrayList<LatLng> listeCoord = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_old_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        listeCommandes = getIntent().getParcelableArrayListExtra("commandes");
        listePoints = getIntent().getParcelableArrayListExtra("points");

        btnStartPath = (Button) findViewById(R.id.activity_main_btn_start_path);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        dessinTrajet = mMap.addPolyline(dessin);

        pathView(); // afficher le trajet enregistré

        androidFirstLocation();   //se positionner

        /* début du trajet */
        btnStartPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast toast = Toast.makeText(OldMapsActivity.this, "Le trajet démarre", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                androidUpdateLocation();
                btnStartPath.setText("Arrêter le trajet");
                btnStartPath.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onPause();
                        Toast.makeText(OldMapsActivity.this, "Trajet termminé", Toast.LENGTH_SHORT).show();
                        btnStartPath.setText("Retour à l'accueil");
                        btnStartPath.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent retourAccueil = new Intent(OldMapsActivity.this, MainActivity.class);
                                startActivity(retourAccueil);
                            }
                        });

                    }
                });

            }
        });

    }

    private LocationManager androidLocationManager;
    private LocationListener androidLocationListener;
    private final static int REQUEST_CODE_UPDATE_LOCATION = 42;
    private boolean commandeCreee = false;
    private int compteurCommande = 0;
    private Point pointSuivant;




    public void pathView() {
        /* cette fonction affiche le chemin qui est enregistré*/

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (int i = 0; i < listePoints.size(); i++) {
            //la taille : parcourir la liste une premiere fois pour voir le premier element non nul??
            Point pointChemin = listePoints.get(i);
            LatLng coordonnees = new LatLng(pointChemin.getLatitude(), pointChemin.getLongitude());
            listeCoord.add(coordonnees);
            dessinTrajet.setPoints(listeCoord);
            BitmapDescriptor point1 = BitmapDescriptorFactory.fromResource(R.drawable.point2_trajet);
            mMap.addMarker(new MarkerOptions().position(coordonnees).alpha(0.7f).icon(point1));
            builder.include(coordonnees);
        }
        LatLngBounds bounds = builder.build();
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 0);
        mMap.animateCamera(cu);  //ajuste la caméra sur l'ensemble des points
    }



    public void androidFirstLocation() {
        /*
        Cette fonction affiche la géolocalisation de l'utilisateur lorsqu'il arrive pour la première fois sur la page.
         */
        if (ActivityCompat.checkSelfPermission(OldMapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    OldMapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_UPDATE_LOCATION);
        } else {
            androidLocationManager = (LocationManager) this.getSystemService(OldMapsActivity.this.LOCATION_SERVICE);
            androidLocationListener = new LocationListener() {
                public void onLocationChanged(Location loc) {
                    /* Affichage des coordonnées & création d'un marqueur */
                    double latNow = loc.getLatitude();
                    double lonNow = loc.getLongitude();
                    Toast.makeText(OldMapsActivity.this, "Coordonnées : " + latNow + " / " + lonNow, Toast.LENGTH_SHORT).show();
                    LatLng youAreHere = new LatLng(latNow, lonNow);
                    Point newPoint = new Point(latNow, lonNow);
                    listePoints.add(newPoint);
                    listeCoord.add(new LatLng(latNow,lonNow));
                    BitmapDescriptor point2 = BitmapDescriptorFactory.fromResource(R.drawable.point2_init);
                    marker = mMap.addMarker(new MarkerOptions().position(youAreHere).title("Vous êtes ici").icon(point2));
                    int padding = 15;
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(youAreHere, padding));
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProviderEnabled(String provider) {}

                public void onProviderDisabled(String provider) {}
            };

            /* Requête unique (première géolocalisation) */
            androidLocationManager.requestSingleUpdate(
                    LocationManager.GPS_PROVIDER,
                    androidLocationListener,
                    null);
        }
    }

    public void androidUpdateLocation() {
        /*
        cette fonction actualise la position et l'affiche au fur et à mesure que l'utilisateur avance */

        if (ActivityCompat.checkSelfPermission(OldMapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    OldMapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_UPDATE_LOCATION);
        } else {
            androidLocationManager = (LocationManager) this.getSystemService(OldMapsActivity.this.LOCATION_SERVICE);
            androidLocationListener = new LocationListener() {
                public void onLocationChanged(Location loc) {

                    // Récupération de la localisation
                    double latNow = loc.getLatitude();
                    double lonNow = loc.getLongitude();
                    LatLng youAreHere = new LatLng(latNow, lonNow);

                    // affichage
                    listeCoord.add(new LatLng(latNow,lonNow));
                    BitmapDescriptor point3 = BitmapDescriptorFactory.fromResource(R.drawable.point2_init);
                    marker = mMap.addMarker(new MarkerOptions().position(youAreHere).icon(point3));

                    if (listeCommandes.size() != 0) {
                    CommandeVocale commande = listeCommandes.get(0);
                    double latComm = commande.getLatitude();
                    double lonComm = commande.getLongitude();

                    // lecture de la commande vocale si on s'approche suffisement près
                    if (sqrt(Math.pow(latNow-lonNow,2)+Math.pow(latComm-lonComm,2))<3) {
                        commande.initCommande(OldMapsActivity.this);
                        listeCommandes.remove(0);
                    } }
                    new CountDownTimer(500, 1) {
                        public void onFinish() {
                            // When timer is finished
                            // Execute your code here
                            marker.remove();
                        }

                        public void onTick(long millisUntilFinished) {
                            // millisUntilFinished    The amount of time until finished.
                        }
                    }.start();


                }
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                public void onProviderEnabled(String provider) {}
                public void onProviderDisabled(String provider) {}
            };

            androidLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10, // en millisecondes
                    1, // en mètres
                    androidLocationListener);

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        /*
        Cette fonction vérifie que le téléphone possède bien les autorisations pour capter la localisation.
         */
        switch (requestCode) {
            case REQUEST_CODE_UPDATE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    androidFirstLocation();
                    androidUpdateLocation();
                } else {
                    Toast.makeText(OldMapsActivity.this, "Permission refusée.", Toast.LENGTH_LONG).show();
                }
                return;
        };
    }

    @Override
    protected void onPause() {
        /*
        Cette fonction stoppe les fonctions en cours en cas de changement d'orientation ou de fermeture de l'application.
         */
        super.onPause();
        if (androidLocationListener != null) {
            if (androidLocationManager == null) {
                androidLocationManager = (LocationManager) this.getSystemService(OldMapsActivity.this.LOCATION_SERVICE);
            }
            androidLocationManager.removeUpdates(androidLocationListener);
            androidLocationManager = null;
            androidLocationListener = null;
        }
    }

}
