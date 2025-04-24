package com.example.yogaadmin;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cursoradapter.widget.ResourceCursorAdapter;

public class ManageClassInstancesActivity extends AppCompatActivity {

    private long courseId; // ID of the course whose instances are being managed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_class_instances);

        // Get course ID from intent
        courseId = getIntent().getLongExtra("courseId", -1);
        if (courseId == -1) {
            Toast.makeText(this, "Invalid course ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set course title (day and time)
        Cursor courseCursor = MainActivity.helper.readYogaCourseById(courseId);
        if (courseCursor.moveToFirst()) {
            String dayOfWeek = courseCursor.getString(courseCursor.getColumnIndexOrThrow("dayofweek"));
            String time = courseCursor.getString(courseCursor.getColumnIndexOrThrow("time"));
            TextView tvCourseTitle = findViewById(R.id.tvCourseTitle);
            tvCourseTitle.setText(dayOfWeek + " at " + time);
        } else {
            Toast.makeText(this, "Course not found", Toast.LENGTH_SHORT).show();
            finish();
        }
        courseCursor.close();

        // Set up button to add a new class instance
        findViewById(R.id.btnAddInstance).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddClassInstanceActivity.class);
            intent.putExtra("courseId", courseId);
            startActivity(intent);
        });

        loadClassInstances();
    }

    // Reload class instances when the activity resumes
    @Override
    protected void onResume() {
        super.onResume();
        loadClassInstances();
    }

    // Load class instances for the course and display them in a ListView
    private void loadClassInstances() {
        Cursor cursor = MainActivity.helper.readClassInstancesByCourseId(courseId);
        Log.d("ManageClassInstances", "Cursor count: " + (cursor != null ? cursor.getCount() : "null"));
        ClassInstanceCursorAdapter adapter = new ClassInstanceCursorAdapter(this, R.layout.class_instance_item, cursor, 0);
        ListView lv = findViewById(R.id.lvClassInstances);
        lv.setAdapter(adapter);
        // Handle clicks to edit a class instance
        lv.setOnItemClickListener((parent, view, position, id) -> {
            Log.d("ManageClassInstances", "Item clicked, position: " + position + ", instanceId: " + id);
            Intent intent = new Intent(this, EditClassInstanceActivity.class);
            intent.putExtra("instanceId", id);
            intent.putExtra("courseId", courseId);
            startActivity(intent);
        });
    }

    // Custom adapter to display class instances in the ListView
    class ClassInstanceCursorAdapter extends ResourceCursorAdapter {
        public ClassInstanceCursorAdapter(Context context, int layout, Cursor cursor, int flags) {
            super(context, layout, cursor, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Bind instance data to the ListView item views
            TextView tvDate = view.findViewById(R.id.tvDate);
            TextView tvTeacher = view.findViewById(R.id.tvTeacher);
            TextView tvComments = view.findViewById(R.id.tvComments);

            tvDate.setText(cursor.getString(cursor.getColumnIndexOrThrow("date")));
            tvTeacher.setText("Teacher: " + cursor.getString(cursor.getColumnIndexOrThrow("teacher")));
            String comments = cursor.getString(cursor.getColumnIndexOrThrow("comments"));
            tvComments.setText(comments != null ? "Comments: " + comments : "No comments");

            // Set up delete button
            Button btnDelete = view.findViewById(R.id.btnDelete);
            long instanceId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            btnDelete.setOnClickListener(v -> showDeleteDialog(instanceId));
        }
    }

    // Show a confirmation dialog to delete a class instance
    private void showDeleteDialog(long instanceId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Class Instance")
                .setMessage("Are you sure you want to delete this class instance?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete the instance and reload the list
                    MainActivity.helper.deleteClassInstance(instanceId);
                    loadClassInstances();
                    Toast.makeText(this, "Class instance deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}