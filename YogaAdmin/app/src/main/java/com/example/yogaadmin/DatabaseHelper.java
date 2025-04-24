package com.example.yogaadmin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private SQLiteDatabase database; // Reference to the SQLite database
    private DatabaseReference firebaseCoursesRef; // Firebase reference for YogaCourses
    private DatabaseReference firebaseInstancesRef; // Firebase reference for ClassInstances

    // Constructor initializing the database
    public DatabaseHelper(Context context) {
        super(context, "YogaDB", null, 1); // Database name and version
        database = getWritableDatabase();
    }

    // Lazy initialization of Firebase reference for YogaCourses
    private DatabaseReference getFirebaseCoursesRef() {
        if (firebaseCoursesRef == null) {
            firebaseCoursesRef = FirebaseDatabase.getInstance().getReference("YogaCourses");
        }
        return firebaseCoursesRef;
    }

    // Lazy initialization of Firebase reference for ClassInstances
    private DatabaseReference getFirebaseInstancesRef() {
        if (firebaseInstancesRef == null) {
            firebaseInstancesRef = FirebaseDatabase.getInstance().getReference("ClassInstances");
        }
        return firebaseInstancesRef;
    }

    // Create database tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // Create YogaCourse table
            String CREATE_TABLE_YOGACOURSE = "CREATE TABLE YogaCourse(" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "dayofweek TEXT, time TEXT, capacity FLOAT, duration TEXT, price FLOAT, " +
                    "type TEXT, description TEXT, last_modified INTEGER)";
            db.execSQL(CREATE_TABLE_YOGACOURSE);

            // Create ClassInstance table with foreign key to YogaCourse
            String CREATE_TABLE_CLASSINSTANCE = "CREATE TABLE ClassInstance(" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "course_id INTEGER," +
                    "date TEXT," +
                    "teacher TEXT," +
                    "comments TEXT," +
                    "last_modified INTEGER," +
                    "FOREIGN KEY(course_id) REFERENCES YogaCourse(_id) ON DELETE CASCADE)";
            db.execSQL(CREATE_TABLE_CLASSINSTANCE);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error creating database", e);
        }
    }

    // Upgrade database by dropping and recreating tables
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS ClassInstance");
        db.execSQL("DROP TABLE IF EXISTS YogaCourse");
        Log.w(this.getClass().getName(), "Database upgrade to version " + newVersion + " - old data lost");
        onCreate(db);
    }

    // Create a new yoga course and sync to Firebase
    public long createNewYogaCourse(String dow, String time, float cap, String dura, float p, String type, String des) {
        ContentValues rowValues = new ContentValues();
        rowValues.put("dayofweek", dow);
        rowValues.put("time", time);
        rowValues.put("capacity", cap);
        rowValues.put("duration", dura);
        rowValues.put("price", p);
        rowValues.put("type", type);
        rowValues.put("description", des);
        rowValues.put("last_modified", System.currentTimeMillis());
        long id = database.insertOrThrow("YogaCourse", null, rowValues);
        syncCourseToFirebase(id); // Sync the new course to Firebase
        return id;
    }

    // Read all yoga courses from the database
    public Cursor readAllYogaCourse() {
        return database.query("YogaCourse",
                new String[]{"_id", "dayofweek", "time", "capacity", "duration", "price", "type", "description", "last_modified"},
                null, null, null, null, null);
    }

    // Delete a yoga course and its associated class instances
    public void deleteYogaCourse(long id) {
        database.delete("YogaCourse", "_id=?", new String[]{String.valueOf(id)});
        getFirebaseCoursesRef().child(String.valueOf(id)).removeValue(); // Remove from Firebase
        // Delete associated class instances in Firebase
        getFirebaseInstancesRef().orderByChild("courseId").equalTo(id)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                        for (com.google.firebase.database.DataSnapshot instanceSnapshot : snapshot.getChildren()) {
                            instanceSnapshot.getRef().removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {
                        Log.e("DatabaseHelper", "Failed to delete class instances for course ID: " + id, error.toException());
                    }
                });
    }

    // Reset the entire database
    public void resetDatabase() {
        database.execSQL("DELETE FROM YogaCourse");
        database.execSQL("DELETE FROM ClassInstance");
        getFirebaseCoursesRef().removeValue(); // Clear Firebase YogaCourses
        getFirebaseInstancesRef().removeValue(); // Clear Firebase ClassInstances
    }

    // Read a specific yoga course by ID
    public Cursor readYogaCourseById(long id) {
        return database.query("YogaCourse",
                new String[]{"_id", "dayofweek", "time", "capacity", "duration", "price", "type", "description", "last_modified"},
                "_id=?", new String[]{String.valueOf(id)}, null, null, null);
    }

    // Update an existing yoga course and sync to Firebase
    public void updateYogaCourse(long id, String dow, String time, float cap, String dura, float price, String type, String des) {
        ContentValues values = new ContentValues();
        values.put("dayofweek", dow);
        values.put("time", time);
        values.put("capacity", cap);
        values.put("duration", dura);
        values.put("price", price);
        values.put("type", type);
        values.put("description", des);
        values.put("last_modified", System.currentTimeMillis());
        database.update("YogaCourse", values, "_id=?", new String[]{String.valueOf(id)});
        syncCourseToFirebase(id); // Sync updated course to Firebase
    }

    // Create a new class instance and sync to Firebase
    public long createClassInstance(long courseId, String date, String teacher, String comments) {
        ContentValues values = new ContentValues();
        values.put("course_id", courseId);
        values.put("date", date);
        values.put("teacher", teacher);
        values.put("comments", comments);
        values.put("last_modified", System.currentTimeMillis());
        long id = database.insertOrThrow("ClassInstance", null, values);
        syncClassInstanceToFirebase(id); // Sync the new instance to Firebase
        return id;
    }

    // Read class instances for a specific course
    public Cursor readClassInstancesByCourseId(long courseId) {
        return database.query("ClassInstance",
                new String[]{"_id", "course_id", "date", "teacher", "comments", "last_modified"},
                "course_id=?", new String[]{String.valueOf(courseId)}, null, null, "date ASC");
    }

    // Read all class instances
    public Cursor readAllClassInstances() {
        return database.query("ClassInstance",
                new String[]{"_id", "course_id", "date", "teacher", "comments", "last_modified"},
                null, null, null, null, null);
    }

    // Delete a class instance
    public void deleteClassInstance(long id) {
        database.delete("ClassInstance", "_id=?", new String[]{String.valueOf(id)});
        getFirebaseInstancesRef().child(String.valueOf(id)).removeValue(); // Remove from Firebase
    }

    // Update an existing class instance and sync to Firebase
    public void updateClassInstance(long id, String date, String teacher, String comments) {
        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("teacher", teacher);
        values.put("comments", comments);
        values.put("last_modified", System.currentTimeMillis());
        database.update("ClassInstance", values, "_id=?", new String[]{String.valueOf(id)});
        syncClassInstanceToFirebase(id); // Sync updated instance to Firebase
    }

    // Read a specific class instance by ID
    public Cursor readClassInstanceById(long id) {
        return database.query("ClassInstance",
                new String[]{"_id", "course_id", "date", "teacher", "comments", "last_modified"},
                "_id=?", new String[]{String.valueOf(id)}, null, null, null);
    }

    // Search class instances by teacher name and/or date
    public Cursor searchClassInstances(String teacherName, String date) {
        String query = "SELECT ci._id, ci.course_id, ci.date, ci.teacher, ci.comments, " +
                "yc.dayofweek, yc.time, yc.capacity, yc.duration, yc.price, yc.type, yc.description " +
                "FROM ClassInstance ci " +
                "JOIN YogaCourse yc ON ci.course_id = yc._id " +
                "WHERE 1=1";
        List<String> params = new ArrayList<>();

        if (teacherName != null && !teacherName.trim().isEmpty()) {
            query += " AND ci.teacher LIKE ?";
            params.add(teacherName.trim() + "%");
        }
        if (date != null && !date.trim().isEmpty()) {
            query += " AND ci.date = ?";
            params.add(date.trim());
        }

        return database.rawQuery(query, params.toArray(new String[0]));
    }

    // Sync a yoga course to Firebase
    private void syncCourseToFirebase(long id) {
        Cursor cursor = readYogaCourseById(id);
        if (cursor.moveToFirst()) {
            // Create a YogaCourse object from the cursor
            YogaCourse course = new YogaCourse(
                    id,
                    cursor.getString(cursor.getColumnIndexOrThrow("dayofweek")),
                    cursor.getString(cursor.getColumnIndexOrThrow("time")),
                    cursor.getFloat(cursor.getColumnIndexOrThrow("capacity")),
                    cursor.getString(cursor.getColumnIndexOrThrow("duration")),
                    cursor.getFloat(cursor.getColumnIndexOrThrow("price")),
                    cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("last_modified"))
            );
            // Upload the course to Firebase
            getFirebaseCoursesRef().child(String.valueOf(id)).setValue(course, (error, ref) -> {
                if (error != null) {
                    Log.e("DatabaseHelper", "Failed to sync course ID: " + id, error.toException());
                } else {
                    Log.d("DatabaseHelper", "Synced course ID: " + id);
                }
            });
        }
        cursor.close();
    }

    // Sync a class instance to Firebase
    private void syncClassInstanceToFirebase(long id) {
        Cursor cursor = readClassInstanceById(id);
        if (cursor.moveToFirst()) {
            // Create a ClassInstance object from the cursor
            ClassInstance instance = new ClassInstance(
                    id,
                    cursor.getLong(cursor.getColumnIndexOrThrow("course_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    cursor.getString(cursor.getColumnIndexOrThrow("teacher")),
                    cursor.getString(cursor.getColumnIndexOrThrow("comments")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("last_modified"))
            );
            // Upload the instance to Firebase
            getFirebaseInstancesRef().child(String.valueOf(id)).setValue(instance, (error, ref) -> {
                if (error != null) {
                    Log.e("DatabaseHelper", "Failed to sync class instance ID: " + id, error.toException());
                } else {
                    Log.d("DatabaseHelper", "Synced class instance ID: " + id);
                }
            });
        }
        cursor.close();
    }

    // Sync a yoga course from Firebase to the local database
    public void syncFromFirebase(YogaCourse course) {
        ContentValues values = new ContentValues();
        values.put("_id", course.getId());
        values.put("dayofweek", course.getDayofweek());
        values.put("time", course.getTime());
        values.put("capacity", course.getCapacity());
        values.put("duration", course.getDuration());
        values.put("price", course.getPrice());
        values.put("type", course.getType());
        values.put("description", course.getDescription());
        values.put("last_modified", course.getLastModified());
        database.replace("YogaCourse", null, values); // Replace or insert the course
    }

    // Sync a class instance from Firebase to the local database
    public void syncFromFirebase(ClassInstance instance) {
        ContentValues values = new ContentValues();
        values.put("_id", instance.getId());
        values.put("course_id", instance.getCourseId());
        values.put("date", instance.getDate());
        values.put("teacher", instance.getTeacher());
        values.put("comments", instance.getComments());
        values.put("last_modified", instance.getLastModified());
        database.replace("ClassInstance", null, values); // Replace or insert the instance
    }
}