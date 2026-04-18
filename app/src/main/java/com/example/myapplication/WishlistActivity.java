package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class WishlistActivity extends Activity {

    DatabaseHelper db;
    String userEmail;
    String userRole;
    LinearLayout wishlistContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        db = new DatabaseHelper(this);
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userRole = getIntent().getStringExtra("USER_ROLE");
        if (userRole == null) userRole = "user";
        wishlistContainer = findViewById(R.id.wishlist_container);

        TextView titleView = findViewById(R.id.wishlist_title);
        if ("host".equals(userRole)) titleView.setText("Likes");

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });
        BottomNavHelper.setup(this, userEmail, userRole, "wishlist");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWishlist();
    }

    private void loadWishlist() {
        wishlistContainer.removeAllViews();
        Cursor cursor = db.getWishlistByUser(userEmail);

        if (cursor == null || cursor.getCount() == 0) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 600);
            empty.setLayoutParams(lp);

            boolean isHost = "host".equals(userRole);

            TextView emoji = new TextView(this);
            emoji.setText("♡");
            emoji.setTextSize(56);
            emoji.setTextColor(0xFFFF385C);
            emoji.setGravity(android.view.Gravity.CENTER);

            TextView msg = new TextView(this);
            msg.setText(isHost ? "No liked listings yet" : "No saved listings yet");
            msg.setTextSize(16);
            msg.setTypeface(null, android.graphics.Typeface.BOLD);
            msg.setTextColor(0xFF222222);
            msg.setGravity(android.view.Gravity.CENTER);
            msg.setPadding(0, 16, 0, 0);

            TextView sub = new TextView(this);
            sub.setText(isHost ? "Tap the ♡ on any listing to like it" : "Tap the ♡ on any listing to save it here");
            sub.setTextSize(13);
            sub.setTextColor(0xFF717171);
            sub.setGravity(android.view.Gravity.CENTER);
            sub.setPadding(32, 8, 32, 0);

            empty.addView(emoji);
            empty.addView(msg);
            empty.addView(sub);
            wishlistContainer.addView(empty);
            return;
        }

        cursor.moveToFirst();
        do {
            final int listingId = cursor.getInt(cursor.getColumnIndex("id"));
            final String title = cursor.getString(cursor.getColumnIndex("title"));
            final String location = cursor.getString(cursor.getColumnIndex("location"));
            final String type = cursor.getString(cursor.getColumnIndex("property_type"));
            final double price = cursor.getDouble(cursor.getColumnIndex("price_per_night"));
            final int guests = cursor.getInt(cursor.getColumnIndex("max_guests"));
            final int beds = cursor.getInt(cursor.getColumnIndex("beds"));
            final double baths = cursor.getDouble(cursor.getColumnIndex("bathrooms"));
            final String imageUriStr = cursor.getString(cursor.getColumnIndex("image_uri"));

            View card = getLayoutInflater().inflate(R.layout.item_listing_feed, wishlistContainer, false);

            android.widget.HorizontalScrollView photoScroll = card.findViewById(R.id.feed_photo_scroll);
            LinearLayout photoStrip = card.findViewById(R.id.feed_photo_strip);
            LinearLayout placeholder = card.findViewById(R.id.feed_image_placeholder);
            LinearLayout pageDots = card.findViewById(R.id.feed_page_dots);

            String[] uris = (imageUriStr != null && !imageUriStr.isEmpty())
                ? imageUriStr.split(",") : new String[0];
            java.util.List<String> validUris = new java.util.ArrayList<>();
            for (String u : uris) { if (!u.trim().isEmpty()) validUris.add(u.trim()); }

            if (!validUris.isEmpty()) {
                placeholder.setVisibility(View.GONE);
                int cardW = getResources().getDisplayMetrics().widthPixels;
                for (String uriStr : validUris) {
                    ImageView img = new ImageView(this);
                    img.setLayoutParams(new LinearLayout.LayoutParams(cardW, LinearLayout.LayoutParams.MATCH_PARENT));
                    img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    try {
                        Uri parsedUri = Uri.parse(uriStr);
                        getContentResolver().openInputStream(parsedUri).close();
                        img.setImageURI(parsedUri);
                    } catch (Exception ignored) {
                        img.setBackgroundColor(0xFFE8E8E8);
                    }
                    photoStrip.addView(img);
                }
                if (validUris.size() > 1) {
                    pageDots.setVisibility(View.VISIBLE);
                    final int totalPages = validUris.size();
                    final View[] dots = new View[totalPages];
                    int dotSize = dp(7); int dotMargin = dp(4);
                    for (int d = 0; d < totalPages; d++) {
                        View dot = new View(this);
                        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dotSize, dotSize);
                        dotLp.setMargins(dotMargin, 0, dotMargin, 0);
                        dot.setLayoutParams(dotLp);
                        dot.setBackgroundColor(d == 0 ? 0xFFFFFFFF : 0x88FFFFFF);
                        dots[d] = dot;
                        pageDots.addView(dot);
                    }
                    photoScroll.getViewTreeObserver().addOnScrollChangedListener(() -> {
                        int page = Math.min((photoScroll.getScrollX() + cardW / 2) / cardW, totalPages - 1);
                        for (int d = 0; d < totalPages; d++)
                            dots[d].setBackgroundColor(d == page ? 0xFFFFFFFF : 0x88FFFFFF);
                    });
                }
            } else {
                placeholder.setVisibility(View.VISIBLE);
            }

            ((TextView) card.findViewById(R.id.feed_location)).setText(location);
            ((TextView) card.findViewById(R.id.feed_type)).setText(type);
            ((TextView) card.findViewById(R.id.feed_rating)).setText("New");
            ((TextView) card.findViewById(R.id.feed_details)).setText(
                guests + " guests · " + beds + " beds · " + baths + " baths");

            String priceStr = (price == Math.floor(price)) ? String.valueOf((int) price) : String.valueOf(price);
            ((TextView) card.findViewById(R.id.feed_price)).setText("₱" + priceStr + " / night");

            // Heart is always filled here (it's the wishlist)
            TextView heartBtn = card.findViewById(R.id.btn_wishlist_heart);
            heartBtn.setText("♥");
            heartBtn.setTextColor(0xFFFF385C);
            heartBtn.setOnClickListener(v -> {
                db.toggleWishlist(userEmail, listingId);
                boolean isHost = "host".equals(userRole);
                Toast.makeText(this, isHost ? "Removed from likes" : "Removed from wishlist", Toast.LENGTH_SHORT).show();
                loadWishlist();
            });

            card.findViewById(R.id.btn_view_details).setOnClickListener(v -> {
                Intent intent = new Intent(WishlistActivity.this, ListingDetailActivity.class);
                intent.putExtra("LISTING_ID", listingId);
                intent.putExtra("USER_EMAIL", userEmail);
                startActivity(intent);
            });

            wishlistContainer.addView(card);

        } while (cursor.moveToNext());
        cursor.close();
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }
}
