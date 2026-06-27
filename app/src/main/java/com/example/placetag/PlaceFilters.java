package com.example.placetag;

import com.example.placetag.model.Place;

import java.util.Locale;

public final class PlaceFilters {
    private PlaceFilters() {
    }

    public static boolean isPending(Place place) {
        return place != null && !Place.SYNCED.equals(place.syncStatus);
    }

    public static boolean isSynced(Place place) {
        return place != null && Place.SYNCED.equals(place.syncStatus);
    }

    public static boolean matchesSearch(Place place, String search) {
        String query = normalize(search);
        if (query.isEmpty()) {
            return true;
        }
        if (place == null) {
            return false;
        }
        String haystack = normalize(place.title) + " "
                + normalize(place.address) + " "
                + normalize(place.description);
        return haystack.contains(query);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
