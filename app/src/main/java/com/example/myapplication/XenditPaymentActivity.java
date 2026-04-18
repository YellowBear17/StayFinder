package com.example.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class XenditPaymentActivity extends Activity {

    // ⚠️ Development keys — replace with live keys for production
    private static final String SECRET_KEY = "xnd_development_vTp96Sg8STaKgb5MlDyTYkhcuYIWiocX7EC9lMQHAESweoQWs2JaDq1UEK5kyZ3";
    private static final String XENDIT_INVOICE_URL = "https://api.xendit.co/v2/invoices";

    private static final String SUCCESS_URL = "https://stayfinder.app/payment/success";
    private static final String FAILURE_URL = "https://stayfinder.app/payment/failure";

    DatabaseHelper db;
    int bookingId;
    double depositAmount;
    String userEmail;
    String listingTitle;

    WebView webView;
    LinearLayout loadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(this);
        bookingId     = getIntent().getIntExtra("BOOKING_ID", -1);
        depositAmount = getIntent().getDoubleExtra("DEPOSIT_AMOUNT", 0);
        userEmail     = getIntent().getStringExtra("USER_EMAIL");
        listingTitle  = getIntent().getStringExtra("LISTING_TITLE");
        if (listingTitle == null) listingTitle = "Booking";

        buildUI();
        createXenditInvoice();
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);
        root.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT));

        // Toolbar
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(0xFFFFFFFF);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(12), dp(12), dp(16), dp(12));
        toolbar.setElevation(dp(4));
        toolbar.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView btnBack = new TextView(this);
        btnBack.setText("←");
        btnBack.setTextSize(22);
        btnBack.setTextColor(0xFF222222);
        btnBack.setPadding(0, 0, dp(12), 0);
        btnBack.setClickable(true);
        btnBack.setFocusable(true);
        btnBack.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        TextView titleTv = new TextView(this);
        titleTv.setText("Secure Payment");
        titleTv.setTextSize(16);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTv.setTextColor(0xFF222222);

        toolbar.addView(btnBack);
        toolbar.addView(titleTv);

        // Loading state
        loadingView = new LinearLayout(this);
        loadingView.setOrientation(LinearLayout.VERTICAL);
        loadingView.setGravity(Gravity.CENTER);
        loadingView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        ProgressBar spinner = new ProgressBar(this);
        spinner.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView loadingTv = new TextView(this);
        loadingTv.setText("Preparing your payment...");
        loadingTv.setTextSize(14);
        loadingTv.setTextColor(0xFF717171);
        loadingTv.setPadding(0, dp(16), 0, 0);
        loadingTv.setGravity(Gravity.CENTER);

        String depositStr = String.format(java.util.Locale.getDefault(), "₱%.2f", depositAmount);
        TextView amountTv = new TextView(this);
        amountTv.setText("50% deposit: " + depositStr);
        amountTv.setTextSize(16);
        amountTv.setTypeface(null, android.graphics.Typeface.BOLD);
        amountTv.setTextColor(0xFFFF385C);
        amountTv.setPadding(0, dp(8), 0, 0);
        amountTv.setGravity(Gravity.CENTER);

        loadingView.addView(spinner);
        loadingView.addView(loadingTv);
        loadingView.addView(amountTv);

        // WebView
        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        webView.setVisibility(View.GONE);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith(SUCCESS_URL)) {
                    handlePaymentSuccess();
                    return true;
                }
                if (url.startsWith(FAILURE_URL)) {
                    handlePaymentFailure();
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                loadingView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }
        });

        root.addView(toolbar);
        root.addView(loadingView);
        root.addView(webView);
        setContentView(root);
    }

    private void createXenditInvoice() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // Build invoice payload
                JSONObject payload = new JSONObject();
                payload.put("external_id", "stayfinder_booking_" + bookingId + "_" + System.currentTimeMillis());
                payload.put("amount", depositAmount);
                payload.put("payer_email", userEmail);
                payload.put("description", "50% deposit for: " + listingTitle);
                payload.put("success_redirect_url", SUCCESS_URL);
                payload.put("failure_redirect_url", FAILURE_URL);
                payload.put("currency", "PHP");

                // HTTP POST to Xendit
                URL url = new URL(XENDIT_INVOICE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization",
                    "Basic " + android.util.Base64.encodeToString(
                        (SECRET_KEY + ":").getBytes(), android.util.Base64.NO_WRAP));
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    responseCode == 200 || responseCode == 201
                        ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject response = new JSONObject(sb.toString());

                if (response.has("invoice_url")) {
                    String invoiceUrl = response.getString("invoice_url");
                    handler.post(() -> webView.loadUrl(invoiceUrl));
                } else {
                    String errMsg = response.optString("message", "Unknown error");
                    handler.post(() -> showError(errMsg));
                }

            } catch (Exception e) {
                handler.post(() -> showError(e.getMessage()));
            }
        });
    }

    private void handlePaymentSuccess() {
        db.markBookingPaid(bookingId, "xendit_paid_" + bookingId, depositAmount);

        String listingTitleStr = db.getListingTitleFromBooking(bookingId);
        String depositStr = String.format(java.util.Locale.getDefault(), "%.2f", depositAmount);

        // Notify host
        String hostEmail = db.getHostEmailForListing(getBookingListingId());
        if (hostEmail != null) {
            db.addNotification(hostEmail,
                "💳 Deposit Paid — Booking Confirmed",
                "The guest has paid the 50% deposit of ₱" + depositStr
                + " for \"" + (listingTitleStr != null ? listingTitleStr : "a listing") + "\". The booking is now fully confirmed.",
                "payment", bookingId);
        }

        // Notify guest
        db.addNotification(userEmail,
            "✅ Payment Successful!",
            "Your 50% deposit of ₱" + depositStr
            + " for \"" + (listingTitleStr != null ? listingTitleStr : "your booking") + "\" has been received. Your reservation is confirmed!",
            "payment_success", bookingId);

        StyledAlert.show(this, StyledAlert.SUCCESS, "Payment Successful! 🎉",
            "Your 50% deposit has been paid. Your reservation is now fully confirmed!");
        new android.os.Handler().postDelayed(() -> {
            setResult(RESULT_OK);
            finish();
        }, 2000);
    }

    private void handlePaymentFailure() {
        StyledAlert.show(this, StyledAlert.ERROR, "Payment Failed",
            "Your payment could not be processed. Please try again.");
        new android.os.Handler().postDelayed(() -> {
            setResult(RESULT_CANCELED);
            finish();
        }, 2000);
    }

    private void showError(String message) {
        loadingView.setVisibility(View.GONE);
        StyledAlert.show(this, StyledAlert.ERROR, "Payment Error",
            "Could not create payment: " + message);
    }

    private int getBookingListingId() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        android.database.Cursor c = dbHelper.getReadableDatabase().rawQuery(
            "SELECT listing_id FROM bookings WHERE id = ?",
            new String[]{String.valueOf(bookingId)});
        int id = -1;
        if (c.moveToFirst()) id = c.getInt(0);
        c.close();
        return id;
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }
}
