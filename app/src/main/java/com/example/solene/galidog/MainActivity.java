package com.example.solene.galidog;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    /*
    La classe MainActivity gère la page d'accueil de l'application et le passage d'un trajet "à enregistrer" à un trajet "enregistré"
    */

    private TextView textStart;

    /* Attributs correspondants aux boutons permettant d'accéder à la création d'un nouveau trajet, ou à l'affichage d'un ancien. */
    private Button btnNewPath;
    private Button btnOldPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
        Fonction qui se lance à l'ouverture de l'application.
         */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        if (intent != null){

            /*
            Association des boutons du layout à l'activité
             */
            textStart = (TextView) findViewById(R.id.activity_main_text_start);
            btnNewPath = (Button) findViewById(R.id.activity_main_btn_new);
            btnOldPath = (Button) findViewById(R.id.activity_main_btn_old);

            /*
            Lancement de l'activité MapsActivity lors du clic sur le bouton
            */
            btnNewPath.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intentMapsActivity = new Intent(MainActivity.this, MapsActivity.class);
                    startActivity(intentMapsActivity);
                }
            });

            /*
            Lancement de l'activité OldMapsActivité lors du clic sur le bouton.
            L'algorithme vérifie au préalable qu'il a bien recu les données de la phase d'enregistrement
             */
            if (intent.hasExtra("commandes")){
                final ArrayList<CommandeVocale> listeCommandes = getIntent().getParcelableArrayListExtra("commandes");
                final ArrayList<Point> listePoints = getIntent().getParcelableArrayListExtra("points");

                btnOldPath.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intentOldMapsActivity = new Intent(MainActivity.this, OldMapsActivity.class);
                        intentOldMapsActivity.putParcelableArrayListExtra("commandes", listeCommandes);
                        intentOldMapsActivity.putParcelableArrayListExtra("points", listePoints);
                        startActivity(intentOldMapsActivity);
                    }
                });
            }
        }
    }
}
