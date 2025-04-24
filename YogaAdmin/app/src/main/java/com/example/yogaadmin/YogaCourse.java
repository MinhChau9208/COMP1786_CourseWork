package com.example.yogaadmin;

public class YogaCourse {
    private long id; // Unique ID of the course
    private String dayofweek; // Day of the week the course is held
    private String time; // Time of the course
    private float capacity; // Maximum number of participants
    private String duration; // Duration of the course
    private float price; // Price of the course
    private String type; // Type of yoga (e.g., Hatha, Vinyasa)
    private String description; // Description of the course
    private long lastModified; // Timestamp of last modification

    // Default constructor for Firebase deserialization
    public YogaCourse() {}

    // Constructor with all fields
    public YogaCourse(long id, String dayofweek, String time, float capacity, String duration, float price, String type, String description, long lastModified) {
        this.id = id;
        this.dayofweek = dayofweek != null ? dayofweek : "";
        this.time = time != null ? time : "";
        this.capacity = capacity;
        this.duration = duration != null ? duration : "";
        this.price = price;
        this.type = type != null ? type : "";
        this.description = description != null ? description : "";
        this.lastModified = lastModified;
    }

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getDayofweek() { return dayofweek != null ? dayofweek : ""; }

    public String getTime() { return time != null ? time : ""; }
    public void setTime(String time) { this.time = time; }
    public float getCapacity() { return capacity; }
    public String getDuration() { return duration != null ? duration : ""; }
    public float getPrice() { return price; }
    public String getType() { return type != null ? type : ""; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description != null ? description : ""; }
    public long getLastModified() { return lastModified; }
}