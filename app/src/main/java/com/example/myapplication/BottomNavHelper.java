package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BottomNavHelper {

    public static void setup(Activity activity, String userEmail, String userRole, String activeTab) {
        DatabaseHelper db = new DatabaseHelper(activity);
        boolean isHost = "host".equals(userRole);

        // Role-based visibility
        View navWishlist   = activity.findViewById(R.id.nav_wishlist);
        View navTrips      = activity.findViewById(R.id.nav_trips);
        View navMyListings = activity.findViewById(R.id.nav_my_listings);

        if (navWishlist != null)   navWishlist.setVisibility(View.VISIBLE);
        if (navTrips != null)      navTrips.setVisibility(isHost ? View.GONE : View.VISIBLE);
        if (navMyListings != null) navMyListings.setVisibility(isHost ? View.VISIBLE : View.GONE);

        TextView wishlistLabel = activity.findViewById(R.id.nav_wishlist_label);
        if (wishlistLabel != null && isHost) wishlistLabel.setText("Likes");

        highlightTab(activity, activeTab);
        updateBadge(activity, db, userEmail);

        // Explore
        View navExplore = activity.findViewById(R.id.nav_explore);
        if (navExplore != null) navExplore.setOnClickListener(v -> {
            if (!"explore".equals(activeTab)) {
                Intent i = new Intent(activity, HomeActivity.class);
                i.putExtra("USER_EMAIL", userEmail);
                i.putExtra("USER_ROLE", userRole);
                i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(i);
                activity.overridePendingTransition(0, 0);
            }
        });

        // Wishlist / Likes
        if (navWishlist != null) navWishlist.setOnClickListener(v -> {
            if (!"wishlist".equals(activeTab)) {
                Intent i = new Intent(activity, WishlistActivity.class);
                i.putExtra("USER_EMAIL", userEmail);
                i.putExtra("USER_ROLE", userRole);
                i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(i);
                activity.overridePendingTransition(0, 0);
            }
        });

        // Trips
        if (navTrips != null) navTrips.setOnClickListener(v -> {
            if (!"trips".equals(activeTab)) {
                Intent i = new Intent(activity, TripsActivity.class);
                i.putExtra("USER_EMAIL", userEmail);
                i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(i);
                activity.overridePendingTransition(0, 0);
            }
        });

        // My Listings (host)
        if (navMyListings != null) navMyListings.setOnClickListener(v -> {
            if (!"my_listings".equals(activeTab)) {
                Intent i = new Intent(activity, HostDashboardActivity.class);
                i.putExtra("USER_EMAIL", userEmail);
                i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(i);
                activity.overridePendingTransition(0, 0);
            }
        });

        // Inbox
        View navInbox = activity.findViewById(R.id.nav_inbox);
        if (navInbox != null) navInbox.setOnClickListener(v -> {
            if (!"inbox".equals(activeTab)) {
                Intent i = new Intent(activity, InboxActivity.class);
                i.putExtra("USER_EMAIL", userEmail);
                i.putExtra("USER_ROLE", userRole);
                i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(i);
                activity.overridePendingTransition(0, 0);
            }
        });

        // Profile
        View navProfile = activity.findViewById(R.id.nav_profile);
        if (navProfile != null) navProfile.setOnClickListener(v -> {
            if (!"profile".equals(activeTab)) {
                Intent i = new Intent(activity, ProfileActivity.class);
                i.putExtra("USER_EMAIL", userEmail);
                i.putExtra("USER_ROLE", userRole);
                i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(i);
                activity.overridePendingTransition(0, 0);
            }
        });
    }

    private static void highlightTab(Activity activity, String activeTab) {
        String[] tabs = {"explore", "wishlist", "trips", "my_listings", "inbox", "profile"};
        int[] ids = {R.id.nav_explore, R.id.nav_wishlist, R.id.nav_trips,
                     R.id.nav_my_listings, R.id.nav_inbox, R.id.nav_profile};

        for (int i = 0; i < ids.length; i++) {
            View tab = activity.findViewById(ids[i]);
            if (!(tab instanceof LinearLayout)) continue;
            LinearLayout ll = (LinearLayout) tab;
            boolean isActive = tabs[i].equals(activeTab);
            for (int c = 0; c < ll.getChildCount(); c++) {
                View child = ll.getChildAt(c);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(isActive ? 0xFFFF385C : 0xFF717171);
                } else if (child instanceof FrameLayout) {
                    // inbox badge container — find label inside
                    FrameLayout fl = (FrameLayout) child;
                    for (int f = 0; f < fl.getChildCount(); f++) {
                        // skip badge TextView
                    }
                }
            }
        }
    }

    public static void updateBadge(Activity activity, DatabaseHelper db, String userEmail) {
        TextView badge = activity.findViewById(R.id.inbox_badge);
        if (badge == null) return;
        int unread = db.getUnreadNotificationCount(userEmail) + db.getUnreadMessageCount(userEmail);
        if (unread > 0) {
            badge.setText(unread > 99 ? "99+" : String.valueOf(unread));
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
        }
    }
}
