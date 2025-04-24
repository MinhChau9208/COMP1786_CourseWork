package com.example.yogaadmin;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddClassInstanceActivity extends AppCompatActivity {

    private long courseId; // ID of the course to which the class instance belongs
    private Spinner spDate; // Spinner for selecting the date of the class instance
    private EditText etTeacher, etComments; // Input fields for teacher name and comments

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_class_instance); // Set the layout for adding a class instance

        // Retrieve courseId from the intent
        courseId = getIntent().getLongExtra("courseId", -1);

        // Initialize UI components
        spDate = findViewById(R.id.spDate);
        etTeacher = findViewById(R.id.etTeacher);
        etComments = findViewById(R.id.etComments);

        setupDateSpinner();

        findViewById(R.id.btnAdd).setOnClickListener(this::onAddClassInstance);
    }

    // Set up the date spinner with dates that match the course's day of the week
    private void setupDateSpinner() {
        // Retrieve the course details to get the day of the week
        Cursor courseCursor = MainActivity.helper.readYogaCourseById(courseId);
        String dayOfWeek = "";
        if (courseCursor.moveToFirst()) {
            dayOfWeek = courseCursor.getString(courseCursor.getColumnIndexOrThrow("dayofweek"));
        }
        courseCursor.close();

        // Generate a list of the next 10 dates matching the course's day of the week
        List<String> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        int targetDay = getDayOfWeekNumber(dayOfWeek);

        // Find the next 10 dates that fall on the specified day of the week
        for (int i = 0; i < 10; i++) {
            while (calendar.get(Calendar.DAY_OF_WEEK) != targetDay) {
                calendar.add(Calendar.DAY_OF_MONTH, 1); // Move to the next day
            }
            dates.add(sdf.format(calendar.getTime())); // Add formatted date to the list
            calendar.add(Calendar.DAY_OF_MONTH, 7); // Move to the same day next week
        }

        // Set up the spinner adapter with the list of dates
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDate.setAdapter(adapter);
    }

    // Convert a day of the week string to a Calendar constant
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

    // Create a new class instance and save it to the database
    private void onAddClassInstance(View v) {
        // Retrieve input values
        String date = spDate.getSelectedItem().toString();
        String teacher = etTeacher.getText().toString().trim();
        String comments = etComments.getText().toString().trim();

        // Validate required fields
        if (date.isEmpty() || teacher.isEmpty()) {
            Toast.makeText(this, "Date and Teacher are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the class instance in the database
        long id = MainActivity.helper.createClassInstance(courseId, date, teacher, comments);
        if (id > 0) {
            Toast.makeText(this, "Class instance added", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to add class instance", Toast.LENGTH_SHORT).show();
        }
    }
}