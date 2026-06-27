package com.example.placetag.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.placetag.model.Place;

import java.util.ArrayList;
import java.util.List;

public class PlaceDao {
    private final PlaceDbHelper helper;

    public PlaceDao(PlaceDbHelper helper) {
        this.helper = helper;
    }

    public long insert(Place place) {
        // Insert = ajoute un nouveau lieu dans SQLite.
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insert(PlaceDbHelper.TABLE_PLACES, null, toValues(place));
        place.idLocal = id;
        return id;
    }

    public void update(Place place) {
        // Update = modifie le lieu dont l'id local correspond.
        SQLiteDatabase db = helper.getWritableDatabase();
        db.update(
                PlaceDbHelper.TABLE_PLACES,
                toValues(place),
                PlaceDbHelper.COL_ID + "=?",
                new String[]{String.valueOf(place.idLocal)}
        );
    }

    public void delete(long idLocal) {
        // Delete = supprime un lieu local par son identifiant.
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(
                PlaceDbHelper.TABLE_PLACES,
                PlaceDbHelper.COL_ID + "=?",
                new String[]{String.valueOf(idLocal)}
        );
    }

    public Place getById(long idLocal) {
        // Query avec WHERE idLocal=? pour recuperer un seul lieu.
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                PlaceDbHelper.TABLE_PLACES,
                null,
                PlaceDbHelper.COL_ID + "=?",
                new String[]{String.valueOf(idLocal)},
                null,
                null,
                null
        );
        try {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        } finally {
            // Toujours fermer un Cursor pour liberer les ressources.
            cursor.close();
        }
    }

    public List<Place> getAll() {
        // Recupere tous les lieux, les plus recents en premier.
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                PlaceDbHelper.TABLE_PLACES,
                null,
                null,
                null,
                null,
                null,
                PlaceDbHelper.COL_CREATED_AT + " DESC"
        );
        return readAll(cursor);
    }

    public List<Place> getPendingSync() {
        // Lieux a envoyer au serveur: tous ceux qui ne sont pas deja SYNCED.
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                PlaceDbHelper.TABLE_PLACES,
                null,
                PlaceDbHelper.COL_SYNC_STATUS + "!=?",
                new String[]{Place.SYNCED},
                null,
                null,
                PlaceDbHelper.COL_CREATED_AT + " DESC"
        );
        return readAll(cursor);
    }

    public Place getByRemoteId(int remoteId) {
        // Sert a eviter les doublons quand on recharge les lieux depuis MySQL.
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                PlaceDbHelper.TABLE_PLACES,
                null,
                PlaceDbHelper.COL_REMOTE_ID + "=?",
                new String[]{String.valueOf(remoteId)},
                null,
                null,
                null
        );
        try {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    private List<Place> readAll(Cursor cursor) {
        // Transforme toutes les lignes du Cursor en objets Place.
        List<Place> places = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                places.add(fromCursor(cursor));
            }
            return places;
        } finally {
            cursor.close();
        }
    }

    private ContentValues toValues(Place place) {
        // ContentValues = format attendu par SQLite pour insert/update.
        ContentValues values = new ContentValues();
        if (place.remoteId == null) {
            values.putNull(PlaceDbHelper.COL_REMOTE_ID);
        } else {
            values.put(PlaceDbHelper.COL_REMOTE_ID, place.remoteId);
        }
        values.put(PlaceDbHelper.COL_TITLE, place.title);
        values.put(PlaceDbHelper.COL_DESCRIPTION, place.description);
        values.put(PlaceDbHelper.COL_LATITUDE, place.latitude);
        values.put(PlaceDbHelper.COL_LONGITUDE, place.longitude);
        values.put(PlaceDbHelper.COL_ADDRESS, place.address);
        values.put(PlaceDbHelper.COL_PHOTO_PATH, place.photoPath);
        values.put(PlaceDbHelper.COL_CREATED_AT, place.createdAt);
        values.put(PlaceDbHelper.COL_SYNC_STATUS, place.syncStatus);
        return values;
    }

    private Place fromCursor(Cursor cursor) {
        // Convertit une ligne SQLite en objet Java Place.
        Place place = new Place();
        place.idLocal = cursor.getLong(cursor.getColumnIndexOrThrow(PlaceDbHelper.COL_ID));
        int remoteIndex = cursor.getColumnIndexOrThrow(PlaceDbHelper.COL_REMOTE_ID);
        place.remoteId = cursor.isNull(remoteIndex) ? null : cursor.getInt(remoteIndex);
        place.title = cursor.getString(cursor.getColumnIndexOrThrow(PlaceDbHelper.COL_TITLE));
        place.description = cursor.getString(cursor.getColumnIndexOrThrow(PlaceDbHelper.COL_DESCRIPTION));
        place.latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(PlaceDbHelper.COL_LATITUDE));
        place.longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(PlaceDbHelper.COL_LONGITUDE));
        place.address = cursor.getString(cursor.getColumnIndexOrThrow(PlaceDbHelper.COL_ADDRESS));
        place.photoPath = cursor.getString(cursor.getColumnIndexOrThrow(PlaceDbHelper.COL_PHOTO_PATH));
        place.createdAt = cursor.getString(cursor.getColumnIndexOrThrow(PlaceDbHelper.COL_CREATED_AT));
        place.syncStatus = cursor.getString(cursor.getColumnIndexOrThrow(PlaceDbHelper.COL_SYNC_STATUS));
        return place;
    }
}
