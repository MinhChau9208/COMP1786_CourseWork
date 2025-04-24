package com.example.yogaadmin;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class EditYogaCourse extends AppCompatActivity {

    private long courseId; // ID of the course being edited
    private EditText etCapacity, etPrice, etDescription; // Input fields for capacity, price, and description
    private Spinner spDays, spTime, spDuration, spType; // Spinners for day, time, duration, and type

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_yoga_course); // Reuse layout from CreateYogaCourse

        // Initialize UI components
        spDays = findViewById(R.id.spDays);
        spTime = findViewById(R.id.spTime);
        spDuration = findViewById(R.id.spDuration);
        spType = findViewById(R.id.spType);
        etCapacity = findViewById(R.id.spCapacity);
        etPrice = findViewById(R.id.edPrice);
        etDescription = findViewById(R.id.edmDes);

        // Get course ID from intent
        Intent intent = getIntent();
        courseId = intent.getLongExtra("courseId", -1);

        loadCourseData();
        Button btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setText("Update");
        btnAdd.setOnClickListener(this::onClickUpdateYogaCourse); // Set click listener to update the course
    }

    // Load existing course data from the database
    private void loadCourseData() {
        Cursor cursor = MainActivity.helper.readYogaCourseById(courseId);
        if (cursor.moveToFirst()) {
            // Set spinner selections
            setSpinnerSelection(spDays, cursor.getString(cursor.getColumnIndexOrThrow("dayofweek")));
            setSpinnerSelection(spTime, cursor.getString(cursor.getColumnIndexOrThrow("time")));
            setSpinnerSelection(spDuration, cursor.getString(cursor.getColumnIndexOrThrow("duration")));
            setSpinnerSelection(spType, cursor.getString(cursor.getColumnIndexOrThrow("type")));

            // Set EditText fields
            etCapacity.setText(String.valueOf(cursor.getFloat(cursor.getColumnIndexOrThrow("capacity"))));
            etPrice.setText(String.valueOf(cursor.getFloat(cursor.getColumnIndexOrThrow("price"))));
            etDescription.setText(cursor.getString(cursor.getColumnIndexOrThrow("description")));
        }
        cursor.close();
    }

    // Set the selection of a spinner based on a value
    private void setSpinnerSelection(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    // Update the yoga course in the database
    public void onClickUpdateYogaCourse(View v) {
        String dayOfWeek = spDays.getSelectedItem().toString();
        String time = spTime.getSelectedItem().toString();
        String duration = spDuration.getSelectedItem().toString();
        String type = spType.getSelectedItem().toString();
        String description = etDescription.getText().toString();
        String capacityText = etCapacity.getText().toString();
        String priceText = etPrice.getText().toString();

        // Validate required fields
        if (dayOfWeek.isEmpty() || time.isEmpty() || duration.isEmpty() || type.isEmpty() ||
                capacityText.isEmpty() || priceText.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            float capacity = Float.parseFloat(capacityText);
            float price = Float.parseFloat(priceText);

            // Update the course in the database
            MainActivity.helper.updateYogaCourse(courseId, dayOfWeek, time, capacity, duration, price, type, description);
            Toast.makeText(this, "Course updated", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NumberFormatException e) {
            // Handle invalid number format
            Toast.makeText(this, "Invalid capacity or price format", Toast.LENGTH_SHORT).show();
        }
    }
}