package com.example.yogaadmin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.cursoradapter.widget.ResourceCursorAdapter;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    public static DatabaseHelper helper; // Reference to the database helper for SQLite operations
    private FusedLocationProviderClient locationClient; // Client for accessing device location

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the database helper
        helper = new DatabaseHelper(getApplicationContext());

        // Initialize location client for retrieving device location
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up button listeners for various actions
        findViewById(R.id.btnAddCourse).setOnClickListener(v -> onCreateYogaCourse(v)); // Navigate to CreateYogaCourse activity
        findViewById(R.id.btnResetDB).setOnClickListener(v -> onResetDatabase()); // Reset the local database
        findViewById(R.id.btnSearch).setOnClickListener(v -> {
            // Navigate to SearchActivity to search class instances
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.btnUploadCloud).setOnClickListener(v -> uploadToCloud()); // Upload data to Firebase
        findViewById(R.id.btnShowLocation).setOnClickListener(v -> showLocationDialog()); // Show device location

        // Set up listeners to sync data with Firebase
        setupFirebaseListeners();
    }

    // Displays a dialog with the device's current location (latitude and longitude)
    private void showLocationDialog() {
        // Check if location permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permission if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        // Retrieve the last known location
        locationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    // Show location in a dialog if available
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Your Location")
                            .setMessage("Latitude: " + latitude + "\nLongitude: " + longitude)
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    Toast.makeText(MainActivity.this, "Unable to get location", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(MainActivity.this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // Handle the result of location permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showLocationDialog();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Navigate to CreateYogaCourse activity to create a new yoga course
    public void onCreateYogaCourse(View v) {
        Intent i = new Intent(getApplicationContext(), CreateYogaCourse.class);
        startActivity(i);
    }

    // Reload courses when the activity resumes
    @Override
    protected void onResume() {
        super.onResume();
        loadCourses();
    }

    // Load all yoga courses from the database and display them in a ListView
    private void loadCourses() {
        // Query all yoga courses from the database
        Cursor r = helper.readAllYogaCourse();
        Log.d("MainActivity", "Cursor count: " + (r != null ? r.getCount() : "null"));
        if (r != null && r.moveToFirst()) {
            // Log course details for debugging
            do {
                Log.d("MainActivity", "Course ID: " + r.getLong(r.getColumnIndexOrThrow("_id")) +
                        ", Day: " + r.getString(r.getColumnIndexOrThrow("dayofweek")) +
                        ", Time: " + r.getString(r.getColumnIndexOrThrow("time")) +
                        ", Capacity: " + r.getFloat(r.getColumnIndexOrThrow("capacity")) +
                        ", Price: " + r.getFloat(r.getColumnIndexOrThrow("price")));
            } while (r.moveToNext());
        } else {
            Toast.makeText(this, "No courses available", Toast.LENGTH_SHORT).show();
        }

        // Set up the adapter to display courses in the ListView
        YogaCourseCursorAdapter adapter = new YogaCourseCursorAdapter(this, R.layout.yoga_course_item, r, 0);
        ListView lv = findViewById(R.id.lvCourse);
        Log.d("MainActivity", "ListView found: " + (lv != null));
        lv.setAdapter(adapter);

        // Handle clicks on a course to navigate to ManageClassInstancesActivity
        lv.setOnItemClickListener((adapterView, view, i, l) -> {
            Log.d("MainActivity", "Item clicked, position: " + i + ", id: " + l);
            Intent intent = new Intent(this, ManageClassInstancesActivity.class);
            intent.putExtra("courseId", l);
            startActivity(intent);
        });
    }

    // Show a confirmation dialog to reset the database
    private void onResetDatabase() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Database")
                .setMessage("Are you sure you want to delete all courses and class instances?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    helper.resetDatabase();
                    loadCourses();
                    Toast.makeText(this, "All courses and instances deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Upload local database data (courses and class instances) to Firebase
    private void uploadToCloud() {
        if (!NetworkUtil.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hasData = false;

        // Upload YogaCourses
        Cursor courseCursor = helper.readAllYogaCourse();
        if (courseCursor.moveToFirst()) {
            hasData = true;
            do {
                // Create a YogaCourse object from the cursor
                long id = courseCursor.getLong(courseCursor.getColumnIndexOrThrow("_id"));
                YogaCourse course = new YogaCourse(
                        id,
                        courseCursor.getString(courseCursor.getColumnIndexOrThrow("dayofweek")),
                        courseCursor.getString(courseCursor.getColumnIndexOrThrow("time")),
                        courseCursor.getFloat(courseCursor.getColumnIndexOrThrow("capacity")),
                        courseCursor.getString(courseCursor.getColumnIndexOrThrow("duration")),
                        courseCursor.getFloat(courseCursor.getColumnIndexOrThrow("price")),
                        courseCursor.getString(courseCursor.getColumnIndexOrThrow("type")),
                        courseCursor.getString(courseCursor.getColumnIndexOrThrow("description")),
                        courseCursor.getLong(courseCursor.getColumnIndexOrThrow("last_modified"))
                );
                // Upload the course to Firebase
                FirebaseDatabase.getInstance().getReference("YogaCourses").child(String.valueOf(id))
                        .setValue(course, (error, ref) -> {
                            if (error == null) {
                                Log.d("MainActivity", "Uploaded course ID: " + id);
                            } else {
                                Log.e("MainActivity", "Failed to upload course ID: " + id, error.toException());
                            }
                        });
            } while (courseCursor.moveToNext());
        }
        courseCursor.close();

        // Upload ClassInstances
        Cursor instanceCursor = helper.readAllClassInstances();
        if (instanceCursor.moveToFirst()) {
            hasData = true;
            do {
                // Create a ClassInstance object from the cursor
                long id = instanceCursor.getLong(instanceCursor.getColumnIndexOrThrow("_id"));
                ClassInstance instance = new ClassInstance(
                        id,
                        instanceCursor.getLong(instanceCursor.getColumnIndexOrThrow("course_id")),
                        instanceCursor.getString(instanceCursor.getColumnIndexOrThrow("date")),
                        instanceCursor.getString(instanceCursor.getColumnIndexOrThrow("teacher")),
                        instanceCursor.getString(instanceCursor.getColumnIndexOrThrow("comments")),
                        instanceCursor.getLong(instanceCursor.getColumnIndexOrThrow("last_modified"))
                );
                // Upload the class instance to Firebase
                FirebaseDatabase.getInstance().getReference("ClassInstances").child(String.valueOf(id))
                        .setValue(instance, (error, ref) -> {
                            if (error == null) {
                                Log.d("MainActivity", "Uploaded class instance ID: " + id);
                            } else {
                                Log.e("MainActivity", "Failed to upload class instance ID: " + id, error.toException());
                            }
                        });
            } while (instanceCursor.moveToNext());
        }
        instanceCursor.close();

        if (hasData) {
            Toast.makeText(this, "Courses and class instances uploaded to cloud", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No data to upload", Toast.LENGTH_SHORT).show();
        }
    }

    // Set up listeners to sync Firebase data with the local database
    private void setupFirebaseListeners() {
        // Listener for YogaCourses
        FirebaseDatabase.getInstance().getReference("YogaCourses").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Iterate through each course in the Firebase snapshot
                for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                    // Deserialize the course data
                    YogaCourse course = courseSnapshot.getValue(YogaCourse.class);
                    if (course != null) {
                        // Set the course ID from the Firebase key
                        course.setId(Long.parseLong(courseSnapshot.getKey()));
                        Log.d("MainActivity", "Firebase sync: Course ID " + courseSnapshot.getKey() +
                                ", Day: " + course.getDayofweek() + ", Time: " + course.getTime());
                        // Sync the course to the local database
                        helper.syncFromFirebase(course);
                    } else {
                        Log.w("MainActivity", "Failed to deserialize course ID: " + courseSnapshot.getKey());
                    }
                }
                // Reload courses to reflect changes
                loadCourses();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Log error if the Firebase sync fails
                Log.e("MainActivity", "Firebase sync failed for courses", error.toException());
            }
        });

        // Listener for ClassInstances
        FirebaseDatabase.getInstance().getReference("ClassInstances").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Iterate through each class instance in the Firebase snapshot
                for (DataSnapshot instanceSnapshot : snapshot.getChildren()) {
                    // Deserialize the class instance data
                    ClassInstance instance = instanceSnapshot.getValue(ClassInstance.class);
                    if (instance != null) {
                        // Set the instance ID from the Firebase key
                        instance.setId(Long.parseLong(instanceSnapshot.getKey()));
                        Log.d("MainActivity", "Firebase sync: ClassInstance ID " + instanceSnapshot.getKey() +
                                ", Course ID: " + instance.getCourseId() + ", Date: " + instance.getDate());
                        // Sync the class instance to the local database
                        helper.syncFromFirebase(instance);
                    } else {
                        Log.w("MainActivity", "Failed to deserialize class instance ID: " + instanceSnapshot.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Log error if the Firebase sync fails
                Log.e("MainActivity", "Firebase sync failed for class instances", error.toException());
            }
        });
    }

    // Show a confirmation dialog to delete a yoga course
    private void showDeleteDialog(long courseId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete this course and all its instances?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete the course and reload the course list
                    helper.deleteYogaCourse(courseId);
                    loadCourses();
                    Toast.makeText(this, "Course deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Custom adapter to display yoga courses in the ListView
    class YogaCourseCursorAdapter extends ResourceCursorAdapter {
        public YogaCourseCursorAdapter(Context context, int layout, Cursor cursor, int flags) {
            super(context, layout, cursor, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Bind course data to the ListView item views
            TextView tvDayOfWeek = view.findViewById(R.id.tvDayOfWeek);
            TextView tvTime = view.findViewById(R.id.tvTime);
            TextView tvCapacity = view.findViewById(R.id.tvCapacity);
            TextView tvDuration = view.findViewById(R.id.tvDuration);
            TextView tvType = view.findViewById(R.id.tvType);
            TextView tvDescription = view.findViewById(R.id.tvDescription);
            TextView tvPrice = view.findViewById(R.id.tvPrice);

            tvDayOfWeek.setText(cursor.getString(cursor.getColumnIndexOrThrow("dayofweek")));
            tvTime.setText(cursor.getString(cursor.getColumnIndexOrThrow("time")));
            tvCapacity.setText(String.format("Capacity: %.0f", cursor.getFloat(cursor.getColumnIndexOrThrow("capacity"))));
            tvDuration.setText(cursor.getString(cursor.getColumnIndexOrThrow("duration")));
            tvType.setText(cursor.getString(cursor.getColumnIndexOrThrow("type")));
            tvDescription.setText(cursor.getString(cursor.getColumnIndexOrThrow("description")));
            tvPrice.setText(String.format("Price: $%.2f", cursor.getFloat(cursor.getColumnIndexOrThrow("price"))));

            // Set up edit and delete buttons for each course
            Button btnEdit = view.findViewById(R.id.btnEdit);
            Button btnDelete = view.findViewById(R.id.btnDelete);
            long courseId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));

            btnEdit.setOnClickListener(v -> {
                // Navigate to EditYogaCourse activity
                Intent intent = new Intent(context, EditYogaCourse.class);
                intent.putExtra("courseId", courseId);
                context.startActivity(intent);
            });

            btnDelete.setOnClickListener(v -> showDeleteDialog(courseId));
        }
    }
}