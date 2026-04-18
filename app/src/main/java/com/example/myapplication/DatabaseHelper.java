package com.example.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.content.ContentValues;

public class DatabaseHelper extends SQLiteOpenHelper {
    
    // Database Info
    private static final String DB_NAME = "stayfinder.db";
    private static final int DB_VERSION = 14;
    
    // Table Names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_LISTINGS = "listings";
    private static final String TABLE_BOOKINGS = "bookings";
    private static final String TABLE_WISHLIST = "wishlist";
    private static final String TABLE_MAINTENANCE = "maintenance";
    private static final String TABLE_REVIEWS = "reviews";
    private static final String TABLE_NOTIFICATIONS = "notifications";
    private static final String TABLE_MESSAGES = "messages";
    
    // User Table Columns
    private static final String COL_ID = "id";
    private static final String COL_FIRSTNAME = "first_name";
    private static final String COL_LASTNAME = "last_name";
    private static final String COL_CONTACT = "contact_number";
    private static final String COL_EMAIL = "email";
    private static final String COL_PASSWORD = "password";
    private static final String COL_ROLE = "role";

    // Listing Table Columns
    private static final String COL_L_ID = "id";
    private static final String COL_L_HOST_EMAIL = "host_email";
    private static final String COL_L_TITLE = "title";
    private static final String COL_L_TYPE = "property_type";
    private static final String COL_L_LOCATION = "location";
    private static final String COL_L_DESCRIPTION = "description";
    private static final String COL_L_PRICE = "price_per_night";
    private static final String COL_L_GUESTS = "max_guests";
    private static final String COL_L_BEDROOMS = "bedrooms";
    private static final String COL_L_BEDS = "beds";
    private static final String COL_L_BATHROOMS = "bathrooms";
    private static final String COL_L_AMENITIES = "amenities";
    private static final String COL_L_IMAGE_URI = "image_uri";

    // Booking Table Columns
    private static final String COL_B_ID = "id";
    private static final String COL_B_USER_EMAIL = "user_email";
    private static final String COL_B_LISTING_ID = "listing_id";
    private static final String COL_B_CHECKIN = "check_in";
    private static final String COL_B_CHECKOUT = "check_out";
    private static final String COL_B_GUESTS = "guests";
    private static final String COL_B_TOTAL = "total_price";
    private static final String COL_B_STATUS = "status";
    private static final String COL_B_CREATED = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_FIRSTNAME + " TEXT NOT NULL, "
                + COL_LASTNAME + " TEXT NOT NULL, "
                + COL_CONTACT + " TEXT NOT NULL, "
                + COL_EMAIL + " TEXT NOT NULL UNIQUE, "
                + COL_PASSWORD + " TEXT NOT NULL, "
                + COL_ROLE + " TEXT NOT NULL DEFAULT 'user'"
                + ")";
        db.execSQL(CREATE_USERS_TABLE);

        String CREATE_LISTINGS_TABLE = "CREATE TABLE " + TABLE_LISTINGS + " ("
                + COL_L_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_L_HOST_EMAIL + " TEXT NOT NULL, "
                + COL_L_TITLE + " TEXT NOT NULL, "
                + COL_L_TYPE + " TEXT NOT NULL, "
                + COL_L_LOCATION + " TEXT NOT NULL, "
                + COL_L_DESCRIPTION + " TEXT, "
                + COL_L_PRICE + " REAL NOT NULL, "
                + COL_L_GUESTS + " INTEGER NOT NULL, "
                + COL_L_BEDROOMS + " INTEGER NOT NULL, "
                + COL_L_BEDS + " INTEGER NOT NULL, "
                + COL_L_BATHROOMS + " REAL NOT NULL, "
                + COL_L_AMENITIES + " TEXT, "
                + COL_L_IMAGE_URI + " TEXT"
                + ")";
        db.execSQL(CREATE_LISTINGS_TABLE);

        String CREATE_BOOKINGS_TABLE = "CREATE TABLE " + TABLE_BOOKINGS + " ("
                + COL_B_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_B_USER_EMAIL + " TEXT NOT NULL, "
                + COL_B_LISTING_ID + " INTEGER NOT NULL, "
                + COL_B_CHECKIN + " TEXT NOT NULL, "
                + COL_B_CHECKOUT + " TEXT NOT NULL, "
                + COL_B_GUESTS + " INTEGER NOT NULL DEFAULT 1, "
                + COL_B_TOTAL + " REAL NOT NULL, "
                + COL_B_STATUS + " TEXT NOT NULL DEFAULT 'confirmed', "
                + COL_B_CREATED + " TEXT NOT NULL"
                + ")";
        db.execSQL(CREATE_BOOKINGS_TABLE);

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WISHLIST + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_email TEXT NOT NULL, "
                + "listing_id INTEGER NOT NULL, "
                + "UNIQUE(user_email, listing_id)"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MAINTENANCE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "listing_id INTEGER NOT NULL, "
                + "start_date TEXT NOT NULL, "
                + "end_date TEXT NOT NULL"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_REVIEWS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "listing_id INTEGER NOT NULL, "
                + "user_email TEXT NOT NULL, "
                + "rating INTEGER NOT NULL, "
                + "comment TEXT, "
                + "created_at TEXT NOT NULL, "
                + "UNIQUE(listing_id, user_email)"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATIONS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_email TEXT NOT NULL, "
                + "title TEXT NOT NULL, "
                + "message TEXT NOT NULL, "
                + "type TEXT NOT NULL DEFAULT 'info', "
                + "is_read INTEGER NOT NULL DEFAULT 0, "
                + "booking_id INTEGER DEFAULT -1, "
                + "created_at TEXT NOT NULL"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGES + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "listing_id INTEGER NOT NULL, "
                + "sender_email TEXT NOT NULL, "
                + "receiver_email TEXT NOT NULL, "
                + "message TEXT NOT NULL, "
                + "is_read INTEGER NOT NULL DEFAULT 0, "
                + "created_at TEXT NOT NULL"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migrate safely — only add new columns/tables, never drop user data
        if (oldVersion < 4) {
            // Add image_uri column to listings if upgrading from v3
            try { db.execSQL("ALTER TABLE " + TABLE_LISTINGS + " ADD COLUMN image_uri TEXT"); } catch (Exception ignored) {}
        }
        if (oldVersion < 5) {
            // Create bookings table if upgrading from v4
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BOOKINGS + " ("
                        + COL_B_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COL_B_USER_EMAIL + " TEXT NOT NULL, "
                        + COL_B_LISTING_ID + " INTEGER NOT NULL, "
                        + COL_B_CHECKIN + " TEXT NOT NULL, "
                        + COL_B_CHECKOUT + " TEXT NOT NULL, "
                        + COL_B_GUESTS + " INTEGER NOT NULL DEFAULT 1, "
                        + COL_B_TOTAL + " REAL NOT NULL, "
                        + COL_B_STATUS + " TEXT NOT NULL DEFAULT 'confirmed', "
                        + COL_B_CREATED + " TEXT NOT NULL"
                        + ")");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 6) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WISHLIST + " ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "user_email TEXT NOT NULL, "
                        + "listing_id INTEGER NOT NULL, "
                        + "UNIQUE(user_email, listing_id)"
                        + ")");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 7) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MAINTENANCE + " ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "listing_id INTEGER NOT NULL, "
                        + "start_date TEXT NOT NULL, "
                        + "end_date TEXT NOT NULL"
                        + ")");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 8) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_REVIEWS + " ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "listing_id INTEGER NOT NULL, "
                        + "user_email TEXT NOT NULL, "
                        + "rating INTEGER NOT NULL, "
                        + "comment TEXT, "
                        + "created_at TEXT NOT NULL, "
                        + "UNIQUE(listing_id, user_email)"
                        + ")");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 9) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATIONS + " ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "user_email TEXT NOT NULL, "
                        + "title TEXT NOT NULL, "
                        + "message TEXT NOT NULL, "
                        + "type TEXT NOT NULL DEFAULT 'info', "
                        + "is_read INTEGER NOT NULL DEFAULT 0, "
                        + "created_at TEXT NOT NULL"
                        + ")");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 10) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGES + " ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "listing_id INTEGER NOT NULL, "
                        + "sender_email TEXT NOT NULL, "
                        + "receiver_email TEXT NOT NULL, "
                        + "message TEXT NOT NULL, "
                        + "is_read INTEGER NOT NULL DEFAULT 0, "
                        + "created_at TEXT NOT NULL"
                        + ")");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 11) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NOTIFICATIONS + " ADD COLUMN booking_id INTEGER DEFAULT -1");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 12) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_BOOKINGS + " ADD COLUMN payment_status TEXT DEFAULT 'unpaid'");
                db.execSQL("ALTER TABLE " + TABLE_BOOKINGS + " ADD COLUMN payment_id TEXT DEFAULT ''");
                db.execSQL("ALTER TABLE " + TABLE_BOOKINGS + " ADD COLUMN deposit_amount REAL DEFAULT 0");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 13) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_BOOKINGS + " ADD COLUMN payment_deadline TEXT DEFAULT ''");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 14) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN profile_image_uri TEXT DEFAULT ''");
            } catch (Exception ignored) {}
        }
    }

    // Add a new user to the database
    public boolean addUser(String firstName, String lastName, String contact, String email, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COL_FIRSTNAME, firstName);
        values.put(COL_LASTNAME, lastName);
        values.put(COL_CONTACT, contact);
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);
        values.put(COL_ROLE, role);
        
        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        
        return result != -1;
    }

    // Check if user exists with email and password (for login)
    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT * FROM " + TABLE_USERS + " WHERE " 
                + COL_EMAIL + " = ? AND " + COL_PASSWORD + " = ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{email, password});
        
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        
        return exists;
    }

    // Check if email already exists
    public boolean checkEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT * FROM " + TABLE_USERS + " WHERE " + COL_EMAIL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{email});
        
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        
        return exists;
    }

    // Get user details by email
    public Cursor getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT * FROM " + TABLE_USERS + " WHERE " + COL_EMAIL + " = ?";
        return db.rawQuery(query, new String[]{email});
    }

    // Get all users
    public Cursor getAllUsers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS, null);
    }

    public boolean updateProfileImage(String email, String imageUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("profile_image_uri", imageUri);
        int result = db.update(TABLE_USERS, values, COL_EMAIL + " = ?", new String[]{email});
        db.close();
        return result > 0;
    }

    public String getProfileImageUri(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT profile_image_uri FROM " + TABLE_USERS
            + " WHERE email = ?", new String[]{email});
        String uri = "";
        if (c.moveToFirst()) uri = c.getString(0);
        c.close();
        return uri != null ? uri : "";
    }

    // Update user information
    public boolean updateUser(String email, String firstName, String lastName, String contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COL_FIRSTNAME, firstName);
        values.put(COL_LASTNAME, lastName);
        values.put(COL_CONTACT, contact);
        
        int result = db.update(TABLE_USERS, values, COL_EMAIL + " = ?", new String[]{email});
        db.close();
        
        return result > 0;
    }

    // Update password
    public boolean updatePassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COL_PASSWORD, newPassword);
        
        int result = db.update(TABLE_USERS, values, COL_EMAIL + " = ?", new String[]{email});
        db.close();
        
        return result > 0;
    }

    // Delete user
    public boolean deleteUser(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        int result = db.delete(TABLE_USERS, COL_EMAIL + " = ?", new String[]{email});
        db.close();
        
        return result > 0;
    }

    // Get user role by email
    public String getUserRole(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT role FROM " + TABLE_USERS + " WHERE email = ?", new String[]{email});
        String role = "user";
        if (cursor.moveToFirst()) {
            role = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return role;
    }

    // ---- Listing Methods ----

    public long addListing(String hostEmail, String title, String type, String location,
                           String description, double price, int guests,
                           int bedrooms, int beds, double bathrooms, String amenities, String imageUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_L_HOST_EMAIL, hostEmail);
        values.put(COL_L_TITLE, title);
        values.put(COL_L_TYPE, type);
        values.put(COL_L_LOCATION, location);
        values.put(COL_L_DESCRIPTION, description);
        values.put(COL_L_PRICE, price);
        values.put(COL_L_GUESTS, guests);
        values.put(COL_L_BEDROOMS, bedrooms);
        values.put(COL_L_BEDS, beds);
        values.put(COL_L_BATHROOMS, bathrooms);
        values.put(COL_L_AMENITIES, amenities);
        values.put(COL_L_IMAGE_URI, imageUri);
        long id = db.insert(TABLE_LISTINGS, null, values);
        db.close();
        return id;
    }

    public Cursor getListingsByHost(String hostEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_LISTINGS + " WHERE host_email = ? ORDER BY id DESC", new String[]{hostEmail});
    }

    public Cursor getAllListings() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT l.*, u.first_name || ' ' || u.last_name AS host_name "
                + "FROM " + TABLE_LISTINGS + " l "
                + "LEFT JOIN " + TABLE_USERS + " u ON l.host_email = u.email "
                + "ORDER BY l.id DESC";
        return db.rawQuery(query, null);
    }

    public Cursor getListingById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Join with users to get host name
        String query = "SELECT l.*, u.first_name || ' ' || u.last_name AS host_name "
                + "FROM " + TABLE_LISTINGS + " l "
                + "LEFT JOIN " + TABLE_USERS + " u ON l.host_email = u.email "
                + "WHERE l.id = ?";
        return db.rawQuery(query, new String[]{String.valueOf(id)});
    }

    public boolean deleteListing(int listingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_LISTINGS, "id = ?", new String[]{String.valueOf(listingId)});
        db.close();
        return result > 0;
    }

    public boolean updateListing(int listingId, String title, String type, String location,
                                  String description, double price, int guests,
                                  int bedrooms, int beds, double bathrooms, String amenities, String imageUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_L_TITLE, title);
        values.put(COL_L_TYPE, type);
        values.put(COL_L_LOCATION, location);
        values.put(COL_L_DESCRIPTION, description);
        values.put(COL_L_PRICE, price);
        values.put(COL_L_GUESTS, guests);
        values.put(COL_L_BEDROOMS, bedrooms);
        values.put(COL_L_BEDS, beds);
        values.put(COL_L_BATHROOMS, bathrooms);
        values.put(COL_L_AMENITIES, amenities);
        if (imageUri != null && !imageUri.isEmpty()) {
            values.put(COL_L_IMAGE_URI, imageUri);
        }
        int result = db.update(TABLE_LISTINGS, values, "id = ?", new String[]{String.valueOf(listingId)});
        db.close();
        return result > 0;
    }

    // ---- Booking Methods ----

    public long addBooking(String userEmail, int listingId, String checkIn, String checkOut, int guests, double total) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_B_USER_EMAIL, userEmail);
        values.put(COL_B_LISTING_ID, listingId);
        values.put(COL_B_CHECKIN, checkIn);
        values.put(COL_B_CHECKOUT, checkOut);
        values.put(COL_B_GUESTS, guests);
        values.put(COL_B_TOTAL, total);
        values.put(COL_B_STATUS, "pending");
        values.put(COL_B_CREATED, new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date()));
        long id = db.insert(TABLE_BOOKINGS, null, values);
        db.close();
        return id;
    }

    public Cursor getBookingsByUser(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Join bookings with listings to get listing info
        String query = "SELECT b.*, l.title, l.location, l.property_type, l.image_uri, l.price_per_night "
                + "FROM " + TABLE_BOOKINGS + " b "
                + "LEFT JOIN " + TABLE_LISTINGS + " l ON b.listing_id = l.id "
                + "WHERE b.user_email = ? "
                + "ORDER BY b.created_at DESC";
        return db.rawQuery(query, new String[]{userEmail});
    }

    // Check if guest already has an active booking for this listing on overlapping dates
    public boolean hasOverlappingBooking(String userEmail, int listingId, String checkIn, String checkOut) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Overlapping condition: existing check_in < new check_out AND existing check_out > new check_in
        Cursor c = db.rawQuery(
            "SELECT id FROM " + TABLE_BOOKINGS
            + " WHERE user_email = ? AND listing_id = ?"
            + " AND status NOT IN ('cancelled', 'declined')"
            + " AND check_in < ? AND check_out > ?",
            new String[]{userEmail, String.valueOf(listingId), checkOut, checkIn});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    public boolean cancelBooking(int bookingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_B_STATUS, "cancelled");
        int result = db.update(TABLE_BOOKINGS, values, "id = ?", new String[]{String.valueOf(bookingId)});
        db.close();
        return result > 0;
    }

    public boolean approveBooking(int bookingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_B_STATUS, "confirmed");
        int result = db.update(TABLE_BOOKINGS, values, "id = ?", new String[]{String.valueOf(bookingId)});
        db.close();
        return result > 0;
    }

    public boolean declineBooking(int bookingId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_B_STATUS, "declined");
        int result = db.update(TABLE_BOOKINGS, values, "id = ?", new String[]{String.valueOf(bookingId)});
        db.close();
        return result > 0;
    }

    // Get pending booking requests for all listings owned by a host
    public Cursor getPendingBookingsForHost(String hostEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT b.*, l.title, l.location, l.image_uri, u.first_name || ' ' || u.last_name AS guest_name "
                + "FROM " + TABLE_BOOKINGS + " b "
                + "LEFT JOIN " + TABLE_LISTINGS + " l ON b.listing_id = l.id "
                + "LEFT JOIN " + TABLE_USERS + " u ON b.user_email = u.email "
                + "WHERE l.host_email = ? AND b.status = 'pending' "
                + "ORDER BY b.created_at DESC";
        return db.rawQuery(query, new String[]{hostEmail});
    }

    // Get all bookings for host's listings (for host inbox)
    public Cursor getAllBookingsForHost(String hostEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT b.*, l.title, l.location, l.image_uri, u.first_name || ' ' || u.last_name AS guest_name "
                + "FROM " + TABLE_BOOKINGS + " b "
                + "LEFT JOIN " + TABLE_LISTINGS + " l ON b.listing_id = l.id "
                + "LEFT JOIN " + TABLE_USERS + " u ON b.user_email = u.email "
                + "WHERE l.host_email = ? "
                + "ORDER BY b.created_at DESC";
        return db.rawQuery(query, new String[]{hostEmail});
    }

    // Get all confirmed bookings for host's listings (active reservations)
    public Cursor getActiveBookingsForHost(String hostEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT b.*, l.title, l.location, l.image_uri, l.price_per_night, "
            + "u.first_name || ' ' || u.last_name AS guest_name, u.email AS guest_email_addr "
            + "FROM " + TABLE_BOOKINGS + " b "
            + "LEFT JOIN " + TABLE_LISTINGS + " l ON b.listing_id = l.id "
            + "LEFT JOIN " + TABLE_USERS + " u ON b.user_email = u.email "
            + "WHERE l.host_email = ? AND b.status = 'confirmed' "
            + "ORDER BY b.check_in ASC";
        return db.rawQuery(query, new String[]{hostEmail});
    }

    public int getActiveBookingCountForHost(String hostEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_BOOKINGS + " b "
            + "LEFT JOIN " + TABLE_LISTINGS + " l ON b.listing_id = l.id "
            + "WHERE l.host_email = ? AND b.status = 'confirmed'",
            new String[]{hostEmail});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public int getPendingCountForHost(String hostEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_BOOKINGS + " b "
            + "LEFT JOIN " + TABLE_LISTINGS + " l ON b.listing_id = l.id "
            + "WHERE l.host_email = ? AND b.status = 'pending'",
            new String[]{hostEmail});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // ---- Wishlist Methods ----

    public boolean toggleWishlist(String userEmail, int listingId) {
        if (isWishlisted(userEmail, listingId)) {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_WISHLIST, "user_email = ? AND listing_id = ?",
                    new String[]{userEmail, String.valueOf(listingId)});
            db.close();
            return false; // removed
        } else {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("user_email", userEmail);
            values.put("listing_id", listingId);
            db.insertOrThrow(TABLE_WISHLIST, null, values);
            db.close();
            return true; // added
        }
    }

    public boolean isWishlisted(String userEmail, int listingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM " + TABLE_WISHLIST
                + " WHERE user_email = ? AND listing_id = ?",
                new String[]{userEmail, String.valueOf(listingId)});
        boolean result = cursor.getCount() > 0;
        cursor.close();
        return result;
    }

    public Cursor getWishlistByUser(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT l.*, u.first_name || ' ' || u.last_name AS host_name "
                + "FROM " + TABLE_LISTINGS + " l "
                + "INNER JOIN " + TABLE_WISHLIST + " w ON l.id = w.listing_id "
                + "LEFT JOIN " + TABLE_USERS + " u ON l.host_email = u.email "
                + "WHERE w.user_email = ?";
        return db.rawQuery(query, new String[]{userEmail});
    }

    // ---- Calendar / Availability Methods ----

    // Returns all confirmed booking date ranges for a listing (only current/future)
    public Cursor getBookedDatesForListing(int listingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(new java.util.Date());
        return db.rawQuery(
            "SELECT check_in, check_out FROM " + TABLE_BOOKINGS
            + " WHERE listing_id = ? AND status = 'confirmed' AND check_out >= ?",
            new String[]{String.valueOf(listingId), today});
    }

    // Returns all maintenance date ranges for a listing
    public Cursor getMaintenanceDatesForListing(int listingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
            "SELECT start_date, end_date FROM " + TABLE_MAINTENANCE
            + " WHERE listing_id = ?",
            new String[]{String.valueOf(listingId)});
    }

    // Add a maintenance window (host only)
    public boolean addMaintenance(int listingId, String startDate, String endDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("listing_id", listingId);
        values.put("start_date", startDate);
        values.put("end_date", endDate);
        long result = db.insert(TABLE_MAINTENANCE, null, values);
        db.close();
        return result != -1;
    }

    // Delete a specific maintenance entry by its id
    public boolean deleteMaintenance(int maintenanceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_MAINTENANCE, "id = ?", new String[]{String.valueOf(maintenanceId)});
        db.close();
        return result > 0;
    }

    // Update an existing maintenance entry's dates
    public boolean updateMaintenance(int maintenanceId, String startDate, String endDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("start_date", startDate);
        values.put("end_date", endDate);
        int result = db.update(TABLE_MAINTENANCE, values, "id = ?", new String[]{String.valueOf(maintenanceId)});
        db.close();
        return result > 0;
    }

    // Returns maintenance entries with their IDs (for host management)
    public Cursor getMaintenanceWithIds(int listingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
            "SELECT id, start_date, end_date FROM " + TABLE_MAINTENANCE
            + " WHERE listing_id = ? ORDER BY start_date ASC",
            new String[]{String.valueOf(listingId)});
    }

    // Get recent likes on host's listings
    public Cursor getLikesForHost(String hostEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT w.user_email, u.first_name || ' ' || u.last_name AS liker_name, "
            + "l.title AS listing_title, w.id "
            + "FROM " + TABLE_WISHLIST + " w "
            + "LEFT JOIN " + TABLE_LISTINGS + " l ON w.listing_id = l.id "
            + "LEFT JOIN " + TABLE_USERS + " u ON w.user_email = u.email "
            + "WHERE l.host_email = ? "
            + "ORDER BY w.id DESC";
        return db.rawQuery(query, new String[]{hostEmail});
    }

    // ---- Like Count ----

    public int getLikeCount(int listingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_WISHLIST + " WHERE listing_id = ?",
                new String[]{String.valueOf(listingId)});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // ---- Review Methods ----

    public boolean addReview(int listingId, String userEmail, int rating, String comment) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("listing_id", listingId);
        values.put("user_email", userEmail);
        values.put("rating", rating);
        values.put("comment", comment);
        values.put("created_at", new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date()));
        try {
            long result = db.insertOrThrow(TABLE_REVIEWS, null, values);
            db.close();
            return result != -1;
        } catch (Exception e) {
            // Already reviewed — update instead
            int result = db.update(TABLE_REVIEWS, values,
                "listing_id = ? AND user_email = ?",
                new String[]{String.valueOf(listingId), userEmail});
            db.close();
            return result > 0;
        }
    }

    public Cursor getReviewsForListing(int listingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT r.*, u.first_name || ' ' || u.last_name AS reviewer_name "
                + "FROM " + TABLE_REVIEWS + " r "
                + "LEFT JOIN " + TABLE_USERS + " u ON r.user_email = u.email "
                + "WHERE r.listing_id = ? "
                + "ORDER BY r.created_at DESC";
        return db.rawQuery(query, new String[]{String.valueOf(listingId)});
    }

    public float getAverageRating(int listingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT AVG(rating) FROM " + TABLE_REVIEWS + " WHERE listing_id = ?",
                new String[]{String.valueOf(listingId)});
        float avg = 0f;
        if (c.moveToFirst() && !c.isNull(0)) avg = c.getFloat(0);
        c.close();
        return avg;
    }

    public int getReviewCount(int listingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_REVIEWS + " WHERE listing_id = ?",
                new String[]{String.valueOf(listingId)});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public boolean hasUserReviewed(int listingId, String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM " + TABLE_REVIEWS
                + " WHERE listing_id = ? AND user_email = ?",
                new String[]{String.valueOf(listingId), userEmail});
        boolean result = c.getCount() > 0;
        c.close();
        return result;
    }

    // Check if guest has a completed (past check-out) confirmed booking for this listing
    public boolean hasCompletedStay(int listingId, String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(new java.util.Date());
        Cursor c = db.rawQuery(
            "SELECT id FROM " + TABLE_BOOKINGS
            + " WHERE listing_id = ? AND user_email = ?"
            + " AND status = 'confirmed'"
            + " AND payment_status = 'paid'"
            + " AND check_out < ?",
            new String[]{String.valueOf(listingId), userEmail, today});
        boolean result = c.getCount() > 0;
        c.close();
        return result;
    }

    // ---- Payment Methods ----

    public boolean setPaymentDeadline(int bookingId) {
        // Deadline = now + 24 hours
        SQLiteDatabase db = this.getWritableDatabase();
        long deadline = System.currentTimeMillis() + (24 * 60 * 60 * 1000L);
        ContentValues values = new ContentValues();
        values.put("payment_deadline", String.valueOf(deadline));
        int result = db.update(TABLE_BOOKINGS, values, "id = ?", new String[]{String.valueOf(bookingId)});
        db.close();
        return result > 0;
    }

    public long getPaymentDeadline(int bookingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT payment_deadline FROM " + TABLE_BOOKINGS + " WHERE id = ?",
            new String[]{String.valueOf(bookingId)});
        long deadline = 0;
        if (c.moveToFirst()) {
            String val = c.getString(0);
            if (val != null && !val.isEmpty()) {
                try { deadline = Long.parseLong(val); } catch (Exception ignored) {}
            }
        }
        c.close();
        return deadline;
    }

    // Auto-cancel bookings where payment deadline has passed and still unpaid
    public void autoExpireUnpaidBookings() {
        SQLiteDatabase db = this.getWritableDatabase();
        long now = System.currentTimeMillis();
        // Get expired bookings to notify guests
        Cursor c = db.rawQuery(
            "SELECT id, user_email, listing_id FROM " + TABLE_BOOKINGS
            + " WHERE status = 'confirmed' AND payment_status = 'unpaid'"
            + " AND payment_deadline != '' AND payment_deadline < ?",
            new String[]{String.valueOf(now)});
        if (c.moveToFirst()) {
            do {
                int bookingId = c.getInt(0);
                String guestEmail = c.getString(1);
                // Cancel the booking
                ContentValues values = new ContentValues();
                values.put(COL_B_STATUS, "cancelled");
                db.update(TABLE_BOOKINGS, values, "id = ?", new String[]{String.valueOf(bookingId)});
                // Notify guest
                if (guestEmail != null) {
                    String title = getListingTitleFromBooking(bookingId);
                    addNotification(guestEmail,
                        "❌ Reservation Cancelled",
                        "Your reservation for \"" + (title != null ? title : "a listing") + "\" was automatically cancelled because the 50% deposit was not paid within 24 hours.",
                        "cancelled", bookingId);
                }
            } while (c.moveToNext());
        }
        c.close();
        db.close();
    }

    public boolean markBookingPaid(int bookingId, String paymentId, double depositAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("payment_status", "paid");
        values.put("payment_id", paymentId);
        values.put("deposit_amount", depositAmount);
        int result = db.update(TABLE_BOOKINGS, values, "id = ?", new String[]{String.valueOf(bookingId)});
        db.close();
        return result > 0;
    }

    public String getPaymentStatus(int bookingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT payment_status FROM " + TABLE_BOOKINGS + " WHERE id = ?",
            new String[]{String.valueOf(bookingId)});
        String status = "unpaid";
        if (c.moveToFirst()) status = c.getString(0);
        c.close();
        return status;
    }

    public double getDepositAmount(int bookingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT deposit_amount FROM " + TABLE_BOOKINGS + " WHERE id = ?",
            new String[]{String.valueOf(bookingId)});
        double amount = 0;
        if (c.moveToFirst()) amount = c.getDouble(0);
        c.close();
        return amount;
    }

    public long addNotification(String userEmail, String title, String message, String type) {
        return addNotification(userEmail, title, message, type, -1);
    }

    public long addNotification(String userEmail, String title, String message, String type, int bookingId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Prevent duplicate notifications for the same booking + type
        if (bookingId > 0) {
            Cursor existing = db.rawQuery(
                "SELECT id FROM " + TABLE_NOTIFICATIONS
                + " WHERE user_email = ? AND type = ? AND booking_id = ?",
                new String[]{userEmail, type, String.valueOf(bookingId)});
            boolean alreadyExists = existing.getCount() > 0;
            existing.close();
            if (alreadyExists) {
                // Update the existing one instead of inserting a duplicate
                ContentValues update = new ContentValues();
                update.put("title", title);
                update.put("message", message);
                update.put("is_read", 0);
                update.put("created_at", new java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()));
                db.update(TABLE_NOTIFICATIONS, update,
                    "user_email = ? AND type = ? AND booking_id = ?",
                    new String[]{userEmail, type, String.valueOf(bookingId)});
                db.close();
                return -1; // not a new insert but not an error
            }
        }

        ContentValues values = new ContentValues();
        values.put("user_email", userEmail);
        values.put("title", title);
        values.put("message", message);
        values.put("type", type);
        values.put("is_read", 0);
        values.put("booking_id", bookingId);
        values.put("created_at", new java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()));
        long id = db.insert(TABLE_NOTIFICATIONS, null, values);
        db.close();
        return id;
    }

    // Remove duplicate notifications — keep only the latest per booking_id + type + user
    public void deduplicateNotifications() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(
            "DELETE FROM " + TABLE_NOTIFICATIONS + " WHERE id NOT IN ("
            + "  SELECT MAX(id) FROM " + TABLE_NOTIFICATIONS
            + "  WHERE booking_id > 0"
            + "  GROUP BY user_email, type, booking_id"
            + ") AND booking_id > 0");
        db.close();
    }

    // Get full booking receipt details (booking + listing + amenities)
    public Cursor getBookingReceipt(int bookingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT b.*, l.title, l.location, l.property_type, l.image_uri, "
            + "l.price_per_night, l.amenities, l.max_guests, l.bedrooms, l.beds, l.bathrooms, "
            + "u.first_name || ' ' || u.last_name AS host_name "
            + "FROM " + TABLE_BOOKINGS + " b "
            + "LEFT JOIN " + TABLE_LISTINGS + " l ON b.listing_id = l.id "
            + "LEFT JOIN " + TABLE_USERS + " u ON l.host_email = u.email "
            + "WHERE b.id = ?";
        return db.rawQuery(query, new String[]{String.valueOf(bookingId)});
    }

    // Mark all messages as read for a user (called when inbox is opened)
    public void markAllMessagesRead(String receiverEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_read", 1);
        db.update(TABLE_MESSAGES, values, "receiver_email = ?", new String[]{receiverEmail});
        db.close();
    }

    public Cursor getNotificationsForUser(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
            "SELECT * FROM " + TABLE_NOTIFICATIONS
            + " WHERE user_email = ? ORDER BY created_at DESC",
            new String[]{userEmail});
    }

    public Cursor getNotificationsByType(String userEmail, String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
            "SELECT * FROM " + TABLE_NOTIFICATIONS
            + " WHERE user_email = ? AND type = ? ORDER BY created_at DESC",
            new String[]{userEmail, type});
    }

    public void markAllNotificationsRead(String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_read", 1);
        db.update(TABLE_NOTIFICATIONS, values, "user_email = ?", new String[]{userEmail});
        db.close();
    }

    public void markNotificationRead(int notificationId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_read", 1);
        db.update(TABLE_NOTIFICATIONS, values, "id = ?", new String[]{String.valueOf(notificationId)});
        db.close();
    }

    public int getUnreadNotificationCount(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_NOTIFICATIONS
            + " WHERE user_email = ? AND is_read = 0",
            new String[]{userEmail});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // Helper: get guest email from a booking id
    public String getGuestEmailFromBooking(int bookingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT user_email FROM " + TABLE_BOOKINGS + " WHERE id = ?",
            new String[]{String.valueOf(bookingId)});
        String email = null;
        if (c.moveToFirst()) email = c.getString(0);
        c.close();
        return email;
    }

    // Helper: get listing title from a booking id
    public String getListingTitleFromBooking(int bookingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT l.title FROM " + TABLE_BOOKINGS + " b "
            + "LEFT JOIN " + TABLE_LISTINGS + " l ON b.listing_id = l.id "
            + "WHERE b.id = ?",
            new String[]{String.valueOf(bookingId)});
        String title = null;
        if (c.moveToFirst()) title = c.getString(0);
        c.close();
        return title;
    }

    // ---- Messaging Methods ----

    public long sendMessage(int listingId, String senderEmail, String receiverEmail, String message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("listing_id", listingId);
        values.put("sender_email", senderEmail);
        values.put("receiver_email", receiverEmail);
        values.put("message", message);
        values.put("is_read", 0);
        values.put("created_at", new java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()));
        long id = db.insert(TABLE_MESSAGES, null, values);
        db.close();
        return id;
    }

    // Get all messages in a conversation (between two users about a listing)
    public Cursor getMessages(int listingId, String userA, String userB) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
            "SELECT * FROM " + TABLE_MESSAGES
            + " WHERE listing_id = ? AND ("
            + "  (sender_email = ? AND receiver_email = ?) OR"
            + "  (sender_email = ? AND receiver_email = ?)"
            + ") ORDER BY created_at ASC",
            new String[]{String.valueOf(listingId), userA, userB, userB, userA});
    }

    // Mark messages as read for a receiver in a conversation
    public void markMessagesRead(int listingId, String receiverEmail, String senderEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_read", 1);
        db.update(TABLE_MESSAGES, values,
            "listing_id = ? AND receiver_email = ? AND sender_email = ?",
            new String[]{String.valueOf(listingId), receiverEmail, senderEmail});
        db.close();
    }

    // Get all unique conversations for a host (grouped by guest + listing)
    public Cursor getConversationsForHost(String hostEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT m.listing_id, m.sender_email AS guest_email, "
            + "u.first_name || ' ' || u.last_name AS guest_name, "
            + "l.title AS listing_title, "
            + "last_m.message AS last_message, last_m.created_at, "
            + "SUM(CASE WHEN m.receiver_email = ? AND m.is_read = 0 THEN 1 ELSE 0 END) AS unread_count "
            + "FROM " + TABLE_MESSAGES + " m "
            + "LEFT JOIN " + TABLE_USERS + " u ON m.sender_email = u.email "
            + "LEFT JOIN " + TABLE_LISTINGS + " l ON m.listing_id = l.id "
            + "LEFT JOIN " + TABLE_MESSAGES + " last_m ON last_m.id = ("
            + "  SELECT id FROM " + TABLE_MESSAGES
            + "  WHERE listing_id = m.listing_id AND sender_email = m.sender_email"
            + "  ORDER BY created_at DESC LIMIT 1"
            + ") "
            + "WHERE l.host_email = ? AND m.sender_email != ? "
            + "GROUP BY m.listing_id, m.sender_email "
            + "ORDER BY last_m.created_at DESC";
        return db.rawQuery(query, new String[]{hostEmail, hostEmail, hostEmail});
    }

    // Get host email for a listing
    public String getHostEmailForListing(int listingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT host_email FROM " + TABLE_LISTINGS + " WHERE id = ?",
            new String[]{String.valueOf(listingId)});
        String email = null;
        if (c.moveToFirst()) email = c.getString(0);
        c.close();
        return email;
    }

    public int getUnreadMessageCount(String receiverEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_MESSAGES
            + " WHERE receiver_email = ? AND is_read = 0",
            new String[]{receiverEmail});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
}
