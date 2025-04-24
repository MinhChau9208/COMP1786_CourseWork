package com.example.yogaadmin;

public class ClassInstance {
    private long id; // Unique ID of the class instance
    private long courseId; // ID of the associated yoga course
    private String date; // Date of the class instance
    private String teacher; // Name of the teacher for the class
    private String comments; // Additional comments for the class instance
    private long lastModified; // Timestamp of the last modification

    // Default constructor required for Firebase deserialization
    public ClassInstance() {}

    // Constructor with all fields
    public ClassInstance(long id, long courseId, String date, String teacher, String comments, long lastModified) {
        this.id = id;
        this.courseId = courseId;
        this.date = date != null ? date : "";
        this.teacher = teacher != null ? teacher : "";
        this.comments = comments != null ? comments : "";
        this.lastModified = lastModified;
    }

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getCourseId() { return courseId; }

    public String getDate() { return date != null ? date : ""; }
    public void setDate(String date) { this.date = date; }

    public String getTeacher() { return teacher != null ? teacher : ""; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getComments() { return comments != null ? comments : ""; }
    public long getLastModified() { return lastModified; }
}