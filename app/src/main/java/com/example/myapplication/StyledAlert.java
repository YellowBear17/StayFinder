package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StyledAlert {

    // Types
    public static final String SUCCESS = "success";
    public static final String ERROR   = "error";
    public static final String WARNING = "warning";
    public static final String INFO    = "info";
    public static final String CONFIRM = "confirm";

    public interface OnConfirm { void onConfirm(); }
    public interface OnConfirmCancel { void onConfirm(); void onCancel(); }

    /** Simple one-button alert */
    public static void show(Context ctx, String type, String title, String message) {
        show(ctx, type, title, message, "OK", null, null, null);
    }

    /** Confirm dialog with positive + negative button */
    public static void confirm(Context ctx, String title, String message,
                               String positiveLabel, OnConfirm onConfirm) {
        show(ctx, CONFIRM, title, message, positiveLabel, "Cancel", onConfirm, null);
    }

    public static void confirm(Context ctx, String title, String message,
                               String positiveLabel, String negativeLabel,
                               OnConfirm onConfirm) {
        show(ctx, CONFIRM, title, message, positiveLabel, negativeLabel, onConfirm, null);
    }

    private static void show(Context ctx, String type, String title, String message,
                              String posLabel, String negLabel,
                              OnConfirm onConfirm, OnConfirmCancel onBoth) {

        String emoji;
        int accentColor;
        int bgColor = 0xFFFFFFFF;

        switch (type) {
            case SUCCESS:
                emoji = "✅"; accentColor = 0xFF00A699; break;
            case ERROR:
                emoji = "❌"; accentColor = 0xFFE74C3C; break;
            case WARNING:
                emoji = "⚠️"; accentColor = 0xFFF39C12; break;
            case CONFIRM:
                emoji = "❓"; accentColor = 0xFFFF385C; break;
            default: // INFO
                emoji = "ℹ️"; accentColor = 0xFF3498DB; break;
        }

        int dp8  = dp(ctx, 8);
        int dp12 = dp(ctx, 12);
        int dp16 = dp(ctx, 16);
        int dp20 = dp(ctx, 20);
        int dp24 = dp(ctx, 24);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp24, dp24, dp24, dp16);
        root.setBackgroundColor(bgColor);

        // Emoji
        TextView emojiTv = new TextView(ctx);
        emojiTv.setText(emoji);
        emojiTv.setTextSize(48);
        emojiTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        eLp.setMargins(0, 0, 0, dp12);
        emojiTv.setLayoutParams(eLp);

        // Title
        TextView titleTv = new TextView(ctx);
        titleTv.setText(title);
        titleTv.setTextSize(18);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTv.setTextColor(0xFF222222);
        titleTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tLp.setMargins(0, 0, 0, dp8);
        titleTv.setLayoutParams(tLp);

        // Message
        TextView msgTv = new TextView(ctx);
        msgTv.setText(message);
        msgTv.setTextSize(14);
        msgTv.setTextColor(0xFF717171);
        msgTv.setGravity(Gravity.CENTER);
        msgTv.setLineSpacing(0, 1.4f);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mLp.setMargins(0, 0, 0, dp20);
        msgTv.setLayoutParams(mLp);

        root.addView(emojiTv);
        root.addView(titleTv);
        root.addView(msgTv);

        AlertDialog dialog = new AlertDialog.Builder(ctx)
            .setView(root)
            .setCancelable(false)
            .create();

        if (negLabel != null) {
            // Two-button row
            LinearLayout btnRow = new LinearLayout(ctx);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            Button negBtn = makeBtn(ctx, negLabel, 0xFF717171, dp8);
            LinearLayout.LayoutParams negLp = new LinearLayout.LayoutParams(0,
                dp(ctx, 48), 1);
            negLp.setMargins(0, 0, dp8, 0);
            negBtn.setLayoutParams(negLp);
            negBtn.setOnClickListener(v -> {
                dialog.dismiss();
                if (onBoth != null) onBoth.onCancel();
            });

            Button posBtn = makeBtn(ctx, posLabel, accentColor, dp8);
            posBtn.setLayoutParams(new LinearLayout.LayoutParams(0, dp(ctx, 48), 1));
            posBtn.setOnClickListener(v -> {
                dialog.dismiss();
                if (onConfirm != null) onConfirm.onConfirm();
                if (onBoth != null) onBoth.onConfirm();
            });

            btnRow.addView(negBtn);
            btnRow.addView(posBtn);
            root.addView(btnRow);
        } else {
            Button okBtn = makeBtn(ctx, posLabel, accentColor, dp8);
            LinearLayout.LayoutParams okLp = new LinearLayout.LayoutParams(
                dp(ctx, 140), LinearLayout.LayoutParams.WRAP_CONTENT);
            okLp.gravity = Gravity.CENTER_HORIZONTAL;
            okBtn.setLayoutParams(okLp);
            okBtn.setOnClickListener(v -> dialog.dismiss());
            root.addView(okBtn);
        }

        dialog.show();
    }

    private static Button makeBtn(Context ctx, String label, int color, int radius) {
        Button btn = new Button(ctx);
        btn.setText(label);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(14);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setBackgroundColor(color);
        btn.setPadding(dp(ctx, 16), dp(ctx, 10), dp(ctx, 16), dp(ctx, 10));
        btn.setAllCaps(false);
        return btn;
    }

    private static int dp(Context ctx, int val) {
        return (int) (val * ctx.getResources().getDisplayMetrics().density);
    }
}
