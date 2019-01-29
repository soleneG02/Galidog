package com.example.solene.galidog;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;


import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Button btnStartRecord;
    private Button btnDroite, btnGauche, btnHalte, btnAutre;
    private Button btnEnreg, btnStop, btnJouer, btnValide;
    private PolylineOptions dessin = new PolylineOptions().width(9).color(Color.BLUE);
    private Polyline dessinTrajet;
    private ArrayList<LatLng> listeCoord = new ArrayList<>();

    private LocationManager androidLocationManager;
    private LocationListener androidLocationListener;
    private final static int REQUEST_CODE_UPDATE_LOCATION = 42;
    private Point pointSuivant;
    private ArrayList<Point> listePoints = new ArrayList<>();
    private ArrayList<CommandeVocale> listeCommandes = new ArrayList<>();

    String AudioSavePathInDevice = null;
    MediaRecorder mediaRecorder;
    private int idNewCommande = 0;
    public static final int RequestPermissionCode = 1;
    MediaPlayer mediaPlayer;

    private int ENREG_NB = 0;

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

        btnEnreg = (Button) findViewById(R.id.activity_main_btn_enreg);
        btnStop = (Button) findViewById(R.id.activity_main_btn_stop);
        btnJouer = (Button) findViewById(R.id.activity_main_btn_jouer);
        btnValide = (Button) findViewById(R.id.activity_main_btn_valide);
        btnEnreg.setVisibility(View.INVISIBLE);
        btnStop.setVisibility(View.INVISIBLE);
        btnJouer.setVisibility(View.INVISIBLE);
        btnValide.setVisibility(View.INVISIBLE);
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
        dessinTrajet = mMap.addPolyline(dessin);

        androidFirstLocation();

        /* Démarrage de l'enregistrement */
        btnStartRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Démarrer l'enregistrement, affichage d'un toast pour vérification */
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
                        btnStartRecord.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent retourAccueil = new Intent(MapsActivity.this, MainActivity.class);
                                retourAccueil.putParcelableArrayListExtra("commandes", listeCommandes);
                                retourAccueil.putParcelableArrayListExtra("points", listePoints);
                                startActivity(retourAccueil);
                            }
                        });
                    }
                }));
            }
        });
    }

    public void androidFirstLocation() {
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
                    Toast.makeText(MapsActivity.this, "Coordonnées : " + latNow + " / " + lonNow, Toast.LENGTH_SHORT).show();
                    LatLng youAreHere = new LatLng(latNow, lonNow);
                    Point newPoint = new Point(latNow, lonNow);
                    listePoints.add(newPoint);

                    listeCoord.add(new LatLng(latNow,lonNow));
                    dessinTrajet.setPoints(listeCoord);
                    BitmapDescriptor point1 = BitmapDescriptorFactory.fromResource(R.drawable.point2_init);
                    mMap.addMarker(new MarkerOptions().position(youAreHere).title("Vous êtes ici").icon(point1));
                    int padding = 17;
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(youAreHere, padding));
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
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

                    //Association à une potentielle commande vocale

                    BoutonDroite(latNow, lonNow);
                    BoutonGauche(latNow, lonNow);
                    BoutonHalte(latNow, lonNow);
                    BoutonAutre(latNow, lonNow);

                    if (RechercheNbSatellite() >= 0) {
                        pointSuivant = new Point(latNow, lonNow);

                        // Ajout à la liste des points du trajet
                        listePoints.add(pointSuivant);
                        listeCoord.add(new LatLng(latNow,lonNow));
                        dessinTrajet.setPoints(listeCoord);

                        // Affichage d'un toast au début de l'enregistrement
                        if (pointSuivant.getIdPoint() == 1) {
                            Toast.makeText(MapsActivity.this, "Trace en cours ", Toast.LENGTH_SHORT).show();
                        }

                        //Création d'un marqueur
                        BitmapDescriptor point2 = BitmapDescriptorFactory.fromResource(R.drawable.point2_trajet);
                        mMap.addMarker(new MarkerOptions().position(youAreHere).title("Point n°" + pointSuivant.getIdPoint()).icon(point2));
                        int padding = 17;
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(youAreHere, padding));
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
            androidLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500, // en millisecondes
                    1, // en mètres
                    androidLocationListener);
        }
    }

    public void BoutonDroite(final double lat, final double lon) {
        //Appel bouton droite
        btnDroite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Enregistrement et lecture de la commande
                CommandeVocale newCommande = null;
                try {
                    newCommande = new CommandeVocale("D", lat, lon, MapsActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                listeCommandes.add(newCommande);
                //Vérification : Log.i("vérif", "Liste des commandes :" + listeCommandes);
                Toast.makeText(MapsActivity.this, "Bouton droite activé : " + newCommande.getIdCommande(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void BoutonGauche(final double lat, final double lon) {
        //Appel bouton gauche
        btnGauche.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommandeVocale newCommande = null;
                try {
                    newCommande = new CommandeVocale("G", lat, lon, MapsActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                listeCommandes.add(newCommande);
                Vérification:
                Log.i("vérif", "Liste des commandes :" + listeCommandes);
                Toast.makeText(MapsActivity.this, "Bouton gauche activé : " + newCommande.getIdCommande(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void BoutonHalte(final double lat, final double lon) {
        //Appel bouton halte
        btnHalte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommandeVocale newCommande = null;
                try {
                    newCommande = new CommandeVocale("H", lat, lon, MapsActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                listeCommandes.add(newCommande);
                //Vérification : Log.i("vérif", "Liste des commandes :" + listeCommandes);
                Toast.makeText(MapsActivity.this, "Bouton halte activé : " + newCommande.getIdCommande(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void BoutonAutre(final double lat, final double lon) {
        //Appel bouton autre
        btnAutre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnGauche.setVisibility(View.INVISIBLE);
                btnDroite.setVisibility(View.INVISIBLE);
                btnHalte.setVisibility(View.INVISIBLE);
                btnAutre.setVisibility(View.INVISIBLE);
                btnEnreg.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.VISIBLE);
                btnJouer.setVisibility(View.VISIBLE);
                btnValide.setVisibility(View.VISIBLE);
                btnStop.setEnabled(false);
                btnJouer.setEnabled(false);
                btnValide.setEnabled(false);
                btnEnreg.setEnabled(true);

                Enregistrement(lat, lon);
            }
        });
    }


    public void Enregistrement(final double lat, final double lon){
        btnEnreg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (checkPermission()) {

                    ENREG_NB++;
                    Log.i("ENREGISTREMENT", "" + ENREG_NB);
                    btnJouer.setEnabled(false);
                    btnValide.setEnabled(false);

                    AudioSavePathInDevice = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
                            "Enregistrement" + idNewCommande + "AudioRecording.3gp";
                    Log.i("Chemin", "Le CHEMIN EST / " + AudioSavePathInDevice);
                    idNewCommande++;

                    MediaRecorderReady();

                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                    } catch (IllegalStateException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    btnEnreg.setEnabled(false);
                    btnStop.setEnabled(true);

                    Toast.makeText(MapsActivity.this, "Enregistrement démarré",
                            Toast.LENGTH_LONG).show();
                } else {
                    requestPermission();
                }

            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaRecorder.stop();
                btnStop.setEnabled(false);
                btnJouer.setEnabled(true);
                btnEnreg.setEnabled(false);
                btnValide.setEnabled(false);

                Toast.makeText(MapsActivity.this, "Enregistrement terminé",
                        Toast.LENGTH_LONG).show();
            }
        });

        btnJouer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) throws IllegalArgumentException,
                    SecurityException, IllegalStateException {

                if (ENREG_NB < 3) {
                    btnStop.setEnabled(false);
                    btnEnreg.setEnabled(true);
                    btnValide.setEnabled(true);
                } else {
                    btnStop.setEnabled(false);
                    btnEnreg.setEnabled(false);
                    btnValide.setEnabled(true);
                }

                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(AudioSavePathInDevice);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mediaPlayer.start();
                Toast.makeText(MapsActivity.this, "Ecoute de l'enregistrement",
                        Toast.LENGTH_LONG).show();
            }
        });

        btnValide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //CONFIRMATION
                MediaPlayer jouer = MediaPlayer.create(MapsActivity.this, R.raw.enreg_valid);
                jouer.start();

                //AJOUT A COMMANDE VOCALE
                CommandeVocale newCommande = null;
                try {
                    newCommande = new CommandeVocale(AudioSavePathInDevice, lat, lon, MapsActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                listeCommandes.add(newCommande);

                //MISE A JOUR DES BOUTONS
                btnEnreg.setVisibility(View.INVISIBLE);
                btnStop.setVisibility(View.INVISIBLE);
                btnJouer.setVisibility(View.INVISIBLE);
                btnValide.setVisibility(View.INVISIBLE);
                btnGauche.setVisibility(View.VISIBLE);
                btnDroite.setVisibility(View.VISIBLE);
                btnHalte.setVisibility(View.VISIBLE);
                btnAutre.setVisibility(View.VISIBLE);
                btnGauche.setEnabled(true);
                btnDroite.setEnabled(true);
                btnAutre.setEnabled(true);
                btnHalte.setEnabled(true);
            }
        });
    }

    // FONCTION : NE RENVOIE RIEN...
    private int nbSat;

    @SuppressLint({"MissingPermission", "NewApi"})
    public int RechercheNbSatellite() {
        new GpsStatus.Listener() {
            public void onGpsStatusChanged(int event) {
                Log.i("SALUT", "SALUT SALUT SALUT SALUT");
                int satellites = 0;
                int satellitesInFix = 0;
                for (GpsSatellite sat : androidLocationManager.getGpsStatus(null).getSatellites()) {
                    if (sat.usedInFix()) {
                        satellitesInFix++;
                    }
                    satellites++;
                }
                Log.i("NOMBRE SATELLITES ", satellites + " Used In Last Fix (" + satellitesInFix + ")");
            }
        };
        Log.i("NOMBRE DE SATELLITES ", "Nombre de satellites accessibles : " + nbSat);
        return nbSat;
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

        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length> 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission) {
                        Toast.makeText(MapsActivity.this, "Permission Granted",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MapsActivity.this,"Permission Denied",
                                Toast.LENGTH_LONG).show();
                    }
                }
                break;
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

    public void MediaRecorderReady(){
        mediaRecorder=new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(AudioSavePathInDevice);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MapsActivity.this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }
}

