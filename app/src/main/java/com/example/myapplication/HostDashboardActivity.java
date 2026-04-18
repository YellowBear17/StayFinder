package com.example.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.drawerlayout.widget.DrawerLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HostDashboardActivity extends Activity {

    DatabaseHelper db;
    String userEmail;
    DrawerLayout drawerLayout;
    LinearLayout listingsContainer;
    LinearLayout emptyState;
    TextView hostGreeting, txtListingCount, statListingsCount, statActiveCount;
    TextView drawerHostName, drawerHostEmail, drawerAvatarLetter;
    ArrayList<String[]> listings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_dashboard);

        db = new DatabaseHelper(this);
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        drawerLayout = findViewById(R.id.drawer_layout);
        listingsContainer = findViewById(R.id.listings_container);
        emptyState = findViewById(R.id.empty_state);
        hostGreeting = findViewById(R.id.host_greeting);
        txtListingCount = findViewById(R.id.txt_listing_count);
        statListingsCount = findViewById(R.id.stat_listings_count);
        statActiveCount = findViewById(R.id.stat_active_count);
        TextView statReservationsCount = findViewById(R.id.stat_reservations_count);
        drawerHostName = findViewById(R.id.drawer_host_name);
        drawerHostEmail = findViewById(R.id.drawer_host_email);
        drawerAvatarLetter = findViewById(R.id.drawer_avatar_letter);

        // Load user info
        Cursor user = db.getUserByEmail(userEmail);
        if (user != null && user.moveToFirst()) {
            String firstName = user.getString(user.getColumnIndex("first_name"));
            String lastName = user.getString(user.getColumnIndex("last_name"));
            hostGreeting.setText("Welcome back, " + firstName + " 👋");
            drawerHostName.setText(firstName + " " + lastName);
            drawerHostEmail.setText(userEmail);
            drawerAvatarLetter.setText(String.valueOf(firstName.charAt(0)).toUpperCase());
            user.close();
        }

        // Hamburger menu
        findViewById(R.id.btn_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(findViewById(R.id.nav_drawer));
            }
        });

        // Bell — show likes notifications
        findViewById(R.id.btn_notifications).setOnClickListener(v -> showLikesDialog());

        // Add listing button (in main content)
        findViewById(R.id.btn_add_listing).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAddListing();
            }
        });

        // Sidebar nav items
        findViewById(R.id.nav_dashboard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
            }
        });

        findViewById(R.id.nav_listings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
                // Scroll to listings section — already on dashboard
            }
        });

        findViewById(R.id.nav_add_listing).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
                openAddListing();
            }
        });

        findViewById(R.id.drawer_nav_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
                Intent intent = new Intent(HostDashboardActivity.this, ProfileActivity.class);
                intent.putExtra("USER_EMAIL", userEmail);
                startActivity(intent);
            }
        });

        findViewById(R.id.drawer_nav_inbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
                Intent intent = new Intent(HostDashboardActivity.this, InboxActivity.class);
                intent.putExtra("USER_EMAIL", userEmail);
                intent.putExtra("USER_ROLE", "host");
                startActivity(intent);
            }
        });

        findViewById(R.id.nav_logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
                logout();
            }
        });

        // Sticky bottom nav
        BottomNavHelper.setup(this, userEmail, "host", "my_listings");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadListings();
    }

    private void openAddListing() {
        Intent intent = new Intent(HostDashboardActivity.this, AddListingActivity.class);
        intent.putExtra("USER_EMAIL", userEmail);
        startActivity(intent);
    }

    private void logout() {
        Intent intent = new Intent(HostDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadListings() {
        listings.clear();
        listingsContainer.removeAllViews();

        Cursor cursor = db.getListingsByHost(userEmail);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                listings.add(new String[]{
                    String.valueOf(cursor.getInt(cursor.getColumnIndex("id"))),
                    cursor.getString(cursor.getColumnIndex("title")),
                    cursor.getString(cursor.getColumnIndex("property_type")),
                    cursor.getString(cursor.getColumnIndex("location")),
                    String.valueOf(cursor.getDouble(cursor.getColumnIndex("price_per_night"))),
                    String.valueOf(cursor.getInt(cursor.getColumnIndex("max_guests"))),
                    String.valueOf(cursor.getInt(cursor.getColumnIndex("beds"))),
                    String.valueOf(cursor.getDouble(cursor.getColumnIndex("bathrooms")))
                });
            } while (cursor.moveToNext());
            cursor.close();
        }

        int count = listings.size();
        txtListingCount.setText("Your Listings (" + count + ")");
        statListingsCount.setText(String.valueOf(count));
        int pending = db.getPendingCountForHost(userEmail);
        statActiveCount.setText(String.valueOf(pending));
        int activeRes = db.getActiveBookingCountForHost(userEmail);
        TextView statResCnt = findViewById(R.id.stat_reservations_count);
        if (statResCnt != null) statResCnt.setText(String.valueOf(activeRes));

        // Update bell badge with total likes count
        Cursor likesCursor = db.getLikesForHost(userEmail);
        int likesCount = likesCursor != null ? likesCursor.getCount() : 0;
        if (likesCursor != null) likesCursor.close();
        TextView notifBadge = findViewById(R.id.notif_badge);
        if (notifBadge != null) {
            if (likesCount > 0) {
                notifBadge.setText(likesCount > 99 ? "99+" : String.valueOf(likesCount));
                notifBadge.setVisibility(View.VISIBLE);
            } else {
                notifBadge.setVisibility(View.GONE);
            }
        }

        // Tap pending card → open inbox showing only pending requests
        findViewById(R.id.card_pending_requests).setOnClickListener(v -> {
            Intent intent = new Intent(HostDashboardActivity.this, InboxActivity.class);
            intent.putExtra("USER_EMAIL", userEmail);
            intent.putExtra("USER_ROLE", "host");
            intent.putExtra("SHOW_PENDING", true);
            startActivity(intent);
        });

        // Tap reservations card → show active reservations dialog
        findViewById(R.id.card_active_reservations).setOnClickListener(v -> showActiveReservations());

        if (listings.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            listingsContainer.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            listingsContainer.setVisibility(View.VISIBLE);

            for (int i = 0; i < listings.size(); i++) {
                final String[] item = listings.get(i);
                View card = getLayoutInflater().inflate(R.layout.item_listing, listingsContainer, false);

                ((TextView) card.findViewById(R.id.item_type_badge)).setText(item[2]);
                ((TextView) card.findViewById(R.id.item_title)).setText(item[1]);
                ((TextView) card.findViewById(R.id.item_type_location)).setText("📍 " + item[3]);
                ((TextView) card.findViewById(R.id.item_details)).setText(
                    item[5] + " guests · " + item[6] + " beds · " + item[7] + " baths");

                // Format price — remove trailing .0 if whole number
                String priceStr = item[4];
                try {
                    double price = Double.parseDouble(priceStr);
                    if (price == Math.floor(price)) {
                        priceStr = String.valueOf((int) price);
                    }
                } catch (NumberFormatException ignored) {}
                ((TextView) card.findViewById(R.id.item_price)).setText("₱" + priceStr);

                final int listingId = Integer.parseInt(item[0]);

                card.findViewById(R.id.btn_delete_listing).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        StyledAlert.confirm(HostDashboardActivity.this,
                            "Delete Listing",
                            "Are you sure you want to delete \"" + item[1] + "\"? This cannot be undone.",
                            "Delete", "Cancel",
                            () -> {
                                db.deleteListing(listingId);
                                StyledAlert.show(HostDashboardActivity.this, StyledAlert.SUCCESS,
                                    "Listing Deleted", "\"" + item[1] + "\" has been removed.");
                                loadListings();
                            });
                    }
                });

                card.findViewById(R.id.btn_edit_listing).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(HostDashboardActivity.this, AddListingActivity.class);
                        intent.putExtra("USER_EMAIL", userEmail);
                        intent.putExtra("LISTING_ID", listingId);
                        startActivity(intent);
                    }
                });

                card.findViewById(R.id.btn_calendar_listing).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCalendarDialog(listingId, item[1]);
                    }
                });

                listingsContainer.addView(card);
            }
        }
    }

    private void showActiveReservations() {
        Cursor cursor = db.getActiveBookingsForHost(userEmail);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);
        scroll.addView(root);

        if (cursor == null || cursor.getCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No active reservations yet.");
            empty.setTextSize(14);
            empty.setTextColor(0xFF717171);
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(dp(24), dp(48), dp(24), dp(48));
            root.addView(empty);
        } else {
            cursor.moveToFirst();
            do {
                int bookingId     = cursor.getInt(cursor.getColumnIndex("id"));
                String guestName  = cursor.getString(cursor.getColumnIndex("guest_name"));
                String title      = cursor.getString(cursor.getColumnIndex("title"));
                String location   = cursor.getString(cursor.getColumnIndex("location"));
                String checkIn    = cursor.getString(cursor.getColumnIndex("check_in"));
                String checkOut   = cursor.getString(cursor.getColumnIndex("check_out"));
                double total      = cursor.getDouble(cursor.getColumnIndex("total_price"));
                String payStatus  = db.getPaymentStatus(bookingId);
                double deposit    = db.getDepositAmount(bookingId);
                long deadline     = db.getPaymentDeadline(bookingId);
                long now          = System.currentTimeMillis();

                // Calculate nights
                int nights = 0;
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    long diff = sdf.parse(checkOut).getTime() - sdf.parse(checkIn).getTime();
                    nights = (int)(diff / (1000 * 60 * 60 * 24));
                } catch (Exception ignored) {}

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackgroundColor(0xFFFFFFFF);
                card.setPadding(dp(20), dp(16), dp(20), dp(16));

                // Guest + payment status row
                LinearLayout topRow = new LinearLayout(this);
                topRow.setOrientation(LinearLayout.HORIZONTAL);
                topRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                TextView guestTv = new TextView(this);
                guestTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                guestTv.setText("👤 " + (guestName != null ? guestName : "Guest"));
                guestTv.setTextSize(14);
                guestTv.setTypeface(null, android.graphics.Typeface.BOLD);
                guestTv.setTextColor(0xFF222222);

                // Payment badge
                TextView payBadge = new TextView(this);
                boolean isPaid = "paid".equals(payStatus);
                boolean awaitingPayment = !isPaid && deadline > 0 && deadline > now;
                payBadge.setText(isPaid ? "💳 Paid" : awaitingPayment ? "⏳ Awaiting Payment" : "⚠️ Unpaid");
                payBadge.setTextSize(11);
                payBadge.setTypeface(null, android.graphics.Typeface.BOLD);
                payBadge.setTextColor(isPaid ? 0xFF00A699 : awaitingPayment ? 0xFFFF9800 : 0xFFE74C3C);
                payBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
                payBadge.setBackgroundColor(isPaid ? 0xFFE8F5E9 : awaitingPayment ? 0xFFFFF3E0 : 0xFFFFEBEE);

                topRow.addView(guestTv);
                topRow.addView(payBadge);
                card.addView(topRow);

                // Property
                addResRow(card, "🏠 " + (title != null ? title : "—"), 0xFF222222, 13, false);
                addResRow(card, "📍 " + (location != null ? location : "—"), 0xFF717171, 12, false);

                // Dates
                addResRow(card, formatDate(checkIn) + "  →  " + formatDate(checkOut)
                    + "  (" + nights + " night" + (nights != 1 ? "s" : "") + ")", 0xFF484848, 13, false);

                // Pricing
                String totalStr = (total == Math.floor(total)) ? String.valueOf((int) total) : String.valueOf(total);
                addResRow(card, "Total: ₱" + totalStr, 0xFF222222, 13, true);

                // Payment details
                if (isPaid) {
                    String depStr = (deposit == Math.floor(deposit)) ? String.valueOf((int) deposit) : String.valueOf(deposit);
                    double remaining = total - deposit;
                    String remStr = (remaining == Math.floor(remaining)) ? String.valueOf((int) remaining) : String.valueOf(remaining);
                    addResRow(card, "✅ Deposit paid: ₱" + depStr + "  |  Remaining: ₱" + remStr, 0xFF00A699, 12, false);
                } else if (awaitingPayment) {
                    long hoursLeft = (deadline - now) / (1000 * 60 * 60);
                    long minsLeft  = ((deadline - now) % (1000 * 60 * 60)) / (1000 * 60);
                    double dep = Math.ceil(total * 0.5);
                    String depStr = (dep == Math.floor(dep)) ? String.valueOf((int) dep) : String.valueOf(dep);
                    addResRow(card, "⏰ Deposit due: ₱" + depStr + "  |  " + hoursLeft + "h " + minsLeft + "m left", 0xFFE65100, 12, false);
                }

                // Divider
                View divider = new View(this);
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divLp.setMargins(0, dp(8), 0, 0);
                divider.setLayoutParams(divLp);
                divider.setBackgroundColor(0xFFF0F0F0);

                root.addView(card);
                root.addView(divider);

            } while (cursor.moveToNext());
            cursor.close();
        }

        new AlertDialog.Builder(this)
            .setTitle("Active Reservations")
            .setView(scroll)
            .setPositiveButton("Close", null)
            .show();
    }

    private void addResRow(LinearLayout parent, String text, int color, int size, boolean bold) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(3), 0, 0);
        tv.setLayoutParams(lp);
        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        parent.addView(tv);
    }

    private String formatDate(String dateStr) {
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
            return out.format(in.parse(dateStr));
        } catch (Exception e) { return dateStr; }
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    private void showLikesDialog() {
        Cursor cursor = db.getLikesForHost(userEmail);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);
        scroll.addView(root);

        if (cursor == null || cursor.getCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("♡  No likes yet\n\nWhen guests like your listings, they'll appear here.");
            empty.setTextSize(14);
            empty.setTextColor(0xFF717171);
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(dp(24), dp(48), dp(24), dp(48));
            empty.setLineSpacing(0, 1.4f);
            root.addView(empty);
        } else {
            cursor.moveToFirst();
            do {
                String likerName    = cursor.getString(cursor.getColumnIndex("liker_name"));
                String listingTitle = cursor.getString(cursor.getColumnIndex("listing_title"));

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setBackgroundColor(0xFFFFFFFF);
                row.setPadding(dp(16), dp(14), dp(16), dp(14));
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                // Heart icon circle
                LinearLayout heart = new LinearLayout(this);
                heart.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
                android.graphics.drawable.GradientDrawable heartBg = new android.graphics.drawable.GradientDrawable();
                heartBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                heartBg.setColor(0xFFFFEBEE);
                heart.setBackground(heartBg);
                heart.setGravity(android.view.Gravity.CENTER);
                TextView heartTv = new TextView(this);
                heartTv.setText("♥");
                heartTv.setTextSize(16);
                heartTv.setTextColor(0xFFFF385C);
                heart.addView(heartTv);

                // Text
                LinearLayout content = new LinearLayout(this);
                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                cLp.setMargins(dp(12), 0, 0, 0);
                content.setLayoutParams(cLp);
                content.setOrientation(LinearLayout.VERTICAL);

                TextView nameTv = new TextView(this);
                nameTv.setText(likerName != null ? likerName : "A guest");
                nameTv.setTextSize(14);
                nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
                nameTv.setTextColor(0xFF222222);

                TextView listingTv = new TextView(this);
                listingTv.setText("liked  " + (listingTitle != null ? listingTitle : "your listing"));
                listingTv.setTextSize(12);
                listingTv.setTextColor(0xFF717171);
                listingTv.setPadding(0, dp(2), 0, 0);

                content.addView(nameTv);
                content.addView(listingTv);
                row.addView(heart);
                row.addView(content);

                View divider = new View(this);
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divider.setLayoutParams(divLp);
                divider.setBackgroundColor(0xFFF0F0F0);

                root.addView(row);
                root.addView(divider);

            } while (cursor.moveToNext());
            cursor.close();
        }

        new AlertDialog.Builder(this)
            .setTitle("❤️  Listing Likes")
            .setView(scroll)
            .setPositiveButton("Close", null)
            .show();
    }

    private void showCalendarDialog(int listingId, String listingTitle) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Set<String> bookedDates = new HashSet<>();
        Set<String> maintenanceDates = new HashSet<>();
        Map<String, Integer> maintDateToId = new HashMap<>();

        // Load booked dates
        Cursor bc = db.getBookedDatesForListing(listingId);
        if (bc != null) {
            while (bc.moveToNext()) {
                expandRange(bc.getString(0), bc.getString(1), bookedDates, sdf);
            }
            bc.close();
        }

        // Load maintenance dates with IDs so we can delete them
        Cursor mc = db.getMaintenanceWithIds(listingId);
        if (mc != null) {
            while (mc.moveToNext()) {
                int mId = mc.getInt(mc.getColumnIndex("id"));
                String mStart = mc.getString(mc.getColumnIndex("start_date"));
                String mEnd = mc.getString(mc.getColumnIndex("end_date"));
                expandRange(mStart, mEnd, maintenanceDates, sdf);
                try {
                    Calendar c = Calendar.getInstance();
                    c.setTime(sdf.parse(mStart));
                    Calendar endCal = Calendar.getInstance();
                    endCal.setTime(sdf.parse(mEnd));
                    while (!c.after(endCal)) {
                        maintDateToId.put(sdf.format(c.getTime()), mId);
                        c.add(Calendar.DAY_OF_MONTH, 1);
                    }
                } catch (Exception ignored) {}
            }
            mc.close();
        }

        // Build calendar view
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 24, 32, 16);
        root.setBackgroundColor(0xFFFFFFFF);

        // Month navigation
        final Calendar[] calMonth = {Calendar.getInstance()};
        calMonth[0].set(Calendar.DAY_OF_MONTH, 1);

        TextView monthLabel = new TextView(this);
        monthLabel.setTextSize(15);
        monthLabel.setTextColor(0xFF222222);
        monthLabel.setGravity(android.view.Gravity.CENTER);

        LinearLayout navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        navRow.setLayoutParams(navLp);

        Button btnPrev = new Button(this);
        btnPrev.setText("‹");
        btnPrev.setTextSize(18);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnPrev.setLayoutParams(btnLp);

        monthLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button btnNext = new Button(this);
        btnNext.setText("›");
        btnNext.setTextSize(18);
        btnNext.setLayoutParams(btnLp);

        navRow.addView(btnPrev);
        navRow.addView(monthLabel);
        navRow.addView(btnNext);

        LinearLayout calGrid = new LinearLayout(this);
        calGrid.setOrientation(LinearLayout.VERTICAL);

        // Legend
        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        legend.setPadding(0, 12, 0, 4);

        TextView legBooked = new TextView(this);
        legBooked.setText("● Reserved  ");
        legBooked.setTextColor(0xFFFF385C);
        legBooked.setTextSize(11);

        TextView legMaint = new TextView(this);
        legMaint.setText("● Maintenance  ");
        legMaint.setTextColor(0xFFFFC107);
        legMaint.setTextSize(11);

        TextView legFree = new TextView(this);
        legFree.setText("● Available");
        legFree.setTextColor(0xFF00C853);
        legFree.setTextSize(11);

        legend.addView(legBooked);
        legend.addView(legMaint);
        legend.addView(legFree);

        // Set maintenance button
        Button btnSetMaintenance = new Button(this);
        btnSetMaintenance.setText("+ Set Maintenance Date");
        btnSetMaintenance.setTextColor(0xFFFFFFFF);
        btnSetMaintenance.setBackgroundColor(0xFFFF385C);
        LinearLayout.LayoutParams maintBtnLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        maintBtnLp.setMargins(0, 16, 0, 0);
        btnSetMaintenance.setLayoutParams(maintBtnLp);

        root.addView(navRow);
        root.addView(calGrid);
        root.addView(legend);
        root.addView(btnSetMaintenance);

        // Render helper
        Runnable render = new Runnable() {
            @Override public void run() {
                SimpleDateFormat mFmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                monthLabel.setText(mFmt.format(calMonth[0].getTime()));
                calGrid.removeAllViews();

                Calendar c = (Calendar) calMonth[0].clone();
                c.set(Calendar.DAY_OF_MONTH, 1);
                int firstDow = c.get(Calendar.DAY_OF_WEEK) - 1;
                int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
                int dayCount = 0;
                LinearLayout row = null;

                for (int cell = 0; cell < firstDow + daysInMonth; cell++) {
                    if (cell % 7 == 0) {
                        row = new LinearLayout(HostDashboardActivity.this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                        calGrid.addView(row);
                    }
                    TextView tv = new TextView(HostDashboardActivity.this);
                    int cellSize = (int) (36 * getResources().getDisplayMetrics().density);
                    tv.setLayoutParams(new LinearLayout.LayoutParams(0, cellSize, 1));
                    tv.setGravity(android.view.Gravity.CENTER);
                    tv.setTextSize(13);

                    if (cell < firstDow) {
                        tv.setText("");
                    } else {
                        dayCount++;
                        c.set(Calendar.DAY_OF_MONTH, dayCount);
                        String dateStr = sdf.format(c.getTime());
                        tv.setText(String.valueOf(dayCount));
                        if (bookedDates.contains(dateStr)) {
                            tv.setBackgroundColor(0xFFFF385C);
                            tv.setTextColor(0xFFFFFFFF);
                        } else if (maintenanceDates.contains(dateStr)) {
                            tv.setBackgroundColor(0xFFFFC107);
                            tv.setTextColor(0xFF222222);
                        } else {
                            tv.setBackgroundColor(0x00000000);
                            tv.setTextColor(0xFF222222);
                        }
                    }
                    if (row != null) row.addView(tv);
                }
                // Fill last row
                if (row != null) {
                    int rem = 7 - row.getChildCount();
                    for (int i = 0; i < rem; i++) {
                        TextView empty = new TextView(HostDashboardActivity.this);
                        empty.setLayoutParams(new LinearLayout.LayoutParams(0,
                            (int)(36 * getResources().getDisplayMetrics().density), 1));
                        row.addView(empty);
                    }
                }
            }
        };

        render.run();
        btnPrev.setOnClickListener(v -> { calMonth[0].add(Calendar.MONTH, -1); render.run(); });
        btnNext.setOnClickListener(v -> { calMonth[0].add(Calendar.MONTH, 1); render.run(); });

        btnSetMaintenance.setOnClickListener(v -> showAddMaintenanceDialog(listingId, bookedDates, maintenanceDates, sdf, render));

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setBackgroundColor(0xFFFFFFFF);
        scrollView.addView(root);

        new AlertDialog.Builder(this)
            .setTitle("Availability: " + listingTitle)
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .show();
    }

    private void showAddMaintenanceDialog(int listingId, Set<String> bookedDates, Set<String> maintenanceDates, SimpleDateFormat sdf, Runnable onSaved) {
        final String[] startDate = {""};
        final String[] endDate = {""};

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);
        layout.setBackgroundColor(0xFFFFFFFF);

        TextView lblStart = new TextView(this);
        lblStart.setText("Start date: not set");
        lblStart.setTextSize(14);
        lblStart.setTextColor(0xFF222222);
        lblStart.setPadding(0, 0, 0, 8);

        Button btnStart = new Button(this);
        btnStart.setText("Pick start date");
        btnStart.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(y, m, d);
                startDate[0] = sdf.format(sel.getTime());
                lblStart.setText("Start: " + startDate[0]);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        TextView lblEnd = new TextView(this);
        lblEnd.setText("End date: not set");
        lblEnd.setTextSize(14);
        lblEnd.setTextColor(0xFF222222);
        lblEnd.setPadding(0, 16, 0, 8);

        Button btnEnd = new Button(this);
        btnEnd.setText("Pick end date");
        btnEnd.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(y, m, d);
                endDate[0] = sdf.format(sel.getTime());
                lblEnd.setText("End: " + endDate[0]);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        layout.addView(lblStart);
        layout.addView(btnStart);
        layout.addView(lblEnd);
        layout.addView(btnEnd);

        new AlertDialog.Builder(this)
            .setTitle("Set Maintenance Period")
            .setView(layout)
            .setPositiveButton("Save", (d, w) -> {
                if (startDate[0].isEmpty() || endDate[0].isEmpty()) {
                    StyledAlert.show(this, StyledAlert.WARNING, "Missing Dates",
                        "Please pick both a start and end date.");
                    return;
                }
                if (endDate[0].compareTo(startDate[0]) < 0) {
                    StyledAlert.show(this, StyledAlert.WARNING, "Invalid Dates",
                        "End date must be on or after the start date.");
                    return;
                }
                db.addMaintenance(listingId, startDate[0], endDate[0]);
                // Refresh maintenance dates set
                maintenanceDates.clear();
                Cursor mc = db.getMaintenanceDatesForListing(listingId);
                if (mc != null) {
                    while (mc.moveToNext()) {
                        expandRange(mc.getString(0), mc.getString(1), maintenanceDates, sdf);
                    }
                    mc.close();
                }
                StyledAlert.show(this, StyledAlert.SUCCESS, "Maintenance Set",
                    "The maintenance period has been saved.");
                onSaved.run();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void expandRange(String start, String end, Set<String> set, SimpleDateFormat sdf) {
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(sdf.parse(start));
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(sdf.parse(end));
            while (!c.after(endCal)) {
                set.add(sdf.format(c.getTime()));
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
        } catch (Exception ignored) {}
    }
}
