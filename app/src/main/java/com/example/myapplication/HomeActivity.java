package com.example.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class HomeActivity extends Activity {

    DatabaseHelper db;
    String userEmail;
    String userRole;
    LinearLayout feedContainer;
    String activeCategory = "All";

    // Active filter state
    String searchQuery = "";
    int maxPriceFilter = 99999;
    String typeFilter = "All";

    // Chip label views for toggling active state
    TextView chipLabelAll, chipLabelBeach, chipLabelPool, chipLabelVilla, chipLabelCabin;
    View chipLineAll, chipLineBeach, chipLinePool, chipLineVilla, chipLineCabin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = new DatabaseHelper(this);
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userRole = getIntent().getStringExtra("USER_ROLE");
        // Always verify role from DB — don't trust intent alone
        if (userEmail != null) {
            String dbRole = db.getUserRole(userEmail);
            if (dbRole != null && !dbRole.isEmpty()) userRole = dbRole;
        }
        if (userRole == null) userRole = "user";
        feedContainer = findViewById(R.id.feed_container);

        // Show/hide nav tabs based on role
        boolean isHost = userRole.equals("host");
        // Rename tab label for hosts
        TextView navWishlistLabel = findViewById(R.id.nav_wishlist_label);
        if (isHost) navWishlistLabel.setText("Likes");

        // Explore resets filters instead of navigating
        findViewById(R.id.nav_explore).setOnClickListener(v -> {
            searchQuery = "";
            typeFilter = "All";
            maxPriceFilter = 99999;
            selectChip("All");
        });

        // All other tabs via shared helper (no animation, consistent sizing)
        BottomNavHelper.setup(this, userEmail, userRole, "explore");
        LinearLayout chipAll   = findViewById(R.id.chip_all);
        LinearLayout chipBeach = findViewById(R.id.chip_beach);
        LinearLayout chipPool  = findViewById(R.id.chip_pool);
        LinearLayout chipVilla = findViewById(R.id.chip_villa);
        LinearLayout chipCabin = findViewById(R.id.chip_cabin);

        chipLabelAll   = (TextView) chipAll.getChildAt(1);
        chipLabelBeach = (TextView) chipBeach.getChildAt(1);
        chipLabelPool  = (TextView) chipPool.getChildAt(1);
        chipLabelVilla = (TextView) chipVilla.getChildAt(1);
        chipLabelCabin = (TextView) chipCabin.getChildAt(1);

        chipLineAll   = chipAll.getChildAt(2);
        chipLineBeach = chipBeach.getChildAt(2);
        chipLinePool  = chipPool.getChildAt(2);
        chipLineVilla = chipVilla.getChildAt(2);
        chipLineCabin = chipCabin.getChildAt(2);

        // Category chip clicks
        chipAll.setOnClickListener(v -> selectChip("All"));
        chipBeach.setOnClickListener(v -> selectChip("Beach"));
        chipPool.setOnClickListener(v -> selectChip("Pool"));
        chipVilla.setOnClickListener(v -> selectChip("Villa"));
        chipCabin.setOnClickListener(v -> selectChip("Cabin"));

        // Search bar click → search dialog
        findViewById(R.id.search_bar).setOnClickListener(v -> showSearchDialog());

        // Filter button click → filter dialog
        findViewById(R.id.btn_filter).setOnClickListener(v -> showFilterDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFeed();
        updateInboxBadge();
    }

    private void updateInboxBadge() {
        TextView badge = findViewById(R.id.inbox_badge);
        if (badge == null) return;
        int unreadNotifs   = db.getUnreadNotificationCount(userEmail);
        int unreadMessages = db.getUnreadMessageCount(userEmail);
        int total = unreadNotifs + unreadMessages;
        if (total > 0) {
            badge.setText(total > 99 ? "99+" : String.valueOf(total));
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private void selectChip(String category) {
        activeCategory = category;

        // Reset all chips
        int grey = 0xFF717171;
        chipLabelAll.setTextColor(grey);   chipLabelAll.setTypeface(null, android.graphics.Typeface.NORMAL);
        chipLabelBeach.setTextColor(grey); chipLabelBeach.setTypeface(null, android.graphics.Typeface.NORMAL);
        chipLabelPool.setTextColor(grey);  chipLabelPool.setTypeface(null, android.graphics.Typeface.NORMAL);
        chipLabelVilla.setTextColor(grey); chipLabelVilla.setTypeface(null, android.graphics.Typeface.NORMAL);
        chipLabelCabin.setTextColor(grey); chipLabelCabin.setTypeface(null, android.graphics.Typeface.NORMAL);

        chipLineAll.setBackgroundColor(0x00000000);
        chipLineBeach.setBackgroundColor(0x00000000);
        chipLinePool.setBackgroundColor(0x00000000);
        chipLineVilla.setBackgroundColor(0x00000000);
        chipLineCabin.setBackgroundColor(0x00000000);

        // Activate selected
        int red = 0xFFFF385C;
        TextView activeLabel;
        View activeLine;
        switch (category) {
            case "Beach": activeLabel = chipLabelBeach; activeLine = chipLineBeach; break;
            case "Pool":  activeLabel = chipLabelPool;  activeLine = chipLinePool;  break;
            case "Villa": activeLabel = chipLabelVilla; activeLine = chipLineVilla; break;
            case "Cabin": activeLabel = chipLabelCabin; activeLine = chipLineCabin; break;
            default:      activeLabel = chipLabelAll;   activeLine = chipLineAll;   break;
        }
        activeLabel.setTextColor(red);
        activeLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        activeLine.setBackgroundColor(red);

        loadFeed();
    }

    private void showSearchDialog() {
        EditText input = new EditText(this);
        input.setHint("Search by location or title...");
        input.setText(searchQuery);
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
            .setTitle("Search listings")
            .setView(input)
            .setPositiveButton("Search", (d, w) -> {
                searchQuery = input.getText().toString().trim().toLowerCase();
                loadFeed();
            })
            .setNegativeButton("Clear", (d, w) -> {
                searchQuery = "";
                loadFeed();
            })
            .show();
    }

    private void showFilterDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_filter, null);

        TextView txtPrice = view.findViewById(R.id.txt_price_value);
        SeekBar seekPrice = view.findViewById(R.id.seek_price);
        EditText inputExact = view.findViewById(R.id.input_price_exact);
        TextView btnSetExact = view.findViewById(R.id.btn_apply_exact_price);

        seekPrice.setMax(10000);
        int progress = maxPriceFilter >= 99999 ? 10000 : maxPriceFilter;
        seekPrice.setProgress(progress);
        txtPrice.setText(progress >= 10000 ? "Any" : "₱" + progress);
        if (maxPriceFilter < 99999) inputExact.setText(String.valueOf(maxPriceFilter));

        // Seekbar updates label and clears exact input
        seekPrice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                txtPrice.setText(p >= 10000 ? "Any" : "₱" + p);
                if (u) inputExact.setText(p >= 10000 ? "" : String.valueOf(p));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // "Set" button — apply exact amount to seekbar and label
        btnSetExact.setOnClickListener(v -> {
            String val = inputExact.getText().toString().trim();
            if (!val.isEmpty()) {
                try {
                    int amount = Integer.parseInt(val);
                    int clamped = Math.min(amount, 10000);
                    seekPrice.setProgress(clamped);
                    txtPrice.setText(clamped >= 10000 ? "Any" : "₱" + clamped);
                } catch (NumberFormatException ignored) {}
            }
        });

        // Property type chips in filter
        String[] types = {"All", "Entire home", "Private room", "Villa", "Cabin", "Apartment", "Hotel room"};
        LinearLayout typeContainer = view.findViewById(R.id.filter_type_container);
        typeContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout currentRow = null;
        for (int i = 0; i < types.length; i++) {
            String t = types[i];
            if (i % 3 == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowLp.setMargins(0, 0, 0, 10);
                currentRow.setLayoutParams(rowLp);
                typeContainer.addView(currentRow);
            }
            TextView chip = new TextView(this);
            chip.setText(t);
            chip.setTextSize(12);
            chip.setPadding(20, 14, 20, 14);
            chip.setTextColor(typeFilter.equals(t) ? 0xFFFFFFFF : 0xFF222222);
            chip.setBackground(typeFilter.equals(t)
                ? getDrawable(R.drawable.btn_view_details)
                : getDrawable(R.drawable.rounded_edittext_dark));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            lp.setMargins(0, 0, i % 3 == 2 ? 0 : 8, 0);
            chip.setLayoutParams(lp);
            chip.setGravity(android.view.Gravity.CENTER);
            final LinearLayout finalRow = currentRow;
            chip.setOnClickListener(cv -> {
                typeFilter = t;
                // Refresh all chip colors across all rows
                for (int r = 0; r < typeContainer.getChildCount(); r++) {
                    LinearLayout row2 = (LinearLayout) typeContainer.getChildAt(r);
                    for (int c = 0; c < row2.getChildCount(); c++) {
                        TextView ch = (TextView) row2.getChildAt(c);
                        boolean sel = ch.getText().toString().equals(t);
                        ch.setTextColor(sel ? 0xFFFFFFFF : 0xFF222222);
                        ch.setBackground(sel ? getDrawable(R.drawable.btn_view_details) : getDrawable(R.drawable.rounded_edittext_dark));
                    }
                }
            });
            currentRow.addView(chip);
        }

        new AlertDialog.Builder(this)
            .setTitle("Filter listings")
            .setView(view)
            .setPositiveButton("Apply", (d, w) -> {
                int p = seekPrice.getProgress();
                maxPriceFilter = p >= 10000 ? 99999 : p;
                loadFeed();
            })
            .setNegativeButton("Reset", (d, w) -> {
                maxPriceFilter = 99999;
                typeFilter = "All";
                loadFeed();
            })
            .show();
    }

    private void loadFeed() {
        feedContainer.removeAllViews();
        Cursor cursor = db.getAllListings();

        boolean anyShown = false;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String type = cursor.getString(cursor.getColumnIndex("property_type"));
                String amenities = cursor.getString(cursor.getColumnIndex("amenities"));
                String location = cursor.getString(cursor.getColumnIndex("location"));
                String title = cursor.getString(cursor.getColumnIndex("title"));
                double price = cursor.getDouble(cursor.getColumnIndex("price_per_night"));

                // Category filter
                if (!activeCategory.equals("All")) {
                    boolean match = false;
                    if (activeCategory.equals("Villa") && type.equalsIgnoreCase("Villa")) match = true;
                    if (activeCategory.equals("Cabin") && type.equalsIgnoreCase("Cabin")) match = true;
                    if (activeCategory.equals("Pool") && amenities != null && amenities.contains("Pool")) match = true;
                    if (activeCategory.equals("Beach") && location.toLowerCase().contains("beach")) match = true;
                    if (!match) continue;
                }

                // Search filter
                if (!searchQuery.isEmpty()) {
                    if (!location.toLowerCase().contains(searchQuery) && !title.toLowerCase().contains(searchQuery)) continue;
                }

                // Price filter
                if (price > maxPriceFilter) continue;

                // Type filter
                if (!typeFilter.equals("All") && !type.equalsIgnoreCase(typeFilter)) continue;

                anyShown = true;
                final int listingId = cursor.getInt(cursor.getColumnIndex("id"));
                final int guests = cursor.getInt(cursor.getColumnIndex("max_guests"));
                final int beds = cursor.getInt(cursor.getColumnIndex("beds"));
                final double baths = cursor.getDouble(cursor.getColumnIndex("bathrooms"));
                final String imageUriStr = cursor.getString(cursor.getColumnIndex("image_uri"));

                View card = getLayoutInflater().inflate(R.layout.item_listing_feed, feedContainer, false);

                android.widget.HorizontalScrollView photoScroll = card.findViewById(R.id.feed_photo_scroll);
                LinearLayout photoStrip = card.findViewById(R.id.feed_photo_strip);
                LinearLayout placeholder = card.findViewById(R.id.feed_image_placeholder);
                LinearLayout pageDots = card.findViewById(R.id.feed_page_dots);

                String[] uris = (imageUriStr != null && !imageUriStr.isEmpty())
                    ? imageUriStr.split(",") : new String[0];

                // Filter valid URIs
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
                            // Check we still have permission before loading
                            getContentResolver().openInputStream(parsedUri).close();
                            img.setImageURI(parsedUri);
                        } catch (Exception ignored) {
                            img.setBackgroundColor(0xFFE8E8E8);
                        }
                        photoStrip.addView(img);
                    }
                    // Page indicator dots
                    if (validUris.size() > 1) {
                        pageDots.setVisibility(View.VISIBLE);
                        final int totalPages = validUris.size();
                        final View[] dots = new View[totalPages];
                        int dotSize = dp(7);
                        int dotMargin = dp(4);
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
                            int scrollX = photoScroll.getScrollX();
                            int page = Math.min((scrollX + cardW / 2) / cardW, totalPages - 1);
                            for (int d = 0; d < totalPages; d++) {
                                dots[d].setBackgroundColor(d == page ? 0xFFFFFFFF : 0x88FFFFFF);
                            }
                        });
                    }
                } else {
                    placeholder.setVisibility(View.VISIBLE);
                }

                String hostName = cursor.getString(cursor.getColumnIndex("host_name"));

                ((TextView) card.findViewById(R.id.feed_location)).setText(location);
                ((TextView) card.findViewById(R.id.feed_type)).setText(type);
                ((TextView) card.findViewById(R.id.feed_rating)).setText("New");
                ((TextView) card.findViewById(R.id.feed_details)).setText(
                    guests + " guests · " + beds + " beds · " + baths + " baths");
                ((TextView) card.findViewById(R.id.feed_host_name)).setText(
                    "Hosted by " + (hostName != null && !hostName.trim().isEmpty() ? hostName : "Host"));

                String priceStr = (price == Math.floor(price)) ? String.valueOf((int) price) : String.valueOf(price);
                ((TextView) card.findViewById(R.id.feed_price)).setText("₱" + priceStr + " / night");

                // Like count + avg rating
                int likeCount = db.getLikeCount(listingId);
                float avgRating = db.getAverageRating(listingId);
                int reviewCount = db.getReviewCount(listingId);
                ((TextView) card.findViewById(R.id.feed_like_count)).setText("♥ " + likeCount);
                if (reviewCount > 0) {
                    ((TextView) card.findViewById(R.id.feed_avg_rating)).setText(
                        String.format(Locale.getDefault(), "★ %.1f (%d)", avgRating, reviewCount));
                } else {
                    ((TextView) card.findViewById(R.id.feed_avg_rating)).setText("★ No reviews");
                }

                // Heart / wishlist toggle
                TextView heartBtn = card.findViewById(R.id.btn_wishlist_heart);
                boolean wishlisted = db.isWishlisted(userEmail, listingId);
                heartBtn.setText(wishlisted ? "♥" : "♡");
                heartBtn.setTextColor(wishlisted ? 0xFFFF385C : 0xFFFFFFFF);

                heartBtn.setOnClickListener(v -> {
                    boolean nowWishlisted = db.toggleWishlist(userEmail, listingId);
                    heartBtn.setText(nowWishlisted ? "♥" : "♡");
                    heartBtn.setTextColor(nowWishlisted ? 0xFFFF385C : 0xFFFFFFFF);
                    boolean isHostUser = "host".equals(userRole);
                    if (isHostUser) {
                        Toast.makeText(this, nowWishlisted ? "Added to likes" : "Removed from likes", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, nowWishlisted ? "Added to wishlist" : "Removed from wishlist", Toast.LENGTH_SHORT).show();
                    }
                });

                card.findViewById(R.id.btn_view_details).setOnClickListener(v -> {
                    Intent intent = new Intent(HomeActivity.this, ListingDetailActivity.class);
                    intent.putExtra("LISTING_ID", listingId);
                    intent.putExtra("USER_EMAIL", userEmail);
                    intent.putExtra("USER_ROLE", userRole);
                    startActivity(intent);
                });

                feedContainer.addView(card);

            } while (cursor.moveToNext());
            cursor.close();
        }

        if (!anyShown) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 500);
            empty.setLayoutParams(lp);

            TextView emoji = new TextView(this);
            emoji.setText("🔍");
            emoji.setTextSize(48);
            emoji.setGravity(android.view.Gravity.CENTER);

            TextView msg = new TextView(this);
            msg.setText(searchQuery.isEmpty() ? "No listings available yet" : "No results for \"" + searchQuery + "\"");
            msg.setTextSize(15);
            msg.setTextColor(0xFF717171);
            msg.setGravity(android.view.Gravity.CENTER);
            msg.setPadding(32, 16, 32, 0);

            empty.addView(emoji);
            empty.addView(msg);
            feedContainer.addView(empty);
        }
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }
}
