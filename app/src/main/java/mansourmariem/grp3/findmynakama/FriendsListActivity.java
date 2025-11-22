package mansourmariem.grp3.findmynakama;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ACTIVITÉ POUR AFFICHER LA LISTE DES AMIS
 * Permet de voir tous ses amis et de les supprimer
 */
public class FriendsListActivity extends AppCompatActivity {

    // VARIABLES
    private RecyclerView friendsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyTextView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private FriendsAdapter adapter;
    private List<Friend> friendsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        // INITIALISATION
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Récupération des vues
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);

        // Configuration du RecyclerView
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendsAdapter(friendsList);
        friendsRecyclerView.setAdapter(adapter);

        // Activer le bouton de retour
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Charger la liste des amis
        loadFriends();
    }

    /**
     * MÉTHODE POUR CHARGER LA LISTE DES AMIS
     */
    private void loadFriends() {
        if (currentUser == null) return;

        progressBar.setVisibility(View.VISIBLE);

        // RÉCUPÉRER LE DOCUMENT DE L'UTILISATEUR ACTUEL
        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Récupérer le Map des amis
                        Map<String, Object> friends = (Map<String, Object>)
                                documentSnapshot.get("friends");

                        if (friends != null && !friends.isEmpty()) {
                            // CHARGER LES INFORMATIONS DE CHAQUE AMI
                            loadFriendsDetails(friends);
                        } else {
                            // Aucun ami
                            progressBar.setVisibility(View.GONE);
                            showEmptyState();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Erreur : " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * MÉTHODE POUR CHARGER LES DÉTAILS DE CHAQUE AMI
     */
    private void loadFriendsDetails(Map<String, Object> friends) {
        friendsList.clear();
        int totalFriends = friends.size();
        final int[] loadedCount = {0};

        for (String friendId : friends.keySet()) {
            // Charger les infos de chaque ami
            db.collection("users")
                    .document(friendId)
                    .get()
                    .addOnSuccessListener(friendDoc -> {
                        if (friendDoc.exists()) {
                            String name = friendDoc.getString("name");
                            String email = friendDoc.getString("email");
                            Boolean sharingLocation = friendDoc.getBoolean("sharingLocation");

                            Friend friend = new Friend(
                                    friendId,
                                    name != null ? name : "Inconnu",
                                    email != null ? email : "",
                                    sharingLocation != null && sharingLocation
                            );
                            friendsList.add(friend);
                        }

                        loadedCount[0]++;
                        if (loadedCount[0] == totalFriends) {
                            // Tous les amis sont chargés
                            progressBar.setVisibility(View.GONE);
                            if (friendsList.isEmpty()) {
                                showEmptyState();
                            } else {
                                hideEmptyState();
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
        }
    }

    /**
     * MÉTHODE POUR SUPPRIMER UN AMI
     */
    private void removeFriend(String friendId, int position) {
        if (currentUser == null) return;

        // DIALOGUE DE CONFIRMATION
        new AlertDialog.Builder(this)
                .setTitle("Supprimer cet ami ?")
                .setMessage("Voulez-vous vraiment supprimer cet ami de votre liste ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    // Supprimer l'ami de ma liste
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("friends." + friendId, null); // null supprime le champ

                    db.collection("users")
                            .document(currentUser.getUid())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                // Supprimer aussi la relation réciproque
                                Map<String, Object> reciprocalUpdates = new HashMap<>();
                                reciprocalUpdates.put("friends." + currentUser.getUid(), null);

                                db.collection("users")
                                        .document(friendId)
                                        .update(reciprocalUpdates);

                                // Mettre à jour l'interface
                                friendsList.remove(position);
                                adapter.notifyItemRemoved(position);

                                if (friendsList.isEmpty()) {
                                    showEmptyState();
                                }

                                Toast.makeText(this, "Ami supprimé",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Erreur : " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private void showEmptyState() {
        emptyTextView.setVisibility(View.VISIBLE);
        friendsRecyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyTextView.setVisibility(View.GONE);
        friendsRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // ========== CLASSE FRIEND (représente un ami) ==========
    private static class Friend {
        String id;
        String name;
        String email;
        boolean sharingLocation;

        Friend(String id, String name, String email, boolean sharingLocation) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.sharingLocation = sharingLocation;
        }
    }

    // ========== ADAPTER POUR LE RECYCLERVIEW ==========
    /**
     * L'Adapter gère l'affichage de chaque élément dans la liste
     */
    private class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

        private List<Friend> friends;

        FriendsAdapter(List<Friend> friends) {
            this.friends = friends;
        }

        @NonNull
        @Override
        public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Créer une nouvelle vue pour chaque élément
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend, parent, false);
            return new FriendViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
            // Remplir la vue avec les données de l'ami
            Friend friend = friends.get(position);
            holder.bind(friend, position);
        }

        @Override
        public int getItemCount() {
            return friends.size();
        }

        // ========== VIEWHOLDER ==========
        /**
         * Le ViewHolder représente une ligne dans la liste
         */
        class FriendViewHolder extends RecyclerView.ViewHolder {

            TextView nameTextView;
            TextView emailTextView;
            TextView statusTextView;
            ImageButton deleteButton;

            FriendViewHolder(@NonNull View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.friendNameTextView);
                emailTextView = itemView.findViewById(R.id.friendEmailTextView);
                statusTextView = itemView.findViewById(R.id.friendStatusTextView);
                deleteButton = itemView.findViewById(R.id.deleteFriendButton);
            }

            void bind(Friend friend, int position) {
                nameTextView.setText(friend.name);
                emailTextView.setText(friend.email);

                // Afficher le statut de partage
                if (friend.sharingLocation) {
                    statusTextView.setText("📍 Partage sa position");
                    statusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    statusTextView.setText("⭕ Ne partage pas sa position");
                    statusTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }

                // Bouton de suppression
                deleteButton.setOnClickListener(v -> removeFriend(friend.id, position));
            }
        }
    }
}