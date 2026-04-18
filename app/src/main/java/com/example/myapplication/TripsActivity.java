package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TripsActivity extends Activity {

    DatabaseHelper db;
    String userEmail;
    LinearLayout tripsContainer;
    boolean showingUpcoming = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trips);

        db = new DatabaseHelper(this);
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        tripsContainer = findViewById(R.id.trips_container);

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                finish();
                overridePendingTransition(0, 0);
            }
        });

        TextView tabUpcoming = findViewById(R.id.tab_upcoming);
        TextView tabPast = findViewById(R.id.tab_past);

        tabUpcoming.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                showingUpcoming = true;
                tabUpcoming.setTextColor(0xFFFF385C);
                tabUpcoming.setTextSize(14);
                tabUpcoming.setTypeface(null, android.graphics.Typeface.BOLD);
                tabUpcoming.setBackgroundResource(R.drawable.tab_selected_bg);
                tabPast.setTextColor(0xFF717171);
                tabPast.setTypeface(null, android.graphics.Typeface.NORMAL);
                tabPast.setBackgroundResource(R.drawable.tab_unselected_bg);
                loadTrips();
            }
        });

        tabPast.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                showingUpcoming = false;
                tabPast.setTextColor(0xFFFF385C);
                tabPast.setTypeface(null, android.graphics.Typeface.BOLD);
                tabPast.setBackgroundResource(R.drawable.tab_selected_bg);
                tabUpcoming.setTextColor(0xFF717171);
                tabUpcoming.setTypeface(null, android.graphics.Typeface.NORMAL);
                tabUpcoming.setBackgroundResource(R.drawable.tab_unselected_bg);
                loadTrips();
            }
        });

        loadTrips();

        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        String userRole = new DatabaseHelper(this).getUserRole(userEmail);
        BottomNavHelper.setup(this, userEmail, userRole, "trips");
    }

    private void loadTrips() {
        tripsContainer.removeAllViews();

        // Auto-expire unpaid bookings before loading
        db.autoExpireUnpaidBookings();

        Cursor cursor = db.getBookingsByUser(userEmail);
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        boolean hasItems = false;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String checkIn = cursor.getString(cursor.getColumnIndex("check_in"));
                String status = cursor.getString(cursor.getColumnIndex("status"));
                boolean isUpcoming = checkIn.compareTo(today) >= 0
                        && !status.equals("cancelled") && !status.equals("declined");

                if (showingUpcoming && !isUpcoming && !status.equals("pending") && !status.equals("confirmed")) continue;
                if (!showingUpcoming && (isUpcoming || status.equals("pending"))) continue;

                hasItems = true;
                final int bookingId = cursor.getInt(cursor.getColumnIndex("id"));
                String title = cursor.getString(cursor.getColumnIndex("title"));
                String location = cursor.getString(cursor.getColumnIndex("location"));
                String checkOut = cursor.getString(cursor.getColumnIndex("check_out"));
                double total = cursor.getDouble(cursor.getColumnIndex("total_price"));
                String imageUriStr = cursor.getString(cursor.getColumnIndex("image_uri"));

                View card = getLayoutInflater().inflate(R.layout.item_trip, tripsContainer, false);

                // Image
                ImageView tripImage = card.findViewById(R.id.trip_image);
                LinearLayout placeholder = card.findViewById(R.id.trip_image_placeholder);
                if (imageUriStr != null && !imageUriStr.isEmpty()) {
                    try {
                        Uri parsedUri = Uri.parse(imageUriStr.split(",")[0].trim());
                        getContentResolver().openInputStream(parsedUri).close();
                        tripImage.setImageURI(parsedUri);
                        tripImage.setVisibility(View.VISIBLE);
                        placeholder.setVisibility(View.GONE);
                    } catch (Exception e) {
                        tripImage.setVisibility(View.GONE);
                        placeholder.setVisibility(View.VISIBLE);
                    }
                } else {
                    tripImage.setVisibility(View.GONE);
                    placeholder.setVisibility(View.VISIBLE);
                }

                // Payment info
                String paymentStatus = db.getPaymentStatus(bookingId);
                long paymentDeadline = db.getPaymentDeadline(bookingId);
                long now = System.currentTimeMillis();
                boolean needsPayment = "confirmed".equals(status)
                    && "unpaid".equals(paymentStatus)
                    && paymentDeadline > 0;

                // Status badge
                TextView badge = card.findViewById(R.id.trip_status_badge);
                if (status.equals("cancelled")) {
                    badge.setText("Cancelled");
                    badge.setBackgroundResource(R.drawable.status_badge_cancelled);
                } else if (status.equals("declined")) {
                    badge.setText("Declined");
                    badge.setBackgroundResource(R.drawable.status_badge_cancelled);
                } else if (status.equals("pending")) {
                    badge.setText("⏳ Pending approval");
                    badge.setBackgroundResource(R.drawable.status_badge_past);
                } else if (needsPayment) {
                    badge.setText("💳 Payment Required");
                    badge.setBackgroundResource(R.drawable.status_badge_cancelled);
                } else if ("paid".equals(paymentStatus) && isUpcoming) {
                    badge.setText("✅ Confirmed & Paid");
                    badge.setBackgroundResource(R.drawable.status_badge_confirmed);
                } else if (isUpcoming) {
                    badge.setText("Confirmed");
                    badge.setBackgroundResource(R.drawable.status_badge_confirmed);
                } else {
                    badge.setText("Completed");
                    badge.setBackgroundResource(R.drawable.status_badge_past);
                }

                ((TextView) card.findViewById(R.id.trip_title)).setText(title != null ? title : "Listing");
                ((TextView) card.findViewById(R.id.trip_location)).setText("📍 " + (location != null ? location : ""));
                ((TextView) card.findViewById(R.id.trip_checkin)).setText(formatDate(checkIn));
                ((TextView) card.findViewById(R.id.trip_checkout)).setText(formatDate(checkOut));

                String totalStr = (total == Math.floor(total)) ? String.valueOf((int) total) : String.valueOf(total);
                ((TextView) card.findViewById(R.id.trip_total)).setText("₱" + totalStr);

                // Pay Now button for approved+unpaid bookings within deadline
                if (needsPayment && paymentDeadline > now) {
                    long hoursLeft = (paymentDeadline - now) / (1000 * 60 * 60);
                    long minsLeft  = ((paymentDeadline - now) % (1000 * 60 * 60)) / (1000 * 60);

                    // Countdown label
                    TextView countdownTv = new TextView(this);
                    LinearLayout.LayoutParams cdLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    cdLp.setMargins(dp(16), dp(8), dp(16), 0);
                    countdownTv.setLayoutParams(cdLp);
                    countdownTv.setText("⏰ Pay within: " + hoursLeft + "h " + minsLeft + "m remaining");
                    countdownTv.setTextSize(12);
                    countdownTv.setTextColor(0xFFE65100);
                    countdownTv.setTypeface(null, android.graphics.Typeface.BOLD);

                    // Pay Now button
                    TextView btnPay = new TextView(this);
                    LinearLayout.LayoutParams payLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
                    payLp.setMargins(dp(16), dp(6), dp(16), dp(12));
                    btnPay.setLayoutParams(payLp);
                    double deposit = Math.ceil(total * 0.5);
                    btnPay.setText("💳  Pay 50% Deposit  ·  ₱" + formatAmount(deposit));
                    btnPay.setTextSize(13);
                    btnPay.setTypeface(null, android.graphics.Typeface.BOLD);
                    btnPay.setTextColor(0xFFFFFFFF);
                    btnPay.setBackgroundColor(0xFFFF385C);
                    btnPay.setGravity(android.view.Gravity.CENTER);
                    btnPay.setClickable(true);
                    btnPay.setFocusable(true);
                    final String tripTitle = title;
                    btnPay.setOnClickListener(v -> {
                        Intent payIntent = new Intent(TripsActivity.this, XenditPaymentActivity.class);
                        payIntent.putExtra("BOOKING_ID", bookingId);
                        payIntent.putExtra("DEPOSIT_AMOUNT", deposit);
                        payIntent.putExtra("USER_EMAIL", userEmail);
                        payIntent.putExtra("LISTING_TITLE", tripTitle != null ? tripTitle : "Booking");
                        startActivityForResult(payIntent, 3001);
                    });

                    if (card instanceof LinearLayout) {
                        ((LinearLayout) card).addView(countdownTv);
                        ((LinearLayout) card).addView(btnPay);
                    }
                }

                // Show cancel button only for upcoming confirmed+paid or pending
                TextView btnCancel = card.findViewById(R.id.btn_cancel_booking);
                if ((isUpcoming || status.equals("pending")) && !status.equals("cancelled")) {
                    btnCancel.setVisibility(View.VISIBLE);
                    btnCancel.setOnClickListener(v ->
                        StyledAlert.confirm(TripsActivity.this,
                            "Cancel Reservation",
                            "Are you sure you want to cancel this booking? This cannot be undone.",
                            "Yes, cancel", "Keep it",
                            () -> {
                                db.cancelBooking(bookingId);
                                StyledAlert.show(TripsActivity.this, StyledAlert.SUCCESS,
                                    "Booking Cancelled", "Your reservation has been cancelled.");
                                loadTrips();
                            }));
                }

                tripsContainer.addView(card);

            } while (cursor.moveToNext());
            cursor.close();
        }

        if (!hasItems) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 500);
            empty.setLayoutParams(lp);

            TextView emoji = new TextView(this);
            emoji.setText(showingUpcoming ? "🧳" : "📋");
            emoji.setTextSize(48);
            emoji.setGravity(android.view.Gravity.CENTER);

            TextView msg = new TextView(this);
            msg.setText(showingUpcoming ? "No upcoming trips" : "No past trips");
            msg.setTextSize(16);
            msg.setTextColor(0xFF222222);
            msg.setTypeface(null, android.graphics.Typeface.BOLD);
            msg.setGravity(android.view.Gravity.CENTER);
            msg.setPadding(0, 16, 0, 0);

            TextView sub = new TextView(this);
            sub.setText(showingUpcoming ? "Time to plan your next adventure!" : "Your completed trips will appear here.");
            sub.setTextSize(13);
            sub.setTextColor(0xFF717171);
            sub.setGravity(android.view.Gravity.CENTER);
            sub.setPadding(32, 8, 32, 0);

            empty.addView(emoji);
            empty.addView(msg);
            empty.addView(sub);
            tripsContainer.addView(empty);
        }
    }

    private String formatDate(String dateStr) {
        try {
            java.text.SimpleDateFormat inFmt = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.text.SimpleDateFormat outFmt = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
            return outFmt.format(inFmt.parse(dateStr));
        } catch (Exception e) {
            return dateStr;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3001) {
            loadTrips(); // Refresh after payment attempt
        }
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    private String formatAmount(double amount) {
        return (amount == Math.floor(amount))
            ? String.valueOf((int) amount)
            : String.format(java.util.Locale.getDefault(), "%.2f", amount);
    }
}
