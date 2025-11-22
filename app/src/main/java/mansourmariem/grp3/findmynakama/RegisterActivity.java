package mansourmariem.grp3.findmynakama;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * ACTIVITÉ D'INSCRIPTION
 * Permet à un nouvel utilisateur de créer un compte
 */
public class RegisterActivity extends AppCompatActivity {

    // DÉCLARATION DES VARIABLES
    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private ProgressBar progressBar;

    // Firebase Authentication et Firestore
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // INITIALISATION DE FIREBASE
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Base de données Firestore

        // RÉCUPÉRATION DES ÉLÉMENTS DE L'INTERFACE
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);

        // CONFIGURATION DU BOUTON D'INSCRIPTION
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    /**
     * MÉTHODE POUR INSCRIRE UN NOUVEL UTILISATEUR
     */
    private void registerUser() {
        // RÉCUPÉRATION DES VALEURS
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // VALIDATION DES DONNÉES

        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Le nom est requis");
            nameEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("L'email est requis");
            emailEditText.requestFocus();
            return;
        }

        // Patterns.EMAIL_ADDRESS vérifie que l'email a un format valide
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Entrez un email valide");
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Le mot de passe est requis");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Le mot de passe doit contenir au moins 6 caractères");
            passwordEditText.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Les mots de passe ne correspondent pas");
            confirmPasswordEditText.requestFocus();
            return;
        }

        // AFFICHER LE CERCLE DE CHARGEMENT
        progressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);

        // CRÉATION DU COMPTE AVEC FIREBASE
        // createUserWithEmailAndPassword() crée un nouveau compte
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // COMPTE CRÉÉ AVEC SUCCÈS
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            // ENREGISTRER LES INFORMATIONS DE L'UTILISATEUR DANS FIRESTORE
                            // On crée un "document" avec les infos de l'utilisateur
                            saveUserToFirestore(user.getUid(), name, email);
                        }
                    } else {
                        // ÉCHEC DE LA CRÉATION DU COMPTE
                        progressBar.setVisibility(View.GONE);
                        registerButton.setEnabled(true);

                        String errorMessage = "Échec de l'inscription";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(RegisterActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * MÉTHODE POUR SAUVEGARDER LES DONNÉES DE L'UTILISATEUR DANS FIRESTORE
     * On stocke le nom, l'email et d'autres infos utiles
     */
    private void saveUserToFirestore(String userId, String name, String email) {
        // CRÉATION D'UN MAP (dictionnaire) AVEC LES DONNÉES
        // Un Map est comme un tableau associatif : clé => valeur
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("sharingLocation", false); // Par défaut, ne partage pas sa position
        user.put("createdAt", System.currentTimeMillis()); // Timestamp de création
        user.put("friends", new HashMap<>()); // Liste d'amis vide au départ

        // ENREGISTREMENT DANS FIRESTORE
        // Collection "users" > Document avec l'ID de l'utilisateur
        db.collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    // SUCCÈS
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(RegisterActivity.this,
                            "Inscription réussie !",
                            Toast.LENGTH_SHORT).show();

                    // Aller à l'écran principal
                    goToMainActivity();
                })
                .addOnFailureListener(e -> {
                    // ÉCHEC
                    progressBar.setVisibility(View.GONE);
                    registerButton.setEnabled(true);
                    Toast.makeText(RegisterActivity.this,
                            "Erreur lors de l'enregistrement : " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * MÉTHODE POUR ALLER À L'ÉCRAN PRINCIPAL
     */
    private void goToMainActivity() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}