package com.example.placetag.model;

public class Place {
    public static final String LOCAL_ONLY = "LOCAL_ONLY";
    public static final String SYNCED = "SYNCED";
    public static final String SYNC_ERROR = "SYNC_ERROR";

    public long idLocal;
    public Integer remoteId;
    public String title;
    public String description;
    public double latitude;
    public double longitude;
    public String address;
    public String photoPath;
    public String createdAt;
    public String syncStatus;

    public Place() {
        syncStatus = LOCAL_ONLY;
    }
}
