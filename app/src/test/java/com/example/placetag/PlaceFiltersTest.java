package com.example.placetag;

import com.example.placetag.model.Place;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlaceFiltersTest {
    @Test
    public void matchesSearchHandlesMissingTextFields() {
        Place place = new Place();
        place.title = "Cafe";
        place.address = null;
        place.description = null;

        assertTrue(PlaceFilters.matchesSearch(place, "cafe"));
        assertFalse(PlaceFilters.matchesSearch(place, "museum"));
    }

    @Test
    public void matchesSearchIncludesDescription() {
        Place place = new Place();
        place.title = "Station";
        place.address = "Paris";
        place.description = "Best sunset view";

        assertTrue(PlaceFilters.matchesSearch(place, "sunset"));
    }

    @Test
    public void syncStatusFiltersAreNullSafe() {
        Place synced = new Place();
        synced.syncStatus = Place.SYNCED;

        Place local = new Place();
        local.syncStatus = Place.LOCAL_ONLY;

        assertTrue(PlaceFilters.isSynced(synced));
        assertFalse(PlaceFilters.isPending(synced));
        assertTrue(PlaceFilters.isPending(local));
        assertFalse(PlaceFilters.isSynced(null));
    }
}
