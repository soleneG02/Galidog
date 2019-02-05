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
import java.util.Random;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    /*
    La classe MapsActivity gère l'enregistrement d'un trajet.
     */

    /* Attributs nécessaires à l'utilisation d'une carte Google Maps, et à la géolocalisation. */
    private GoogleMap mMap;
    private Button btnStartRecord;
    private LocationManager androidLocationManager;
    private LocationListener androidLocationListener;
    private final static int REQUEST_CODE_UPDATE_LOCATION = 42;

    /* Attributs correspondants aux différents boutons de l'activité : commandes pré enregistrées et enregistrement d'une nouvelle commande. */
    private Button btnDroite, btnGauche, btnHalte, btnAutre;
    private Button btnEnreg, btnStop, btnJouer, btnValide;

    /* Attributs pour design de l'application. */
    private PolylineOptions dessin = new PolylineOptions().width(9).color(Color.BLUE);
    private Polyline dessinTrajet;
    private ArrayList<LatLng> listeCoord = new ArrayList<>();

    /* Attributs utiles au fonctionnement de l'algorithme. */
    private Point pointSuivant;
    private ArrayList<Point> listePoints = new ArrayList<>();
    private ArrayList<CommandeVocale> listeCommandes = new ArrayList<>();

    /* Attributs nécessaire à la lecture et l'enregistrement de commandes vocales. */
    String AudioSavePathInDevice = null;
    MediaRecorder mediaRecorder;
    private int idNewCommande = 0;
    public static final int RequestPermissionCode = 1;
    MediaPlayer mediaPlayer;
    private int ENREG_NB = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
        Fonction qui se lance à l'ouverture de l'activité d'enregistrement.
        */

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps2);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /*
         Association des boutons du layout à l'activité.
         Certains boutons (enregistrement d'une commande) sont pour l'instant invisibles à l'écran.
         */
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        /*
        Fonction qui se lance lorsque la carte Google Maps est prête.
         */

        mMap = googleMap;

        /* Initialisation du trajet dessiné */
        dessinTrajet = mMap.addPolyline(dessin);

        /* La fonction androidFirstLocation() affiche la localisation de l'utilisateur à son entrée dans l'activité MapsActivity. */
        androidFirstLocation();

        /* Démarrage de l'enregistrement */
        btnStartRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast toast = Toast.makeText(MapsActivity.this, "L'enregistrement démarre", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                /*La fonction androidUpdateLocation() enregistre les points du trajet et les commandes vocales associées. */
                androidUpdateLocation();
                btnStartRecord.setText("Arrêter l'enregistrement");

                /* Arrêt de l'enregistrement */
                btnStartRecord.setOnClickListener((new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onPause();
                        Toast.makeText(MapsActivity.this, "Fin de l'enregistrement", Toast.LENGTH_SHORT).show();
                        btnStartRecord.setText("Retour à l'accueil");

                        /* Retour à la page d'accueil, avec envoi des données enregistrées. */
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
        Fonction qui affiche la géolocalisation de l'utilisateur lorsqu'il arrive pour la première fois sur la page.
         */

        /* Vérification des permissions du téléphone en terme de géolocalisation. */
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_UPDATE_LOCATION);
        }

        /* Si les permissions sont accordées, mise en place de l'enregistrement. */
        else {
            androidLocationManager = (LocationManager) this.getSystemService(MapsActivity.this.LOCATION_SERVICE);
            androidLocationListener = new LocationListener() {
                public void onLocationChanged(Location loc) {

                    /* Récupération des coordonnées actuelles */
                    double latNow = loc.getLatitude();
                    double lonNow = loc.getLongitude();
                    Toast.makeText(MapsActivity.this, "Coordonnées : " + latNow + " / " + lonNow, Toast.LENGTH_SHORT).show();

                    /* Ajout du point à la liste des points visités. */
                    LatLng youAreHere = new LatLng(latNow, lonNow);
                    Point newPoint = new Point(latNow, lonNow);
                    listePoints.add(newPoint);

                    /* Démarrage du tracé du trajet. */
                    listeCoord.add(new LatLng(latNow,lonNow));
                    dessinTrajet.setPoints(listeCoord);

                    /* Affichage d'un marqueur à l'emplacement de l'utilisateur. */
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
            androidLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, androidLocationListener, null);
        }
    }

    public void androidUpdateLocation() {
        /*
        Fonction qui enregistre les différentes positions d'un utilisateur en mouvement, les affiche et les associe aux commandes vocales.
         */

        /* Vérification des permissions du téléphone en terme de géolocalisation. */
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_UPDATE_LOCATION);
        }

        /* Si les permissions sont accordées, mise en place de l'enregistrement. */
        else {
            androidLocationManager = (LocationManager) this.getSystemService(MapsActivity.this.LOCATION_SERVICE);
            androidLocationListener = new LocationListener() {
                public void onLocationChanged(Location loc) {

                    /* Récupération des coordonnées actuelles */
                    double latNow = loc.getLatitude();
                    double lonNow = loc.getLongitude();
                    LatLng youAreHere = new LatLng(latNow, lonNow);

                    /* Création d'une potentielle commande vocale, associé à la localisation actuelle */
                    BoutonDroite(latNow, lonNow);
                    BoutonGauche(latNow, lonNow);
                    BoutonHalte(latNow, lonNow);
                    BoutonAutre(latNow, lonNow);

                    /* Ajout du point à la liste des points visités. */
                    pointSuivant = new Point(latNow, lonNow);
                    listePoints.add(pointSuivant);

                    /* Actualisation du tracé du trajet. */
                    listeCoord.add(new LatLng(latNow,lonNow));
                    dessinTrajet.setPoints(listeCoord);

                    /* Affichage d'un message lors du premier point enregistré. */
                    if (pointSuivant.getIdPoint() == 1) {
                        Toast.makeText(MapsActivity.this, "Trace en cours ", Toast.LENGTH_SHORT).show();
                    }

                    /* Affichage d'un marqueur à l'emplacement de l'utilisateur. */
                    BitmapDescriptor point2 = BitmapDescriptorFactory.fromResource(R.drawable.point2_trajet);
                    mMap.addMarker(new MarkerOptions().position(youAreHere).title("Point n°" + pointSuivant.getIdPoint()).icon(point2));
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

            /* Requête multiple, suivi de la géolocalisation */
            androidLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, androidLocationListener);
        }
    }

    public void BoutonDroite(final double lat, final double lon) {
        /*
        Fonction qui crée une commande vocale "Droite" lors de l'appui sur le bouton.
         */

        btnDroite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CommandeVocale newCommande = null;

                try {
                    /* Création d'une nouvelle commande vocale "Droite". */
                    newCommande = new CommandeVocale("D", lat, lon, MapsActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                /* Ajout à la liste des commandes du trajet.*/
                listeCommandes.add(newCommande);

                /* Affichage d'un marqueur pour signifier l'emplacement associé à la commande. */
                BitmapDescriptor pointX = BitmapDescriptorFactory.fromResource(R.drawable.diamond_green);
                mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("Point n°" + pointSuivant.getIdPoint()).icon(pointX));

                Toast.makeText(MapsActivity.this, "Bouton droite activé", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void BoutonGauche(final double lat, final double lon) {
        /*
        Fonction qui crée une commande vocale "Gauche" lors de l'appui sur le bouton.
         */

        btnGauche.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CommandeVocale newCommande = null;

                try {
                    /* Création d'une nouvelle commande vocale "Gauche". */
                    newCommande = new CommandeVocale("G", lat, lon, MapsActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                /* Ajout à la liste des commandes du trajet.*/
                listeCommandes.add(newCommande);

                /* Affichage d'un marqueur pour signifier l'emplacement associé à la commande. */
                BitmapDescriptor pointX = BitmapDescriptorFactory.fromResource(R.drawable.diamond_green);
                mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("Point n°" + pointSuivant.getIdPoint()).icon(pointX));

                Toast.makeText(MapsActivity.this, "Bouton gauche activé", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void BoutonHalte(final double lat, final double lon) {
        /*
        Fonction qui crée une commande vocale "Halte" lors de l'appui sur le bouton.
         */

        btnHalte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CommandeVocale newCommande = null;

                try {
                    /* Création d'une nouvelle commande vocale "Gauche". */
                    newCommande = new CommandeVocale("H", lat, lon, MapsActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                /* Ajout à la liste des commandes du trajet.*/
                listeCommandes.add(newCommande);

                /* Affichage d'un marqueur pour signifier l'emplacement associé à la commande. */
                BitmapDescriptor pointX = BitmapDescriptorFactory.fromResource(R.drawable.diamond_green);
                mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("Point n°" + pointSuivant.getIdPoint()).icon(pointX));

                Toast.makeText(MapsActivity.this, "Bouton halte activé", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void BoutonAutre(final double lat, final double lon) {
        /*
        Fonction qui crée une commande vocale "Autre" lors de l'appui sur le bouton.
         */

        btnAutre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* Affichage des boutons associés à l'enregistrement d'une commande. */
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
                btnStartRecord.setEnabled(false);

                /* La fonction Enregistrement() gère les étapes de l'enregistrement d'une nouvelle commande. */
                Enregistrement(lat, lon);
            }
        });
    }


    public void Enregistrement(final double lat, final double lon){
        /*
        Fonction permettant d'enregistrer une nouvelle commande vocale et de l'associer à une géolocalisation.
        */

        /* Démarrage de l'enregistrement de la commande. */
        btnEnreg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                /* Vérification des permissions du téléphone en terme de microphone. */
                if (checkPermission()) {

                    /* Compteur du nombre d'enregistrement effectué.
                    Autorisation de 3 enregistrements.
                     */
                    ENREG_NB++;

                    /* Désactivation de l'accessibilité à certains boutons. */
                    btnJouer.setEnabled(false);
                    btnValide.setEnabled(false);

                    /* Création du chemin où la commande sera enregistrée. */
                    Random random = new Random();
                    AudioSavePathInDevice = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + random +
                            "Enregistrement" + idNewCommande + "AudioRecording.3gp";
                    Log.i("Chemin", "Le CHEMIN EST / " + AudioSavePathInDevice);
                    idNewCommande++;

                    /* La fonction MediaRecorderReady() prépare le téléphone à l'enregistrement. */
                    MediaRecorderReady();

                    try {
                        /* Lancement de l'enregistrement. */
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                    } catch (IllegalStateException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    /* Modification de l'accessibilité à certains boutons. */
                    btnEnreg.setEnabled(false);
                    btnStop.setEnabled(true);

                    Toast.makeText(MapsActivity.this, "Enregistrement démarré", Toast.LENGTH_LONG).show();
                }
                /* Demande de permission d'accès au microphone si celle ci n'est pas accordée. */
                else {
                    requestPermission();
                }

            }
        });

        /* Arrêt de l'enregistrement de la commande. */
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* Arrêt de l'enregistreur. */
                mediaRecorder.stop();

                /* Modification de l'accessibilité à certains boutons. */
                btnStop.setEnabled(false);
                btnJouer.setEnabled(true);
                btnEnreg.setEnabled(false);
                btnValide.setEnabled(false);

                Toast.makeText(MapsActivity.this, "Enregistrement terminé", Toast.LENGTH_LONG).show();
            }
        });

        /* Lecture de l'enregistrement pour vérification par l'utilisateur. */
        btnJouer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) throws IllegalArgumentException, SecurityException, IllegalStateException {

                /* Si l'utilisateur a dépasser le nombre autorisé d'enregistrement, seul le bouton "Valider" sera disponible. */
                if (ENREG_NB < 3) {

                    /* Modification de l'accessibilité à certains boutons. */
                    btnStop.setEnabled(false);
                    btnEnreg.setEnabled(true);
                    btnValide.setEnabled(true);
                }

                /* Sinon, l'utilisateur pourra valider ou enregistrer à nouveau. */
                else {
                    /* Modification de l'accessibilité à certains boutons. */
                    btnStop.setEnabled(false);
                    btnEnreg.setEnabled(false);
                    btnValide.setEnabled(true);
                }

                /* Lecture de l'enregistrement. */
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(AudioSavePathInDevice);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaPlayer.start();

                Toast.makeText(MapsActivity.this, "Ecoute de l'enregistrement", Toast.LENGTH_LONG).show();
            }
        });

        /* Validation de l'enregistrement. */
        btnValide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                /* Confirmation à l'utilisateur que l'enregistrement a été pris en compte. */
                MediaPlayer jouer = MediaPlayer.create(MapsActivity.this, R.raw.enreg_valid);
                jouer.start();

                /* Création d'une commande vocale associée au nouvel enregistrement. */
                CommandeVocale newCommande = null;
                try {
                    newCommande = new CommandeVocale(AudioSavePathInDevice, lat, lon, MapsActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                /* Ajout à la liste des commandes du trajet.*/
                listeCommandes.add(newCommande);

                /* Affichage d'un marqueur pour signifier l'emplacement associé à la commande. */
                BitmapDescriptor pointX = BitmapDescriptorFactory.fromResource(R.drawable.diamond_green);
                mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("Point n°" + pointSuivant.getIdPoint()).icon(pointX));

                /* Affichage des boutons associés aux commandes pré-enregistrées. */
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
                btnStartRecord.setEnabled(true);
            }
        });
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
        Fonction qui stoppe les fonctions en cours en cas de changement d'orientation ou de fermeture de l'application.
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
        /*
        Fonction de préparation du téléphone à l'enregistrement d'une nouvelle commande.
         */
        mediaRecorder=new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);

        /* Choix du chemin de l'enregistrement. */
        mediaRecorder.setOutputFile(AudioSavePathInDevice);
    }

    private void requestPermission() {
        /*
        Fonction qui demande à l'utilisateur la permission.
         */
        ActivityCompat.requestPermissions(MapsActivity.this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }

    public boolean checkPermission() {
        /*
        Fonction qui vérifie les permissions.
         */
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }
}
