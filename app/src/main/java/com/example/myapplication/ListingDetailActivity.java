package com.example.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ListingDetailActivity extends Activity {

    DatabaseHelper db;
    String userEmail;
    int listingId;
    double pricePerNight;
    String checkInDate = "", checkOutDate = "";

    // Calendar state
    Calendar calendarMonth;
    Set<String> bookedDates = new HashSet<>();
    Set<String> maintenanceDates = new HashSet<>();
    Map<String, Integer> maintenanceDateToId = new HashMap<>();
    LinearLayout calGrid;
    TextView calMonthLabel;

    // Reviews
    LinearLayout reviewsContainer;
    boolean isHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_detail);

        db = new DatabaseHelper(this);
        listingId = getIntent().getIntExtra("LISTING_ID", -1);
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        String userRole = getIntent().getStringExtra("USER_ROLE");
        isHost = "host".equals(userRole);

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        if (listingId == -1) { finish(); return; }

        Cursor cursor = db.getListingById(listingId);
        if (cursor == null || !cursor.moveToFirst()) { finish(); return; }

        String title = cursor.getString(cursor.getColumnIndex("title"));
        String location = cursor.getString(cursor.getColumnIndex("location"));
        String type = cursor.getString(cursor.getColumnIndex("property_type"));
        String description = cursor.getString(cursor.getColumnIndex("description"));
        String amenities = cursor.getString(cursor.getColumnIndex("amenities"));
        pricePerNight = cursor.getDouble(cursor.getColumnIndex("price_per_night"));
        int maxGuests = cursor.getInt(cursor.getColumnIndex("max_guests"));
        int bedrooms = cursor.getInt(cursor.getColumnIndex("bedrooms"));
        int beds = cursor.getInt(cursor.getColumnIndex("beds"));
        double baths = cursor.getDouble(cursor.getColumnIndex("bathrooms"));
        String imageUriStr = cursor.getString(cursor.getColumnIndex("image_uri"));
        String hostName = cursor.getString(cursor.getColumnIndex("host_name"));
        String hostEmail = cursor.getString(cursor.getColumnIndex("host_email"));
        cursor.close();

        // Build photo strip
        LinearLayout photoStrip = findViewById(R.id.detail_photo_strip);
        LinearLayout placeholder = findViewById(R.id.detail_image_placeholder);
        TextView photoCount = findViewById(R.id.detail_photo_count);

        String[] uris = (imageUriStr != null && !imageUriStr.isEmpty())
            ? imageUriStr.split(",") : new String[0];

        if (uris.length > 0) {
            placeholder.setVisibility(View.GONE);
            int screenW = getResources().getDisplayMetrics().widthPixels;
            for (String uriStr : uris) {
                if (uriStr.trim().isEmpty()) continue;
                ImageView img = new ImageView(this);
                img.setLayoutParams(new LinearLayout.LayoutParams(screenW, LinearLayout.LayoutParams.MATCH_PARENT));
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                try {
                    Uri parsedUri = Uri.parse(uriStr.trim());
                    getContentResolver().openInputStream(parsedUri).close();
                    img.setImageURI(parsedUri);
                } catch (Exception ignored) {
                    img.setBackgroundColor(0xFFE8E8E8);
                }
                photoStrip.addView(img);
            }
            if (uris.length > 1) {
                photoCount.setText("1 / " + uris.length);
                photoCount.setVisibility(View.VISIBLE);
                // Update count as user scrolls
                HorizontalScrollView scrollView = findViewById(R.id.detail_photo_scroll);
                scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                    int scrollX = scrollView.getScrollX();
                    int page = (scrollX / screenW) + 1;
                    photoCount.setText(page + " / " + uris.length);
                });
            }
        } else {
            placeholder.setVisibility(View.VISIBLE);
        }

        ((TextView) findViewById(R.id.detail_title)).setText(title);
        ((TextView) findViewById(R.id.detail_location)).setText("📍 " + location);
        ((TextView) findViewById(R.id.detail_type)).setText(type);
        ((TextView) findViewById(R.id.detail_specs)).setText(
            maxGuests + " guests · " + bedrooms + " bedrooms · " + beds + " beds · " + baths + " baths");
        ((TextView) findViewById(R.id.detail_description)).setText(
            (description != null && !description.isEmpty()) ? description : "No description provided.");

        if (amenities != null && !amenities.isEmpty()) {
            String[] items = amenities.split(", ");
            StringBuilder sb = new StringBuilder();
            for (String item : items) sb.append("✓  ").append(item).append("\n");
            ((TextView) findViewById(R.id.detail_amenities)).setText(sb.toString().trim());
        } else {
            ((TextView) findViewById(R.id.detail_amenities)).setText("No amenities listed.");
        }

        String priceStr = (pricePerNight == Math.floor(pricePerNight)) ? String.valueOf((int) pricePerNight) : String.valueOf(pricePerNight);
        ((TextView) findViewById(R.id.detail_price)).setText("₱" + priceStr);

        // Host section
        String displayHost = (hostName != null && !hostName.trim().isEmpty()) ? hostName : "Host";
        ((TextView) findViewById(R.id.detail_host_name)).setText("Hosted by " + displayHost);
        String initial = displayHost.length() > 0 ? String.valueOf(displayHost.charAt(0)).toUpperCase() : "H";
        ((TextView) findViewById(R.id.host_avatar_initial)).setText(initial);

        // Load host profile photo if available
        android.widget.ImageView hostAvatarImage = findViewById(R.id.host_avatar_image);
        android.widget.LinearLayout hostAvatarFallback = findViewById(R.id.host_avatar_fallback);
        String hostProfileUri = db.getProfileImageUri(hostEmail != null ? hostEmail : "");
        if (hostProfileUri != null && !hostProfileUri.isEmpty()) {
            try {
                getContentResolver().openInputStream(android.net.Uri.parse(hostProfileUri)).close();
                hostAvatarImage.setImageURI(android.net.Uri.parse(hostProfileUri));
                hostAvatarImage.setVisibility(View.VISIBLE);
                hostAvatarFallback.setVisibility(View.GONE);
            } catch (Exception ignored) {
                hostAvatarImage.setVisibility(View.GONE);
                hostAvatarFallback.setVisibility(View.VISIBLE);
            }
        } else {
            hostAvatarImage.setVisibility(View.GONE);
            hostAvatarFallback.setVisibility(View.VISIBLE);
        }

        Button reserveBtn = findViewById(R.id.btn_reserve);
        TextView msgBtn = findViewById(R.id.btn_message_host);

        if (isHost) {
            reserveBtn.setText("Set Maintenance");
            reserveBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { showMaintenanceDialog(); }
            });
            msgBtn.setVisibility(View.GONE);
        } else {
            reserveBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { showBookingDialog(); }
            });
            // Show message button for guests — opens chat with host
            final String finalHostEmail = hostEmail;
            final String finalTitle = ((TextView) findViewById(R.id.detail_title)).getText().toString();
            msgBtn.setVisibility(View.VISIBLE);
            msgBtn.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(
                    ListingDetailActivity.this, ChatActivity.class);
                intent.putExtra("USER_EMAIL", userEmail);
                intent.putExtra("OTHER_EMAIL", finalHostEmail);
                intent.putExtra("LISTING_ID", listingId);
                intent.putExtra("LISTING_TITLE", finalTitle);
                startActivity(intent);
            });
        }

        // Build availability calendar
        calGrid = findViewById(R.id.cal_grid);
        calMonthLabel = findViewById(R.id.cal_month_label);
        calendarMonth = Calendar.getInstance();
        calendarMonth.set(Calendar.DAY_OF_MONTH, 1);
        loadAvailabilityDates();

        findViewById(R.id.cal_prev).setOnClickListener(v -> {
            calendarMonth.add(Calendar.MONTH, -1);
            renderCalendar();
        });
        findViewById(R.id.cal_next).setOnClickListener(v -> {
            calendarMonth.add(Calendar.MONTH, 1);
            renderCalendar();
        });

        // Reviews section
        reviewsContainer = findViewById(R.id.reviews_container);
        TextView btnWriteReview = findViewById(R.id.btn_write_review);
        if (!isHost && db.hasCompletedStay(listingId, userEmail)) {
            btnWriteReview.setVisibility(View.VISIBLE);
            btnWriteReview.setOnClickListener(v -> showReviewDialog());
        } else {
            btnWriteReview.setVisibility(View.GONE);
        }
        loadReviews();
    }

    private void showBookingDialog() {
        checkInDate = "";
        checkOutDate = "";

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_booking, null);

        TextView txtCheckIn = dialogView.findViewById(R.id.txt_checkin_value);
        TextView txtCheckOut = dialogView.findViewById(R.id.txt_checkout_value);
        TextView txtNights = dialogView.findViewById(R.id.txt_nights_summary);
        TextView txtTotal = dialogView.findViewById(R.id.txt_total_summary);
        LinearLayout bookingCalGrid = dialogView.findViewById(R.id.booking_cal_grid);
        TextView bookingCalMonth = dialogView.findViewById(R.id.booking_cal_month);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        final Calendar[] calMonth = {Calendar.getInstance()};
        calMonth[0].set(Calendar.DAY_OF_MONTH, 1);

        // picking state: 0 = pick check-in, 1 = pick check-out
        final int[] pickState = {0};

        Runnable renderBookingCal = new Runnable() {
            @Override public void run() {
                bookingCalMonth.setText(monthFmt.format(calMonth[0].getTime()));
                bookingCalGrid.removeAllViews();

                Calendar c = (Calendar) calMonth[0].clone();
                c.set(Calendar.DAY_OF_MONTH, 1);
                int firstDow = c.get(Calendar.DAY_OF_WEEK) - 1;
                int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
                int dayCount = 0;
                LinearLayout row = null;
                int cellH = (int)(32 * getResources().getDisplayMetrics().density);

                for (int cell = 0; cell < firstDow + daysInMonth; cell++) {
                    if (cell % 7 == 0) {
                        row = new LinearLayout(ListingDetailActivity.this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        bookingCalGrid.addView(row);
                    }
                    TextView tv = new TextView(ListingDetailActivity.this);
                    tv.setLayoutParams(new LinearLayout.LayoutParams(0, cellH, 1));
                    tv.setGravity(android.view.Gravity.CENTER);
                    tv.setTextSize(12);

                    if (cell < firstDow) {
                        tv.setText("");
                    } else {
                        dayCount++;
                        c.set(Calendar.DAY_OF_MONTH, dayCount);
                        String dateStr = sdf.format(c.getTime());
                        tv.setText(String.valueOf(dayCount));

                        String todayStr = sdf.format(Calendar.getInstance().getTime());
                        boolean isPast   = dateStr.compareTo(todayStr) < 0;
                        boolean isBooked = bookedDates.contains(dateStr);
                        boolean isMaint  = maintenanceDates.contains(dateStr);
                        boolean isCheckIn  = dateStr.equals(checkInDate);
                        boolean isCheckOut = dateStr.equals(checkOutDate);
                        boolean inRange = !checkInDate.isEmpty() && !checkOutDate.isEmpty()
                            && dateStr.compareTo(checkInDate) > 0 && dateStr.compareTo(checkOutDate) < 0;

                        if (isPast) {
                            // Past dates — greyed out, not selectable
                            tv.setBackgroundColor(0x00000000);
                            tv.setTextColor(0xFFCCCCCC);
                        } else if (isBooked) {
                            tv.setBackgroundColor(0xFFFF385C);
                            tv.setTextColor(0xFFFFFFFF);
                        } else if (isMaint) {
                            tv.setBackgroundColor(0xFFFFC107);
                            tv.setTextColor(0xFF222222);
                        } else if (isCheckIn || isCheckOut) {
                            tv.setBackgroundColor(0xFFFF385C);
                            tv.setTextColor(0xFFFFFFFF);
                        } else if (inRange) {
                            tv.setBackgroundColor(0x33FF385C);
                            tv.setTextColor(0xFF222222);
                        } else {
                            tv.setBackgroundColor(0x00000000);
                            tv.setTextColor(0xFF222222);
                        }

                        if (!isPast && !isBooked && !isMaint) {
                            final String ds = dateStr;
                            tv.setClickable(true);
                            tv.setFocusable(true);
                            tv.setOnClickListener(v2 -> {
                                if (pickState[0] == 0) {
                                    checkInDate = ds;
                                    checkOutDate = "";
                                    txtCheckIn.setText(formatDisplay(ds));
                                    txtCheckOut.setText("Tap a date");
                                    pickState[0] = 1;
                                } else {
                                    if (ds.compareTo(checkInDate) <= 0) {
                                        checkInDate = ds;
                                        checkOutDate = "";
                                        txtCheckIn.setText(formatDisplay(ds));
                                        txtCheckOut.setText("Tap a date");
                                    } else {
                                        checkOutDate = ds;
                                        txtCheckOut.setText(formatDisplay(ds));
                                        pickState[0] = 0;
                                        updateSummary(txtNights, txtTotal);
                                    }
                                }
                                this.run();
                            });
                        }
                    }
                    if (row != null) row.addView(tv);
                }
                if (row != null) {
                    int rem = 7 - row.getChildCount();
                    for (int i = 0; i < rem; i++) {
                        TextView empty = new TextView(ListingDetailActivity.this);
                        empty.setLayoutParams(new LinearLayout.LayoutParams(0, cellH, 1));
                        row.addView(empty);
                    }
                }
            }
        };

        renderBookingCal.run();

        dialogView.findViewById(R.id.booking_cal_prev).setOnClickListener(v -> {
            calMonth[0].add(Calendar.MONTH, -1);
            renderBookingCal.run();
        });
        dialogView.findViewById(R.id.booking_cal_next).setOnClickListener(v -> {
            calMonth[0].add(Calendar.MONTH, 1);
            renderBookingCal.run();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Confirm booking", null)
            .setNegativeButton("Cancel", null)
            .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (checkInDate.isEmpty() || checkOutDate.isEmpty()) {
                    showStyledAlert("Incomplete Dates", "Please select both a check-in and check-out date.", "⚠️", 0xFFF39C12);
                    return;
                }
                if (checkOutDate.compareTo(checkInDate) <= 0) {
                    showStyledAlert("Invalid Dates", "Check-out must be after check-in.", "⚠️", 0xFFF39C12);
                    return;
                }
                String todayCheck = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
                if (checkInDate.compareTo(todayCheck) < 0) {
                    showStyledAlert("Invalid Dates", "Check-in date cannot be in the past.", "⚠️", 0xFFF39C12);
                    return;
                }
                if (hasConflict(checkInDate, checkOutDate)) {
                    showStyledAlert("Dates Unavailable",
                        "Your selected dates include reserved or maintenance days.\n\nPlease choose different dates.",
                        "🚫", 0xFFE74C3C);
                    return;
                }
                if (db.hasOverlappingBooking(userEmail, listingId, checkInDate, checkOutDate)) {
                    showStyledAlert("Duplicate Booking",
                        "You already have an active reservation for this place on overlapping dates.",
                        "⚠️", 0xFFF39C12);
                    return;
                }
                int nights = nightsBetween(checkInDate, checkOutDate);
                double total = nights * pricePerNight;
                long result = db.addBooking(userEmail, listingId, checkInDate, checkOutDate, 1, total);
                dialog.dismiss();
                if (result != -1) {
                    // Notify the host about the new reservation request
                    String hostEmail = db.getHostEmailForListing(listingId);
                    if (hostEmail != null) {
                        Cursor guestCursor = db.getUserByEmail(userEmail);
                        String guestName = userEmail;
                        if (guestCursor != null && guestCursor.moveToFirst()) {
                            guestName = guestCursor.getString(guestCursor.getColumnIndex("first_name"))
                                + " " + guestCursor.getString(guestCursor.getColumnIndex("last_name"));
                            guestCursor.close();
                        }
                        String listingTitleStr = ((TextView) findViewById(R.id.detail_title)).getText().toString();
                        db.addNotification(hostEmail,
                            "📥 New Reservation Request",
                            guestName + " has requested to book \"" + listingTitleStr + "\" from "
                                + formatDisplay(checkInDate) + " to " + formatDisplay(checkOutDate) + ".",
                            "booking_request");
                    }
                    // Just show confirmation — payment happens after host approves
                    StyledAlert.show(ListingDetailActivity.this, StyledAlert.SUCCESS,
                        "Request Sent! ⏳",
                        "Your reservation request has been sent to the host.\n\nYou'll be notified once the host approves. After approval, you'll have 24 hours to pay the 50% deposit.");
                } else {
                    showStyledAlert("Booking Failed", "Something went wrong. Please try again.", "❌", 0xFFE74C3C);
                }
            });
        });

        dialog.show();
    }

    private void showStyledAlert(String title, String message, String emoji, int accentColor) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(android.view.Gravity.CENTER);
        root.setPadding(dp(24), dp(28), dp(24), dp(16));
        root.setBackgroundColor(0xFFFFFFFF);

        TextView emojiTv = new TextView(this);
        emojiTv.setText(emoji);
        emojiTv.setTextSize(48);
        emojiTv.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        eLp.setMargins(0, 0, 0, dp(12));
        emojiTv.setLayoutParams(eLp);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextSize(18);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTv.setTextColor(0xFF222222);
        titleTv.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tLp.setMargins(0, 0, 0, dp(8));
        titleTv.setLayoutParams(tLp);

        TextView msgTv = new TextView(this);
        msgTv.setText(message);
        msgTv.setTextSize(14);
        msgTv.setTextColor(0xFF717171);
        msgTv.setGravity(android.view.Gravity.CENTER);
        msgTv.setLineSpacing(0, 1.4f);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mLp.setMargins(0, 0, 0, dp(24));
        msgTv.setLayoutParams(mLp);

        Button okBtn = new Button(this);
        okBtn.setText("OK");
        okBtn.setTextColor(0xFFFFFFFF);
        okBtn.setBackgroundColor(accentColor);
        okBtn.setPadding(dp(32), dp(10), dp(32), dp(10));
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        okBtn.setLayoutParams(bLp);

        root.addView(emojiTv);
        root.addView(titleTv);
        root.addView(msgTv);
        root.addView(okBtn);

        AlertDialog alert = new AlertDialog.Builder(this)
            .setView(root)
            .create();
        okBtn.setOnClickListener(v -> alert.dismiss());
        alert.show();
    }

    private boolean hasConflict(String checkIn, String checkOut) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(sdf.parse(checkIn));
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(sdf.parse(checkOut));
            // Check each day from check-in up to (but not including) check-out
            while (c.before(endCal)) {
                String dateStr = sdf.format(c.getTime());
                if (bookedDates.contains(dateStr) || maintenanceDates.contains(dateStr)) {
                    return true;
                }
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void updateSummary(TextView txtNights, TextView txtTotal) {
        if (!checkInDate.isEmpty() && !checkOutDate.isEmpty() && checkOutDate.compareTo(checkInDate) > 0) {
            int nights = nightsBetween(checkInDate, checkOutDate);
            double total = nights * pricePerNight;
            String priceStr = (pricePerNight == Math.floor(pricePerNight)) ? String.valueOf((int) pricePerNight) : String.valueOf(pricePerNight);
            String totalStr = (total == Math.floor(total)) ? String.valueOf((int) total) : String.valueOf(total);
            txtNights.setText(nights + " night" + (nights > 1 ? "s" : "") + " × ₱" + priceStr);
            txtTotal.setText("₱" + totalStr + " total");
        }
    }

    private void showBookingConfirmation(int nights, double total) {
        String totalStr = (total == Math.floor(total)) ? String.valueOf((int) total) : String.valueOf(total);
        new AlertDialog.Builder(this)
            .setTitle("⏳ Request sent!")
            .setMessage("Your booking request has been sent to the host.\n\n" +
                "Check-in: " + formatDisplay(checkInDate) + "\n" +
                "Check-out: " + formatDisplay(checkOutDate) + "\n" +
                "Duration: " + nights + " night" + (nights > 1 ? "s" : "") + "\n" +
                "Total: ₱" + totalStr + "\n\n" +
                "You'll be notified once the host approves your request.")
            .setPositiveButton("View my trips", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    Intent intent = new Intent(ListingDetailActivity.this, TripsActivity.class);
                    intent.putExtra("USER_EMAIL", userEmail);
                    startActivity(intent);
                }
            })
            .setNegativeButton("Done", null)
            .show();
    }

    private int nightsBetween(String checkIn, String checkOut) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            long diff = sdf.parse(checkOut).getTime() - sdf.parse(checkIn).getTime();
            return (int) (diff / (1000 * 60 * 60 * 24));
        } catch (Exception e) { return 0; }
    }

    private String formatDisplay(String dateStr) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return out.format(in.parse(dateStr));
        } catch (Exception e) { return dateStr; }
    }

    // ---- Calendar ----

    private void showMaintenanceDialog() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        final String[] startDate = {""};
        final String[] endDate = {""};

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        TextView lblStart = new TextView(this);
        lblStart.setText("Start date: not set");
        lblStart.setTextSize(14);
        lblStart.setTextColor(0xFF222222);
        lblStart.setPadding(0, 0, 0, 8);

        TextView lblEnd = new TextView(this);
        lblEnd.setText("End date: not set");
        lblEnd.setTextSize(14);
        lblEnd.setTextColor(0xFF222222);
        lblEnd.setPadding(0, 8, 0, 0);

        Button btnStart = new Button(this);
        btnStart.setText("Pick start date");
        btnStart.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(y, m, d);
                startDate[0] = sdf.format(sel.getTime());
                lblStart.setText("Start: " + formatDisplay(startDate[0]));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        Button btnEnd = new Button(this);
        btnEnd.setText("Pick end date");
        btnEnd.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(y, m, d);
                endDate[0] = sdf.format(sel.getTime());
                lblEnd.setText("End: " + formatDisplay(endDate[0]));
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
                StyledAlert.show(this, StyledAlert.SUCCESS, "Maintenance Set",
                    "The maintenance period has been saved.");
                loadAvailabilityDates();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ---- Calendar ----

    private void loadAvailabilityDates() {
        bookedDates.clear();
        maintenanceDates.clear();
        maintenanceDateToId.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Cursor bc = db.getBookedDatesForListing(listingId);
        if (bc != null) {
            while (bc.moveToNext()) {
                expandDateRange(bc.getString(0), bc.getString(1), bookedDates, sdf);
            }
            bc.close();
        }

        // Use getMaintenanceWithIds so we can map each date back to its record id
        Cursor mc = db.getMaintenanceWithIds(listingId);
        if (mc != null) {
            while (mc.moveToNext()) {
                int mId = mc.getInt(mc.getColumnIndex("id"));
                String mStart = mc.getString(mc.getColumnIndex("start_date"));
                String mEnd = mc.getString(mc.getColumnIndex("end_date"));
                expandDateRange(mStart, mEnd, maintenanceDates, sdf);
                // Map every date in this range to the record id
                try {
                    Calendar c = Calendar.getInstance();
                    c.setTime(sdf.parse(mStart));
                    Calendar endCal = Calendar.getInstance();
                    endCal.setTime(sdf.parse(mEnd));
                    while (!c.after(endCal)) {
                        maintenanceDateToId.put(sdf.format(c.getTime()), mId);
                        c.add(Calendar.DAY_OF_MONTH, 1);
                    }
                } catch (Exception ignored) {}
            }
            mc.close();
        }

        renderCalendar();
    }

    private void expandDateRange(String start, String end, Set<String> set, SimpleDateFormat sdf) {
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

    private void renderCalendar() {
        SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        calMonthLabel.setText(monthFmt.format(calendarMonth.getTime()));

        calGrid.removeAllViews();

        Calendar c = (Calendar) calendarMonth.clone();
        c.set(Calendar.DAY_OF_MONTH, 1);
        int firstDow = c.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
        int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        int dayCount = 0;
        LinearLayout row = null;

        for (int cell = 0; cell < firstDow + daysInMonth; cell++) {
            if (cell % 7 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
                calGrid.addView(row);
            }

            TextView cell_tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                (int) (getResources().getDisplayMetrics().density * 36), 1);
            cell_tv.setLayoutParams(lp);
            cell_tv.setGravity(android.view.Gravity.CENTER);
            cell_tv.setTextSize(13);

            if (cell < firstDow) {
                // Empty cell before month starts
                cell_tv.setText("");
                cell_tv.setBackgroundColor(0x00000000);
            } else {
                dayCount++;
                c.set(Calendar.DAY_OF_MONTH, dayCount);
                String dateStr = sdf.format(c.getTime());
                cell_tv.setText(String.valueOf(dayCount));

                if (bookedDates.contains(dateStr)) {
                    cell_tv.setBackgroundColor(0xFFFF385C);
                    cell_tv.setTextColor(0xFFFFFFFF);
                } else if (maintenanceDates.contains(dateStr)) {
                    cell_tv.setBackgroundColor(0xFFFFC107);
                    cell_tv.setTextColor(0xFF222222);
                    if (isHost) {
                        final String ds = dateStr;
                        cell_tv.setClickable(true);
                        cell_tv.setFocusable(true);
                        cell_tv.setOnClickListener(v -> {
                            Integer mId = maintenanceDateToId.get(ds);
                            if (mId == null) return;
                            showMaintenanceOptionsDialog(mId, ds);
                        });
                    }
                } else {
                    cell_tv.setBackgroundColor(0x00000000);
                    cell_tv.setTextColor(0xFF222222);
                }
            }

            row.addView(cell_tv);
        }

        // Fill remaining cells in last row
        if (row != null) {
            int remaining = 7 - row.getChildCount();
            for (int i = 0; i < remaining; i++) {
                TextView empty = new TextView(this);
                empty.setLayoutParams(new LinearLayout.LayoutParams(0,
                    (int) (getResources().getDisplayMetrics().density * 36), 1));
                row.addView(empty);
            }
        }
    }

    // ---- Reviews ----

    private void showMaintenanceOptionsDialog(int maintenanceId, String tappedDate) {
        new AlertDialog.Builder(this)
            .setTitle("Maintenance — " + formatDisplay(tappedDate))
            .setMessage("What would you like to do with this maintenance period?")
            .setPositiveButton("✏️  Update dates", (d, w) -> showUpdateMaintenanceDialog(maintenanceId))
            .setNeutralButton("🗑️  Remove", (d, w) -> {
                new AlertDialog.Builder(this)
                    .setTitle("Remove maintenance?")
                    .setMessage("This will clear the maintenance block and make these dates available for booking.")
                    .setPositiveButton("Remove", (d2, w2) -> {
                        db.deleteMaintenance(maintenanceId);
                        StyledAlert.show(this, StyledAlert.SUCCESS, "Maintenance Removed",
                            "These dates are now available for booking.");
                        loadAvailabilityDates();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showUpdateMaintenanceDialog(int maintenanceId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Pre-fill with existing dates for this record
        final String[] startDate = {""};
        final String[] endDate = {""};
        Cursor existing = db.getMaintenanceWithIds(listingId);
        if (existing != null) {
            while (existing.moveToNext()) {
                if (existing.getInt(existing.getColumnIndex("id")) == maintenanceId) {
                    startDate[0] = existing.getString(existing.getColumnIndex("start_date"));
                    endDate[0] = existing.getString(existing.getColumnIndex("end_date"));
                    break;
                }
            }
            existing.close();
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(20), dp(24), dp(8));

        TextView lblStart = new TextView(this);
        lblStart.setText("Start: " + (startDate[0].isEmpty() ? "not set" : formatDisplay(startDate[0])));
        lblStart.setTextSize(14);
        lblStart.setTextColor(0xFF222222);

        Button btnStart = new Button(this);
        btnStart.setText("Change start date");
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, dp(4), 0, dp(12));
        btnStart.setLayoutParams(btnLp);
        btnStart.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(y, m, d);
                startDate[0] = sdf.format(sel.getTime());
                lblStart.setText("Start: " + formatDisplay(startDate[0]));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        TextView lblEnd = new TextView(this);
        lblEnd.setText("End: " + (endDate[0].isEmpty() ? "not set" : formatDisplay(endDate[0])));
        lblEnd.setTextSize(14);
        lblEnd.setTextColor(0xFF222222);

        Button btnEnd = new Button(this);
        btnEnd.setText("Change end date");
        LinearLayout.LayoutParams btnLp2 = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp2.setMargins(0, dp(4), 0, 0);
        btnEnd.setLayoutParams(btnLp2);
        btnEnd.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(y, m, d);
                endDate[0] = sdf.format(sel.getTime());
                lblEnd.setText("End: " + formatDisplay(endDate[0]));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        layout.addView(lblStart);
        layout.addView(btnStart);
        layout.addView(lblEnd);
        layout.addView(btnEnd);

        new AlertDialog.Builder(this)
            .setTitle("Update Maintenance Period")
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
                db.updateMaintenance(maintenanceId, startDate[0], endDate[0]);
                StyledAlert.show(this, StyledAlert.SUCCESS, "Maintenance Updated",
                    "The maintenance period has been updated.");
                loadAvailabilityDates();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ---- Reviews ----

    private void showReviewDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        // Star rating row
        TextView lblStars = new TextView(this);
        lblStars.setText("Your rating:");
        lblStars.setTextSize(14);
        lblStars.setTextColor(0xFF222222);
        lblStars.setPadding(0, 0, 0, 8);

        final int[] selectedRating = {5};

        LinearLayout starRow = new LinearLayout(this);
        starRow.setOrientation(LinearLayout.HORIZONTAL);
        starRow.setPadding(0, 0, 0, 16);
        TextView[] stars = new TextView[5];

        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            stars[i] = new TextView(this);
            stars[i].setText("★");
            stars[i].setTextSize(32);
            stars[i].setTextColor(starIndex <= selectedRating[0] ? 0xFFFF9800 : 0xFFCCCCCC);
            stars[i].setPadding(4, 0, 4, 0);
            stars[i].setClickable(true);
            stars[i].setFocusable(true);
            final TextView[] starsFinal = stars;
            stars[i].setOnClickListener(v -> {
                selectedRating[0] = starIndex;
                for (int j = 0; j < 5; j++) {
                    starsFinal[j].setTextColor(j < starIndex ? 0xFFFF9800 : 0xFFCCCCCC);
                }
            });
            starRow.addView(stars[i]);
        }

        // Comment input
        android.widget.EditText commentInput = new android.widget.EditText(this);
        commentInput.setHint("Share your experience...");
        commentInput.setMinLines(3);
        commentInput.setMaxLines(5);
        commentInput.setBackground(getDrawable(R.drawable.rounded_edittext_dark));
        commentInput.setPadding(24, 20, 24, 20);

        layout.addView(lblStars);
        layout.addView(starRow);
        layout.addView(commentInput);

        boolean alreadyReviewed = db.hasUserReviewed(listingId, userEmail);
        String title = alreadyReviewed ? "Update your review" : "Write a review";

        new AlertDialog.Builder(this)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("Submit", (d, w) -> {
                String comment = commentInput.getText().toString().trim();
                db.addReview(listingId, userEmail, selectedRating[0], comment);
                StyledAlert.show(this, StyledAlert.SUCCESS, "Review Submitted",
                    "Thank you for sharing your experience!");
                loadReviews();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void loadReviews() {
        reviewsContainer.removeAllViews();

        float avg = db.getAverageRating(listingId);
        int count = db.getReviewCount(listingId);
        TextView avgTv = findViewById(R.id.detail_avg_rating);
        if (count > 0) {
            avgTv.setText(String.format(Locale.getDefault(), "★ %.1f  ·  %d review%s", avg, count, count > 1 ? "s" : ""));
        } else {
            avgTv.setText("★ No reviews yet");
        }

        // Update write-review button label if already reviewed
        TextView btnWriteReview = findViewById(R.id.btn_write_review);
        if (!isHost && db.hasCompletedStay(listingId, userEmail)) {
            btnWriteReview.setVisibility(View.VISIBLE);
            btnWriteReview.setText(db.hasUserReviewed(listingId, userEmail) ? "Edit review" : "Write a review");
        }

        Cursor cursor = db.getReviewsForListing(listingId);
        if (cursor == null || cursor.getCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No reviews yet. Be the first to review!");
            empty.setTextSize(13);
            empty.setTextColor(0xFF717171);
            empty.setPadding(0, 8, 0, 16);
            reviewsContainer.addView(empty);
            return;
        }

        while (cursor.moveToNext()) {
            String reviewerName = cursor.getString(cursor.getColumnIndex("reviewer_name"));
            int rating = cursor.getInt(cursor.getColumnIndex("rating"));
            String comment = cursor.getString(cursor.getColumnIndex("comment"));
            String createdAt = cursor.getString(cursor.getColumnIndex("created_at"));

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, 0, 0, dp(12));
            card.setLayoutParams(cardLp);
            card.setBackgroundColor(0xFFF7F7F7);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));

            // Reviewer name + date row
            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView nameTv = new TextView(this);
            nameTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            nameTv.setText(reviewerName != null ? reviewerName : "Guest");
            nameTv.setTextSize(13);
            nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
            nameTv.setTextColor(0xFF222222);

            TextView dateTv = new TextView(this);
            dateTv.setText(createdAt != null ? createdAt : "");
            dateTv.setTextSize(11);
            dateTv.setTextColor(0xFFAAAAAA);

            topRow.addView(nameTv);
            topRow.addView(dateTv);

            // Stars
            StringBuilder starStr = new StringBuilder();
            for (int i = 0; i < 5; i++) starStr.append(i < rating ? "★" : "☆");
            TextView starTv = new TextView(this);
            starTv.setText(starStr.toString());
            starTv.setTextSize(14);
            starTv.setTextColor(0xFFFF9800);
            starTv.setPadding(0, dp(4), 0, dp(4));

            card.addView(topRow);
            card.addView(starTv);

            // Comment
            if (comment != null && !comment.isEmpty()) {
                TextView commentTv = new TextView(this);
                commentTv.setText(comment);
                commentTv.setTextSize(13);
                commentTv.setTextColor(0xFF484848);
                commentTv.setLineSpacing(0, 1.3f);
                card.addView(commentTv);
            }

            reviewsContainer.addView(card);
        }
        cursor.close();
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }
}
