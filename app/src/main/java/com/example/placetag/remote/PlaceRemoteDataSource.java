package com.example.placetag.remote;

import com.example.placetag.model.Place;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PlaceRemoteDataSource {
    // URL de l'API PHP, par exemple http://10.0.2.2/placetag/places.php.
    private final String baseUrl;

    public PlaceRemoteDataSource(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int save(Place place) throws Exception {
        // POST: envoie un lieu local a l'API PHP pour insertion/update MySQL.
        HttpURLConnection connection = open("POST");
        try {
            JSONObject body = toJson(place);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    connection.getOutputStream(),
                    StandardCharsets.UTF_8
            ))) {
                writer.write(body.toString());
            }

            int code = connection.getResponseCode();
            // Les codes 2xx signifient succes HTTP.
            if (!isSuccessful(code)) {
                throw httpException(connection, code);
            }

            String response = read(connection.getInputStream());
            if (response.trim().isEmpty()) {
                return place.remoteId == null ? -1 : place.remoteId;
            }
            JSONObject json = new JSONObject(response);
            // L'API renvoie l'id distant cree dans MySQL.
            return json.optInt("remoteId", json.optInt("id", place.remoteId == null ? -1 : place.remoteId));
        } finally {
            connection.disconnect();
        }
    }

    public List<Place> loadAll() throws Exception {
        // GET: recupere tous les lieux sauvegardes cote MySQL.
        HttpURLConnection connection = open("GET");
        try {
            int code = connection.getResponseCode();
            if (!isSuccessful(code)) {
                throw httpException(connection, code);
            }

            JSONArray array = new JSONArray(read(connection.getInputStream()));
            List<Place> places = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.getJSONObject(i);
                Place place = new Place();
                place.remoteId = optRemoteId(json);
                place.title = optString(json, "title");
                place.description = optNullableString(json, "description");
                place.latitude = json.optDouble("latitude");
                place.longitude = json.optDouble("longitude");
                place.address = optNullableString(json, "address");
                place.photoPath = optNullableString(json, "photoName");
                place.createdAt = optString(json, "createdAt");
                place.syncStatus = Place.SYNCED;
                places.add(place);
            }
            return places;
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection open(String method) throws Exception {
        // Configuration commune de la connexion HTTP.
        URL url = new URL(baseUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(6000);
        connection.setReadTimeout(6000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setUseCaches(false);
        if ("POST".equals(method)) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        }
        return connection;
    }

    private JSONObject toJson(Place place) throws Exception {
        // Transforme notre objet Java en JSON comprehensible par l'API PHP.
        JSONObject json = new JSONObject();
        if (place.remoteId != null) {
            json.put("remoteId", place.remoteId);
        }
        json.put("title", place.title);
        json.put("description", place.description == null ? JSONObject.NULL : place.description);
        json.put("latitude", place.latitude);
        json.put("longitude", place.longitude);
        json.put("address", place.address == null ? JSONObject.NULL : place.address);
        json.put("photoName", place.photoPath == null ? JSONObject.NULL : place.photoPath);
        json.put("createdAt", place.createdAt);
        return json;
    }

    private String read(InputStream stream) throws Exception {
        // Lit toute la reponse HTTP sous forme de texte.
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    private boolean isSuccessful(int code) {
        return code >= 200 && code < 300;
    }

    private IllegalStateException httpException(HttpURLConnection connection, int code) throws Exception {
        String details = read(connection.getErrorStream()).trim();
        if (details.isEmpty()) {
            details = connection.getResponseMessage();
        }
        return new IllegalStateException(details == null || details.isEmpty()
                ? "HTTP " + code
                : "HTTP " + code + ": " + details);
    }

    private Integer optRemoteId(JSONObject json) {
        if (json.has("remoteId") && !json.isNull("remoteId")) {
            return json.optInt("remoteId");
        }
        if (json.has("id") && !json.isNull("id")) {
            return json.optInt("id");
        }
        return null;
    }

    private String optString(JSONObject json, String key) {
        String value = optNullableString(json, key);
        return value == null ? "" : value;
    }

    private String optNullableString(JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            return null;
        }
        return json.optString(key, null);
    }
}
