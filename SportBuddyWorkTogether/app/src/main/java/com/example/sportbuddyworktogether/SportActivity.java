package com.example.sportbuddyworktogether;

public class SportActivity {
    public String userId;
    public String type;
    public int duration;
    public String date;

    // Boş constructor ekliyoruz
    public SportActivity() {
        // Firebase için gerekli
    }

    public SportActivity(String userId, String type, int duration, String date) {
        this.userId = userId;
        this.type = type;
        this.duration = duration;
        this.date = date;
    }

    // Getter ve setter metodları da ekleyebilirsiniz
}