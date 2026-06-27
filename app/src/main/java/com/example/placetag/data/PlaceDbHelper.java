package com.example.placetag.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PlaceDbHelper extends SQLiteOpenHelper {
    // Nom et version de la base locale SQLite.
    public static final String DB_NAME = "placetag.db";
    public static final int DB_VERSION = 1;
    public static final String TABLE_PLACES = "places";

    // Noms des colonnes: les constantes evitent les fautes de frappe dans les requetes.
    public static final String COL_ID = "idLocal";
    public static final String COL_REMOTE_ID = "remoteId";
    public static final String COL_TITLE = "title";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_LATITUDE = "latitude";
    public static final String COL_LONGITUDE = "longitude";
    public static final String COL_ADDRESS = "address";
    public static final String COL_PHOTO_PATH = "photoPath";
    public static final String COL_CREATED_AT = "createdAt";
    public static final String COL_SYNC_STATUS = "syncStatus";

    public PlaceDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Appele automatiquement la premiere fois que la base est creee.
        db.execSQL("CREATE TABLE " + TABLE_PLACES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_REMOTE_ID + " INTEGER, " +
                COL_TITLE + " TEXT NOT NULL, " +
                COL_DESCRIPTION + " TEXT, " +
                COL_LATITUDE + " REAL NOT NULL, " +
                COL_LONGITUDE + " REAL NOT NULL, " +
                COL_ADDRESS + " TEXT, " +
                COL_PHOTO_PATH + " TEXT, " +
                COL_CREATED_AT + " TEXT NOT NULL, " +
                COL_SYNC_STATUS + " TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Strategie simple pour le projet: on recree la table si la version change.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLACES);
        onCreate(db);
    }
}
