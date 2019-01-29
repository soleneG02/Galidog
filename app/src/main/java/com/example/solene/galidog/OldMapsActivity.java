package com.example.solene.galidog;

import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
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

public class OldMapsActivity extends FragmentActivity implements OnMapReadyCallback {
    /*
    La classe OldMapsActivity gère l'affichage du trajet, et le lancement des commandes vocales de guidage.
     */

    /* Attributs nécessaires à l'utilisation d'une carte Google Maps, et à la géolocalisation. */
    private GoogleMap mMap;
    private Button btnStartPath;
    private Marker marker;
    private LocationManager androidLocationManager;
    private LocationListener androidLocationListener;
    private final static int REQUEST_CODE_UPDATE_LOCATION = 42;

    /* Attributs utiles au fonctionnement de l'algorithme. */
    private ArrayList<CommandeVocale> listeCommandes;
    private ArrayList<Point> listePoints;

    /* Attributs pour design de l'application. */
    private PolylineOptions dessin = new PolylineOptions().width(9).color(Color.BLUE);
    private Polyline dessinTrajet;
    private ArrayList<LatLng> listeCoord = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
        Fonction qui se lance à l'ouverture de l'activité d'affichage.
        */

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_old_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /* Récupération des données. */
        listeCommandes = getIntent().getParcelableArrayListExtra("commandes");
        listePoints = getIntent().getParcelableArrayListExtra("points");

        /* Association des boutons du layout à l'activité. */
        btnStartPath = (Button) findViewById(R.id.activity_main_btn_start_path);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        /*
        Fonction qui se lance lorsque la carte Google Maps est prête.
         */

        mMap = googleMap;

        /* Initialisation du trajet dessiné */
        dessinTrajet = mMap.addPolyline(dessin);

        /* La fonction PathView() affiche le trajet enregistré. */
        pathView();

        /* La fonction androidFirstLocation() affiche la localisation de l'utilisateur tout au long de l'utilisation de l'activité. */
        androidFirstLocation();

        /* Démarrage de la comparaison entre la localisation et le trajet enregistré. */
        btnStartPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast toast = Toast.makeText(OldMapsActivity.this, "Le trajet démarre", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                /*La fonction androidUpdateLocation() lance les commandes vocales selon la position de l'utilisateur. */
                androidUpdateLocation();

                btnStartPath.setText("Arrêter le trajet");

                /* Arrêt du guidage. */
                btnStartPath.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        onPause();
                        Toast.makeText(OldMapsActivity.this, "Trajet termminé", Toast.LENGTH_SHORT).show();
                        btnStartPath.setText("Retour à l'accueil");

                        /* Retour à l'accueil. */
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

    public void pathView() {
        /*
        Fonction qui affiche les points et commandes enregistrées précedemment.
         */

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        /* Affichage de chaque point de la liste des points. */
        for (int i = 0; i < listePoints.size(); i++) {

            /* Récupération du premier point de la liste. */
            Point pointChemin = listePoints.get(i);
            LatLng coordonnees = new LatLng(pointChemin.getLatitude(), pointChemin.getLongitude());

            /* Affichage du tracé entre les points. */
            listeCoord.add(coordonnees);
            dessinTrajet.setPoints(listeCoord);

            /* Affichage d'un marqueur associé à la localisation. */
            BitmapDescriptor point1 = BitmapDescriptorFactory.fromResource(R.drawable.point2_trajet);
            mMap.addMarker(new MarkerOptions().position(coordonnees).alpha(0.7f).icon(point1));

            /* Mise en commun des toutes les coordonnées pour connaitres les limites. */
            builder.include(coordonnees);
        }

        /* Affichage de chaque point de la liste des commandes vocales. */
        for (int j=0; j < listeCommandes.size(); j++) {

            /* Récupération de la première commande vocale de la liste. */
            CommandeVocale pointCommande = listeCommandes.get(j);
            LatLng coordonnees = new LatLng(pointCommande.getLatitude(), pointCommande.getLongitude());

            /* Affichage d'un marqueur associé à la localisation. */
            BitmapDescriptor point1 = BitmapDescriptorFactory.fromResource(R.drawable.diamond_green);
            mMap.addMarker(new MarkerOptions().position(coordonnees).alpha(0.7f).icon(point1));
        }

        /* Ajustement de la caméra sur l'ensemble des points. */
        LatLngBounds bounds = builder.build();
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 5);
        mMap.animateCamera(cu);
    }

    public void androidFirstLocation() {
        /*
        Fonction qui affiche la géolocalisation de l'utilisateur lors du guidage.
         */

        /* Vérification des permissions du téléphone en terme de géolocalisation. */
        if (ActivityCompat.checkSelfPermission(OldMapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    OldMapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_UPDATE_LOCATION);
        }

        /* Si les permissions sont accordées, mise en place de l'enregistrement. */
        else {
            androidLocationManager = (LocationManager) this.getSystemService(OldMapsActivity.this.LOCATION_SERVICE);
            androidLocationListener = new LocationListener() {
                public void onLocationChanged(Location loc) {

                    /* Récupération des coordonnées actuelles */
                    double latNow = loc.getLatitude();
                    double lonNow = loc.getLongitude();
                    LatLng youAreHere = new LatLng(latNow, lonNow);

                    /* Suppression de l'ancien marqueur. */
                    if (marker != null){
                        marker.remove();
                    }

                    /* Affichage d'un marqueur à l'emplacement de l'utilisateur. */
                    BitmapDescriptor point2 = BitmapDescriptorFactory.fromResource(R.drawable.point_rouge);
                    marker = mMap.addMarker(new MarkerOptions().position(youAreHere).title("Vous êtes ici").icon(point2));
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProviderEnabled(String provider) {}

                public void onProviderDisabled(String provider) {}
            };

            /* Requête multiple, suivi de la géolocalisation */
            androidLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, androidLocationListener);
        }
    }

    public void androidUpdateLocation() {
        /*
        Fonction qui lance les commandes vocales lorsqu'elles sont proches de la position de l'utilisateur.
         */

        /* Vérification des permissions du téléphone en terme de géolocalisation. */
        if (ActivityCompat.checkSelfPermission(OldMapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    OldMapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_UPDATE_LOCATION);
        }

        /* Si les permissions sont accordées, mise en place de l'enregistrement. */
        else {
            androidLocationManager = (LocationManager) this.getSystemService(OldMapsActivity.this.LOCATION_SERVICE);
            androidLocationListener = new LocationListener() {
                public void onLocationChanged(Location loc) {

                    /* Récupération des coordonnées actuelles */
                    double latNow = loc.getLatitude();
                    double lonNow = loc.getLongitude();

                    /* Tant que la liste des commandes n'est pas vide. */
                    if (listeCommandes.size() != 0) {

                        /* Récupération de la première commande. */
                        CommandeVocale commande = listeCommandes.get(0);
                        double latComm = commande.getLatitude();
                        double lonComm = commande.getLongitude();

                        /* Calcul de la distance entre la position de l'utilisateur et la position de la commande. */
                        double dist = TransformCoordToMeter(latNow, lonNow, latComm, lonComm);

                        /* Si les points sont à moins de 5m. */
                        if (dist < 5) {
                            /* Lancement de la commande vocale audio. */
                            commande.initCommande(OldMapsActivity.this);
                            listeCommandes.remove(0);
                        }
                    }
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };

            /* Requête multiple, suivi de la géolocalisation */
            androidLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, androidLocationListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        /*
        Fonction qui vérifie que le téléphone possède bien les permissions en terme de localisation.
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
        Fonction qui stoppe les fonctions en cours en cas de changement d'orientation ou de fermeture de l'application.
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

    public double TransformCoordToMeter(double lat1, double lon1, double lat2, double lon2){
        /*
        Fonction qui calcule la distance entre deux points données par leurs coordonnées.
         */

        /* Rayon de la terre en mètre. */
        int R = 6378000;

        /* Transformation en radian. */
        double lat1new = (Math.PI * lat1)/180;
        double lon1new = (Math.PI * lon1)/180;
        double lat2new = (Math.PI * lat2)/180;
        double lon2new = (Math.PI * lon2)/180;

        /* Calcul de la distance sur un plan. */
        double dist = R * (Math.PI/2 - Math.asin( Math.sin(lat2new) * Math.sin(lat1new) + Math.cos(lon2new - lon1new) * Math.cos(lat2new) * Math.cos(lat1new)));

        return dist;
    }
}
