package com.example.sportbuddyworktogether;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BuddyPageActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText buddyIdEnterEditText, buddyNicknameEditText;
    private Button addBuddyToBuddiesButton, backToMainPageButton;
    private LinearLayout buddyListLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.buddy_page);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        buddyIdEnterEditText = findViewById(R.id.buddyIdEnterEditText);
        buddyNicknameEditText = findViewById(R.id.buddyNicknameEditText);
        addBuddyToBuddiesButton = findViewById(R.id.addBuddyButton);
        backToMainPageButton = findViewById(R.id.backToMainPageButton);
        buddyListLayout = findViewById(R.id.buddyListLayout);

        addBuddyToBuddiesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String buddyId = buddyIdEnterEditText.getText().toString().trim();
                String buddyNickname = buddyNicknameEditText.getText().toString().trim();
                if (!buddyId.isEmpty() && !buddyNickname.isEmpty()) {
                    saveBuddyToDatabase(buddyId, buddyNickname);
                } else {
                    Toast.makeText(getApplicationContext(), "Please enter both Buddy ID and Nickname", Toast.LENGTH_SHORT).show();
                }
            }
        });

        backToMainPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BuddyPageActivity.this, MainActivity.class));
                finish();
            }
        });

        loadBuddyList();
    }

    private void saveBuddyToDatabase(String buddyId, String buddyNickname) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        mDatabase.child("users").child(buddyId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    Map<String, String> buddyData = new HashMap<>();
                    buddyData.put("nickname", buddyNickname);
                    buddyData.put("id", buddyId);

                    DatabaseReference friendListRef = mDatabase.child("friendList").child(userId);
                    String key = friendListRef.push().getKey();
                    friendListRef.child(key).setValue(buddyData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getApplicationContext(), "Buddy Added", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getApplicationContext(), "Buddy Add Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(getApplicationContext(), "Invalid Buddy ID", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Error checking Buddy ID: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBuddyList() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference friendListRef = mDatabase.child("friendList").child(userId);

        friendListRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                buddyListLayout.removeAllViews();

                for (DataSnapshot buddySnapshot : dataSnapshot.getChildren()) {
                    Map<String, String> buddyData = (Map<String, String>) buddySnapshot.getValue();
                    String buddyId = buddyData.get("id");
                    String buddyNickname = buddyData.get("nickname");

                    if (buddyId != null && buddyNickname != null) {
                        View buddyView = getLayoutInflater().inflate(R.layout.buddy_item, null);
                        TextView buddyTextView = buddyView.findViewById(R.id.buddyTextView);
                        TextView streakTextView = buddyView.findViewById(R.id.streakTextView);

                        buddyTextView.setText(buddyNickname);

                        checkAndDisplayStreak(userId, buddyId, streakTextView);

                        buddyListLayout.addView(buddyView);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(BuddyPageActivity.this, "Failed to load buddy list: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndDisplayStreak(String userId, String buddyId, TextView streakTextView) {
        mDatabase.child("activities").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int streak = calculateStreak(dataSnapshot, userId, buddyId);
                streakTextView.setText("Streak: " + String.valueOf(streak));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(BuddyPageActivity.this, "Failed to check streak: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int calculateStreak(DataSnapshot dataSnapshot, String userId, String buddyId) {
        int streak = 0;
        Calendar today = Calendar.getInstance();
        Calendar tempCal = Calendar.getInstance();
        tempCal.add(Calendar.DAY_OF_YEAR, -1);  // Başlangıçta bir gün geriye git

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
            String dateString = sdf.format(tempCal.getTime());
            if (dataSnapshot.child(dateString).hasChild(userId) && dataSnapshot.child(dateString).hasChild(buddyId)) {
                streak++;
                tempCal.add(Calendar.DAY_OF_YEAR, -1);  // Bir gün daha geriye git
            } else {
                break;
            }
        }

        return streak;
    }
}
