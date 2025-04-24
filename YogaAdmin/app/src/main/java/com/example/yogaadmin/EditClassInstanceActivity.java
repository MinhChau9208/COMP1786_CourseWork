package com.example.yogaadmin;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EditClassInstanceActivity extends AppCompatActivity {

    private long instanceId, courseId; // IDs for the class instance and its associated course
    private Spinner spDate; // Spinner for selecting the date
    private EditText etTeacher, etComments; // Input fields for teacher and comments

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_class_instance); // Reuse layout from AddClassInstanceActivity

        // Retrieve instanceId and courseId from the intent
        instanceId = getIntent().getLongExtra("instanceId", -1);
        courseId = getIntent().getLongExtra("courseId", -1);

        // Validate the IDs
        if (instanceId == -1 || courseId == -1) {
            Toast.makeText(this, "Invalid instance or course ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        spDate = findViewById(R.id.spDate);
        etTeacher = findViewById(R.id.etTeacher);
        etComments = findViewById(R.id.etComments);

        // Update button text to "Update"
        Button btnUpdate = findViewById(R.id.btnAdd);
        if (btnUpdate == null) {
            Log.e("EditClassInstance", "Button with ID btnAdd not found");
            Toast.makeText(this, "UI error: Button not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        btnUpdate.setText("Update");
        btnUpdate.setOnClickListener(this::onUpdateClassInstance); // Set click listener to update the instance

        loadInstanceData();
        setupDateSpinner();
    }

    // Load data for the class instance from the database
    private void loadInstanceData() {
        Cursor cursor = MainActivity.helper.readClassInstanceById(instanceId);
        if (cursor.moveToFirst()) {
            // Populate the teacher and comments fields
            etTeacher.setText(cursor.getString(cursor.getColumnIndexOrThrow("teacher")));
            String comments = cursor.getString(cursor.getColumnIndexOrThrow("comments"));
            etComments.setText(comments != null ? comments : "");
        } else {
            Toast.makeText(this, "Class instance not found", Toast.LENGTH_SHORT).show();
            finish();
        }
        cursor.close();
    }

    // Set up the date spinner with dates matching the course's day of the week
    private void setupDateSpinner() {
        // Retrieve the course's day of the week
        Cursor courseCursor = MainActivity.helper.readYogaCourseById(courseId);
        String dayOfWeek = "";
        if (courseCursor.moveToFirst()) {
            dayOfWeek = courseCursor.getString(courseCursor.getColumnIndexOrThrow("dayofweek"));
        } else {
            Toast.makeText(this, "Course not found", Toast.LENGTH_SHORT).show();
            finish();
        }
        courseCursor.close();

        // Generate a list of the next 10 dates matching the day of the week
        List<String> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        int targetDay = getDayOfWeekNumber(dayOfWeek);

        // Find the next 10 dates matching the day of week
        for (int i = 0; i < 10; i++) {
            while (calendar.get(Calendar.DAY_OF_WEEK) != targetDay) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
            dates.add(sdf.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_MONTH, 7); // Move to the next week
        }

        // Set up the spinner adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDate.setAdapter(adapter);

        // Set the current date in the spinner
        Cursor instanceCursor = MainActivity.helper.readClassInstanceById(instanceId);
        if (instanceCursor.moveToFirst()) {
            String currentDate = instanceCursor.getString(instanceCursor.getColumnIndexOrThrow("date"));
            for (int i = 0; i < dates.size(); i++) {
                if (dates.get(i).equals(currentDate)) {
                    spDate.setSelection(i);
                    break;
                }
            }
        }
        instanceCursor.close();
    }

    // Convert day of week string to Calendar constant
    private int getDayOfWeekNumber(String dayOfWeek) {
        switch (dayOfWeek.toLowerCase()) {
            case "sunday": return Calendar.SUNDAY;
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return Calendar.MONDAY;
        }
    }

    // Update the class instance in the database
    private void onUpdateClassInstance(View v) {
        String date = spDate.getSelectedItem().toString();
        String teacher = etTeacher.getText().toString().trim();
        String comments = etComments.getText().toString().trim();

        // Validate required fields
        if (date.isEmpty() || teacher.isEmpty()) {
            Toast.makeText(this, "Date and Teacher are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the class instance in the database
        MainActivity.helper.updateClassInstance(instanceId, date, teacher, comments);
        Toast.makeText(this, "Class instance updated", Toast.LENGTH_SHORT).show();
        finish();
    }
}