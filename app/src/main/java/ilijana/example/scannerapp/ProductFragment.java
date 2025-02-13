package ilijana.example.scannerapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProductFragment extends Fragment {
    private String productName, genericName, barcode, imageUrl, nutriGrade;
    private int nutriScore;
    private TextView nameTextView, genericNameTextView, barcodeTextView;
    private ImageView productImageView;
    private ImageButton backButton;
    private FloatingActionButton saveButton;
    private Button viewDetailsButton;
    private boolean isSaved = false;
    private DatabaseReference dbRef;
    private FirebaseAuth auth;
    private FirebaseStorage storage;

    private ArrayList<String> allergens, countries, brands, categories;
    private ArrayList<HashMap<String, Float>> nutrimentsList;

    private RecyclerView allergensRecyclerView, countriesRecyclerView, nutrimentsRecyclerView, brandsRecyclerView, categoriesRecyclerView;
    private SimpleListAdapter allergensAdapter, countriesAdapter, brandsAdapter, categoriesAdapter;
    private NutrimentsAdapter nutrimentsAdapter;

    public static ProductFragment newInstance(String name, ArrayList<String> brands, String genericName,
                                              ArrayList<String> categories, ArrayList<String> countries,
                                              String barcode, String imageUrl,
                                              ArrayList<String> allergens,
                                              ArrayList<HashMap<String, Float>> nutrimentsList,int nutriScore, String nutriGrade) {
        ProductFragment fragment = new ProductFragment();
        Bundle args = new Bundle();
        args.putString("product_name", name);
        args.putStringArrayList("brands", brands);
        args.putString("generic_name", genericName);
        args.putStringArrayList("categories", categories);
        args.putStringArrayList("countries", countries);
        args.putString("barcode", barcode);
        args.putString("image_url", imageUrl);
        args.putStringArrayList("allergens", allergens);
        args.putSerializable("nutriments", nutrimentsList);
        args.putString("nutriGrade", nutriGrade);
        args.putInt("nutriScore", nutriScore);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            productName = getArguments().getString("product_name", "Unknown");
            brands = (ArrayList<String>) getArguments().getSerializable("brands");
            categories = (ArrayList<String>) getArguments().getSerializable("categories");
            genericName = getArguments().getString("generic_name", "Unknown");
            countries = getArguments().getStringArrayList("countries");
            barcode = getArguments().getString("barcode", "Unknown");
            imageUrl = getArguments().getString("image_url", "");
            allergens = (ArrayList<String>) getArguments().getSerializable("allergens");
            nutrimentsList = (ArrayList<HashMap<String, Float>>) getArguments().getSerializable("nutriments");
            nutriScore = getArguments().getInt("nutriScore", nutriScore);
            nutriGrade = getArguments().getString("nutriGrade", nutriGrade);
        }

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getActivity(), "Please log in first!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
            return;
        }

        dbRef = FirebaseDatabase.getInstance().getReference("users")
                .child(auth.getCurrentUser().getUid())
                .child("products")
                .child(barcode);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product, container, false);
        Log.d("ProductFragment", "onCreateView: ProductFragment is being created.");
        nameTextView = view.findViewById(R.id.product_name);
        genericNameTextView = view.findViewById(R.id.product_generic_name);
        barcodeTextView = view.findViewById(R.id.product_code);
        productImageView = view.findViewById(R.id.product_image);
        saveButton = view.findViewById(R.id.save_button);
        backButton = view.findViewById(R.id.back_button);
        viewDetailsButton = view.findViewById(R.id.view_details_button);

        allergensRecyclerView = view.findViewById(R.id.allergens_list);
        nutrimentsRecyclerView = view.findViewById(R.id.nutriments_list);
        countriesRecyclerView = view.findViewById(R.id.countries_list);
        brandsRecyclerView = view.findViewById(R.id.brands_list);
        categoriesRecyclerView = view.findViewById(R.id.categories_list);

        nameTextView.setText(productName != null ? productName : "Product Name not available");
        genericNameTextView.setText(genericName != null ? genericName : "Generic Name not available");
        barcodeTextView.setText(barcode != null ? barcode : "Barcode not available");

        allergensRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        countriesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        brandsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        nutrimentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (allergens != null && !allergens.isEmpty()) {
            allergensAdapter = new SimpleListAdapter(allergens,false);
            allergensRecyclerView.setAdapter(allergensAdapter);
        }

        if (countries != null && !countries.isEmpty()) {
            Log.d("ProductFragment", "Setting Countries Adapter: " + countries);
            countriesAdapter = new SimpleListAdapter(countries, false);
            countriesRecyclerView.setAdapter(countriesAdapter);
        } else {
            Log.w("ProductFragment", "Countries list is empty or null!");
        }

        if (nutrimentsList != null) {
            nutrimentsAdapter = new NutrimentsAdapter(nutrimentsList, false);
            nutrimentsRecyclerView.setAdapter(nutrimentsAdapter);
        }

        if (categories != null && !categories.isEmpty()) {
            categoriesAdapter = new SimpleListAdapter(categories,false);
            categoriesRecyclerView.setAdapter(categoriesAdapter);
        }

        if (brands != null && !brands.isEmpty()) {
            brandsAdapter = new SimpleListAdapter(brands,false);
            brandsRecyclerView.setAdapter(brandsAdapter);
        }

        if (!imageUrl.isEmpty()) {
            Picasso.get().load(imageUrl).placeholder(R.drawable.image).into(productImageView);
        } else {
            productImageView.setImageResource(R.drawable.image);
        }

        saveButton.setOnClickListener(v -> toggleProductInDatabase());

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), UserActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        viewDetailsButton.setOnClickListener(v -> {
            ProductDetailsFragment productDetailsFragment = ProductDetailsFragment.newInstance(
                    productName, barcode, allergens, nutrimentsList
            );

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, productDetailsFragment)
                    .addToBackStack(null)
                    .commit();
        });


        checkIfProductExists();

        return view;
    }

    private void checkIfProductExists() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    isSaved = true;
                    saveButton.setImageResource(R.drawable.baseline_favorite_24);
                    saveButton.setEnabled(true);
                } else {
                    isSaved = false;
                    saveButton.setImageResource(R.drawable.baseline_favorite_border_24);
                    saveButton.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "Database error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleProductInDatabase() {
        if (isSaved) {
            dbRef.removeValue().addOnSuccessListener(aVoid -> {
                isSaved = false;
                saveButton.setImageResource(R.drawable.baseline_favorite_border_24);
                Toast.makeText(getActivity(), "Product removed from database", Toast.LENGTH_SHORT).show();
            });
        } else {
            Map<String, Object> productData = new HashMap<>();
            productData.put("product_name", productName);
            productData.put("brands", brands);
            productData.put("generic_name", genericName);
            productData.put("categories", categories);
            productData.put("countries", countries);
            productData.put("barcode", barcode);
            productData.put("image_url", imageUrl);
            productData.put("allergens", allergens);
            productData.put("nutriments", nutrimentsList);
            productData.put("nutriScore", nutriScore);
            productData.put("nutriGrade", nutriGrade);

            dbRef.setValue(productData).addOnSuccessListener(aVoid -> {
                isSaved = true;
                saveButton.setImageResource(R.drawable.baseline_favorite_24);
                Toast.makeText(getActivity(), "Product added to database", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
