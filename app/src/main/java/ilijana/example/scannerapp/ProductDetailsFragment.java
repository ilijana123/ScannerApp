package ilijana.example.scannerapp;

import static ilijana.example.scannerapp.R.*;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDetailsFragment extends Fragment {
    private String productName, barcode, imageUrl;
    private List<String> allergens;
    private ArrayList<HashMap<String, Float>> nutrimentsList;
    private ImageButton backButton;
    private TextView productNameText, barcodeText, allergyWarningText;
    private RecyclerView allergensRecyclerView;
    PieChart nutritionalPieChart;
    private FirebaseAuth auth;
    private DatabaseReference userAllergyRef;
    private CardView allergyCard;
    private static final Map<String, String> ALLERGY_VARIANTS = new HashMap<>();

    static {
        ALLERGY_VARIANTS.put("Egg", "Eggs");
        ALLERGY_VARIANTS.put("Soy", "Soybeans");
        ALLERGY_VARIANTS.put("Nut", "Nuts");
        ALLERGY_VARIANTS.put("Peanut", "Peanuts");
        ALLERGY_VARIANTS.put("Milk", "Milk");
        ALLERGY_VARIANTS.put("Wheat", "Wheat");
    }


    public static ProductDetailsFragment newInstance(String name, String barcode,
                                                     List<String> allergens, ArrayList<HashMap<String, Float>> nutrimentsList) {
        ProductDetailsFragment fragment = new ProductDetailsFragment();
        Bundle args = new Bundle();
        args.putString("product_name", name);
        args.putString("barcode", barcode);
        args.putStringArrayList("allergens", new ArrayList<>(allergens));
        args.putSerializable("nutriments", nutrimentsList);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            productName = getArguments().getString("product_name", "Unknown");
            barcode = getArguments().getString("barcode", "Unknown");
            allergens = getArguments().getStringArrayList("allergens");
            nutrimentsList = (ArrayList<HashMap<String, Float>>) getArguments().getSerializable("nutriments");
        }

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            userAllergyRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(auth.getCurrentUser().getUid())
                    .child("allergies");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_details, container, false);

        productNameText = view.findViewById(R.id.product_name);
        barcodeText = view.findViewById(R.id.product_code);
        allergyWarningText = view.findViewById(R.id.allergy_warning);
        allergyCard = view.findViewById(id.allergy_card);
        nutritionalPieChart = view.findViewById(R.id.nutritional_pie_chart);
        backButton = view.findViewById(R.id.back_button);
        setupNutritionalPieChart(nutritionalPieChart, nutrimentsList);
        productNameText.setText(productName);
        barcodeText.setText("Barcode: " + barcode);
        fetchUserAllergies();
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        return view;
    }
    private void setupNutritionalPieChart(PieChart pieChart, ArrayList<HashMap<String, Float>> nutrimentsList) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        for (HashMap<String, Float> nutriment : nutrimentsList) {
            for (Map.Entry<String, Float> entry : nutriment.entrySet()) {
                if (entry.getValue() > 0) {
                    entries.add(new PieEntry(entry.getValue(), entry.getKey().replace("_", " ")));
                }
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "Nutritional Breakdown");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setWordWrapEnabled(true);
        legend.setTextSize(12f);
        legend.setFormSize(12f);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setYOffset(10f);

        pieChart.setExtraBottomOffset(50f);
        pieChart.setExtraTopOffset(10f);
        pieChart.setExtraLeftOffset(10f);
        pieChart.setExtraRightOffset(10f);
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.animateY(1000);

        pieChart.invalidate();
    }


    private void fetchUserAllergies() {
        if (userAllergyRef == null) return;

        userAllergyRef.get().addOnSuccessListener(dataSnapshot -> {
            List<String> userAllergies = new ArrayList<>();
            if (dataSnapshot.exists()) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    userAllergies.add(snapshot.getValue(String.class));
                }
            }
            checkForAllergenMatch(userAllergies);
        }).addOnFailureListener(e -> Log.e("Firebase Error", "Failed to fetch allergies", e));
    }

    private void checkForAllergenMatch(List<String> userAllergies) {
        List<String> foundAllergens = new ArrayList<>();

        for (String userAllergen : userAllergies) {
            String standardizedAllergen = ALLERGY_VARIANTS.getOrDefault(userAllergen, userAllergen);

            for (String productAllergen : allergens) {
                if (productAllergen.equalsIgnoreCase(standardizedAllergen) ||
                        productAllergen.equalsIgnoreCase(userAllergen)) {
                    foundAllergens.add(productAllergen);
                    break;
                }
            }
        }

        if (!foundAllergens.isEmpty()) {
            showAllergyWarning(foundAllergens);
        }
    }

    private void showAllergyWarning(List<String> foundAllergens) {
        String message;
        int cardColor;

        if (!foundAllergens.isEmpty()) {
            message = "⚠ Warning! This product contains: " + String.join(", ", foundAllergens);
            cardColor = Color.RED;
        } else {
            message = "✅ Safe to consume";
            cardColor = Color.GREEN;
        }

        allergyWarningText.setText(message);
        allergyWarningText.setVisibility(View.VISIBLE);
        allergyWarningText.setTextColor(Color.WHITE);
        allergyCard.setCardBackgroundColor(cardColor);
    }

}
