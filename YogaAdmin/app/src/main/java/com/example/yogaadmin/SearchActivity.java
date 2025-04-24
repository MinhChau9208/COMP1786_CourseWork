package com.example.yogaadmin;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cursoradapter.widget.ResourceCursorAdapter;

public class SearchActivity extends AppCompatActivity {

    private EditText etTeacherName, etDate; // Input fields for teacher name and date
    private ListView lvResults; // ListView to display search results

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize UI components
        etTeacherName = findViewById(R.id.etTeacherName);
        etDate = findViewById(R.id.etDate);
        lvResults = findViewById(R.id.lvResults);

        // Set up search button
        findViewById(R.id.btnSearch).setOnClickListener(v -> performSearch());
    }

    // Perform a search for class instances based on teacher name and/or date
    private void performSearch() {
        String teacherName = etTeacherName.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        // Validate that at least one criterion is provided
        if (teacherName.isEmpty() && date.isEmpty()) {
            Toast.makeText(this, "Please enter at least one search criterion", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query the database for matching class instances
        Cursor cursor = MainActivity.helper.searchClassInstances(
                teacherName.isEmpty() ? null : teacherName,
                date.isEmpty() ? null : date
        );
        if (cursor.getCount() == 0) {
            Toast.makeText(this, "No matching classes found", Toast.LENGTH_SHORT).show();
            cursor.close();
            lvResults.setAdapter(null);
            return;
        }

        // Set up the adapter to display search results
        SearchResultCursorAdapter adapter = new SearchResultCursorAdapter(this, R.layout.search_result_item, cursor, 0);
        lvResults.setAdapter(adapter);
        // Handle clicks to view class instance details
        lvResults.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, ClassInstanceDetailsActivity.class);
            intent.putExtra("instanceId", id);
            startActivity(intent);
        });
    }

    // Custom adapter to display search results in the ListView
    class SearchResultCursorAdapter extends ResourceCursorAdapter {
        public SearchResultCursorAdapter(Context context, int layout, Cursor cursor, int flags) {
            super(context, layout, cursor, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Bind search result data to the ListView item views
            TextView tvTeacher = view.findViewById(R.id.tvTeacher);
            TextView tvDate = view.findViewById(R.id.tvDate);
            TextView tvCourse = view.findViewById(R.id.tvCourse);

            tvTeacher.setText(cursor.getString(cursor.getColumnIndexOrThrow("teacher")));
            tvDate.setText(cursor.getString(cursor.getColumnIndexOrThrow("date")));
            String courseInfo = cursor.getString(cursor.getColumnIndexOrThrow("dayofweek")) + " at " +
                    cursor.getString(cursor.getColumnIndexOrThrow("time")) + " (" +
                    cursor.getString(cursor.getColumnIndexOrThrow("type")) + ")";
            tvCourse.setText(courseInfo);
        }
    }
}