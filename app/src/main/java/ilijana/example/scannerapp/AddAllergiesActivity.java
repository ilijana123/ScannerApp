package ilijana.example.scannerapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.core.content.ContextCompat;
import androidx.gridlayout.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AddAllergiesActivity extends AppCompatActivity {
    private HashMap<String, Boolean> selectedAllergens = new HashMap<>();
    private Button btnNext;
    private DatabaseReference userAllergyRef;
    private FirebaseUser currentUser;
    private GridLayout allergensGrid;
    private final int selectedColor = R.color.lavender_gray;
    private final int defaultColor = android.R.color.white;
    private List<Allergen> allergens = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_allergies);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        btnNext = findViewById(R.id.btn_next);
        allergensGrid = findViewById(R.id.allergens_grid);

        allergens.add(new Allergen("Egg", R.drawable.egg));
        allergens.add(new Allergen("Milk", R.drawable.milk));
        allergens.add(new Allergen("Soy", R.drawable.soy));
        allergens.add(new Allergen("Peanut", R.drawable.peanut));
        allergens.add(new Allergen("Sesame", R.drawable.sesame));
        allergens.add(new Allergen("Crustacea", R.drawable.crab));

        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();
            userAllergyRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("allergies");

            userAllergyRef.get().addOnSuccessListener(dataSnapshot -> {
                List<String> savedAllergies = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        savedAllergies.add(snapshot.getValue(String.class));
                    }
                }
                setupAllergiesUI(savedAllergies);
            }).addOnFailureListener(e -> {
                Log.e("Firebase Error", "Failed to fetch allergies", e);
            });

        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnNext.setOnClickListener(view -> saveAllergiesToFirebase());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupAllergiesUI(List<String> savedAllergies) {
        for (Allergen allergen : allergens) {
            addCardView(allergen, savedAllergies.contains(allergen.name));
        }
    }

    private void addCardView(Allergen allergen, boolean isAlreadySelected) {
        View view = LayoutInflater.from(this).inflate(R.layout.allergen_card_layout, allergensGrid, false);

        CardView cardView = view.findViewById(R.id.card_view);
        ImageView allergenImage = view.findViewById(R.id.allergen_image);
        TextView allergenName = view.findViewById(R.id.allergen_name);

        allergenImage.setImageResource(allergen.imageResId);
        allergenName.setText(allergen.name);

        cardView.setTag(allergen.name);
        selectedAllergens.put(allergen.name, isAlreadySelected);
        cardView.setCardBackgroundColor(ContextCompat.getColor(this, isAlreadySelected ? selectedColor : defaultColor));

        cardView.setOnClickListener(v -> toggleSelection(cardView, allergen.name));

        allergensGrid.addView(view);
    }

    private void toggleSelection(CardView cardView, String allergen) {
        boolean isSelected = selectedAllergens.get(allergen);
        selectedAllergens.put(allergen, !isSelected);

        if (!isSelected) {
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, selectedColor));
        } else {
            cardView.setCardBackgroundColor(ContextCompat.getColor(this, defaultColor));
        }
    }

    private void saveAllergiesToFirebase() {
        List<String> selectedList = new ArrayList<>();
        for (String allergen : selectedAllergens.keySet()) {
            if (selectedAllergens.get(allergen)) {
                selectedList.add(allergen);
            }
        }

        if (selectedList.isEmpty()) {
            Toast.makeText(this, "Please select at least one allergen", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("Saving Allergies", "Saving to Firebase: " + selectedList);

        userAllergyRef.setValue(selectedList)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase Success", "Allergies saved: " + selectedList);
                    Toast.makeText(this, "Allergies saved successfully!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(AddAllergiesActivity.this, UserActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase Error", "Failed to save allergies", e);
                    Toast.makeText(this, "Failed to save allergies: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
