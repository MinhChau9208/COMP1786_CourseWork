package com.example.yogaadmin;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class YogaAdminApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Enable Firebase offline persistence to allow data access when offline
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}