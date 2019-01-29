package com.example.solene.galidog;

import android.os.Parcel;
import android.os.Parcelable;

public class Point implements Parcelable {
    /*
    La classe Point gère les points du trajet.
     */

    /* Attributs de définition des points. */
    private double latitude;
    private double longitude;
    private double idPoint;
    private static double id = 0;

    public Point(double latitude, double longitude) {
        /*
        Fonction de création d'une commande vocale.
         */

        this.latitude = latitude;
        this.longitude = longitude;
        this.idPoint = id ++;
    }

    protected Point(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
        idPoint = in.readDouble();
    }

    public static final Creator<Point> CREATOR = new Creator<Point>() {
        @Override
        public Point createFromParcel(Parcel in) {
            return new Point(in);
        }

        @Override
        public Point[] newArray(int size) {
            return new Point[size];
        }
    };

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getIdPoint() {
        return idPoint;
    }

    @Override
    public String toString() {
        /*
        Affichage de la commande vocale.
         */

        return "Point{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", idPoint=" + idPoint +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeDouble(idPoint);
    }
}
