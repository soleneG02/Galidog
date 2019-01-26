package com.example.solene.galidog;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;

public class CommandeVocale implements Parcelable {

    private int idCommande;
    private static int id = 0;
    private double latitude;
    private double longitude;
    private String direction;

    public CommandeVocale(String direct, double lat, double lon, Context context) throws IOException {
        this.idCommande = id ++;
        this.latitude = lat;
        this.longitude = lon;
        this.direction = direct;
        if (direction == "D") {
            MediaPlayer jouer = MediaPlayer.create(context, R.raw.droite);
            jouer.start();
        }
        else if (direction == "G") {
            MediaPlayer jouer = MediaPlayer.create(context, R.raw.gauche);
            jouer.start();
        }
        else if (direction == "H") {
            MediaPlayer jouer = MediaPlayer.create(context, R.raw.halte);
            jouer.start();
        }
        else {
        }
    }

    public void initCommande(Context context) {
        if (direction == "D") {
            MediaPlayer jouer = MediaPlayer.create(context, R.raw.droite);
            jouer.start();
        }
        else if (direction == "G") {
            MediaPlayer jouer = MediaPlayer.create(context, R.raw.gauche);
            jouer.start();
        }
        else if (direction == "H") {
            MediaPlayer jouer = MediaPlayer.create(context, R.raw.halte);
            jouer.start();
        }
        else {
            Uri myUri = Uri.parse(direction);
            MediaPlayer jouer = MediaPlayer.create(context, myUri);
            jouer.start();
        }
    }

    protected CommandeVocale(Parcel in) {
        idCommande = in.readInt();
        latitude = in.readDouble();
        longitude = in.readDouble();
        direction = in.readString();
    }

    public static final Creator<CommandeVocale> CREATOR = new Creator<CommandeVocale>() {
        @Override
        public CommandeVocale createFromParcel(Parcel in) {
            return new CommandeVocale(in);
        }

        @Override
        public CommandeVocale[] newArray(int size) {
            return new CommandeVocale[size];
        }
    };

    public int getIdCommande() {
        return idCommande;
    }

    public void setIdCommande(int indiceCommande) {
        this.idCommande = indiceCommande;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "CommandeVocale{" +
                "idCommande=" + idCommande +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(idCommande);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(direction);
    }
}
