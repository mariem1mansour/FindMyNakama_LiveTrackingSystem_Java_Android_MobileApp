package mansourmariem.grp3.findmynakama;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

/**
 * ACTIVITÉ PRINCIPALE AVEC LA CARTE
 * C'est l'écran principal où on voit la carte avec les positions des amis
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    // CONSTANTES POUR LES PERMISSIONS
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // VARIABLES POUR LA CARTE
    private GoogleMap mMap;
    private Marker myMarker; // Marqueur pour ma position
    private Map<String, Marker> friendMarkers = new HashMap<>(); // Marqueurs des amis

    // VARIABLES FIREBASE
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // VARIABLES POUR LA LOCALISATION
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isLocationSharingEnabled = false;

    // LISTENER FIRESTORE (pour écouter les changements en temps réel)
    private ListenerRegistration userListener;
    private ListenerRegistration friendsListener;

    // BOUTONS FLOTTANTS
    private FloatingActionButton fabToggleSharing;
    private FloatingActionButton fabAddFriend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // INITIALISATION DE FIREBASE
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Vérifier si l'utilisateur est connecté
        if (currentUser == null) {
            // Si pas connecté, retour à l'écran de connexion
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // INITIALISATION DE LA CARTE
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // INITIALISATION DU CLIENT DE LOCALISATION
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // CONFIGURATION DES BOUTONS FLOTTANTS
        fabToggleSharing = findViewById(R.id.fabToggleSharing);
        fabAddFriend = findViewById(R.id.fabAddFriend);

        // Bouton pour activer/désactiver le partage de position
        fabToggleSharing.setOnClickListener(v -> toggleLocationSharing());

        // Bouton pour ajouter un ami
        fabAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddFriendActivity.class);
            startActivity(intent);
        });

        // CONFIGURATION DU CALLBACK DE LOCALISATION
        // Ce code s'exécute chaque fois qu'on reçoit une nouvelle position
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        updateMyLocationOnMap(location);
                        if (isLocationSharingEnabled) {
                            uploadLocationToFirestore(location);
                        }
                    }
                }
            }
        };

        // CHARGER L'ÉTAT DU PARTAGE DE LOCALISATION
        loadUserSettings();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // CONFIGURATION DE LA CARTE
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        // DEMANDER LA PERMISSION DE LOCALISATION
        checkLocationPermission();
    }

    /**
     * MÉTHODE POUR VÉRIFIER ET DEMANDER LA PERMISSION DE LOCALISATION
     */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission accordée
            enableMyLocation();
        } else {
            // Demander la permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée
                enableMyLocation();
            } else {
                // Permission refusée
                Toast.makeText(this, "Permission de localisation refusée",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * MÉTHODE POUR ACTIVER LA LOCALISATION SUR LA CARTE
     */
    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Activer le layer "Ma position" sur la carte
        mMap.setMyLocationEnabled(true);

        // Obtenir la dernière position connue
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Centrer la carte sur ma position
                        LatLng myLatLng = new LatLng(location.getLatitude(),
                                location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15f));
                    }
                });

        // Démarrer les mises à jour de localisation
        startLocationUpdates();
    }

    /**
     * MÉTHODE POUR DÉMARRER LES MISES À JOUR DE LOCALISATION
     * La position sera mise à jour toutes les 10 secondes
     */
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Configuration de la requête de localisation
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10000) // Toutes les 10 secondes
                .setMinUpdateIntervalMillis(5000) // Minimum 5 secondes entre deux mises à jour
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    /**
     * MÉTHODE POUR METTRE À JOUR MA POSITION SUR LA CARTE
     */
    private void updateMyLocationOnMap(Location location) {
        LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (myMarker == null) {
            // Créer un nouveau marqueur
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(myLatLng)
                    .title("Moi")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            myMarker = mMap.addMarker(markerOptions);
        } else {
            // Mettre à jour la position du marqueur existant
            myMarker.setPosition(myLatLng);
        }
    }

    /**
     * MÉTHODE POUR TÉLÉCHARGER MA POSITION VERS FIRESTORE
     */
    private void uploadLocationToFirestore(Location location) {
        if (currentUser == null) return;

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("timestamp", System.currentTimeMillis());

        db.collection("users")
                .document(currentUser.getUid())
                .update(locationData)
                .addOnFailureListener(e -> {
                    // En cas d'erreur silencieuse (pas de toast pour ne pas spam)
                });
    }

    /**
     * MÉTHODE POUR ACTIVER/DÉSACTIVER LE PARTAGE DE LOCALISATION
     */
    private void toggleLocationSharing() {
        isLocationSharingEnabled = !isLocationSharingEnabled;

        // Mettre à jour l'icône du bouton
        if (isLocationSharingEnabled) {
            fabToggleSharing.setImageResource(R.drawable.ic_location_on);
            Toast.makeText(this, "Partage de localisation activé", Toast.LENGTH_SHORT).show();
        } else {
            fabToggleSharing.setImageResource(R.drawable.ic_location_off);
            Toast.makeText(this, "Partage de localisation désactivé", Toast.LENGTH_SHORT).show();
        }

        // Sauvegarder dans Firestore
        if (currentUser != null) {
            db.collection("users")
                    .document(currentUser.getUid())
                    .update("sharingLocation", isLocationSharingEnabled);
        }
    }

    /**
     * MÉTHODE POUR CHARGER LES PARAMÈTRES DE L'UTILISATEUR
     */
    private void loadUserSettings() {
        if (currentUser == null) return;

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean sharing = documentSnapshot.getBoolean("sharingLocation");
                        isLocationSharingEnabled = sharing != null && sharing;

                        // Mettre à jour l'icône
                        if (isLocationSharingEnabled) {
                            fabToggleSharing.setImageResource(R.drawable.ic_location_on);
                        }
                    }

                    // Commencer à écouter les positions des amis
                    listenToFriendsLocations();
                });
    }

    /**
     * MÉTHODE POUR ÉCOUTER LES POSITIONS DES AMIS EN TEMPS RÉEL
     */
    private void listenToFriendsLocations() {
        if (currentUser == null) return;

        // Écouter les changements dans le document de l'utilisateur actuel
        userListener = db.collection("users")
                .document(currentUser.getUid())
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null || documentSnapshot == null) return;

                    // Récupérer la liste des amis
                    Map<String, Object> friends = (Map<String, Object>)
                            documentSnapshot.get("friends");

                    if (friends != null) {
                        for (String friendId : friends.keySet()) {
                            Boolean status = (Boolean) friends.get(friendId);
                            if (status != null && status) {
                                // Ami approuvé, écouter sa position
                                listenToFriendLocation(friendId);
                            }
                        }
                    }
                });
    }

    /**
     * MÉTHODE POUR ÉCOUTER LA POSITION D'UN AMI SPÉCIFIQUE
     */
    private void listenToFriendLocation(String friendId) {
        db.collection("users")
                .document(friendId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null || documentSnapshot == null) return;

                    // Vérifier si l'ami partage sa position
                    Boolean sharingLocation = documentSnapshot.getBoolean("sharingLocation");
                    if (sharingLocation == null || !sharingLocation) {
                        // Ne pas afficher si l'ami ne partage pas
                        removeFriendMarker(friendId);
                        return;
                    }

                    // Récupérer la position
                    Double latitude = documentSnapshot.getDouble("latitude");
                    Double longitude = documentSnapshot.getDouble("longitude");
                    String name = documentSnapshot.getString("name");

                    if (latitude != null && longitude != null) {
                        updateFriendMarker(friendId, latitude, longitude, name);
                    }
                });
    }

    /**
     * MÉTHODE POUR METTRE À JOUR LE MARQUEUR D'UN AMI
     */
    private void updateFriendMarker(String friendId, double latitude, double longitude,
                                    String name) {
        LatLng position = new LatLng(latitude, longitude);
        Marker marker = friendMarkers.get(friendId);

        if (marker == null) {
            // Créer un nouveau marqueur
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(position)
                    .title(name != null ? name : "Ami")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN));
            marker = mMap.addMarker(markerOptions);
            friendMarkers.put(friendId, marker);
        } else {
            // Mettre à jour la position
            marker.setPosition(position);
        }
    }

    /**
     * MÉTHODE POUR SUPPRIMER LE MARQUEUR D'UN AMI
     */
    private void removeFriendMarker(String friendId) {
        Marker marker = friendMarkers.get(friendId);
        if (marker != null) {
            marker.remove();
            friendMarkers.remove(friendId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Arrêter les listeners quand l'app est en arrière-plan
        if (userListener != null) {
            userListener.remove();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Arrêter les mises à jour de localisation
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // ========== MENU EN HAUT À DROITE ==========

    /**
     * MÉTHODE POUR CRÉER LE MENU
     * Cette méthode est appelée automatiquement par Android pour afficher le menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Charger le fichier menu XML
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true; // IMPORTANT : Retourner true pour afficher le menu !
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_friends_list) {
            // Ouvrir la liste des amis
            Intent intent = new Intent(this, FriendsListActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_logout) {
            // FONCTION DE DÉCONNEXION
            performLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * MÉTHODE POUR SE DÉCONNECTER
     * Cette fonction gère toute la procédure de déconnexion
     */
    private void performLogout() {
        // 1. Arrêter les mises à jour de localisation
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // 2. Arrêter tous les listeners Firestore
        if (userListener != null) {
            userListener.remove();
        }

        // 3. Supprimer tous les marqueurs de la carte
        if (myMarker != null) {
            myMarker.remove();
        }
        for (Marker marker : friendMarkers.values()) {
            marker.remove();
        }
        friendMarkers.clear();

        // 4. Désactiver le partage de localisation dans Firestore
        if (currentUser != null && isLocationSharingEnabled) {
            db.collection("users")
                    .document(currentUser.getUid())
                    .update("sharingLocation", false);
        }

        // 5. Se déconnecter de Firebase Auth
        mAuth.signOut();

        // 6. Retourner à l'écran de connexion
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
    }
}