package com.example.myapplication;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ChatActivity extends Activity {

    DatabaseHelper db;
    String userEmail;
    String otherEmail;   // host email (if guest) or guest email (if host)
    int listingId;
    String listingTitle;
    LinearLayout messagesContainer;
    ScrollView scrollView;
    EditText inputMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(this);
        userEmail    = getIntent().getStringExtra("USER_EMAIL");
        otherEmail   = getIntent().getStringExtra("OTHER_EMAIL");
        listingId    = getIntent().getIntExtra("LISTING_ID", -1);
        listingTitle = getIntent().getStringExtra("LISTING_TITLE");
        if (listingTitle == null) listingTitle = "Listing";

        buildUI();
        loadMessages();
        db.markMessagesRead(listingId, userEmail, otherEmail);
    }

    private void buildUI() {
        // Root
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF7F7F7);
        root.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT));

        // Toolbar
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(0xFFFFFFFF);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(12), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        toolbar.setLayoutParams(tbLp);
        toolbar.setElevation(dp(4));

        TextView btnBack = new TextView(this);
        btnBack.setText("←");
        btnBack.setTextSize(22);
        btnBack.setTextColor(0xFF222222);
        btnBack.setPadding(0, 0, dp(12), 0);
        btnBack.setClickable(true);
        btnBack.setFocusable(true);
        btnBack.setOnClickListener(v -> finish());

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView titleTv = new TextView(this);
        titleTv.setText(listingTitle);
        titleTv.setTextSize(15);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTv.setTextColor(0xFF222222);
        titleTv.setMaxLines(1);
        titleTv.setEllipsize(android.text.TextUtils.TruncateAt.END);

        TextView subtitleTv = new TextView(this);
        subtitleTv.setText("Chat with " + formatEmail(otherEmail));
        subtitleTv.setTextSize(11);
        subtitleTv.setTextColor(0xFF717171);

        titleCol.addView(titleTv);
        titleCol.addView(subtitleTv);
        toolbar.addView(btnBack);
        toolbar.addView(titleCol);

        // Messages scroll area
        scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        scrollView.setFillViewport(true);

        messagesContainer = new LinearLayout(this);
        messagesContainer.setOrientation(LinearLayout.VERTICAL);
        messagesContainer.setPadding(dp(12), dp(12), dp(12), dp(12));
        scrollView.addView(messagesContainer);

        // Input bar
        LinearLayout inputBar = new LinearLayout(this);
        inputBar.setOrientation(LinearLayout.HORIZONTAL);
        inputBar.setBackgroundColor(0xFFFFFFFF);
        inputBar.setGravity(Gravity.CENTER_VERTICAL);
        inputBar.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams ibLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        inputBar.setLayoutParams(ibLp);

        inputMessage = new EditText(this);
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        inputLp.setMargins(0, 0, dp(8), 0);
        inputMessage.setLayoutParams(inputLp);
        inputMessage.setHint("Type a message...");
        inputMessage.setBackground(getDrawable(R.drawable.rounded_edittext_dark));
        inputMessage.setPadding(dp(14), dp(10), dp(14), dp(10));
        inputMessage.setTextSize(14);
        inputMessage.setMaxLines(3);

        TextView btnSend = new TextView(this);
        btnSend.setText("Send");
        btnSend.setTextSize(14);
        btnSend.setTypeface(null, android.graphics.Typeface.BOLD);
        btnSend.setTextColor(0xFFFFFFFF);
        btnSend.setBackground(getDrawable(R.drawable.btn_view_details));
        btnSend.setPadding(dp(20), dp(10), dp(20), dp(10));
        btnSend.setGravity(Gravity.CENTER);
        btnSend.setClickable(true);
        btnSend.setFocusable(true);
        btnSend.setOnClickListener(v -> sendMessage());

        inputBar.addView(inputMessage);
        inputBar.addView(btnSend);

        root.addView(toolbar);
        root.addView(scrollView);
        root.addView(inputBar);

        setContentView(root);
    }

    private void loadMessages() {
        messagesContainer.removeAllViews();
        Cursor cursor = db.getMessages(listingId, userEmail, otherEmail);

        if (cursor == null || cursor.getCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No messages yet.\nSay hello! 👋");
            empty.setTextSize(14);
            empty.setTextColor(0xFFAAAAAA);
            empty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(40), 0, 0);
            empty.setLayoutParams(lp);
            messagesContainer.addView(empty);
            return;
        }

        while (cursor.moveToNext()) {
            String sender  = cursor.getString(cursor.getColumnIndex("sender_email"));
            String text    = cursor.getString(cursor.getColumnIndex("message"));
            String time    = cursor.getString(cursor.getColumnIndex("created_at"));
            boolean isMe   = sender.equals(userEmail);
            addMessageBubble(text, time, isMe);
        }
        cursor.close();

        // Scroll to bottom
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void addMessageBubble(String text, String time, boolean isMe) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wLp.setMargins(0, 0, 0, dp(8));
        wrapper.setLayoutParams(wLp);
        wrapper.setGravity(isMe ? Gravity.END : Gravity.START);

        // Bubble
        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextSize(14);
        bubble.setTextColor(isMe ? 0xFFFFFFFF : 0xFF222222);
        bubble.setBackgroundColor(isMe ? 0xFFFF385C : 0xFFFFFFFF);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        bubble.setLineSpacing(0, 1.3f);

        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bLp.setMargins(isMe ? dp(60) : 0, 0, isMe ? 0 : dp(60), 0);
        bubble.setLayoutParams(bLp);
        bubble.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.75));

        // Timestamp
        TextView timeTv = new TextView(this);
        timeTv.setText(time);
        timeTv.setTextSize(10);
        timeTv.setTextColor(0xFFAAAAAA);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tLp.setMargins(isMe ? 0 : dp(4), dp(2), isMe ? dp(4) : 0, 0);
        timeTv.setLayoutParams(tLp);

        wrapper.addView(bubble);
        wrapper.addView(timeTv);
        messagesContainer.addView(wrapper);
    }

    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        db.sendMessage(listingId, userEmail, otherEmail, text);
        inputMessage.setText("");
        loadMessages();
    }

    private String formatEmail(String email) {
        if (email == null) return "Host";
        // Try to get name from DB
        Cursor c = db.getUserByEmail(email);
        if (c != null && c.moveToFirst()) {
            String name = c.getString(c.getColumnIndex("first_name"))
                + " " + c.getString(c.getColumnIndex("last_name"));
            c.close();
            return name;
        }
        return email;
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }
}
