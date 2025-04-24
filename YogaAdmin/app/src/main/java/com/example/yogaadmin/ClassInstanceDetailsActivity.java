package com.example.yogaadmin;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ClassInstanceDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_instance_details); // Set the layout for displaying class instance details

        // Retrieve instanceId from the intent
        long instanceId = getIntent().getLongExtra("instanceId", -1);
        if (instanceId == -1) {
            finish();
            return;
        }

        // Initialize UI components for displaying details
        TextView tvTeacher = findViewById(R.id.tvTeacher);
        TextView tvDate = findViewById(R.id.tvDate);
        TextView tvComments = findViewById(R.id.tvComments);
        TextView tvCourseDetails = findViewById(R.id.tvCourseDetails);

        // Query all class instances to find the one with the specified ID
        Cursor cursor = MainActivity.helper.searchClassInstances(null, null);
        if (cursor.moveToPosition(getPositionForId(cursor, instanceId))) {
            // Populate the UI with class instance details
            tvTeacher.setText(cursor.getString(cursor.getColumnIndexOrThrow("teacher")));
            tvDate.setText(cursor.getString(cursor.getColumnIndexOrThrow("date")));
            String comments = cursor.getString(cursor.getColumnIndexOrThrow("comments"));
            tvComments.setText(comments != null ? comments : "No comments");

            // Format and display course details associated with the class instance
            String courseDetails = String.format("Course: %s at %s\nType: %s\nCapacity: %.0f\nDuration: %s\nPrice: $%.2f\nDescription: %s",
                    cursor.getString(cursor.getColumnIndexOrThrow("dayofweek")),
                    cursor.getString(cursor.getColumnIndexOrThrow("time")),
                    cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    cursor.getFloat(cursor.getColumnIndexOrThrow("capacity")),
                    cursor.getString(cursor.getColumnIndexOrThrow("duration")),
                    cursor.getFloat(cursor.getColumnIndexOrThrow("price")),
                    cursor.getString(cursor.getColumnIndexOrThrow("description")));
            tvCourseDetails.setText(courseDetails);
        }
        cursor.close();
    }

    // Find the position of a specific instanceId in the cursor
    private int getPositionForId(Cursor cursor, long id) {
        int position = 0;
        if (cursor.moveToFirst()) {
            do {
                if (cursor.getLong(cursor.getColumnIndexOrThrow("_id")) == id) {
                    return position;
                }
                position++;
            } while (cursor.moveToNext());
        }
        return -1;
    }
}