package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class RegistrationActivity extends Activity {

    private static final int RC_SIGN_IN = 9001;

    DatabaseHelper db;
    GoogleSignInClient googleSignInClient;

    EditText firstNameInput, lastNameInput, contactInput, emailInput, passwordInput;
    Button createAccountButton, btnRoleUser, btnRoleHost;
    TextView alreadyHaveAccountText;
    LinearLayout stepGoogle, stepForm;
    String selectedRole = "user";
    boolean googleLinked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        db = new DatabaseHelper(this);

        // Google Sign-In setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Views
        stepGoogle = findViewById(R.id.step_google);
        stepForm   = findViewById(R.id.step_form);

        firstNameInput      = findViewById(R.id.reg_firstname);
        lastNameInput       = findViewById(R.id.reg_lastname);
        contactInput        = findViewById(R.id.reg_contact);
        emailInput          = findViewById(R.id.reg_email);
        passwordInput       = findViewById(R.id.reg_password);
        createAccountButton = findViewById(R.id.btn_create_account_submit);
        alreadyHaveAccountText = findViewById(R.id.already_have_account);
        btnRoleUser = findViewById(R.id.btn_role_user);
        btnRoleHost = findViewById(R.id.btn_role_host);

        // Eye toggle for password
        View togglePwd = findViewById(R.id.toggle_reg_password);
        if (togglePwd != null) {
            togglePwd.setOnClickListener(v ->
                togglePassword(passwordInput, (android.widget.TextView) v));
        }

        // Step 1 — Google button (required, no skip allowed)
        findViewById(R.id.btn_google_signin).setOnClickListener(v -> launchGoogleSignIn());

        // If redirected from LoginActivity with Google account info, skip step 1
        String googleEmail = getIntent().getStringExtra("GOOGLE_EMAIL");
        String googleName  = getIntent().getStringExtra("GOOGLE_NAME");
        if (googleEmail != null) {
            googleLinked = true;
            // Split display name: last word = last name, everything before = first name
            String firstName = "", lastName = "";
            if (googleName != null && !googleName.trim().isEmpty()) {
                String[] parts = googleName.trim().split(" ");
                lastName = parts[parts.length - 1];
                firstName = googleName.trim().substring(0, googleName.trim().length() - lastName.length()).trim();
            }
            showForm(firstName, lastName, googleEmail);
        }

        // Role toggle
        btnRoleUser.setOnClickListener(v -> {
            selectedRole = "user";
            btnRoleUser.setBackgroundResource(R.drawable.role_btn_selected);
            btnRoleHost.setBackgroundResource(R.drawable.role_btn_unselected);
        });
        btnRoleHost.setOnClickListener(v -> {
            selectedRole = "host";
            btnRoleHost.setBackgroundResource(R.drawable.role_btn_selected);
            btnRoleUser.setBackgroundResource(R.drawable.role_btn_unselected);
        });

        // Submit
        createAccountButton.setOnClickListener(v -> submitRegistration());

        alreadyHaveAccountText.setOnClickListener(v -> {
            startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
            finish();
        });
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

    private void launchGoogleSignIn() {
        // Sign out first so the account picker always shows
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                // Use given/family name directly — more accurate than splitting display name
                String firstName = account.getGivenName() != null ? account.getGivenName() : "";
                String lastName  = account.getFamilyName() != null ? account.getFamilyName() : "";
                String email     = account.getEmail() != null ? account.getEmail() : "";
                googleLinked = true;
                showForm(firstName, lastName, email);
            } catch (ApiException e) {
                StyledAlert.show(this, StyledAlert.ERROR, "Google Sign-In Failed",
                    "Could not sign in with Google. A Google account is required to register.");
            }
        }
    }

    private void showForm(String firstName, String lastName, String email) {
        stepGoogle.setVisibility(View.GONE);
        stepForm.setVisibility(View.VISIBLE);

        if (firstName != null && !firstName.isEmpty()) firstNameInput.setText(firstName);
        if (lastName  != null && !lastName.isEmpty())  lastNameInput.setText(lastName);

        if (email != null && !email.isEmpty()) {
            emailInput.setText(email);
            // Lock email field if it came from Google
            emailInput.setFocusable(false);
            emailInput.setFocusableInTouchMode(false);
            emailInput.setAlpha(0.7f);
        }
    }

    private void submitRegistration() {
        // Must have signed in with Google
        if (!googleLinked) {
            StyledAlert.show(this, StyledAlert.WARNING, "Google Required",
                "You must sign in with Google to create an account.");
            return;
        }

        String firstName = firstNameInput.getText().toString().trim();
        String lastName  = lastNameInput.getText().toString().trim();
        String contact   = contactInput.getText().toString().trim();
        String email     = emailInput.getText().toString().trim();
        String password  = passwordInput.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || contact.isEmpty() || email.isEmpty()) {
            StyledAlert.show(this, StyledAlert.WARNING, "Missing Fields",
                "Please fill out all the fields to continue.");
            return;
        }
        if (!contact.matches("\\d{11}")) {
            StyledAlert.show(this, StyledAlert.WARNING, "Invalid Contact Number",
                "Contact number must be exactly 11 digits (e.g. 09123456789).");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            StyledAlert.show(this, StyledAlert.WARNING, "Invalid Email",
                "Please enter a valid email address.");
            return;
        }
        if (db.checkEmailExists(email)) {
            StyledAlert.show(this, StyledAlert.ERROR, "Email Taken",
                "This email is already registered. Try logging in instead.");
            return;
        }
        // If Google-linked, allow empty password (set a placeholder)
        if (!googleLinked && password.length() < 6) {
            StyledAlert.show(this, StyledAlert.WARNING, "Weak Password",
                "Password must be at least 6 characters long.");
            return;
        }

        // Use a placeholder password for Google-linked accounts
        String finalPassword = googleLinked && password.isEmpty() ? "google_auth_" + email : password;

        boolean success = db.addUser(firstName, lastName, contact, email, finalPassword, selectedRole);
        if (success) {
            String roleLabel = selectedRole.equals("host") ? "Host" : "Guest";
            StyledAlert.show(this, StyledAlert.SUCCESS, "Account Created!",
                "Welcome to stayFinder as a " + roleLabel + "! You can now log in.");
            new android.os.Handler().postDelayed(() -> {
                startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                finish();
            }, 1800);
        } else {
            StyledAlert.show(this, StyledAlert.ERROR, "Registration Failed",
                "Something went wrong. Please try again.");
        }
    }
}


