package com.example.placetag;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.placetag.data.PlaceDao;
import com.example.placetag.data.PlaceDbHelper;
import com.example.placetag.model.Place;
import com.example.placetag.remote.PlaceRemoteDataSource;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    // Code utilise pour reconnaitre la reponse Android apres une demande de permissions.
    private static final int PERMISSION_REQUEST = 42;

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private LinearLayout screenMap;
    private ScrollView screenForm;
    private LinearLayout screenPlaces;
    private LinearLayout screenSync;

    private TextView currentAddress;
    private TextView currentCoordinates;
    private TextView formAddress;
    private EditText inputTitle;
    private EditText inputDescription;
    private EditText inputSearch;
    private ImageView photoPreview;
    private Button buttonSavePlace;
    private Button buttonDeletePlace;
    private RadioGroup filterGroup;
    private LinearLayout placesContainer;
    private TextView syncOutput;

    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private String lastAddress;
    private boolean requestingLocationUpdates;

    private PlaceDao placeDao;
    private PlaceRemoteDataSource remoteDataSource;
    private long editingPlaceId = -1;
    private String currentPhotoPath;
    private String pendingPhotoPath;
    private Uri pendingPhotoUri;
    private ActivityResultLauncher<Uri> takePictureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lie cette Activity au layout XML activity_main.xml.
        setContentView(R.layout.activity_main);

        // Initialisation des couches de donnees: SQLite local, API distante, localisation Android.
        placeDao = new PlaceDao(new PlaceDbHelper(this));
        remoteDataSource = new PlaceRemoteDataSource(getString(R.string.api_base_url));
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        lastAddress = getString(R.string.waiting_location);

        // Separation volontaire: chaque methode prepare une responsabilite de l'ecran.
        bindViews();
        setupMap();
        setupToolbarAndDrawer();
        setupPhotoLauncher();
        setupLocationCallback();
        setupActions();

        requestNeededPermissions();
        showScreen(screenMap);
        refreshPlaces();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // onResume: l'ecran redevient actif, donc on relance carte et localisation.
        map.onResume();
        startLocationUpdatesIfAllowed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // onPause: on arrete les mises a jour pour economiser batterie et ressources.
        map.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        requestingLocationUpdates = false;
    }

    private void bindViews() {
        // findViewById connecte les vues XML avec les variables Java.
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.navigation_view);
        screenMap = findViewById(R.id.screen_map);
        screenForm = findViewById(R.id.screen_form);
        screenPlaces = findViewById(R.id.screen_places);
        screenSync = findViewById(R.id.screen_sync);
        map = findViewById(R.id.map);
        currentAddress = findViewById(R.id.current_address);
        currentCoordinates = findViewById(R.id.current_coordinates);
        formAddress = findViewById(R.id.form_address);
        inputTitle = findViewById(R.id.input_title);
        inputDescription = findViewById(R.id.input_description);
        inputSearch = findViewById(R.id.input_search);
        photoPreview = findViewById(R.id.photo_preview);
        buttonSavePlace = findViewById(R.id.button_save_place);
        buttonDeletePlace = findViewById(R.id.button_delete_place);
        filterGroup = findViewById(R.id.filter_group);
        placesContainer = findViewById(R.id.places_container);
        syncOutput = findViewById(R.id.sync_output);
    }

    private void setupMap() {
        // osmdroid affiche OpenStreetMap sans cle Google Maps.
        Configuration.getInstance().setUserAgentValue(getPackageName());
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(16.0);
        map.getController().setCenter(new GeoPoint(48.8566, 2.3522));
    }

    private void setupToolbarAndDrawer() {
        // Le DrawerLayout est le menu lateral de navigation.
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_center) {
                centerMapOnLastLocation();
                return true;
            }
            return false;
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            // Ici on ne change pas d'Activity: on affiche/cache des blocs dans MainActivity.
            if (id == R.id.nav_map) {
                showScreen(screenMap);
            } else if (id == R.id.nav_new_place) {
                prepareNewPlaceForm();
                showScreen(screenForm);
            } else if (id == R.id.nav_places) {
                refreshPlaces();
                showScreen(screenPlaces);
            } else if (id == R.id.nav_sync) {
                showScreen(screenSync);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupPhotoLauncher() {
        // Lance l'application camera et recupere le resultat dans ce callback.
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && pendingPhotoUri != null && pendingPhotoPath != null) {
                currentPhotoPath = pendingPhotoPath;
                photoPreview.setImageURI(pendingPhotoUri);
            } else {
                deletePendingPhoto();
            }
            pendingPhotoUri = null;
            pendingPhotoPath = null;
        });
    }

    private void setupActions() {
        // Tous les clics des boutons sont branches ici.
        findViewById(R.id.button_save_here).setOnClickListener(v -> {
            prepareNewPlaceForm();
            showScreen(screenForm);
        });
        findViewById(R.id.button_take_photo).setOnClickListener(v -> takePhoto());
        buttonSavePlace.setOnClickListener(v -> savePlaceFromForm());
        buttonDeletePlace.setOnClickListener(v -> deleteEditingPlace());
        findViewById(R.id.button_clear_form).setOnClickListener(v -> prepareNewPlaceForm());
        findViewById(R.id.button_sync_pending).setOnClickListener(v -> syncPendingPlaces());
        findViewById(R.id.button_load_remote).setOnClickListener(v -> loadRemotePlaces());
        filterGroup.setOnCheckedChangeListener((group, checkedId) -> refreshPlaces());
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshPlaces();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        inputSearch.setOnEditorActionListener((v, actionId, event) -> {
            refreshPlaces();
            return false;
        });
        inputSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                refreshPlaces();
            }
        });
    }

    private void setupLocationCallback() {
        // Callback appele a chaque nouvelle position recue par Android.
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateLocation(location);
                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdatesIfAllowed() {
        // On ne peut pas utiliser la localisation sans permission runtime.
        if (!hasLocationPermission() || requestingLocationUpdates) {
            return;
        }
        // Configuration: precision elevee, mise a jour toutes les quelques secondes.
        LocationRequest request = LocationRequest.create();
        request.setInterval(5000);
        request.setFastestInterval(2000);
        request.setSmallestDisplacement(5);
        request.setPriority(hasFineLocationPermission()
                ? LocationRequest.PRIORITY_HIGH_ACCURACY
                : LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                updateLocation(location);
            }
        });
        requestingLocationUpdates = true;
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
                .addOnFailureListener(error -> requestingLocationUpdates = false);
    }

    private void updateLocation(Location location) {
        // Une nouvelle position met a jour le texte, la carte et l'adresse.
        lastLocation = location;
        String coords = String.format(Locale.US, "%.6f, %.6f", location.getLatitude(), location.getLongitude());
        currentCoordinates.setText(coords);
        formAddress.setText(coords);
        updateMapMarker(location);
        resolveAddress(location);
    }

    private void updateMapMarker(Location location) {
        // Conversion latitude/longitude Android vers GeoPoint utilise par osmdroid.
        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
        map.getOverlays().clear();
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(getString(R.string.current_position));
        map.getOverlays().add(marker);
        map.getController().animateTo(point);
        map.invalidate();
    }

    private void centerMapOnLastLocation() {
        // Bouton toolbar: recentrer la carte sur la derniere position connue.
        if (lastLocation != null) {
            GeoPoint point = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
            map.getController().setZoom(16.0);
            map.getController().animateTo(point);
        }
    }

    private void resolveAddress(Location location) {
        // Le geocodage peut etre lent: on le lance dans un thread secondaire.
        new Thread(() -> {
            String addressText = getString(R.string.unknown_address);
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (results != null && !results.isEmpty()) {
                    Address address = results.get(0);
                    String line = address.getAddressLine(0);
                    if (line != null && !line.trim().isEmpty()) {
                        addressText = line;
                    }
                }
            } catch (Exception ignored) {
            }
            String finalAddressText = addressText;
            runOnUiThread(() -> {
                // Seul le thread UI a le droit de modifier les TextView.
                lastAddress = finalAddressText;
                currentAddress.setText(finalAddressText);
                formAddress.setText(finalAddressText);
            });
        }).start();
    }

    private void prepareNewPlaceForm() {
        // Remet le formulaire en mode creation d'un nouveau lieu.
        editingPlaceId = -1;
        currentPhotoPath = null;
        pendingPhotoPath = null;
        pendingPhotoUri = null;
        inputTitle.setText("");
        inputDescription.setText("");
        formAddress.setText(lastAddress);
        photoPreview.setImageDrawable(null);
        buttonSavePlace.setText(R.string.save_place);
        buttonDeletePlace.setVisibility(View.GONE);
    }

    private void savePlaceFromForm() {
        String title = inputTitle.getText().toString().trim();
        // Validation simple: un lieu doit avoir un titre.
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.missing_title, Toast.LENGTH_SHORT).show();
            return;
        }
        if (lastLocation == null && editingPlaceId < 0) {
            Toast.makeText(this, R.string.missing_location, Toast.LENGTH_SHORT).show();
            return;
        }

        // Si editingPlaceId >= 0, on modifie un lieu existant, sinon on cree un nouveau lieu.
        Place place = editingPlaceId >= 0 ? placeDao.getById(editingPlaceId) : new Place();
        if (place == null) {
            return;
        }
        place.title = title;
        place.description = inputDescription.getText().toString().trim();
        if (editingPlaceId < 0) {
            place.latitude = lastLocation.getLatitude();
            place.longitude = lastLocation.getLongitude();
            place.createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
        }
        place.address = formAddress.getText().toString();
        place.photoPath = currentPhotoPath;
        // Toute modification locale doit etre resynchronisee avec le serveur.
        place.syncStatus = Place.LOCAL_ONLY;

        if (editingPlaceId >= 0) {
            placeDao.update(place);
        } else {
            placeDao.insert(place);
        }
        Toast.makeText(this, R.string.place_saved, Toast.LENGTH_SHORT).show();
        refreshPlaces();
        showScreen(screenPlaces);
    }

    private void editPlace(Place place) {
        // Charge un lieu existant dans le formulaire.
        editingPlaceId = place.idLocal;
        currentPhotoPath = place.photoPath;
        inputTitle.setText(place.title);
        inputDescription.setText(place.description);
        formAddress.setText(place.address);
        if (place.photoPath != null && new File(place.photoPath).exists()) {
            photoPreview.setImageURI(Uri.fromFile(new File(place.photoPath)));
        } else {
            photoPreview.setImageDrawable(null);
        }
        buttonSavePlace.setText(R.string.update_place);
        buttonDeletePlace.setVisibility(View.VISIBLE);
        showScreen(screenForm);
    }

    private void deleteEditingPlace() {
        // Suppression locale SQLite du lieu actuellement ouvert.
        if (editingPlaceId >= 0) {
            placeDao.delete(editingPlaceId);
            Toast.makeText(this, R.string.place_deleted, Toast.LENGTH_SHORT).show();
            prepareNewPlaceForm();
            refreshPlaces();
            showScreen(screenPlaces);
        }
    }

    private void refreshPlaces() {
        // Reconstruit la liste a partir de SQLite, puis applique recherche + filtre.
        placesContainer.removeAllViews();
        List<Place> places = placeDao.getAll();
        String search = inputSearch.getText().toString();
        int filterId = filterGroup.getCheckedRadioButtonId();
        int visibleCount = 0;

        for (Place place : places) {
            if (!matchesFilter(place, filterId) || !PlaceFilters.matchesSearch(place, search)) {
                continue;
            }
            placesContainer.addView(createPlaceRow(place));
            visibleCount++;
        }

        if (visibleCount == 0) {
            TextView empty = new TextView(this);
            empty.setText(R.string.places_empty);
            empty.setPadding(0, 24, 0, 0);
            placesContainer.addView(empty);
        }
    }

    private boolean matchesFilter(Place place, int filterId) {
        // Filtre par statut de synchronisation.
        if (filterId == R.id.filter_pending) {
            return PlaceFilters.isPending(place);
        }
        if (filterId == R.id.filter_synced) {
            return PlaceFilters.isSynced(place);
        }
        return true;
    }

    private View createPlaceRow(Place place) {
        // Cree une carte de liste en Java pour chaque lieu sauvegarde.
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
        card.setCardElevation(dp(1));
        card.setRadius(dp(8));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.mint_light));
        card.setStrokeWidth(dp(1));
        card.setContentPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        card.addView(row);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(header);

        File photoFile = localPhotoFile(place.photoPath);
        if (photoFile != null) {
            ImageView thumbnail = new ImageView(this);
            LinearLayout.LayoutParams thumbnailParams = new LinearLayout.LayoutParams(dp(72), dp(72));
            thumbnailParams.setMargins(0, 0, dp(12), 0);
            thumbnail.setLayoutParams(thumbnailParams);
            thumbnail.setContentDescription(getString(R.string.place_photo));
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnail.setImageURI(Uri.fromFile(photoFile));
            header.addView(thumbnail);
        }

        LinearLayout titleColumn = new LinearLayout(this);
        titleColumn.setOrientation(LinearLayout.VERTICAL);
        header.addView(titleColumn, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView title = new TextView(this);
        String titleText = safeText(place.title);
        title.setText(titleText.isEmpty() ? getString(R.string.untitled_place) : titleText);
        title.setTextColor(ContextCompat.getColor(this, R.color.ink));
        title.setTextSize(18);
        title.setTypeface(null, 1);
        titleColumn.addView(title);

        TextView status = new TextView(this);
        status.setText(readableStatus(place.syncStatus));
        status.setTextColor(statusColor(place.syncStatus));
        status.setTextSize(13);
        titleColumn.addView(status);

        TextView details = new TextView(this);
        details.setText(formatPlaceDetails(place));
        details.setPadding(0, dp(8), 0, 0);
        row.addView(details);

        Button open = new Button(this);
        open.setText(R.string.open_place);
        open.setAllCaps(false);
        open.setOnClickListener(v -> editPlace(place));
        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        openParams.setMargins(0, dp(8), 0, 0);
        row.addView(open, openParams);
        return card;
    }

    private String formatPlaceDetails(Place place) {
        StringBuilder details = new StringBuilder();
        String address = safeText(place.address);
        if (!address.isEmpty()) {
            details.append(address).append('\n');
        }
        details.append(String.format(Locale.US, "%.6f, %.6f", place.latitude, place.longitude));

        String createdAt = safeText(place.createdAt);
        if (!createdAt.isEmpty()) {
            details.append('\n').append(createdAt);
        }

        String description = safeText(place.description);
        if (!description.isEmpty()) {
            details.append('\n').append(description);
        }
        return details.toString();
    }

    private int statusColor(String status) {
        if (Place.SYNCED.equals(status)) {
            return ContextCompat.getColor(this, R.color.forest);
        }
        if (Place.SYNC_ERROR.equals(status)) {
            return ContextCompat.getColor(this, R.color.brick);
        }
        return ContextCompat.getColor(this, R.color.ink);
    }

    private File localPhotoFile(String photoPath) {
        if (!hasText(photoPath)) {
            return null;
        }
        File file = new File(photoPath);
        return file.exists() ? file : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String readableStatus(String status) {
        if (Place.SYNCED.equals(status)) {
            return getString(R.string.synced);
        }
        if (Place.SYNC_ERROR.equals(status)) {
            return getString(R.string.sync_error);
        }
        return getString(R.string.local_only);
    }

    private void syncPendingPlaces() {
        syncOutput.setText(R.string.sync_pending);
        // Reseau = thread secondaire pour ne pas bloquer l'interface.
        new Thread(() -> {
            List<Place> places = placeDao.getPendingSync();
            int ok = 0;
            int ko = 0;
            for (Place place : places) {
                try {
                    // Envoie le lieu a l'API PHP/MySQL.
                    int remoteId = remoteDataSource.save(place);
                    if (remoteId > 0) {
                        place.remoteId = remoteId;
                    }
                    place.syncStatus = Place.SYNCED;
                    placeDao.update(place);
                    ok++;
                } catch (Exception e) {
                    place.syncStatus = Place.SYNC_ERROR;
                    placeDao.update(place);
                    ko++;
                }
            }
            int finalOk = ok;
            int finalKo = ko;
            runOnUiThread(() -> {
                // Retour sur le thread UI pour afficher le resultat.
                refreshPlaces();
                syncOutput.setText(getString(R.string.sync_complete) + "\nOK: " + finalOk + "\nErrors: " + finalKo);
            });
        }).start();
    }

    private void loadRemotePlaces() {
        syncOutput.setText(R.string.load_remote);
        // Charge les lieux presents sur MySQL via l'API PHP.
        new Thread(() -> {
            try {
                List<Place> remotePlaces = remoteDataSource.loadAll();
                int imported = 0;
                for (Place remotePlace : remotePlaces) {
                    if (remotePlace.remoteId == null) {
                        continue;
                    }
                    Place existing = placeDao.getByRemoteId(remotePlace.remoteId);
                    if (existing == null) {
                        placeDao.insert(remotePlace);
                    } else {
                        remotePlace.idLocal = existing.idLocal;
                        placeDao.update(remotePlace);
                    }
                    imported++;
                }
                int finalImported = imported;
                runOnUiThread(() -> {
                    refreshPlaces();
                    syncOutput.setText(getString(R.string.sync_complete) + "\nRemote places: " + finalImported);
                });
            } catch (Exception e) {
                runOnUiThread(() -> syncOutput.setText(R.string.sync_failed));
            }
        }).start();
    }

    private void takePhoto() {
        // La camera est une permission dangereuse: il faut verifier avant de l'utiliser.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestNeededPermissions();
            return;
        }
        try {
            File photo = createPhotoFile();
            pendingPhotoPath = photo.getAbsolutePath();
            // FileProvider donne une Uri securisee a l'application camera.
            pendingPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photo);
            takePictureLauncher.launch(pendingPhotoUri);
        } catch (Exception e) {
            deletePendingPhoto();
            pendingPhotoPath = null;
            pendingPhotoUri = null;
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deletePendingPhoto() {
        if (pendingPhotoPath == null) {
            return;
        }
        File file = new File(pendingPhotoPath);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private File createPhotoFile() throws Exception {
        // Les photos sont stockees dans le dossier prive externe de l'application.
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (directory == null) {
            throw new IllegalStateException("Pictures directory unavailable");
        }
        return File.createTempFile("PLACETAG_" + timestamp + "_", ".jpg", directory);
    }

    private void showScreen(View selected) {
        // Navigation interne: un seul bloc visible a la fois.
        screenMap.setVisibility(selected == screenMap ? View.VISIBLE : View.GONE);
        screenForm.setVisibility(selected == screenForm ? View.VISIBLE : View.GONE);
        screenPlaces.setVisibility(selected == screenPlaces ? View.VISIBLE : View.GONE);
        screenSync.setVisibility(selected == screenSync ? View.VISIBLE : View.GONE);
    }

    private boolean hasLocationPermission() {
        // Les permissions de localisation doivent etre accordees avant requestLocationUpdates.
        return hasFineLocationPermission()
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasFineLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNeededPermissions() {
        List<String> permissions = new ArrayList<>();
        if (!hasLocationPermission()) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }

        if (hasLocationPermission()) {
            startLocationUpdatesIfAllowed();
        }

        // Si tout est deja accepte, on peut directement lancer la localisation.
        if (permissions.isEmpty()) {
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                permissions.toArray(new String[0]),
                PERMISSION_REQUEST
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Android revient ici apres la popup de permissions.
        if (requestCode == PERMISSION_REQUEST) {
            if (hasLocationPermission()) {
                startLocationUpdatesIfAllowed();
            } else {
                Toast.makeText(this, R.string.permission_needed, Toast.LENGTH_LONG).show();
            }
        }
    }
}
