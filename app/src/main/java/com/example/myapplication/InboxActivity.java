package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.drawerlayout.widget.DrawerLayout;

public class InboxActivity extends Activity {

    DatabaseHelper db;
    String userEmail;
    String userRole;
    LinearLayout inboxContainer;
    DrawerLayout drawerLayout;
    String activeFilter = "all"; // all | booking | payment | messages

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        db = new DatabaseHelper(this);
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userRole = getIntent().getStringExtra("USER_ROLE");
        if (userRole == null) userRole = db.getUserRole(userEmail);
        inboxContainer = findViewById(R.id.inbox_container);
        drawerLayout = findViewById(R.id.inbox_drawer_layout);

        boolean showPendingOnly = getIntent().getBooleanExtra("SHOW_PENDING", false);
        if (showPendingOnly) {
            TextView titleTv = findViewById(R.id.inbox_title);
            if (titleTv != null) titleTv.setText("Pending Requests");
            activeFilter = "booking";
        }

        // InboxActivity back
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        // Show hamburger for ALL users
        TextView btnMenu = findViewById(R.id.btn_inbox_menu);
        btnMenu.setVisibility(View.VISIBLE);
        btnMenu.setOnClickListener(v ->
            drawerLayout.openDrawer(findViewById(R.id.inbox_nav_drawer)));

        boolean isHost = "host".equals(userRole);

        // Show/hide role-specific nav items
        // Host items: All, Booking Requests, Payments, Messages
        // Guest items: All, Notifications, Messages, Booking Updates
        findViewById(R.id.nav_inbox_booking).setVisibility(isHost ? View.VISIBLE : View.GONE);
        findViewById(R.id.nav_inbox_payment).setVisibility(isHost ? View.VISIBLE : View.GONE);
        // Guest-only
        findViewById(R.id.nav_inbox_notifications).setVisibility(isHost ? View.GONE : View.VISIBLE);
        findViewById(R.id.nav_inbox_booking_updates).setVisibility(isHost ? View.GONE : View.VISIBLE);
        findViewById(R.id.divider_guest_booking).setVisibility(isHost ? View.GONE : View.VISIBLE);

        // Sidebar nav clicks — host
        setupNavItem(R.id.nav_inbox_all,      "all",      "Inbox");
        setupNavItem(R.id.nav_inbox_booking,  "booking",  "Booking Requests");
        setupNavItem(R.id.nav_inbox_payment,  "payment",  "Payments");
        setupNavItem(R.id.nav_inbox_messages, "messages", "Messages");
        // Sidebar nav clicks — guest
        setupNavItem(R.id.nav_inbox_notifications,    "notifications",    "Notifications");
        setupNavItem(R.id.nav_inbox_booking_updates,  "booking_updates",  "Booking Updates");

        // Persistent bottom nav
        BottomNavHelper.setup(this, userEmail, userRole, "inbox");
    }

    private void setupNavItem(int viewId, String filter, String title) {
        LinearLayout item = findViewById(viewId);
        if (item == null) return;
        item.setOnClickListener(v -> {
            activeFilter = filter;
            drawerLayout.closeDrawers();
            TextView titleTv = findViewById(R.id.inbox_title);
            if (titleTv != null) titleTv.setText(title);
            highlightNavItem(viewId);
            if ("host".equals(userRole)) {
                loadHostInboxFiltered();
            } else {
                loadGuestInboxFiltered();
            }
        });
    }

    private void highlightNavItem(int selectedId) {
        int[] ids = {R.id.nav_inbox_all, R.id.nav_inbox_booking,
                     R.id.nav_inbox_payment, R.id.nav_inbox_messages,
                     R.id.nav_inbox_notifications, R.id.nav_inbox_booking_updates};
        for (int id : ids) {
            LinearLayout item = findViewById(id);
            if (item == null) continue;
            boolean selected = id == selectedId;
            item.setBackgroundResource(selected
                ? R.drawable.nav_item_selected
                : android.R.color.transparent);
            // Update label color
            if (item.getChildCount() >= 2 && item.getChildAt(1) instanceof TextView) {
                ((TextView) item.getChildAt(1)).setTextColor(
                    selected ? 0xFFFF385C : 0xFF222222);
                ((TextView) item.getChildAt(1)).setTypeface(null,
                    selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        db.markAllMessagesRead(userEmail);

        boolean showPendingOnly = getIntent().getBooleanExtra("SHOW_PENDING", false);
        if ("host".equals(userRole)) {
            updateDrawerBadges();
            if (showPendingOnly && activeFilter.equals("all")) activeFilter = "booking";
            loadHostInboxFiltered();
        } else {
            updateGuestDrawerBadges();
            loadGuestInboxFiltered();
        }
    }

    private void updateDrawerBadges() {
        // Booking badge — pending count
        int pending = db.getPendingCountForHost(userEmail);
        setBadge(R.id.badge_booking, pending);
        setBadge(R.id.badge_all, pending); // show total pending on "All"

        // Payment badge — unread payment notifs
        Cursor p = db.getNotificationsByType(userEmail, "payment");
        int unreadPay = 0;
        if (p != null) {
            while (p.moveToNext()) if (p.getInt(p.getColumnIndex("is_read")) == 0) unreadPay++;
            p.close();
        }
        setBadge(R.id.badge_payment, unreadPay);

        // Messages badge — unread messages
        int unreadMsg = db.getUnreadMessageCount(userEmail);
        setBadge(R.id.badge_messages, unreadMsg);
    }

    private void setBadge(int viewId, int count) {
        TextView badge = findViewById(viewId);
        if (badge == null) return;
        if (count > 0) {
            badge.setText(String.valueOf(count));
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private void loadHostInboxFiltered() {
        switch (activeFilter) {
            case "booking":  loadHostBookingSection(); break;
            case "payment":  loadHostPaymentSection(); break;
            case "messages": loadHostMessagesSection(); break;
            default:         loadHostInbox(); break;
        }
    }

    private void loadHostBookingSection() {
        inboxContainer.removeAllViews();
        Cursor allBookings = db.getAllBookingsForHost(userEmail);
        int total = allBookings != null ? allBookings.getCount() : 0;
        addCategoryHeader("📋  Booking Requests", total, 0xFF3498DB);
        if (total == 0) { addInlinePlaceholder("No booking requests yet"); return; }

        Cursor pending = db.getPendingBookingsForHost(userEmail);
        int pendingCount = pending != null ? pending.getCount() : 0;
        if (pendingCount > 0) {
            addSectionHeader("🔔 Pending Approval (" + pendingCount + ")");
            if (pending.moveToFirst()) do { addHostBookingCard(pending, true); } while (pending.moveToNext());
        }
        if (pending != null) pending.close();

        if (total - pendingCount > 0) {
            addSectionHeader("📄 All Requests");
            if (allBookings != null && allBookings.moveToFirst()) {
                do {
                    if (!"pending".equals(allBookings.getString(allBookings.getColumnIndex("status"))))
                        addHostBookingCard(allBookings, false);
                } while (allBookings.moveToNext());
            }
        }
        if (allBookings != null) allBookings.close();
    }

    private void loadHostPaymentSection() {
        inboxContainer.removeAllViews();
        Cursor payNotifs = db.getNotificationsByType(userEmail, "payment");
        int count = payNotifs != null ? payNotifs.getCount() : 0;
        addCategoryHeader("💳  Payments", count, 0xFF00A699);
        if (count == 0) { addInlinePlaceholder("No payment notifications yet"); return; }
        payNotifs.moveToFirst();
        do {
            addHostPaymentCard(
                payNotifs.getString(payNotifs.getColumnIndex("title")),
                payNotifs.getString(payNotifs.getColumnIndex("message")),
                payNotifs.getInt(payNotifs.getColumnIndex("is_read")) == 0,
                payNotifs.getString(payNotifs.getColumnIndex("created_at")),
                payNotifs.getInt(payNotifs.getColumnIndex("booking_id")));
        } while (payNotifs.moveToNext());
        payNotifs.close();
    }

    private void loadHostMessagesSection() {
        inboxContainer.removeAllViews();
        Cursor convCursor = db.getConversationsForHost(userEmail);
        int count = convCursor != null ? convCursor.getCount() : 0;
        addCategoryHeader("💬  Messages", count, 0xFFFF385C);
        if (count == 0) { addInlinePlaceholder("No messages yet"); return; }
        convCursor.moveToFirst();
        do {
            addConversationCard(
                convCursor.getInt(convCursor.getColumnIndex("listing_id")),
                convCursor.getString(convCursor.getColumnIndex("guest_email")),
                convCursor.getString(convCursor.getColumnIndex("guest_name")),
                convCursor.getString(convCursor.getColumnIndex("listing_title")),
                convCursor.getString(convCursor.getColumnIndex("last_message")),
                convCursor.getInt(convCursor.getColumnIndex("unread_count")));
        } while (convCursor.moveToNext());
        convCursor.close();
    }

    @Override
    protected void onPause() {
        super.onPause();
        db.markAllNotificationsRead(userEmail);
    }

    // ---- PENDING ONLY view (from dashboard card tap) ----
    private void loadPendingOnly() {
        inboxContainer.removeAllViews();
        Cursor cursor = db.getPendingBookingsForHost(userEmail);
        if (cursor == null || cursor.getCount() == 0) {
            addEmptyState("✅", "No pending requests", "All booking requests have been handled.");
            return;
        }
        addSectionHeader("🔔 Pending Approval (" + cursor.getCount() + ")");
        cursor.moveToFirst();
        do { addHostBookingCard(cursor, true); } while (cursor.moveToNext());
        cursor.close();
    }

    // ---- HOST INBOX: shows all booking requests with approve/decline ----
    private void loadHostInbox() {
        inboxContainer.removeAllViews();

        boolean hasAnything = false;

        // ── 1. BOOKING REQUESTS ──────────────────────────────────────────
        Cursor allBookings = db.getAllBookingsForHost(userEmail);
        int totalBookings = allBookings != null ? allBookings.getCount() : 0;

        Cursor pendingCursor = db.getPendingBookingsForHost(userEmail);
        int pendingCount = pendingCursor != null ? pendingCursor.getCount() : 0;

        addCategoryHeader("📋  Booking Requests", totalBookings, 0xFF3498DB);

        if (totalBookings == 0) {
            addInlinePlaceholder("No booking requests yet");
        } else {
            hasAnything = true;
            if (pendingCount > 0) {
                addSectionHeader("🔔 Pending Approval (" + pendingCount + ")");
                if (pendingCursor != null && pendingCursor.moveToFirst()) {
                    do { addHostBookingCard(pendingCursor, true); } while (pendingCursor.moveToNext());
                }
            }
            if (pendingCursor != null) pendingCursor.close();

            int confirmedOrOther = totalBookings - pendingCount;
            if (confirmedOrOther > 0) {
                addSectionHeader("📄 All Requests");
                if (allBookings != null && allBookings.moveToFirst()) {
                    do {
                        String status = allBookings.getString(allBookings.getColumnIndex("status"));
                        if (!status.equals("pending")) {
                            addHostBookingCard(allBookings, false);
                        }
                    } while (allBookings.moveToNext());
                }
            }
        }
        if (allBookings != null) allBookings.close();

        addSpacing();

        // ── 2. PAYMENT NOTIFICATIONS ─────────────────────────────────────
        Cursor payNotifs = db.getNotificationsByType(userEmail, "payment");
        int payCount = payNotifs != null ? payNotifs.getCount() : 0;
        int unreadPay = 0;
        if (payNotifs != null && payNotifs.moveToFirst()) {
            do {
                if (payNotifs.getInt(payNotifs.getColumnIndex("is_read")) == 0) unreadPay++;
            } while (payNotifs.moveToNext());
            payNotifs.moveToFirst();
        }

        addCategoryHeader("💳  Payment Notifications", payCount, 0xFF00A699);

        if (payCount == 0) {
            addInlinePlaceholder("No payment notifications yet");
        } else {
            hasAnything = true;
            payNotifs.moveToFirst();
            do {
                String notifTitle = payNotifs.getString(payNotifs.getColumnIndex("title"));
                String notifMsg   = payNotifs.getString(payNotifs.getColumnIndex("message"));
                int isRead        = payNotifs.getInt(payNotifs.getColumnIndex("is_read"));
                String createdAt  = payNotifs.getString(payNotifs.getColumnIndex("created_at"));
                int bookingId     = payNotifs.getInt(payNotifs.getColumnIndex("booking_id"));
                addHostPaymentCard(notifTitle, notifMsg, isRead == 0, createdAt, bookingId);
            } while (payNotifs.moveToNext());
        }
        if (payNotifs != null) payNotifs.close();

        addSpacing();

        // ── 3. MESSAGES ───────────────────────────────────────────────────
        Cursor convCursor = db.getConversationsForHost(userEmail);
        int convCount = convCursor != null ? convCursor.getCount() : 0;
        int unreadMsgs = 0;
        if (convCursor != null && convCursor.moveToFirst()) {
            do {
                unreadMsgs += convCursor.getInt(convCursor.getColumnIndex("unread_count"));
            } while (convCursor.moveToNext());
            convCursor.moveToFirst();
        }

        addCategoryHeader("💬  Messages", convCount, 0xFFFF385C);

        if (convCount == 0) {
            addInlinePlaceholder("No messages yet");
        } else {
            hasAnything = true;
            convCursor.moveToFirst();
            do {
                int listingId     = convCursor.getInt(convCursor.getColumnIndex("listing_id"));
                String guestEmail = convCursor.getString(convCursor.getColumnIndex("guest_email"));
                String guestName  = convCursor.getString(convCursor.getColumnIndex("guest_name"));
                String listTitle  = convCursor.getString(convCursor.getColumnIndex("listing_title"));
                String lastMsg    = convCursor.getString(convCursor.getColumnIndex("last_message"));
                int unread        = convCursor.getInt(convCursor.getColumnIndex("unread_count"));
                addConversationCard(listingId, guestEmail, guestName, listTitle, lastMsg, unread);
            } while (convCursor.moveToNext());
        }
        if (convCursor != null) convCursor.close();

        if (!hasAnything) {
            inboxContainer.removeAllViews();
            addEmptyState("📭", "Your inbox is empty", "Booking requests, payments and messages will appear here");
        }
    }

    private void addCategoryHeader(String title, int count, int accentColor) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, 0);
        header.setLayoutParams(lp);
        header.setBackgroundColor(0xFFFFFFFF);
        header.setPadding(dp(16), dp(14), dp(16), dp(14));

        // Accent bar
        android.view.View bar = new android.view.View(this);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(4),
            LinearLayout.LayoutParams.MATCH_PARENT);
        barLp.setMargins(0, 0, dp(12), 0);
        bar.setLayoutParams(barLp);
        bar.setBackgroundColor(accentColor);

        TextView titleTv = new TextView(this);
        titleTv.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        titleTv.setText(title);
        titleTv.setTextSize(15);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTv.setTextColor(0xFF222222);

        if (count > 0) {
            TextView countTv = new TextView(this);
            countTv.setText(String.valueOf(count));
            countTv.setTextSize(12);
            countTv.setTypeface(null, android.graphics.Typeface.BOLD);
            countTv.setTextColor(0xFFFFFFFF);
            countTv.setBackgroundColor(accentColor);
            countTv.setPadding(dp(8), dp(3), dp(8), dp(3));
            header.addView(bar);
            header.addView(titleTv);
            header.addView(countTv);
        } else {
            header.addView(bar);
            header.addView(titleTv);
        }

        // Bottom border
        android.view.View border = new android.view.View(this);
        border.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(2)));
        border.setBackgroundColor(accentColor);

        inboxContainer.addView(header);
        inboxContainer.addView(border);
    }

    private void addInlinePlaceholder(String text) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 0);
        tv.setLayoutParams(lp);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(0xFFAAAAAA);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(dp(16), dp(16), dp(16), dp(16));
        tv.setBackgroundColor(0xFFFAFAFA);
        inboxContainer.addView(tv);
    }

    private void addSpacing() {
        android.view.View space = new android.view.View(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(12)));
        space.setBackgroundColor(0xFFF0F0F0);
        inboxContainer.addView(space);
    }

    private void addConversationCard(int listingId, String guestEmail, String guestName,
                                     String listingTitle, String lastMsg, int unread) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundColor(unread > 0 ? 0xFFFFF8E1 : 0xFFFFFFFF);
        int p = dp(16);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(1));
        card.setLayoutParams(cardLp);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(InboxActivity.this, ChatActivity.class);
            intent.putExtra("USER_EMAIL", userEmail);
            intent.putExtra("OTHER_EMAIL", guestEmail);
            intent.putExtra("LISTING_ID", listingId);
            intent.putExtra("LISTING_TITLE", listingTitle != null ? listingTitle : "Listing");
            startActivity(intent);
        });

        // Avatar
        LinearLayout avatar = new LinearLayout(this);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        android.graphics.drawable.GradientDrawable avBg = new android.graphics.drawable.GradientDrawable();
        avBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        avBg.setColor(0xFFFF385C);
        avatar.setBackground(avBg);
        avatar.setGravity(android.view.Gravity.CENTER);
        TextView avatarTv = new TextView(this);
        String initial = (guestName != null && !guestName.isEmpty())
            ? String.valueOf(guestName.charAt(0)).toUpperCase() : "G";
        avatarTv.setText(initial);
        avatarTv.setTextSize(18);
        avatarTv.setTextColor(0xFFFFFFFF);
        avatarTv.setTypeface(null, android.graphics.Typeface.BOLD);
        avatar.addView(avatarTv);

        // Content
        LinearLayout content = new LinearLayout(this);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        cLp.setMargins(dp(12), 0, 0, 0);
        content.setLayoutParams(cLp);
        content.setOrientation(LinearLayout.VERTICAL);

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView nameTv = new TextView(this);
        nameTv.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        nameTv.setText(guestName != null ? guestName : guestEmail);
        nameTv.setTextSize(14);
        nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
        nameTv.setTextColor(0xFF222222);

        if (unread > 0) {
            TextView badge = new TextView(this);
            badge.setText(String.valueOf(unread));
            badge.setTextSize(11);
            badge.setTextColor(0xFFFFFFFF);
            badge.setBackgroundColor(0xFFFF385C);
            badge.setPadding(dp(6), dp(2), dp(6), dp(2));
            nameRow.addView(nameTv);
            nameRow.addView(badge);
        } else {
            nameRow.addView(nameTv);
        }

        TextView listingTv = new TextView(this);
        listingTv.setText(listingTitle != null ? listingTitle : "");
        listingTv.setTextSize(12);
        listingTv.setTextColor(0xFF717171);
        listingTv.setPadding(0, dp(2), 0, 0);

        TextView previewTv = new TextView(this);
        previewTv.setText(lastMsg != null ? lastMsg : "");
        previewTv.setTextSize(13);
        previewTv.setTextColor(0xFF484848);
        previewTv.setMaxLines(1);
        previewTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        previewTv.setPadding(0, dp(2), 0, 0);

        content.addView(nameRow);
        content.addView(listingTv);
        content.addView(previewTv);

        card.addView(avatar);
        card.addView(content);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFFF0F0F0);

        inboxContainer.addView(card);
        inboxContainer.addView(divider);
    }

    private void addHostBookingCard(Cursor cursor, boolean showActions) {
        final int bookingId = cursor.getInt(cursor.getColumnIndex("id"));
        String guestName = cursor.getString(cursor.getColumnIndex("guest_name"));
        String title = cursor.getString(cursor.getColumnIndex("title"));
        String checkIn = cursor.getString(cursor.getColumnIndex("check_in"));
        String checkOut = cursor.getString(cursor.getColumnIndex("check_out"));
        String status = cursor.getString(cursor.getColumnIndex("status"));
        double total = cursor.getDouble(cursor.getColumnIndex("total_price"));
        final int listingId = cursor.getInt(cursor.getColumnIndex("listing_id"));

        String totalStr = (total == Math.floor(total)) ? String.valueOf((int) total) : String.valueOf(total);
        String statusLabel;
        int statusColor;
        switch (status) {
            case "confirmed": statusLabel = "✅ Confirmed"; statusColor = 0xFF00A699; break;
            case "declined":  statusLabel = "❌ Declined";  statusColor = 0xFFFF385C; break;
            case "cancelled": statusLabel = "🚫 Cancelled"; statusColor = 0xFF717171; break;
            default:          statusLabel = "⏳ Pending";   statusColor = 0xFFFF9800; break;
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFFFFFFF);
        int p = dp(16);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(1));
        card.setLayoutParams(cardLp);

        // Tap card → open listing detail
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            Intent intent = new Intent(InboxActivity.this, ListingDetailActivity.class);
            intent.putExtra("LISTING_ID", listingId);
            intent.putExtra("USER_EMAIL", userEmail);
            intent.putExtra("USER_ROLE", "host");
            startActivity(intent);
        });

        // Guest name + status row
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

        TextView statusTv = new TextView(this);
        statusTv.setText(statusLabel);
        statusTv.setTextSize(12);
        statusTv.setTextColor(statusColor);

        topRow.addView(guestTv);
        topRow.addView(statusTv);

        // Listing title
        TextView listingTv = new TextView(this);
        listingTv.setText(title != null ? title : "Listing");
        listingTv.setTextSize(13);
        listingTv.setTextColor(0xFF717171);
        listingTv.setPadding(0, dp(4), 0, 0);

        // Dates + total
        TextView datesTv = new TextView(this);
        datesTv.setText(formatDate(checkIn) + " → " + formatDate(checkOut) + "  ·  ₱" + totalStr);
        datesTv.setTextSize(13);
        datesTv.setTextColor(0xFF484848);
        datesTv.setPadding(0, dp(4), 0, 0);

        card.addView(topRow);
        card.addView(listingTv);
        card.addView(datesTv);

        // Approve / Decline buttons for pending
        if (showActions && status.equals("pending")) {

            // View Reservation Details button
            TextView btnViewDetails = new TextView(this);
            LinearLayout.LayoutParams viewLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(40));
            viewLp.setMargins(0, dp(10), 0, dp(8));
            btnViewDetails.setLayoutParams(viewLp);
            btnViewDetails.setText("📋  View Reservation Details");
            btnViewDetails.setTextSize(13);
            btnViewDetails.setTextColor(0xFF222222);
            btnViewDetails.setBackground(getDrawable(R.drawable.rounded_edittext_dark));
            btnViewDetails.setGravity(android.view.Gravity.CENTER);
            btnViewDetails.setClickable(true);
            btnViewDetails.setFocusable(true);
            btnViewDetails.setOnClickListener(v -> showReservationReceipt(bookingId, guestName, title, checkIn, checkOut, total));
            card.addView(btnViewDetails);

            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            btnRowLp.setMargins(0, dp(12), 0, 0);
            btnRow.setLayoutParams(btnRowLp);

            TextView btnApprove = new TextView(this);
            LinearLayout.LayoutParams approveLp = new LinearLayout.LayoutParams(0, dp(40), 1);
            approveLp.setMargins(0, 0, dp(8), 0);
            btnApprove.setLayoutParams(approveLp);
            btnApprove.setText("✓  Approve");
            btnApprove.setTextSize(13);
            btnApprove.setTextColor(0xFFFFFFFF);
            btnApprove.setBackground(getDrawable(R.drawable.status_badge_confirmed));
            btnApprove.setGravity(android.view.Gravity.CENTER);
            btnApprove.setClickable(true);
            btnApprove.setFocusable(true);
            btnApprove.setOnClickListener(v -> {
                StyledAlert.confirm(this, "Approve Booking",
                    "Approve the reservation from " + (guestName != null ? guestName : "this guest") + "?",
                    "Approve", "Cancel", () -> {
                        db.approveBooking(bookingId);
                        db.setPaymentDeadline(bookingId); // 24-hour payment window starts now
                        String guestEmail = db.getGuestEmailFromBooking(bookingId);
                        String listingTitle = db.getListingTitleFromBooking(bookingId);
                        double bookingTotal = total;
                        double deposit = Math.ceil(bookingTotal * 0.5);
                        if (guestEmail != null) {
                            db.addNotification(guestEmail,
                                "🎉 Reservation Approved — Pay Now!",
                                "Your reservation for \"" + (listingTitle != null ? listingTitle : "a listing") + "\" has been approved!\n\n"
                                + "Please pay the 50% deposit of ₱" + formatAmt(deposit) + " within 24 hours to confirm your booking. "
                                + "Your reservation will be automatically cancelled if payment is not received in time.",
                                "approved", bookingId);
                        }
                        StyledAlert.show(this, StyledAlert.SUCCESS, "Booking Approved",
                            "The guest has been notified and has 24 hours to pay the 50% deposit.");
                        boolean showPendingOnly = getIntent().getBooleanExtra("SHOW_PENDING", false);
                        loadHostInboxFiltered();
                    });
            });

            TextView btnDecline = new TextView(this);
            LinearLayout.LayoutParams declineLp = new LinearLayout.LayoutParams(0, dp(40), 1);
            btnDecline.setLayoutParams(declineLp);
            btnDecline.setText("✕  Decline");
            btnDecline.setTextSize(13);
            btnDecline.setTextColor(0xFFFFFFFF);
            btnDecline.setBackground(getDrawable(R.drawable.status_badge_cancelled));
            btnDecline.setGravity(android.view.Gravity.CENTER);
            btnDecline.setClickable(true);
            btnDecline.setFocusable(true);
            btnDecline.setOnClickListener(v ->
                StyledAlert.confirm(this, "Decline Booking",
                    "Decline the request from " + (guestName != null ? guestName : "this guest") + "?",
                    "Decline", "Cancel", () -> {
                        db.declineBooking(bookingId);
                        String guestEmail = db.getGuestEmailFromBooking(bookingId);
                        String listingTitle = db.getListingTitleFromBooking(bookingId);
                        if (guestEmail != null) {
                            db.addNotification(guestEmail,
                                "❌ Reservation Declined",
                                "Unfortunately, your reservation for \"" + (listingTitle != null ? listingTitle : "a listing") + "\" was declined by the host.",
                                "declined", bookingId);
                        }
                        StyledAlert.show(this, StyledAlert.INFO, "Booking Declined",
                            "The guest has been notified.");
                        boolean showPendingOnly = getIntent().getBooleanExtra("SHOW_PENDING", false);
                        loadHostInboxFiltered();
                    }));

            btnRow.addView(btnApprove);
            btnRow.addView(btnDecline);
            card.addView(btnRow);
        }

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFFF0F0F0);

        inboxContainer.addView(card);
        inboxContainer.addView(divider);
    }

    private void showReservationReceipt(int bookingId, String guestName, String listingTitle,
                                         String checkIn, String checkOut, double total) {
        // Fetch full listing details via booking receipt
        android.database.Cursor c = db.getBookingReceipt(bookingId);
        if (c == null || !c.moveToFirst()) return;

        String location   = c.getString(c.getColumnIndex("location"));
        String propType   = c.getString(c.getColumnIndex("property_type"));
        String amenities  = c.getString(c.getColumnIndex("amenities"));
        int maxGuests     = c.getInt(c.getColumnIndex("max_guests"));
        int bedrooms      = c.getInt(c.getColumnIndex("bedrooms"));
        int beds          = c.getInt(c.getColumnIndex("beds"));
        double baths      = c.getDouble(c.getColumnIndex("bathrooms"));
        double pricePerNight = c.getDouble(c.getColumnIndex("price_per_night"));
        c.close();

        String paymentStatus = db.getPaymentStatus(bookingId);
        double depositPaid   = db.getDepositAmount(bookingId);
        double remaining     = total - depositPaid;

        // Calculate nights
        int nights = 0;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            long diff = sdf.parse(checkOut).getTime() - sdf.parse(checkIn).getTime();
            nights = (int)(diff / (1000 * 60 * 60 * 24));
        } catch (Exception ignored) {}

        // Build receipt scroll view
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(8));
        root.setBackgroundColor(0xFFFFFFFF);
        scroll.addView(root);

        // Header
        addRow(root, null, "📋  Reservation Receipt", true, 0xFF222222, 17);
        addDividerLine(root);

        // Payment status banner
        boolean isPaid = "paid".equals(paymentStatus);
        android.widget.LinearLayout banner = new android.widget.LinearLayout(this);
        banner.setBackgroundColor(isPaid ? 0xFFE8F5E9 : 0xFFFFF3E0);
        banner.setPadding(dp(12), dp(10), dp(12), dp(10));
        android.widget.LinearLayout.LayoutParams bannerLp = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        bannerLp.setMargins(0, 0, 0, dp(12));
        banner.setLayoutParams(bannerLp);
        android.widget.TextView bannerTv = new android.widget.TextView(this);
        bannerTv.setText(isPaid
            ? "✅  50% deposit paid — ₱" + formatAmt(depositPaid)
            : "⚠️  Awaiting 50% deposit payment");
        bannerTv.setTextSize(13);
        bannerTv.setTypeface(null, android.graphics.Typeface.BOLD);
        bannerTv.setTextColor(isPaid ? 0xFF2E7D32 : 0xFFE65100);
        banner.addView(bannerTv);
        root.addView(banner);

        // Guest info
        addRow(root, "Guest", guestName != null ? guestName : "—", false, 0xFF222222, 13);
        addDividerLine(root);

        // Property info
        addRow(root, "Property", listingTitle != null ? listingTitle : "—", false, 0xFF222222, 14);
        addRow(root, "Type", propType != null ? propType : "—", false, 0xFF717171, 13);
        addRow(root, "Location", "📍 " + (location != null ? location : "—"), false, 0xFF717171, 13);
        addDividerLine(root);

        // Room specs
        addRow(root, "Max guests", String.valueOf(maxGuests), false, 0xFF222222, 13);
        addRow(root, "Bedrooms", String.valueOf(bedrooms), false, 0xFF222222, 13);
        addRow(root, "Beds", String.valueOf(beds), false, 0xFF222222, 13);
        addRow(root, "Bathrooms", String.valueOf(baths), false, 0xFF222222, 13);
        addDividerLine(root);

        // Dates
        addRow(root, "Check-in", formatDate(checkIn), false, 0xFF222222, 13);
        addRow(root, "Check-out", formatDate(checkOut), false, 0xFF222222, 13);
        addRow(root, "Duration", nights + " night" + (nights != 1 ? "s" : ""), false, 0xFF222222, 13);
        addDividerLine(root);

        // Pricing breakdown
        addRow(root, "Price / night", "₱" + formatAmt(pricePerNight), false, 0xFF222222, 13);
        addRow(root, nights + " nights ×", "₱" + formatAmt(pricePerNight), false, 0xFF717171, 13);
        addDividerLine(root);
        addRow(root, "Total amount", "₱" + formatAmt(total), true, 0xFF222222, 15);
        addRow(root, "50% deposit", "₱" + formatAmt(total * 0.5), false,
            isPaid ? 0xFF00A699 : 0xFFFF9800, 13);
        addRow(root, isPaid ? "Deposit paid" : "Deposit pending",
            isPaid ? "₱" + formatAmt(depositPaid) : "Not yet paid",
            true, isPaid ? 0xFF00A699 : 0xFFFF9800, 13);
        if (isPaid) {
            addRow(root, "Remaining balance", "₱" + formatAmt(remaining), false, 0xFF717171, 13);
        }
        addDividerLine(root);

        // Amenities
        if (amenities != null && !amenities.isEmpty()) {
            addRow(root, "Amenities", null, true, 0xFF222222, 14);
            for (String item : amenities.split(", ")) {
                addRow(root, null, "✓  " + item, false, 0xFF484848, 13);
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setView(scroll)
            .setPositiveButton("Close", null)
            .show();
    }

    private void addRow(android.widget.LinearLayout parent, String label, String value,
                        boolean bold, int valueColor, int textSize) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        android.widget.LinearLayout.LayoutParams rowLp = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, dp(5), 0, dp(5));
        row.setLayoutParams(rowLp);

        if (label != null) {
            android.widget.TextView labelTv = new android.widget.TextView(this);
            labelTv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            labelTv.setText(label);
            labelTv.setTextSize(textSize);
            labelTv.setTextColor(0xFF717171);
            row.addView(labelTv);
        }
        if (value != null) {
            android.widget.TextView valueTv = new android.widget.TextView(this);
            valueTv.setLayoutParams(label != null
                ? new android.widget.LinearLayout.LayoutParams(0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1)
                : new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
            valueTv.setText(value);
            valueTv.setTextSize(textSize);
            valueTv.setTextColor(valueColor);
            if (bold) valueTv.setTypeface(null, android.graphics.Typeface.BOLD);
            if (label != null) valueTv.setGravity(android.view.Gravity.END);
            row.addView(valueTv);
        }
        parent.addView(row);
    }

    private void addDividerLine(android.widget.LinearLayout parent) {
        android.view.View divider = new android.view.View(this);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, dp(4), 0, dp(4));
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(0xFFF0F0F0);
        parent.addView(divider);
    }

    private String formatAmt(double amount) {
        return (amount == Math.floor(amount))
            ? String.valueOf((int) amount)
            : String.format(java.util.Locale.getDefault(), "%.2f", amount);
    }

    private void addHostPaymentCard(String title, String message, boolean isUnread,
                                     String time, int bookingId) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(isUnread ? 0xFFE8F5E9 : 0xFFFFFFFF);
        int p = dp(16);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(1));
        card.setLayoutParams(cardLp);

        // Tap → show reservation receipt
        if (bookingId > 0) {
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> showHostReservationDetail(bookingId));
        }

        // Title row
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Icon
        TextView iconTv = new TextView(this);
        iconTv.setText("💳");
        iconTv.setTextSize(22);
        iconTv.setPadding(0, 0, dp(12), 0);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextSize(14);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTv.setTextColor(0xFF222222);

        if (isUnread) {
            LinearLayout titleWithDot = new LinearLayout(this);
            titleWithDot.setOrientation(LinearLayout.HORIZONTAL);
            titleWithDot.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            titleTv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            View dot = new View(this);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(8), dp(8));
            dotLp.gravity = android.view.Gravity.CENTER_VERTICAL;
            dot.setLayoutParams(dotLp);
            dot.setBackgroundColor(0xFF00A699);
            titleWithDot.addView(titleTv);
            titleWithDot.addView(dot);
            content.addView(titleWithDot);
        } else {
            content.addView(titleTv);
        }

        TextView msgTv = new TextView(this);
        msgTv.setText(message);
        msgTv.setTextSize(12);
        msgTv.setTextColor(0xFF484848);
        msgTv.setPadding(0, dp(4), 0, 0);
        msgTv.setMaxLines(2);
        msgTv.setEllipsize(android.text.TextUtils.TruncateAt.END);

        TextView timeTv = new TextView(this);
        timeTv.setText((time != null ? time : "") + "  ·  Tap to view details");
        timeTv.setTextSize(11);
        timeTv.setTextColor(0xFF00A699);
        timeTv.setPadding(0, dp(3), 0, 0);

        content.addView(msgTv);
        content.addView(timeTv);

        titleRow.addView(iconTv);
        titleRow.addView(content);
        card.addView(titleRow);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFFF0F0F0);

        inboxContainer.addView(card);
        inboxContainer.addView(divider);
    }

    private void showHostReservationDetail(int bookingId) {
        Cursor c = db.getBookingReceipt(bookingId);
        if (c == null || !c.moveToFirst()) return;

        String listingTitle  = c.getString(c.getColumnIndex("title"));
        String location      = c.getString(c.getColumnIndex("location"));
        String propType      = c.getString(c.getColumnIndex("property_type"));
        String checkIn       = c.getString(c.getColumnIndex("check_in"));
        String checkOut      = c.getString(c.getColumnIndex("check_out"));
        double total         = c.getDouble(c.getColumnIndex("total_price"));
        double pricePerNight = c.getDouble(c.getColumnIndex("price_per_night"));
        String amenities     = c.getString(c.getColumnIndex("amenities"));
        int maxGuests        = c.getInt(c.getColumnIndex("max_guests"));
        int bedrooms         = c.getInt(c.getColumnIndex("bedrooms"));
        int beds             = c.getInt(c.getColumnIndex("beds"));
        double baths         = c.getDouble(c.getColumnIndex("bathrooms"));
        String guestEmail    = c.getString(c.getColumnIndex("user_email"));
        c.close();

        // Get guest name
        String guestName = guestEmail;
        Cursor gc = db.getUserByEmail(guestEmail);
        if (gc != null && gc.moveToFirst()) {
            guestName = gc.getString(gc.getColumnIndex("first_name"))
                + " " + gc.getString(gc.getColumnIndex("last_name"));
            gc.close();
        }

        String paymentStatus = db.getPaymentStatus(bookingId);
        double depositPaid   = db.getDepositAmount(bookingId);
        boolean isPaid       = "paid".equals(paymentStatus);

        int nights = 0;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            long diff = sdf.parse(checkOut).getTime() - sdf.parse(checkIn).getTime();
            nights = (int)(diff / (1000 * 60 * 60 * 24));
        } catch (Exception ignored) {}

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(8));
        root.setBackgroundColor(0xFFFFFFFF);
        scroll.addView(root);

        addReceiptRow(root, null, "💳  Payment & Reservation Details", true, 0xFF222222, 17);
        addDividerLine(root);

        // Payment status banner
        LinearLayout banner = new LinearLayout(this);
        banner.setBackgroundColor(isPaid ? 0xFFE8F5E9 : 0xFFFFF3E0);
        banner.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bannerLp.setMargins(0, 0, 0, dp(12));
        banner.setLayoutParams(bannerLp);
        TextView bannerTv = new TextView(this);
        bannerTv.setText(isPaid
            ? "✅  50% deposit received — ₱" + formatAmt(depositPaid)
            : "⚠️  Deposit not yet paid");
        bannerTv.setTextSize(13);
        bannerTv.setTypeface(null, android.graphics.Typeface.BOLD);
        bannerTv.setTextColor(isPaid ? 0xFF2E7D32 : 0xFFE65100);
        banner.addView(bannerTv);
        root.addView(banner);

        addReceiptRow(root, "Guest", guestName, false, 0xFF222222, 13);
        addDividerLine(root);
        addReceiptRow(root, "Property", listingTitle != null ? listingTitle : "—", false, 0xFF222222, 14);
        addReceiptRow(root, "Type", propType != null ? propType : "—", false, 0xFF717171, 13);
        addReceiptRow(root, "Location", "📍 " + (location != null ? location : "—"), false, 0xFF717171, 13);
        addDividerLine(root);
        addReceiptRow(root, "Guests", String.valueOf(maxGuests), false, 0xFF222222, 13);
        addReceiptRow(root, "Bedrooms", String.valueOf(bedrooms), false, 0xFF222222, 13);
        addReceiptRow(root, "Beds", String.valueOf(beds), false, 0xFF222222, 13);
        addReceiptRow(root, "Bathrooms", String.valueOf(baths), false, 0xFF222222, 13);
        addDividerLine(root);
        addReceiptRow(root, "Check-in", formatDate(checkIn), false, 0xFF222222, 13);
        addReceiptRow(root, "Check-out", formatDate(checkOut), false, 0xFF222222, 13);
        addReceiptRow(root, "Duration", nights + " night" + (nights != 1 ? "s" : ""), false, 0xFF222222, 13);
        addDividerLine(root);
        addReceiptRow(root, "Price/night", "₱" + formatAmt(pricePerNight), false, 0xFF222222, 13);
        addReceiptRow(root, "Total", "₱" + formatAmt(total), true, 0xFF222222, 15);
        addDividerLine(root);
        addReceiptRow(root, "50% Deposit", "₱" + formatAmt(total * 0.5), false, 0xFF222222, 13);
        addReceiptRow(root, isPaid ? "Deposit Received" : "Deposit Status",
            isPaid ? "✅ ₱" + formatAmt(depositPaid) : "⚠️ Not yet paid",
            true, isPaid ? 0xFF00A699 : 0xFFFF9800, 13);
        if (isPaid) {
            addReceiptRow(root, "Remaining Balance", "₱" + formatAmt(total - depositPaid), false, 0xFF717171, 13);
        }

        if (amenities != null && !amenities.isEmpty()) {
            addDividerLine(root);
            addReceiptRow(root, "Amenities", null, true, 0xFF222222, 14);
            for (String item : amenities.split(", ")) {
                addReceiptRow(root, null, "✓  " + item, false, 0xFF484848, 13);
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setView(scroll)
            .setPositiveButton("Close", null)
            .show();
    }

    private void addSectionHeader(String text) {
        TextView header = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, 0);
        header.setLayoutParams(lp);
        header.setText(text);
        header.setTextSize(13);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setTextColor(0xFF717171);
        header.setPadding(dp(16), dp(10), dp(16), dp(6));
        header.setBackgroundColor(0xFFF7F7F7);
        inboxContainer.addView(header);
    }

    // ---- GUEST INBOX: shows notifications + booking status updates ----
    private void updateGuestDrawerBadges() {
        // Notifications badge
        int unreadNotifs = db.getUnreadNotificationCount(userEmail);
        setBadge(R.id.badge_notifications, unreadNotifs);
        setBadge(R.id.badge_all, unreadNotifs);
        // Messages badge
        int unreadMsg = db.getUnreadMessageCount(userEmail);
        setBadge(R.id.badge_messages, unreadMsg);
    }

    private void loadGuestInboxFiltered() {
        switch (activeFilter) {
            case "notifications":   loadGuestNotificationsSection(); break;
            case "messages":        loadGuestMessagesSection(); break;
            case "booking_updates": loadGuestBookingUpdatesSection(); break;
            default:                loadGuestInbox(); break;
        }
    }

    private void loadGuestNotificationsSection() {
        inboxContainer.removeAllViews();
        Cursor notifCursor = db.getNotificationsForUser(userEmail);
        int count = notifCursor != null ? notifCursor.getCount() : 0;
        addCategoryHeader("🔔  Notifications", count, 0xFFFF9800);
        if (count == 0) { addInlinePlaceholder("No notifications yet"); return; }
        notifCursor.moveToFirst();
        do {
            String notifTitle = notifCursor.getString(notifCursor.getColumnIndex("title"));
            String notifMsg   = notifCursor.getString(notifCursor.getColumnIndex("message"));
            String notifType  = notifCursor.getString(notifCursor.getColumnIndex("type"));
            int isRead        = notifCursor.getInt(notifCursor.getColumnIndex("is_read"));
            String createdAt  = notifCursor.getString(notifCursor.getColumnIndex("created_at"));
            int bookingId     = notifCursor.getInt(notifCursor.getColumnIndex("booking_id"));
            addNotificationCard(0, notifTitle, notifMsg, notifType, isRead == 0, createdAt, bookingId);
        } while (notifCursor.moveToNext());
        notifCursor.close();
        db.markAllNotificationsRead(userEmail);
    }

    private void loadGuestMessagesSection() {
        inboxContainer.removeAllViews();
        SQLiteDatabase sqlDb = db.getReadableDatabase();
        android.database.Cursor msgCursor = sqlDb.rawQuery(
            "SELECT m.listing_id, "
            + "CASE WHEN m.sender_email = ? THEN m.receiver_email ELSE m.sender_email END AS host_email, "
            + "u.first_name || ' ' || u.last_name AS host_name, "
            + "l.title AS listing_title, "
            + "last_m.message AS last_message, last_m.created_at, "
            + "SUM(CASE WHEN m.receiver_email = ? AND m.is_read = 0 THEN 1 ELSE 0 END) AS unread_count "
            + "FROM messages m "
            + "LEFT JOIN users u ON (CASE WHEN m.sender_email = ? THEN m.receiver_email ELSE m.sender_email END) = u.email "
            + "LEFT JOIN listings l ON m.listing_id = l.id "
            + "LEFT JOIN messages last_m ON last_m.id = ("
            + "  SELECT id FROM messages WHERE listing_id = m.listing_id"
            + "  AND ((sender_email = ? AND receiver_email != ?) OR (receiver_email = ? AND sender_email != ?))"
            + "  ORDER BY created_at DESC LIMIT 1) "
            + "WHERE m.sender_email = ? OR m.receiver_email = ? "
            + "GROUP BY m.listing_id, host_email ORDER BY last_m.created_at DESC",
            new String[]{userEmail, userEmail, userEmail, userEmail, userEmail, userEmail, userEmail, userEmail, userEmail});
        int count = msgCursor != null ? msgCursor.getCount() : 0;
        addCategoryHeader("💬  Messages", count, 0xFFFF385C);
        if (count == 0) { addInlinePlaceholder("No messages yet"); if (msgCursor != null) msgCursor.close(); return; }
        msgCursor.moveToFirst();
        do {
            addConversationCard(
                msgCursor.getInt(msgCursor.getColumnIndex("listing_id")),
                msgCursor.getString(msgCursor.getColumnIndex("host_email")),
                msgCursor.getString(msgCursor.getColumnIndex("host_name")),
                msgCursor.getString(msgCursor.getColumnIndex("listing_title")),
                msgCursor.getString(msgCursor.getColumnIndex("last_message")),
                msgCursor.getInt(msgCursor.getColumnIndex("unread_count")));
        } while (msgCursor.moveToNext());
        msgCursor.close();
    }

    private void loadGuestBookingUpdatesSection() {
        inboxContainer.removeAllViews();
        Cursor cursor = db.getBookingsByUser(userEmail);
        int count = cursor != null ? cursor.getCount() : 0;
        addCategoryHeader("🧳  Booking Updates", count, 0xFF3498DB);
        if (count == 0) { addInlinePlaceholder("No bookings yet"); return; }
        addSystemMessage("👋 Welcome to stayFinder!", "Explore listings and book your next stay.", "stayFinder", -1);
        cursor.moveToFirst();
        do {
            String title   = cursor.getString(cursor.getColumnIndex("title"));
            String checkIn = cursor.getString(cursor.getColumnIndex("check_in"));
            String checkOut= cursor.getString(cursor.getColumnIndex("check_out"));
            String status  = cursor.getString(cursor.getColumnIndex("status"));
            double total   = cursor.getDouble(cursor.getColumnIndex("total_price"));
            int bookingListingId = cursor.getInt(cursor.getColumnIndex("listing_id"));
            String statusEmoji, statusText;
            switch (status) {
                case "confirmed": statusEmoji = "✅"; statusText = "Booking confirmed"; break;
                case "declined":  statusEmoji = "❌"; statusText = "Booking declined by host"; break;
                case "cancelled": statusEmoji = "🚫"; statusText = "Booking cancelled"; break;
                default:          statusEmoji = "⏳"; statusText = "Awaiting host approval"; break;
            }
            String totalStr = (total == Math.floor(total)) ? String.valueOf((int) total) : String.valueOf(total);
            String body = (title != null ? title : "Your listing") + "\n"
                + formatDate(checkIn) + " → " + formatDate(checkOut) + "\n"
                + "Total: ₱" + totalStr;
            addSystemMessage(statusEmoji + " " + statusText, body, "stayFinder Bookings", bookingListingId);
        } while (cursor.moveToNext());
        cursor.close();
    }

    private void loadGuestInbox() {
        inboxContainer.removeAllViews();
        db.deduplicateNotifications();

        boolean hasAnything = false;

        // ── 1. NOTIFICATIONS (latest first) ──────────────────────────────
        Cursor notifCursor = db.getNotificationsForUser(userEmail);
        int notifCount = notifCursor != null ? notifCursor.getCount() : 0;
        addCategoryHeader("🔔  Notifications", notifCount, 0xFFFF9800);
        if (notifCount == 0) {
            addInlinePlaceholder("No notifications yet");
        } else {
            hasAnything = true;
            notifCursor.moveToFirst();
            do {
                int notifId       = notifCursor.getInt(notifCursor.getColumnIndex("id"));
                String notifTitle = notifCursor.getString(notifCursor.getColumnIndex("title"));
                String notifMsg   = notifCursor.getString(notifCursor.getColumnIndex("message"));
                String notifType  = notifCursor.getString(notifCursor.getColumnIndex("type"));
                int isRead        = notifCursor.getInt(notifCursor.getColumnIndex("is_read"));
                String createdAt  = notifCursor.getString(notifCursor.getColumnIndex("created_at"));
                int bookingId     = notifCursor.getInt(notifCursor.getColumnIndex("booking_id"));
                addNotificationCard(notifId, notifTitle, notifMsg, notifType, isRead == 0, createdAt, bookingId);
            } while (notifCursor.moveToNext());
            notifCursor.close();
        }
        if (notifCursor != null && !notifCursor.isClosed()) notifCursor.close();

        addSpacing();

        // ── 2. MESSAGES ───────────────────────────────────────────────────
        SQLiteDatabase sqlDb = db.getReadableDatabase();
        android.database.Cursor msgCursor = sqlDb.rawQuery(
            "SELECT m.listing_id, "
            + "CASE WHEN m.sender_email = ? THEN m.receiver_email ELSE m.sender_email END AS host_email, "
            + "u.first_name || ' ' || u.last_name AS host_name, "
            + "l.title AS listing_title, "
            + "last_m.message AS last_message, last_m.created_at, "
            + "SUM(CASE WHEN m.receiver_email = ? AND m.is_read = 0 THEN 1 ELSE 0 END) AS unread_count "
            + "FROM messages m "
            + "LEFT JOIN users u ON (CASE WHEN m.sender_email = ? THEN m.receiver_email ELSE m.sender_email END) = u.email "
            + "LEFT JOIN listings l ON m.listing_id = l.id "
            + "LEFT JOIN messages last_m ON last_m.id = ("
            + "  SELECT id FROM messages WHERE listing_id = m.listing_id"
            + "  AND ((sender_email = ? AND receiver_email != ?) OR (receiver_email = ? AND sender_email != ?))"
            + "  ORDER BY created_at DESC LIMIT 1) "
            + "WHERE m.sender_email = ? OR m.receiver_email = ? "
            + "GROUP BY m.listing_id, host_email ORDER BY last_m.created_at DESC",
            new String[]{userEmail, userEmail, userEmail, userEmail, userEmail,
                         userEmail, userEmail, userEmail, userEmail});
        int msgCount = msgCursor != null ? msgCursor.getCount() : 0;
        addCategoryHeader("💬  Messages", msgCount, 0xFFFF385C);
        if (msgCount == 0) {
            addInlinePlaceholder("No messages yet");
        } else {
            hasAnything = true;
            msgCursor.moveToFirst();
            do {
                addConversationCard(
                    msgCursor.getInt(msgCursor.getColumnIndex("listing_id")),
                    msgCursor.getString(msgCursor.getColumnIndex("host_email")),
                    msgCursor.getString(msgCursor.getColumnIndex("host_name")),
                    msgCursor.getString(msgCursor.getColumnIndex("listing_title")),
                    msgCursor.getString(msgCursor.getColumnIndex("last_message")),
                    msgCursor.getInt(msgCursor.getColumnIndex("unread_count")));
            } while (msgCursor.moveToNext());
            msgCursor.close();
        }
        if (msgCursor != null && !msgCursor.isClosed()) msgCursor.close();

        addSpacing();

        // ── 3. BOOKING UPDATES (latest first) ────────────────────────────
        Cursor bookingCursor = db.getBookingsByUser(userEmail);
        int bookingCount = bookingCursor != null ? bookingCursor.getCount() : 0;
        addCategoryHeader("🧳  Booking Updates", bookingCount, 0xFF3498DB);
        if (bookingCount == 0) {
            addInlinePlaceholder("No bookings yet");
        } else {
            hasAnything = true;
            bookingCursor.moveToFirst();
            do {
                String title   = bookingCursor.getString(bookingCursor.getColumnIndex("title"));
                String checkIn = bookingCursor.getString(bookingCursor.getColumnIndex("check_in"));
                String checkOut= bookingCursor.getString(bookingCursor.getColumnIndex("check_out"));
                String status  = bookingCursor.getString(bookingCursor.getColumnIndex("status"));
                double total   = bookingCursor.getDouble(bookingCursor.getColumnIndex("total_price"));
                int bookingListingId = bookingCursor.getInt(bookingCursor.getColumnIndex("listing_id"));
                String statusEmoji, statusText;
                switch (status) {
                    case "confirmed": statusEmoji = "✅"; statusText = "Booking confirmed"; break;
                    case "declined":  statusEmoji = "❌"; statusText = "Booking declined by host"; break;
                    case "cancelled": statusEmoji = "🚫"; statusText = "Booking cancelled"; break;
                    default:          statusEmoji = "⏳"; statusText = "Awaiting host approval"; break;
                }
                String totalStr = (total == Math.floor(total)) ? String.valueOf((int) total) : String.valueOf(total);
                String body = (title != null ? title : "Your listing") + "\n"
                    + formatDate(checkIn) + " → " + formatDate(checkOut) + "\n"
                    + "Total: ₱" + totalStr;
                addSystemMessage(statusEmoji + " " + statusText, body, "stayFinder Bookings", bookingListingId);
            } while (bookingCursor.moveToNext());
            bookingCursor.close();
        }

        if (!hasAnything) {
            inboxContainer.removeAllViews();
            addEmptyState("💬", "Your inbox is empty", "Notifications, messages and booking updates will appear here");
        }
    }

    private void addNotificationCard(int notifId, String title, String message, String type, boolean isUnread, String time, int bookingId) {
        // Outer wrapper to add left accent bar for unread
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wLp.setMargins(0, 0, 0, dp(1));
        wrapper.setLayoutParams(wLp);
        wrapper.setBackgroundColor(isUnread ? 0xFFFFF3E0 : 0xFFFFFFFF);

        // Left accent bar (only for unread)
        final View[] accentRef = {null};
        if (isUnread) {
            View accent = new View(this);
            accent.setLayoutParams(new LinearLayout.LayoutParams(dp(4),
                LinearLayout.LayoutParams.MATCH_PARENT));
            accent.setBackgroundColor(0xFFFF385C);
            accentRef[0] = accent;
            wrapper.addView(accent);
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundColor(0x00000000);
        int p = dp(16);
        card.setPadding(isUnread ? dp(12) : p, p, p, p);
        card.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        // Tap → mark as read instantly + change background + show receipt
        if (bookingId > 0) {
            wrapper.setClickable(true);
            wrapper.setFocusable(true);
            wrapper.setOnClickListener(v -> {
                // Mark read in DB
                db.markNotificationRead(notifId);
                // Change background to white immediately
                wrapper.setBackgroundColor(0xFFFFFFFF);
                if (accentRef[0] != null) accentRef[0].setVisibility(View.GONE);
                card.setPadding(p, p, p, p);
                showBookingReceipt(bookingId);
            });
        }

        // Icon based on type — light background with colored emoji, not overpowering
        String icon;
        int iconBgLight;
        int iconEmojiColor;
        switch (type) {
            case "approved":
            case "payment_success":
                icon = "✅"; iconBgLight = 0xFFE8F5E9; iconEmojiColor = 0xFF00A699; break;
            case "declined":
            case "cancelled":
                icon = "❌"; iconBgLight = 0xFFFFEBEE; iconEmojiColor = 0xFFE53935; break;
            case "payment":
                icon = "💳"; iconBgLight = 0xFFE3F2FD; iconEmojiColor = 0xFF1976D2; break;
            case "booking_request":
                icon = "📋"; iconBgLight = 0xFFFFF3E0; iconEmojiColor = 0xFFFF9800; break;
            default:
                icon = "🔔"; iconBgLight = 0xFFFFF8E1; iconEmojiColor = 0xFFFF9800; break;
        }

        LinearLayout iconCircle = new LinearLayout(this);
        iconCircle.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        iconCircle.setBackgroundColor(iconBgLight);
        iconCircle.setGravity(android.view.Gravity.CENTER);
        // Make it a rounded square by using a simple background
        android.graphics.drawable.GradientDrawable iconBgDrawable = new android.graphics.drawable.GradientDrawable();
        iconBgDrawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        iconBgDrawable.setColor(iconBgLight);
        iconCircle.setBackground(iconBgDrawable);

        TextView iconTv = new TextView(this);
        iconTv.setText(icon);
        iconTv.setTextSize(20);
        iconTv.setGravity(android.view.Gravity.CENTER);
        iconCircle.addView(iconTv);

        LinearLayout content = new LinearLayout(this);
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        contentLp.setMargins(dp(12), 0, 0, 0);
        content.setLayoutParams(contentLp);
        content.setOrientation(LinearLayout.VERTICAL);

        // Title row with unread dot
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView titleTv = new TextView(this);
        titleTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        titleTv.setText(title);
        titleTv.setTextSize(14);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTv.setTextColor(0xFF222222);

        if (isUnread) {
            View dot = new View(this);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(8), dp(8));
            dotLp.gravity = android.view.Gravity.CENTER_VERTICAL;
            dot.setLayoutParams(dotLp);
            dot.setBackgroundColor(0xFFFF385C);
            titleRow.addView(titleTv);
            titleRow.addView(dot);
        } else {
            titleRow.addView(titleTv);
        }

        TextView msgTv = new TextView(this);
        msgTv.setText(message);
        msgTv.setTextSize(13);
        msgTv.setTextColor(0xFF484848);
        msgTv.setPadding(0, dp(4), 0, 0);
        msgTv.setLineSpacing(0, 1.3f);

        TextView timeTv = new TextView(this);
        timeTv.setText(time != null ? time : "");
        timeTv.setTextSize(11);
        timeTv.setTextColor(0xFFAAAAAA);
        timeTv.setPadding(0, dp(4), 0, 0);

        content.addView(titleRow);
        content.addView(msgTv);
        content.addView(timeTv);

        card.addView(iconCircle);
        card.addView(content);

        wrapper.addView(card);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFFF0F0F0);

        inboxContainer.addView(wrapper);
        inboxContainer.addView(divider);
    }

    private void addSystemMessage(String title, String body, String sender, int listingId) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(0xFFFFFFFF);
        int pad = dp(16);
        row.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, dp(1));
        row.setLayoutParams(rowLp);

        // Tap to open listing detail if listingId is valid
        if (listingId > 0) {
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> {
                Intent intent = new Intent(InboxActivity.this, ListingDetailActivity.class);
                intent.putExtra("LISTING_ID", listingId);
                intent.putExtra("USER_EMAIL", userEmail);
                intent.putExtra("USER_ROLE", userRole);
                startActivity(intent);
            });
        }

        LinearLayout avatar = new LinearLayout(this);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        android.graphics.drawable.GradientDrawable avBg = new android.graphics.drawable.GradientDrawable();
        avBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        avBg.setColor(0xFFFF385C);
        avatar.setBackground(avBg);
        avatar.setGravity(android.view.Gravity.CENTER);
        TextView avatarLetter = new TextView(this);
        avatarLetter.setText("S");
        avatarLetter.setTextSize(18);
        avatarLetter.setTextColor(0xFFFFFFFF);
        avatarLetter.setTypeface(null, android.graphics.Typeface.BOLD);
        avatar.addView(avatarLetter);

        LinearLayout content = new LinearLayout(this);
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        contentLp.setMargins(dp(12), 0, 0, 0);
        content.setLayoutParams(contentLp);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView senderView = new TextView(this);
        senderView.setText(sender);
        senderView.setTextSize(13);
        senderView.setTypeface(null, android.graphics.Typeface.BOLD);
        senderView.setTextColor(0xFF222222);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(14);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setTextColor(0xFF222222);
        titleView.setPadding(0, dp(2), 0, 0);

        TextView bodyView = new TextView(this);
        bodyView.setText(body);
        bodyView.setTextSize(13);
        bodyView.setTextColor(0xFF717171);
        bodyView.setPadding(0, dp(4), 0, 0);
        bodyView.setLineSpacing(dp(2), 1);

        content.addView(senderView);
        content.addView(titleView);
        content.addView(bodyView);
        row.addView(avatar);
        row.addView(content);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFFF0F0F0);

        inboxContainer.addView(row);
        inboxContainer.addView(divider);
    }

    private void showBookingReceipt(int bookingId) {
        Cursor c = db.getBookingReceipt(bookingId);
        if (c == null || !c.moveToFirst()) return;

        String listingTitle  = c.getString(c.getColumnIndex("title"));
        String location      = c.getString(c.getColumnIndex("location"));
        String propType      = c.getString(c.getColumnIndex("property_type"));
        String checkIn       = c.getString(c.getColumnIndex("check_in"));
        String checkOut      = c.getString(c.getColumnIndex("check_out"));
        String status        = c.getString(c.getColumnIndex("status"));
        double total         = c.getDouble(c.getColumnIndex("total_price"));
        double pricePerNight = c.getDouble(c.getColumnIndex("price_per_night"));
        String amenities     = c.getString(c.getColumnIndex("amenities"));
        String hostName      = c.getString(c.getColumnIndex("host_name"));
        int maxGuests        = c.getInt(c.getColumnIndex("max_guests"));
        int bedrooms         = c.getInt(c.getColumnIndex("bedrooms"));
        int beds             = c.getInt(c.getColumnIndex("beds"));
        double baths         = c.getDouble(c.getColumnIndex("bathrooms"));
        c.close();

        // Calculate nights
        int nights = 0;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            long diff = sdf.parse(checkOut).getTime() - sdf.parse(checkIn).getTime();
            nights = (int)(diff / (1000 * 60 * 60 * 24));
        } catch (Exception ignored) {}

        String priceStr = (pricePerNight == Math.floor(pricePerNight))
            ? String.valueOf((int) pricePerNight) : String.valueOf(pricePerNight);
        String totalStr = (total == Math.floor(total))
            ? String.valueOf((int) total) : String.valueOf(total);

        // Build receipt layout
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(8));
        root.setBackgroundColor(0xFFFFFFFF);
        scroll.addView(root);

        // Header
        addReceiptRow(root, null, "🧾  Booking Receipt", true, 0xFF222222, 17);
        addDivider(root);

        // Status badge
        String statusLabel = "confirmed".equals(status) ? "✅ Confirmed"
            : "pending".equals(status) ? "⏳ Pending Approval"
            : "declined".equals(status) ? "❌ Declined" : "🚫 Cancelled";
        addReceiptRow(root, "Status", statusLabel, false, "confirmed".equals(status) ? 0xFF00A699 : 0xFFFF9800, 14);
        addDivider(root);

        // Property info
        addReceiptRow(root, "Property", listingTitle != null ? listingTitle : "", false, 0xFF222222, 14);
        addReceiptRow(root, "Type", propType != null ? propType : "", false, 0xFF717171, 13);
        addReceiptRow(root, "Location", "📍 " + (location != null ? location : ""), false, 0xFF717171, 13);
        addReceiptRow(root, "Host", hostName != null ? hostName : "Host", false, 0xFF717171, 13);
        addDivider(root);

        // Room details
        addReceiptRow(root, "Guests", String.valueOf(maxGuests), false, 0xFF222222, 13);
        addReceiptRow(root, "Bedrooms", String.valueOf(bedrooms), false, 0xFF222222, 13);
        addReceiptRow(root, "Beds", String.valueOf(beds), false, 0xFF222222, 13);
        addReceiptRow(root, "Bathrooms", String.valueOf(baths), false, 0xFF222222, 13);
        addDivider(root);

        // Dates
        addReceiptRow(root, "Check-in", formatDate(checkIn), false, 0xFF222222, 13);
        addReceiptRow(root, "Check-out", formatDate(checkOut), false, 0xFF222222, 13);
        addReceiptRow(root, "Duration", nights + " night" + (nights != 1 ? "s" : ""), false, 0xFF222222, 13);
        addDivider(root);

        // Pricing
        addReceiptRow(root, "Price/night", "₱" + priceStr, false, 0xFF222222, 13);
        addReceiptRow(root, nights + " night" + (nights != 1 ? "s" : "") + " ×", "₱" + priceStr, false, 0xFF717171, 13);
        addDivider(root);
        addReceiptRow(root, "Total", "₱" + totalStr, true, 0xFFFF385C, 16);
        addDivider(root);

        // Amenities
        if (amenities != null && !amenities.isEmpty()) {
            addReceiptRow(root, "Amenities", null, true, 0xFF222222, 14);
            String[] items = amenities.split(", ");
            for (String item : items) {
                addReceiptRow(root, null, "✓  " + item, false, 0xFF484848, 13);
            }
        }

        // Payment section — only for confirmed bookings
        String paymentStatus = db.getPaymentStatus(bookingId);
        long paymentDeadline = db.getPaymentDeadline(bookingId);
        long now = System.currentTimeMillis();
        boolean needsPayment = "confirmed".equals(status) && "unpaid".equals(paymentStatus);
        boolean deadlineActive = paymentDeadline > 0 && paymentDeadline > now;

        if ("confirmed".equals(status)) {
            addDivider(root);
            double deposit = Math.ceil(total * 0.5);
            double depositPaid = db.getDepositAmount(bookingId);

            if ("paid".equals(paymentStatus)) {
                // Already paid
                addReceiptRow(root, "50% Deposit", "₱" + formatAmt(deposit), false, 0xFF00A699, 13);
                addReceiptRow(root, "Deposit Paid", "✅ ₱" + formatAmt(depositPaid), true, 0xFF00A699, 13);
                addReceiptRow(root, "Remaining Balance", "₱" + formatAmt(total - depositPaid), false, 0xFF717171, 13);
            } else if (needsPayment && deadlineActive) {
                // Needs payment — show countdown + pay button
                long hoursLeft = (paymentDeadline - now) / (1000 * 60 * 60);
                long minsLeft  = ((paymentDeadline - now) % (1000 * 60 * 60)) / (1000 * 60);

                addReceiptRow(root, "50% Deposit Due", "₱" + formatAmt(deposit), true, 0xFFFF385C, 14);
                addReceiptRow(root, "⏰ Time Remaining",
                    hoursLeft + "h " + minsLeft + "m", false, 0xFFE65100, 13);

                // Pay Now button
                android.widget.Button btnPay = new android.widget.Button(this);
                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
                btnLp.setMargins(0, dp(16), 0, dp(4));
                btnPay.setLayoutParams(btnLp);
                btnPay.setText("💳  Pay 50% Deposit  ·  ₱" + formatAmt(deposit));
                btnPay.setTextColor(0xFFFFFFFF);
                btnPay.setTextSize(14);
                btnPay.setTypeface(null, android.graphics.Typeface.BOLD);
                btnPay.setBackgroundColor(0xFFFF385C);
                btnPay.setAllCaps(false);
                root.addView(btnPay);

                // Wire pay button — dismiss dialog then launch payment
                final double finalDeposit = deposit;
                final String finalTitle = listingTitle;
                android.app.AlertDialog[] dialogRef = {null};

                android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                    .setView(scroll)
                    .setPositiveButton("Close", null)
                    .create();
                dialogRef[0] = dialog;

                btnPay.setOnClickListener(v -> {
                    dialog.dismiss();
                    Intent payIntent = new Intent(InboxActivity.this, XenditPaymentActivity.class);
                    payIntent.putExtra("BOOKING_ID", bookingId);
                    payIntent.putExtra("DEPOSIT_AMOUNT", finalDeposit);
                    payIntent.putExtra("USER_EMAIL", userEmail);
                    payIntent.putExtra("LISTING_TITLE", finalTitle != null ? finalTitle : "Booking");
                    startActivity(payIntent);
                });

                dialog.show();
                return; // already shown
            }
        }

        new android.app.AlertDialog.Builder(this)
            .setView(scroll)
            .setPositiveButton("Close", null)
            .show();
    }

    private void addReceiptRow(LinearLayout parent, String label, String value, boolean bold, int valueColor, int textSize) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, dp(5), 0, dp(5));
        row.setLayoutParams(rowLp);

        if (label != null) {
            TextView labelTv = new TextView(this);
            labelTv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            labelTv.setText(label);
            labelTv.setTextSize(textSize);
            labelTv.setTextColor(0xFF717171);
            row.addView(labelTv);
        }

        if (value != null) {
            TextView valueTv = new TextView(this);
            valueTv.setLayoutParams(label != null
                ? new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)
                : new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            valueTv.setText(value);
            valueTv.setTextSize(textSize);
            valueTv.setTextColor(valueColor);
            if (bold) valueTv.setTypeface(null, android.graphics.Typeface.BOLD);
            if (label != null) valueTv.setGravity(android.view.Gravity.END);
            row.addView(valueTv);
        }

        parent.addView(row);
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, dp(4), 0, dp(4));
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(0xFFF0F0F0);
        parent.addView(divider);
    }

    private void addEmptyState(String emoji, String title, String sub) {
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 600));

        TextView emojiTv = new TextView(this);
        emojiTv.setText(emoji);
        emojiTv.setTextSize(56);
        emojiTv.setGravity(android.view.Gravity.CENTER);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextSize(16);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTv.setTextColor(0xFF222222);
        titleTv.setGravity(android.view.Gravity.CENTER);
        titleTv.setPadding(0, dp(16), 0, 0);

        TextView subTv = new TextView(this);
        subTv.setText(sub);
        subTv.setTextSize(13);
        subTv.setTextColor(0xFF717171);
        subTv.setGravity(android.view.Gravity.CENTER);
        subTv.setPadding(dp(32), dp(8), dp(32), 0);

        empty.addView(emojiTv);
        empty.addView(titleTv);
        empty.addView(subTv);
        inboxContainer.addView(empty);
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    private String formatDate(String dateStr) {
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault());
            return out.format(in.parse(dateStr));
        } catch (Exception e) { return dateStr; }
    }
}

