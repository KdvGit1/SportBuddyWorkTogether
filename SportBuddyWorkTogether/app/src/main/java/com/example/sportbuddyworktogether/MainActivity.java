package com.example.sportbuddyworktogether;

import com.example.sportbuddyworktogether.SportActivity;
import com.applandeo.materialcalendarview.EventDay;
import java.util.Calendar;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.applandeo.materialcalendarview.CalendarDay;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.applandeo.materialcalendarview.listeners.OnDayClickListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextView welcomeTextView;
    private EditText durationEditText;
    private Button addActivityButton, manageFriendsButton, logoutButton, updateCalender;
    private Spinner activityTypeSpinner;
    private List<CalendarDay> calendarDays = new ArrayList<>();
    private CalendarView calendarView;
    private TextView activityInfoTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Burada CalendarView'i initialize ediyoruz.
        calendarView = findViewById(R.id.calendarView);

        mAuth = FirebaseAuth.getInstance();
        // Initialize Firebase Realtime Database reference
        mDatabase = FirebaseDatabase.getInstance().getReference();

        welcomeTextView = findViewById(R.id.welcomeTextView);
        activityTypeSpinner = findViewById(R.id.activityTypeSpinner);
        durationEditText = findViewById(R.id.durationEditText);
        addActivityButton = findViewById(R.id.addActivityButton);
        manageFriendsButton = findViewById(R.id.manageFriendsButton);
        logoutButton = findViewById(R.id.logoutButton);
        updateCalender = findViewById(R.id.updateCalender);
        activityInfoTextView = findViewById(R.id.activityInfoTextView);

        setupActivityTypeSpinner();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            welcomeTextView.setText("Welcome, " + currentUser.getUid());
        }

        welcomeTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Kullanıcı ID'sini al
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                // Clipboard yöneticisini al
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

                // Metni ClipData nesnesine dönüştür
                ClipData clip = ClipData.newPlainText("User ID", userId);

                // Metni panoya kopyala
                clipboard.setPrimaryClip(clip);

                // Kullanıcıya bilgi ver
                Toast.makeText(MainActivity.this, "User ID copied to clipboard", Toast.LENGTH_SHORT).show();

                return true; // Olayın tüketildiğini belirt
            }
        });

        calendarView.setOnDayClickListener(new OnDayClickListener() {
            @Override
            public void onDayClick(EventDay eventDay) {
                Calendar clickedDayCalendar = eventDay.getCalendar();
                String clickedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(clickedDayCalendar.getTime());
                loadActivitiesForDate(clickedDate);
            }
        });

        addActivityButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                // TODO: Implement add activity functionality
                if (!durationEditText.getText().toString().isEmpty() && (Integer.parseInt(durationEditText.getText().toString()) > 0)) {
                    saveActivityToDatabase(activityTypeSpinner.getSelectedItem().toString(),Integer.parseInt(durationEditText.getText().toString()));
                    markCalendarDays();
                } else {
                    Toast.makeText(getApplicationContext(), "Duration girilmedi veya anlamsız bir değer aldı", Toast.LENGTH_SHORT).show();
                    markCalendarDays();
                }
            }
        });

        manageFriendsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement manage friends functionality
                startActivity(new Intent(MainActivity.this, BuddyPageActivity.class));
                finish();
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });

        updateCalender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markCalendarDays();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        markCalendarDays();
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void saveActivityToDatabase(String type, int duration) {
        // Kullanıcı ID'sini al
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Tarih formatını ayarla (YYYY-MM-DD)
        String date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Yeni bir aktivite oluştur
        SportActivity activity = new SportActivity(userId, type, duration, date);

        // Veritabanı referansını oluştur
        DatabaseReference dateRef = mDatabase.child("activities").child(date);
        DatabaseReference userRef = dateRef.child(userId);

        // Veriyi kaydet
        String key = userRef.push().getKey();
        userRef.child(key).setValue(activity)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Başarılı kayıt
                        Toast.makeText(getApplicationContext(), "Aktivite kaydedildi", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Hata durumu
                        Toast.makeText(getApplicationContext(), "Kayıt başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void setupActivityTypeSpinner() {
        String[] activityTypes = {"Fitness", "Yoga", "Walking/Running", "Bike", "Swimming"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activityTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activityTypeSpinner.setAdapter(adapter);
    }

    private void markCalendarDays() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        mDatabase.child("activities").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<EventDay> events = new ArrayList<>();

                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    String date = dateSnapshot.getKey();

                    // Kullanıcının aktivitelerini kontrol et
                    if (dateSnapshot.hasChild(userId)) {
                        try {
                            // Tarihi CalendarDay formatına çevir
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            Date activityDate = sdf.parse(date);

                            if (activityDate != null) {
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(activityDate);

                                // EventDay oluştur ve mini_flame.xml ikonu ile işaretle
                                EventDay eventDay = new EventDay(calendar, R.drawable.mini_flame);
                                events.add(eventDay);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                // Eklenen olay sayısını kontrol et
                Toast.makeText(getApplicationContext(), "Total events: " + events.size(), Toast.LENGTH_SHORT).show();

                // Takvimde günleri işaretle
                calendarView.setEvents(events);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Veri alınamadı: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loadActivitiesForDate(String date) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference activitiesRef = mDatabase.child("activities").child(date).child(userId);

        activitiesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                StringBuilder infoBuilder = new StringBuilder();
                infoBuilder.append("Activities for ").append(date).append(":\n\n");
                int totalDuration = 0;

                for (DataSnapshot activitySnapshot : dataSnapshot.getChildren()) {
                    SportActivity activity = activitySnapshot.getValue(SportActivity.class);
                    if (activity != null) {
                        infoBuilder.append("Type: ").append(activity.type)
                                .append(", Duration: ").append(activity.duration)
                                .append(" minutes\n");
                        totalDuration += activity.duration;
                    }
                }

                if (totalDuration > 0) {
                    infoBuilder.append("\nTotal Duration: ").append(totalDuration).append(" minutes");
                    activityInfoTextView.setText(infoBuilder.toString());
                } else {
                    activityInfoTextView.setText("No activities found for " + date);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                activityInfoTextView.setText("Error loading activities: " + databaseError.getMessage());
            }
        });
    }

}
