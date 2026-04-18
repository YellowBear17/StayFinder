package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProfileActivity extends Activity {

    private static final int PICK_AVATAR = 201;

    DatabaseHelper db;
    String userEmail;
    String firstName, lastName, contact, role;

    TextView profileAvatarLetter, profileFullName, profileMemberSince;
    TextView profileEmail, profileContact, profileRole;
    ImageView profileAvatarImage;
    LinearLayout profileAvatarFallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = new DatabaseHelper(this);
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        profileAvatarLetter   = findViewById(R.id.profile_avatar_letter);
        profileAvatarImage    = findViewById(R.id.profile_avatar_image);
        profileAvatarFallback = findViewById(R.id.profile_avatar_fallback);
        profileFullName       = findViewById(R.id.profile_full_name);
        profileMemberSince    = findViewById(R.id.profile_member_since);
        profileEmail          = findViewById(R.id.profile_email);
        profileContact        = findViewById(R.id.profile_contact);
        profileRole           = findViewById(R.id.profile_role);

        // Make profile image circular via outline clipping
        profileAvatarImage.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        profileAvatarImage.setClipToOutline(true);

        loadProfile();

        // ProfileActivity back
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        // Tap avatar → pick image
        findViewById(R.id.avatar_container).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, PICK_AVATAR);
        });

        findViewById(R.id.btn_edit_profile).setOnClickListener(v -> showEditProfileDialog());
        findViewById(R.id.btn_change_password).setOnClickListener(v -> showChangePasswordDialog());

        // My Trips (guest) / My Listings (host)
        String currentRole = db.getUserRole(userEmail);
        android.widget.TextView myTripsLabel = findViewById(R.id.txt_my_trips_label);
        if ("host".equals(currentRole)) {
            if (myTripsLabel != null) myTripsLabel.setText("My Listings");
        }

        findViewById(R.id.btn_my_trips).setOnClickListener(v -> {
            String role = db.getUserRole(userEmail);
            if ("host".equals(role)) {
                Intent intent = new Intent(ProfileActivity.this, HostDashboardActivity.class);
                intent.putExtra("USER_EMAIL", userEmail);
                startActivity(intent);
            } else {
                Intent intent = new Intent(ProfileActivity.this, TripsActivity.class);
                intent.putExtra("USER_EMAIL", userEmail);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_help).setOnClickListener(v ->
            StyledAlert.show(this, StyledAlert.INFO, "Help & Support",
                "Support features are coming soon. Thank you for your patience!"));

        findViewById(R.id.btn_logout).setOnClickListener(v ->
            StyledAlert.confirm(this, "Log Out", "Are you sure you want to log out?",
                "Log out", "Cancel", () -> {
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }));

        String userRole = getIntent().getStringExtra("USER_ROLE");
        if (userRole == null) userRole = db.getUserRole(userEmail);
        BottomNavHelper.setup(this, userEmail, userRole, "profile");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AVATAR && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            showPhotoPreviewDialog(uri);
        }
    }

    private void showPhotoPreviewDialog(Uri uri) {
        // Build preview layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setPadding(dp(24), dp(24), dp(24), dp(8));
        layout.setBackgroundColor(0xFFFFFFFF);

        // Circular preview image
        ImageView preview = new ImageView(this);
        int size = dp(120);
        android.widget.LinearLayout.LayoutParams imgLp =
            new android.widget.LinearLayout.LayoutParams(size, size);
        imgLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        imgLp.setMargins(0, 0, 0, dp(16));
        preview.setLayoutParams(imgLp);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackground(getDrawable(R.drawable.avatar_circle_large));
        preview.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        preview.setClipToOutline(true);
        try {
            getContentResolver().openInputStream(uri).close();
            preview.setImageURI(uri);
        } catch (Exception ignored) {}

        android.widget.TextView label = new android.widget.TextView(this);
        label.setText("Use this photo as your profile picture?");
        label.setTextSize(14);
        label.setTextColor(0xFF717171);
        label.setGravity(android.view.Gravity.CENTER);

        layout.addView(preview);
        layout.addView(label);

        new android.app.AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("Use this photo", (d, w) -> {
                db.updateProfileImage(userEmail, uri.toString());
                loadAvatarImage(uri.toString());
                StyledAlert.show(this, StyledAlert.SUCCESS, "Photo Updated",
                    "Your profile photo has been saved.");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    private void loadProfile() {
        Cursor cursor = db.getUserByEmail(userEmail);
        if (cursor != null && cursor.moveToFirst()) {
            firstName = cursor.getString(cursor.getColumnIndex("first_name"));
            lastName  = cursor.getString(cursor.getColumnIndex("last_name"));
            contact   = cursor.getString(cursor.getColumnIndex("contact_number"));
            role      = cursor.getString(cursor.getColumnIndex("role"));
            cursor.close();

            profileAvatarLetter.setText(String.valueOf(firstName.charAt(0)).toUpperCase());
            profileFullName.setText(firstName + " " + lastName);
            profileMemberSince.setText("stayFinder " + (role.equals("host") ? "Host" : "Guest"));
            profileEmail.setText(userEmail);
            profileContact.setText(contact);
            profileRole.setText(role.equals("host") ? "Host" : "Guest");
        }

        // Load profile image if set
        String imageUri = db.getProfileImageUri(userEmail);
        loadAvatarImage(imageUri);
    }

    private void loadAvatarImage(String uriStr) {
        if (uriStr != null && !uriStr.isEmpty()) {
            try {
                Uri parsedUri = Uri.parse(uriStr);
                getContentResolver().openInputStream(parsedUri).close();
                profileAvatarImage.setImageURI(parsedUri);
                profileAvatarImage.setVisibility(View.VISIBLE);
                profileAvatarFallback.setVisibility(View.GONE);
                return;
            } catch (Exception ignored) {}
        }
        profileAvatarImage.setVisibility(View.GONE);
        profileAvatarFallback.setVisibility(View.VISIBLE);
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        EditText editFirst   = dialogView.findViewById(R.id.edit_first_name);
        EditText editLast    = dialogView.findViewById(R.id.edit_last_name);
        EditText editContact = dialogView.findViewById(R.id.edit_contact);

        editFirst.setText(firstName);
        editLast.setText(lastName);
        editContact.setText(contact);

        new android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save", (d, w) -> {
                String newFirst   = editFirst.getText().toString().trim();
                String newLast    = editLast.getText().toString().trim();
                String newContact = editContact.getText().toString().trim();

                if (newFirst.isEmpty() || newLast.isEmpty()) {
                    StyledAlert.show(this, StyledAlert.WARNING, "Missing Fields",
                        "Name fields cannot be empty.");
                    return;
                }
                if (db.updateUser(userEmail, newFirst, newLast, newContact)) {
                    StyledAlert.show(this, StyledAlert.SUCCESS, "Profile Updated",
                        "Your profile information has been saved.");
                    loadProfile();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showChangePasswordDialog() {
        final android.widget.EditText inputCurrent = new android.widget.EditText(this);
        inputCurrent.setHint("Current password");
        inputCurrent.setInputType(android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        final android.widget.EditText inputNew = new android.widget.EditText(this);
        inputNew.setHint("New password");
        inputNew.setInputType(android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        final android.widget.EditText inputConfirm = new android.widget.EditText(this);
        inputConfirm.setHint("Confirm new password");
        inputConfirm.setInputType(android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, 0);

        layout.addView(makePasswordRow(inputCurrent));
        layout.addView(makePasswordRow(inputNew));
        layout.addView(makePasswordRow(inputConfirm));

        new android.app.AlertDialog.Builder(this)
            .setTitle("Change password")
            .setView(layout)
            .setPositiveButton("Update", (d, w) -> {
                String current = inputCurrent.getText().toString();
                String newPass = inputNew.getText().toString();
                String confirm = inputConfirm.getText().toString();

                if (!db.checkUser(userEmail, current)) {
                    StyledAlert.show(this, StyledAlert.ERROR, "Wrong Password",
                        "Your current password is incorrect.");
                    return;
                }
                if (newPass.length() < 6) {
                    StyledAlert.show(this, StyledAlert.WARNING, "Weak Password",
                        "New password must be at least 6 characters.");
                    return;
                }
                if (!newPass.equals(confirm)) {
                    StyledAlert.show(this, StyledAlert.ERROR, "Passwords Don't Match",
                        "The new password and confirmation don't match.");
                    return;
                }
                if (db.updatePassword(userEmail, newPass)) {
                    StyledAlert.show(this, StyledAlert.SUCCESS, "Password Updated",
                        "Your password has been changed successfully.");
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private android.widget.FrameLayout makePasswordRow(android.widget.EditText field) {
        android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        frame.setLayoutParams(lp);
        field.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        field.setPadding(dp(4), dp(8), dp(44), dp(8));

        android.widget.TextView eye = new android.widget.TextView(this);
        android.widget.FrameLayout.LayoutParams eyeLp = new android.widget.FrameLayout.LayoutParams(
            dp(44), android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
        eyeLp.gravity = android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL;
        eye.setLayoutParams(eyeLp);
        eye.setText("👁");
        eye.setTextSize(18);
        eye.setGravity(android.view.Gravity.CENTER);
        eye.setClickable(true);
        eye.setFocusable(true);
        eye.setOnClickListener(v -> togglePassword(field, eye));

        frame.addView(field);
        frame.addView(eye);
        return frame;
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
}


