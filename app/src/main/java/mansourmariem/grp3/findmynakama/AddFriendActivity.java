package mansourmariem.grp3.findmynakama;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * ACTIVITÉ POUR AJOUTER UN AMI
 * Permet d'envoyer une demande d'ami en utilisant l'email
 */
public class AddFriendActivity extends AppCompatActivity {

    // VARIABLES
    private EditText friendEmailEditText;
    private Button addFriendButton;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        // INITIALISATION
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Récupération des vues
        friendEmailEditText = findViewById(R.id.friendEmailEditText);
        addFriendButton = findViewById(R.id.addFriendButton);
        progressBar = findViewById(R.id.progressBar);

        // Configuration du bouton
        addFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFriend();
            }
        });

        // Activer le bouton de retour dans la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * MÉTHODE POUR AJOUTER UN AMI
     */
    private void addFriend() {
        String friendEmail = friendEmailEditText.getText().toString().trim();

        // VALIDATION
        if (TextUtils.isEmpty(friendEmail)) {
            friendEmailEditText.setError("Entrez l'email de votre ami");
            friendEmailEditText.requestFocus();
            return;
        }

        if (currentUser != null && friendEmail.equals(currentUser.getEmail())) {
            Toast.makeText(this, "Vous ne pouvez pas vous ajouter vous-même !",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // AFFICHER LE CHARGEMENT
        progressBar.setVisibility(View.VISIBLE);
        addFriendButton.setEnabled(false);

        // RECHERCHER L'UTILISATEUR PAR EMAIL
        // On cherche dans la collection "users" un document où l'email correspond
        db.collection("users")
                .whereEqualTo("email", friendEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // AUCUN UTILISATEUR TROUVÉ
                        progressBar.setVisibility(View.GONE);
                        addFriendButton.setEnabled(true);
                        Toast.makeText(this, "Aucun utilisateur trouvé avec cet email",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // UTILISATEUR TROUVÉ
                        DocumentSnapshot friendDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String friendId = friendDoc.getId();
                        String friendName = friendDoc.getString("name");

                        // Ajouter la relation d'amitié
                        sendFriendRequest(friendId, friendName);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    addFriendButton.setEnabled(true);
                    Toast.makeText(this, "Erreur : " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * MÉTHODE POUR ENVOYER UNE DEMANDE D'AMI
     * Dans cette version simplifiée, l'ami est ajouté automatiquement (pas de système d'approbation)
     */
    private void sendFriendRequest(String friendId, String friendName) {
        if (currentUser == null) return;

        String currentUserId = currentUser.getUid();

        // AJOUTER L'AMI DANS MON DOCUMENT
        // On met à jour le champ "friends" avec un nouveau Map
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("friends." + friendId, true); // true = ami accepté

        db.collection("users")
                .document(currentUserId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    // AJOUTER LA RELATION RÉCIPROQUE
                    // L'ami doit aussi m'avoir dans sa liste
                    Map<String, Object> reciprocalData = new HashMap<>();
                    reciprocalData.put("friends." + currentUserId, true);

                    db.collection("users")
                            .document(friendId)
                            .update(reciprocalData)
                            .addOnSuccessListener(aVoid2 -> {
                                // SUCCÈS
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this,
                                        friendName + " a été ajouté à vos amis !",
                                        Toast.LENGTH_SHORT).show();

                                // Retourner à l'écran précédent
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                addFriendButton.setEnabled(true);
                                Toast.makeText(this,
                                        "Erreur lors de l'ajout : " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    addFriendButton.setEnabled(true);
                    Toast.makeText(this, "Erreur : " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Gérer le bouton de retour
        finish();
        return true;
    }
}