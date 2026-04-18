package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends Activity {

    private static final int RC_GOOGLE_LOGIN = 9002;

    DatabaseHelper db;
    GoogleSignInClient googleSignInClient;
    EditText emailInput, passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new DatabaseHelper(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        emailInput    = findViewById(R.id.login_email);
        passwordInput = findViewById(R.id.login_password);

        // Eye toggle for password
        findViewById(R.id.toggle_login_password).setOnClickListener(v -> togglePassword(passwordInput, (android.widget.TextView) v));

        // Email + password login
        findViewById(R.id.btn_sign_in).setOnClickListener(v -> {
            String email    = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                StyledAlert.show(this, StyledAlert.WARNING, "Missing Fields",
                    "Please enter your email and password.");
                return;
            }
            if (db.checkUser(email, password)) {
                navigateHome(email);
            } else {
                StyledAlert.show(this, StyledAlert.ERROR, "Login Failed",
                    "Invalid email or password. Please try again.");
            }
        });

        // Google Sign-In login
        findViewById(R.id.btn_google_login).setOnClickListener(v -> {
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_GOOGLE_LOGIN);
            });
        });

        // Create account
        findViewById(R.id.btn_create_account).setOnClickListener(v ->
            startActivity(new Intent(LoginActivity.this, RegistrationActivity.class)));

        // Forgot password
        findViewById(R.id.forgot_password).setOnClickListener(v -> showForgotPasswordDialog());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_LOGIN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String email = account.getEmail();
                if (email == null) {
                    StyledAlert.show(this, StyledAlert.ERROR, "Sign-In Failed",
                        "Could not retrieve your Google email.");
                    return;
                }
                if (db.checkEmailExists(email)) {
                    // Account exists — log in directly
                    navigateHome(email);
                } else {
                    // No account yet — redirect to registration with email pre-filled
                    StyledAlert.confirm(this, "No Account Found",
                        "No stayFinder account found for " + email + ". Would you like to create one?",
                        "Register", "Cancel", () -> {
                            Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                            intent.putExtra("GOOGLE_EMAIL", email);
                            intent.putExtra("GOOGLE_NAME", account.getDisplayName());
                            startActivity(intent);
                        });
                }
            } catch (ApiException e) {
                StyledAlert.show(this, StyledAlert.ERROR, "Google Sign-In Failed",
                    "Could not sign in with Google. Please try again.");
            }
        }
    }

    private void navigateHome(String email) {
        String role = db.getUserRole(email);
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.putExtra("USER_EMAIL", email);
        intent.putExtra("USER_ROLE", role);
        startActivity(intent);
        finish();
    }

    private static void togglePassword(android.widget.EditText field, android.widget.TextView eyeBtn) {
        int variation = field.getInputType() & android.text.InputType.TYPE_MASK_VARIATION;
        boolean isHidden = variation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                        || variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
        if (isHidden) {
            field.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            eyeBtn.setText("🙈");
        } else {
            field.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            eyeBtn.setText("👁");
        }
        field.setSelection(field.getText().length());
    }

    private void showForgotPasswordDialog() {        android.widget.EditText emailInput = new android.widget.EditText(this);
        emailInput.setHint("Enter your registered email");
        emailInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setPadding(dp(16), dp(12), dp(16), dp(12));

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(8));
        layout.addView(emailInput);

        new android.app.AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your registered email address and we'll send you a temporary password.")
            .setView(layout)
            .setPositiveButton("Send", (d, w) -> {
                String email = emailInput.getText().toString().trim();
                if (email.isEmpty()) {
                    StyledAlert.show(this, StyledAlert.WARNING, "Email Required",
                        "Please enter your email address.");
                    return;
                }
                if (!db.checkEmailExists(email)) {
                    StyledAlert.show(this, StyledAlert.ERROR, "Email Not Found",
                        "No account found with that email address.");
                    return;
                }
                sendTempPassword(email);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void sendTempPassword(String email) {
        // Show loading
        StyledAlert.show(this, StyledAlert.INFO, "Sending...",
            "Sending a temporary password to " + email);

        String tempPassword = EmailSender.generateTempPassword();

        EmailSender.sendPasswordReset(email, tempPassword, new EmailSender.EmailCallback() {
            @Override
            public void onSuccess() {
                // Save the temp password to DB
                db.updatePassword(email, tempPassword);
                StyledAlert.show(LoginActivity.this, StyledAlert.SUCCESS,
                    "Email Sent! ✉️",
                    "A temporary password has been sent to " + email
                    + ".\n\nPlease check your inbox and log in with the temporary password, "
                    + "then change it in your profile settings.");
            }

            @Override
            public void onFailure(String error) {
                StyledAlert.show(LoginActivity.this, StyledAlert.ERROR,
                    "Failed to Send Email",
                    "Error: " + error);
            }
        });
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }
}


