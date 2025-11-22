package mansourmariem.grp3.findmynakama;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * ACTIVITÉ DE CONNEXION
 * C'est le premier écran que l'utilisateur voit quand il ouvre l'app
 * Il permet de se connecter avec email et mot de passe
 */
public class LoginActivity extends AppCompatActivity {

    // DÉCLARATION DES VARIABLES
    // EditText = zone de texte où l'utilisateur peut écrire
    private EditText emailEditText, passwordEditText;

    // Button = bouton cliquable
    private Button loginButton;

    // TextView = texte cliquable (pour aller vers l'inscription)
    private TextView registerTextView;

    // ProgressBar = cercle de chargement
    private ProgressBar progressBar;

    // FirebaseAuth = objet qui gère l'authentification avec Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // INITIALISATION DE FIREBASE AUTH
        // C'est comme "démarrer" le système d'authentification
        mAuth = FirebaseAuth.getInstance();

        // RÉCUPÉRATION DES ÉLÉMENTS DE L'INTERFACE
        // findViewById() trouve les éléments du fichier XML par leur ID
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);
        progressBar = findViewById(R.id.progressBar);

        // CONFIGURATION DU BOUTON DE CONNEXION
        // setOnClickListener() = définit ce qui se passe quand on clique
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // CONFIGURATION DU LIEN VERS L'INSCRIPTION
        registerTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent = permet de passer d'une activité à une autre
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // VÉRIFIER SI L'UTILISATEUR EST DÉJÀ CONNECTÉ
        // Si oui, on l'envoie directement à l'écran principal
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // L'utilisateur est déjà connecté
            goToMainActivity();
        }
    }

    /**
     * MÉTHODE POUR CONNECTER L'UTILISATEUR
     * Cette fonction est appelée quand on clique sur le bouton "Connexion"
     */
    private void loginUser() {
        // RÉCUPÉRATION DES VALEURS ENTRÉES PAR L'UTILISATEUR
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // VALIDATION DES DONNÉES
        // On vérifie que l'utilisateur a bien rempli tous les champs

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("L'email est requis");
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

        // AFFICHER LE CERCLE DE CHARGEMENT
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        // CONNEXION AVEC FIREBASE
        // signInWithEmailAndPassword() tente de connecter l'utilisateur
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // Cette partie s'exécute quand Firebase a fini de traiter la demande
                    progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        // CONNEXION RÉUSSIE
                        Toast.makeText(LoginActivity.this,
                                "Connexion réussie !",
                                Toast.LENGTH_SHORT).show();
                        goToMainActivity();
                    } else {
                        // CONNEXION ÉCHOUÉE
                        String errorMessage = "Échec de la connexion";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(LoginActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * MÉTHODE POUR ALLER À L'ÉCRAN PRINCIPAL
     * Appelée quand la connexion est réussie
     */
    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        // FLAG_ACTIVITY_CLEAR_TOP efface toutes les activités précédentes
        // L'utilisateur ne pourra pas revenir en arrière à l'écran de connexion
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Ferme l'activité de connexion
    }
}