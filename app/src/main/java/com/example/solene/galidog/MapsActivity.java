package com.example.solene.galidog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.DrawableRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Button btnStartRecord;
    private Button btnDroite;
    private Button btnGauche;
    private Button btnHalte;
    private Button btnAutre;
    private boolean ENREGISTREMENT_TERMINE;

    private LocationManager androidLocationManager;                       
    private LocationListener androidLocationListener;                     
    private final static int REQUEST_CODE_UPDATE_LOCATION=42;             
    private Point pointSuivant;                                           
    private ArrayList<Point> listePoints = new ArrayList<>();
    private ArrayList<CommandeVocale> listeCommandes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps2);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btnStartRecord = (Button) findViewById(R.id.activity_main_btn_start_record);
        btnDroite = (Button) findViewById(R.id.activity_main_btn_droite);
        btnGauche = (Button) findViewById(R.id.activity_main_btn_gauche);
        btnHalte = (Button) findViewById(R.id.activity_main_btn_halte);
        btnAutre = (Button) findViewById(R.id.activity_main_btn_autre);

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
        /*
        Cette fonction appelle les fonctions ci-dessous après le chargement de la carte, et gère le bouton d'nregistrement.
         */
        mMap = googleMap;

        androidFirstLocation();

        /* Démarrage de l'enregistrement */
        btnStartRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Démarrer l'enregistrement, affichage d'un toast pour vérification */
                ENREGISTREMENT_TERMINE = false;
                Toast toast = Toast.makeText(MapsActivity.this, "L'enregistrement démarre", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                androidUpdateLocation();
                btnStartRecord.setText("Arrêter l'enregistrement");
                btnStartRecord.setOnClickListener((new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onPause();
                        Toast.makeText(MapsActivity.this, "Fin de l'enregistrement", Toast.LENGTH_SHORT).show();
                        btnStartRecord.setText("Retour à l'accueil");
                        ENREGISTREMENT_TERMINE = true;
                        btnStartRecord.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                TransfertDonnees donnees = new TransfertDonnees(ArrayToTabCommande(listeCommandes), ArrayToTabPoint(listePoints));
                                Intent retourAccueil = new Intent(MapsActivity.this, MainActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putParcelable("donnees", donnees);
                                retourAccueil.putExtras(bundle);
                                startActivity(retourAccueil);
                            }
                        });
                    }
                }));
            }
        });
    }

    public void androidFirstLocation(){
        /*
        Cette fonction affiche la géolocalisation de l'utilisateur lorsqu'il arrive pour la première fois sur la page.
         */
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_UPDATE_LOCATION);
        } else {
            androidLocationManager = (LocationManager) this.getSystemService(MapsActivity.this.LOCATION_SERVICE);
            androidLocationListener = new LocationListener() {
                public void onLocationChanged(Location loc) {
                    /* Affichage des coordonnées & création d'un marqueur */
                    double latNow = loc.getLatitude();
                    double lonNow = loc.getLongitude();
                    Toast.makeText(MapsActivity.this, "Coordonnées : "+latNow+" / "+lonNow, Toast.LENGTH_SHORT).show();
                    LatLng youAreHere = new LatLng(latNow, lonNow);
                    Point newPoint = new Point(latNow, lonNow);
                    listePoints.add(newPoint);
                    mMap.addMarker(new MarkerOptions().position(youAreHere).title("Vous êtes ici"));
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

    public void androidUpdateLocation(){
        /*
        Cette fonction enregistre les différentes positions d'un utilisateur en mouvement, les affiche et les associe aux commandes vocales.
         */
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_UPDATE_LOCATION);
        } else {
            androidLocationManager = (LocationManager) this.getSystemService(MapsActivity.this.LOCATION_SERVICE);
            androidLocationListener = new LocationListener() {
                public void onLocationChanged(Location loc) {
                    // Récupération de la localisation
                    double latNow = loc.getLatitude();
                    double lonNow = loc.getLongitude();
                    LatLng youAreHere = new LatLng(latNow, lonNow);

                    //Association à une potentielle commande vocale : pas de mise à jour de l'idCommande...

                    BoutonDroite(youAreHere);
                    BoutonGauche(youAreHere);
                    BoutonHalte(youAreHere);
                    BoutonAutre(youAreHere);

                    if (RechercheNbSatellite() >= 5) {
                        pointSuivant = new Point(latNow, lonNow);

                        // Ajout à la liste des points du trajet
                        listePoints.add(pointSuivant);
                        Vérificaion:
                        Log.i("verifPoints", "Liste des points : " + listePoints);

                        // Affichage d'un toast au début de l'enregistrement
                        if (pointSuivant.getIdPoint() == 1) {
                            Toast.makeText(MapsActivity.this, "Trace en cours ", Toast.LENGTH_SHORT).show();
                        }

                        //Création d'un marqueur
                        mMap.addMarker(new MarkerOptions().position(youAreHere).title("Point n°" + pointSuivant.getIdPoint()));
                    }
                }
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                public void onProviderEnabled(String provider) {}
                public void onProviderDisabled(String provider) {}
            };

            /* Requête multiple, suivi de la géolocalisation */
            androidLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10, // en millisecondes
                    1, // en mètres
                    androidLocationListener);
        }
    }

    public void BoutonDroite(final LatLng youAreHere) {
        //Appel bouton droite
        btnDroite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommandeVocale newCommande = new CommandeVocale("D", youAreHere);
                listeCommandes.add(newCommande);
                //Vérification : Log.i("vérif", "Liste des commandes :" + listeCommandes);
                Toast.makeText(MapsActivity.this, "Bouton droite activé : " + newCommande.getIdCommande(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    public void BoutonGauche(final LatLng youAreHere) {
        //Appel bouton gauche
        btnGauche.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommandeVocale newCommande = new CommandeVocale("G", youAreHere);
                listeCommandes.add(newCommande);
                Vérification : Log.i("vérif", "Liste des commandes :" + listeCommandes);
                Toast.makeText(MapsActivity.this, "Bouton gauche activé : " + newCommande.getIdCommande(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    public void BoutonHalte(final LatLng youAreHere) {
        //Appel bouton halte
        btnHalte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommandeVocale newCommande = new CommandeVocale("H", youAreHere);
                listeCommandes.add(newCommande);
                //Vérification : Log.i("vérif", "Liste des commandes :" + listeCommandes);
                Toast.makeText(MapsActivity.this, "Bouton halte activé : " + newCommande.getIdCommande(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    public void BoutonAutre(final LatLng youAreHere) {
        //Appel bouton autre
        btnAutre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent enregistrement = new Intent(MapsActivity.this, Enregistrement.class);
                //startActivity(enregistrement);

                btnDroite.setText("Record");
                btnDroite.setBackground(getDrawable(R.drawable.button_record));
                btnGauche.setText("Stop");
                btnHalte.setText("Play");
                btnAutre.setText("Valider");

                //Mettre le code d'enregistrement Fonction()
                btnAutre.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        btnDroite.setText("A droite");
                        btnDroite.setBackground(getDrawable(R.drawable.button_record));
                        btnGauche.setText("A gauche");
                        btnHalte.setText("Halte");
                        btnAutre.setText("Autre Commande");
                    }
                });

                //Vérification : Log.i("vérif", "Liste des commandes :" + listeCommandes);
                Toast.makeText(MapsActivity.this, "Bouton autre activé " , Toast.LENGTH_SHORT).show();
                }
            });
    }

    // FONCTION A VERIFIER
    public int RechercheNbSatellite() {
        int totalSat = 0;
        for (GpsSatellite satellite : androidLocationManager.getGpsStatus(null).getSatellites()) {
            if(satellite .usedInFix()) {
                totalSat ++;
            }
        }
        Log.i("NbSatellites", "Nombre de satellites accessibles : " + totalSat);
        return totalSat;
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
                    Toast.makeText(MapsActivity.this, "Permission refusée.", Toast.LENGTH_LONG).show();
                }
                return;
        }
    }

    @Override
    protected void onPause() {
        /*
        Cette fonction stoppe les fonctions en cours en cas de changement d'orientation ou de fermeture de l'application.
         */
        super.onPause();
        if(androidLocationListener!=null) {
            if (androidLocationManager == null) {
                androidLocationManager = (LocationManager) this.getSystemService(MapsActivity.this.LOCATION_SERVICE);
            }
            androidLocationManager.removeUpdates(androidLocationListener);
            androidLocationManager=null;
            androidLocationListener=null;
        }
    }

    public Point[] ArrayToTabPoint(ArrayList<Point> listePoints) {
        int size = listePoints.size();
        Point[] listePointsTab = new Point[size];
        for(int i=0 ; i < listePoints.size() ; i++) {
            Point point = listePoints.get(i);
            listePointsTab[i] = point;
        }
        return listePointsTab;
    }

    public CommandeVocale[] ArrayToTabCommande(ArrayList<CommandeVocale> listeCommandes) {
        int size = listeCommandes.size();
        CommandeVocale[] listeCommandesTab = new CommandeVocale[size];
        for(int i=0 ; i < listeCommandes.size() ; i++) {
            CommandeVocale comVoc = listeCommandes.get(i);
            listeCommandesTab[i] = comVoc;
        }
        return listeCommandesTab;
    }
}
