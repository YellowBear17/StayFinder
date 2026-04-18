package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;

public class AddListingActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 101;
    private static final int MAX_PHOTOS = 10;

    DatabaseHelper db;
    String userEmail;
    int editListingId = -1;

    // Multi-photo list
    ArrayList<String> photoUris = new ArrayList<>();
    LinearLayout photosStrip;

    Spinner spinnerType;
    EditText inputTitle, inputLocation, inputDescription, inputPrice;
    TextView txtGuests, txtBedrooms, txtBeds, txtBaths;
    CheckBox chkWifi, chkKitchen, chkParking, chkPool, chkAc, chkTv, chkWasher, chkWorkspace;
    Button btnSubmit;
    TextView txtImageStatus, txtFormTitle;

    final String[] typeOptions = {"Entire home", "Private room", "Shared room", "Hotel room", "Villa", "Cabin", "Apartment", "Condo"};
    int guests = 1, bedrooms = 1, beds = 1, baths = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_listing);

        db = new DatabaseHelper(this);
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        editListingId = getIntent().getIntExtra("LISTING_ID", -1);

        txtFormTitle    = findViewById(R.id.txt_form_title);
        spinnerType     = findViewById(R.id.spinner_property_type);
        spinnerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, typeOptions));
        inputTitle      = findViewById(R.id.input_title);
        inputLocation   = findViewById(R.id.input_location);
        inputDescription= findViewById(R.id.input_description);
        inputPrice      = findViewById(R.id.input_price);
        txtGuests       = findViewById(R.id.txt_guests);
        txtBedrooms     = findViewById(R.id.txt_bedrooms);
        txtBeds         = findViewById(R.id.txt_beds);
        txtBaths        = findViewById(R.id.txt_baths);
        chkWifi         = findViewById(R.id.chk_wifi);
        chkKitchen      = findViewById(R.id.chk_kitchen);
        chkParking      = findViewById(R.id.chk_parking);
        chkPool         = findViewById(R.id.chk_pool);
        chkAc           = findViewById(R.id.chk_ac);
        chkTv           = findViewById(R.id.chk_tv);
        chkWasher       = findViewById(R.id.chk_washer);
        chkWorkspace    = findViewById(R.id.chk_workspace);
        btnSubmit       = findViewById(R.id.btn_submit_listing);
        txtImageStatus  = findViewById(R.id.txt_image_status);
        photosStrip     = findViewById(R.id.photos_strip);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_pick_image).setOnClickListener(v -> {
            if (photoUris.size() >= MAX_PHOTOS) {
                StyledAlert.show(this, StyledAlert.WARNING, "Limit Reached",
                    "You can add up to " + MAX_PHOTOS + " photos.");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        setupCounter(R.id.btn_guests_minus,   R.id.btn_guests_plus,   txtGuests,   "guests");
        setupCounter(R.id.btn_bedrooms_minus, R.id.btn_bedrooms_plus, txtBedrooms, "bedrooms");
        setupCounter(R.id.btn_beds_minus,     R.id.btn_beds_plus,     txtBeds,     "beds");
        setupCounter(R.id.btn_baths_minus,    R.id.btn_baths_plus,    txtBaths,    "baths");

        if (editListingId != -1) {
            txtFormTitle.setText("Edit Listing");
            btnSubmit.setText("Save Changes");
            prefillFields();
        }

        btnSubmit.setOnClickListener(v -> {
            if (editListingId != -1) updateListing();
            else submitListing();
        });
    }

    private void prefillFields() {
        Cursor cursor = db.getListingById(editListingId);
        if (cursor == null || !cursor.moveToFirst()) return;

        inputTitle.setText(cursor.getString(cursor.getColumnIndex("title")));
        inputLocation.setText(cursor.getString(cursor.getColumnIndex("location")));
        inputDescription.setText(cursor.getString(cursor.getColumnIndex("description")));

        double price = cursor.getDouble(cursor.getColumnIndex("price_per_night"));
        inputPrice.setText(price == Math.floor(price) ? String.valueOf((int) price) : String.valueOf(price));

        String type = cursor.getString(cursor.getColumnIndex("property_type"));
        for (int i = 0; i < typeOptions.length; i++) {
            if (typeOptions[i].equalsIgnoreCase(type)) { spinnerType.setSelection(i); break; }
        }

        guests   = cursor.getInt(cursor.getColumnIndex("max_guests"));
        bedrooms = cursor.getInt(cursor.getColumnIndex("bedrooms"));
        beds     = cursor.getInt(cursor.getColumnIndex("beds"));
        baths    = cursor.getInt(cursor.getColumnIndex("bathrooms"));
        txtGuests.setText(String.valueOf(guests));
        txtBedrooms.setText(String.valueOf(bedrooms));
        txtBeds.setText(String.valueOf(beds));
        txtBaths.setText(String.valueOf(baths));

        String amenities = cursor.getString(cursor.getColumnIndex("amenities"));
        if (amenities != null) {
            chkWifi.setChecked(amenities.contains("Wifi"));
            chkKitchen.setChecked(amenities.contains("Kitchen"));
            chkParking.setChecked(amenities.contains("Free parking"));
            chkPool.setChecked(amenities.contains("Pool"));
            chkAc.setChecked(amenities.contains("Air conditioning"));
            chkTv.setChecked(amenities.contains("TV"));
            chkWasher.setChecked(amenities.contains("Washer"));
            chkWorkspace.setChecked(amenities.contains("Workspace"));
        }

        // Load existing photos
        String imageUriStr = cursor.getString(cursor.getColumnIndex("image_uri"));
        if (imageUriStr != null && !imageUriStr.isEmpty()) {
            for (String uri : imageUriStr.split(",")) {
                if (!uri.trim().isEmpty()) photoUris.add(uri.trim());
            }
        }
        cursor.close();
        renderPhotoStrip();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            photoUris.add(uri.toString());
            renderPhotoStrip();
        }
    }

    private void renderPhotoStrip() {
        photosStrip.removeAllViews();
        int size = dp(110);
        int margin = dp(8);

        for (int i = 0; i < photoUris.size(); i++) {
            final int index = i;
            final String uriStr = photoUris.get(i);

            // Wrapper with relative positioning for the X button
            FrameLayout frame = new FrameLayout(this);
            LinearLayout.LayoutParams frameLp = new LinearLayout.LayoutParams(size, size);
            frameLp.setMargins(0, 0, margin, 0);
            frame.setLayoutParams(frameLp);

            // Thumbnail
            ImageView thumb = new ImageView(this);
            thumb.setLayoutParams(new FrameLayout.LayoutParams(size, size));
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setBackground(getDrawable(R.drawable.rounded_edittext_dark));
            try {
                Uri parsedUri = Uri.parse(uriStr);
                getContentResolver().openInputStream(parsedUri).close();
                thumb.setImageURI(parsedUri);
            } catch (Exception ignored) {
                thumb.setBackgroundColor(0xFFE8E8E8);
            }

            // Cover badge on first photo
            if (i == 0) {
                TextView coverBadge = new TextView(this);
                FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                badgeLp.gravity = Gravity.BOTTOM | Gravity.START;
                badgeLp.setMargins(dp(4), 0, 0, dp(4));
                coverBadge.setLayoutParams(badgeLp);
                coverBadge.setText("Cover");
                coverBadge.setTextSize(10);
                coverBadge.setTextColor(0xFFFFFFFF);
                coverBadge.setBackgroundColor(0xAAFF385C);
                coverBadge.setPadding(dp(4), dp(2), dp(4), dp(2));
                frame.addView(thumb);
                frame.addView(coverBadge);
            } else {
                frame.addView(thumb);
            }

            // Remove (X) button
            TextView removeBtn = new TextView(this);
            FrameLayout.LayoutParams xLp = new FrameLayout.LayoutParams(dp(22), dp(22));
            xLp.gravity = Gravity.TOP | Gravity.END;
            xLp.setMargins(0, dp(4), dp(4), 0);
            removeBtn.setLayoutParams(xLp);
            removeBtn.setText("✕");
            removeBtn.setTextSize(11);
            removeBtn.setTextColor(0xFFFFFFFF);
            removeBtn.setBackgroundColor(0xCCFF385C);
            removeBtn.setGravity(Gravity.CENTER);
            removeBtn.setClickable(true);
            removeBtn.setFocusable(true);
            removeBtn.setOnClickListener(v -> {
                photoUris.remove(index);
                renderPhotoStrip();
            });
            frame.addView(removeBtn);

            photosStrip.addView(frame);
        }

        // Update status text
        int count = photoUris.size();
        if (count == 0) {
            txtImageStatus.setText("No photos selected (optional)");
            txtImageStatus.setTextColor(0xFFAAAAAA);
        } else {
            txtImageStatus.setText("✓ " + count + " photo" + (count > 1 ? "s" : "") + " added");
            txtImageStatus.setTextColor(0xFF00A699);
        }
    }

    private void setupCounter(int minusId, int plusId, final TextView display, final String field) {
        findViewById(minusId).setOnClickListener(v -> {
            if (field.equals("guests")   && guests > 1)    { guests--;    display.setText(String.valueOf(guests)); }
            else if (field.equals("bedrooms") && bedrooms > 0) { bedrooms--; display.setText(String.valueOf(bedrooms)); }
            else if (field.equals("beds")     && beds > 1)    { beds--;     display.setText(String.valueOf(beds)); }
            else if (field.equals("baths")    && baths > 1) { baths--;     display.setText(String.valueOf(baths)); }
        });
        findViewById(plusId).setOnClickListener(v -> {
            if (field.equals("guests")   && guests < 16)   { guests++;    display.setText(String.valueOf(guests)); }
            else if (field.equals("bedrooms") && bedrooms < 10) { bedrooms++; display.setText(String.valueOf(bedrooms)); }
            else if (field.equals("beds")     && beds < 10)    { beds++;     display.setText(String.valueOf(beds)); }
            else if (field.equals("baths")    && baths < 10)   { baths++;     display.setText(String.valueOf(baths)); }
        });
    }

    private String buildAmenities() {
        ArrayList<String> list = new ArrayList<>();
        if (chkWifi.isChecked())      list.add("Wifi");
        if (chkKitchen.isChecked())   list.add("Kitchen");
        if (chkParking.isChecked())   list.add("Free parking");
        if (chkPool.isChecked())      list.add("Pool");
        if (chkAc.isChecked())        list.add("Air conditioning");
        if (chkTv.isChecked())        list.add("TV");
        if (chkWasher.isChecked())    list.add("Washer");
        if (chkWorkspace.isChecked()) list.add("Workspace");
        return android.text.TextUtils.join(", ", list);
    }

    private String buildPhotoUriString() {
        return android.text.TextUtils.join(",", photoUris);
    }

    private boolean validateInputs() {
        if (inputTitle.getText().toString().trim().isEmpty()) {
            StyledAlert.show(this, StyledAlert.WARNING, "Missing Title", "Please enter a title for your listing.");
            return false;
        }
        if (inputLocation.getText().toString().trim().isEmpty()) {
            StyledAlert.show(this, StyledAlert.WARNING, "Missing Location", "Please enter the location of your listing.");
            return false;
        }
        if (inputPrice.getText().toString().trim().isEmpty()) {
            StyledAlert.show(this, StyledAlert.WARNING, "Missing Price", "Please enter a price per night.");
            return false;
        }
        return true;
    }

    private void submitListing() {
        if (!validateInputs()) return;
        double price;
        try { price = Double.parseDouble(inputPrice.getText().toString().trim()); }
        catch (NumberFormatException e) {
            StyledAlert.show(this, StyledAlert.WARNING, "Invalid Price", "Please enter a valid number for the price.");
            return;
        }
        long result = db.addListing(userEmail,
            inputTitle.getText().toString().trim(),
            spinnerType.getSelectedItem().toString(),
            inputLocation.getText().toString().trim(),
            inputDescription.getText().toString().trim(),
            price, guests, bedrooms, beds, baths,
            buildAmenities(), buildPhotoUriString());

        if (result != -1) {
            StyledAlert.show(this, StyledAlert.SUCCESS, "Listing Published!", "Your listing is now live on stayFinder.");
            new android.os.Handler().postDelayed(this::finish, 1600);
        } else {
            StyledAlert.show(this, StyledAlert.ERROR, "Publish Failed", "Something went wrong. Please try again.");
        }
    }

    private void updateListing() {
        if (!validateInputs()) return;
        double price;
        try { price = Double.parseDouble(inputPrice.getText().toString().trim()); }
        catch (NumberFormatException e) {
            StyledAlert.show(this, StyledAlert.WARNING, "Invalid Price", "Please enter a valid number for the price.");
            return;
        }
        boolean success = db.updateListing(editListingId,
            inputTitle.getText().toString().trim(),
            spinnerType.getSelectedItem().toString(),
            inputLocation.getText().toString().trim(),
            inputDescription.getText().toString().trim(),
            price, guests, bedrooms, beds, baths,
            buildAmenities(), buildPhotoUriString());

        if (success) {
            StyledAlert.show(this, StyledAlert.SUCCESS, "Listing Updated!", "Your changes have been saved.");
            new android.os.Handler().postDelayed(this::finish, 1600);
        } else {
            StyledAlert.show(this, StyledAlert.ERROR, "Update Failed", "Something went wrong. Please try again.");
        }
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }
}
