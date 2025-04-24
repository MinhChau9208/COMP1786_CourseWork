package com.example.yogaadmin;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CreateYogaCourse extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_yoga_course);
        findViewById(R.id.btnAdd).setOnClickListener(this::onClickCreateYogaCourse);
    }

    // Create a new yoga course in the database
    public void onClickCreateYogaCourse(View v) {
        // Retrieve UI components
        Spinner spDays = findViewById(R.id.spDays);
        Spinner spTime = findViewById(R.id.spTime);
        Spinner spDuration = findViewById(R.id.spDuration);
        Spinner spType = findViewById(R.id.spType);
        EditText etCapacity = findViewById(R.id.spCapacity);
        EditText etPrice = findViewById(R.id.edPrice);
        EditText etDescription = findViewById(R.id.edmDes);

        // Get values with null checks
        String dayOfWeek = spDays.getSelectedItem() != null ? spDays.getSelectedItem().toString() : "";
        String time = spTime.getSelectedItem() != null ? spTime.getSelectedItem().toString() : "";
        String duration = spDuration.getSelectedItem() != null ? spDuration.getSelectedItem().toString() : "";
        String type = spType.getSelectedItem() != null ? spType.getSelectedItem().toString() : "";
        String capacityText = etCapacity.getText().toString().trim();
        String priceText = etPrice.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Validate required fields
        StringBuilder errors = new StringBuilder();
        if (dayOfWeek.isEmpty()) errors.append("Day of Week\n");
        if (time.isEmpty()) errors.append("Time\n");
        if (capacityText.isEmpty()) errors.append("Capacity\n");
        if (duration.isEmpty()) errors.append("Duration\n");
        if (type.isEmpty()) errors.append("Type\n");
        if (priceText.isEmpty()) errors.append("Price\n");

        // Show errors if any
        if (errors.length() > 0) {
            Toast.makeText(this, "Please fill in:\n" + errors, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            // Parse and create course
            float capacity = Float.parseFloat(capacityText);
            float price = Float.parseFloat(priceText);
            MainActivity.helper.createNewYogaCourse(dayOfWeek, time, capacity, duration, price, type, description);
            Toast.makeText(this, "Yoga class created", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid Capacity or Price format", Toast.LENGTH_SHORT).show();
        }
    }
}