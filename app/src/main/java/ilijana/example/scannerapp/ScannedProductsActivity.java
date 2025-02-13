package ilijana.example.scannerapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScannedProductsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ScannedProductAdapter adapter;
    private List<ScannedProduct> scannedProducts, filteredList;
    private EditText searchBar;
    private Spinner categorySpinner, brandSpinner;
    private DatabaseReference userScannedProductsRef;
    private FirebaseAuth auth;

    private Set<String> categoriesSet = new HashSet<>();
    private Set<String> brandsSet = new HashSet<>();
    private Set<String> countriesSet = new HashSet<>();

    private Set<String> allergensSet = new HashSet<>();
    private String selectedCategory = "All Categories";
    private String selectedBrand = "All Brands";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanned_products);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.scanned_products_recycler);
        searchBar = findViewById(R.id.search_bar);
        categorySpinner = findViewById(R.id.category_spinner);
        brandSpinner = findViewById(R.id.brand_spinner);

        scannedProducts = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new ScannedProductAdapter(this, filteredList, product -> openProductFragment(product));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            userScannedProductsRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("products");

            loadScannedProducts();
        }

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) { filterList(); }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = parent.getItemAtPosition(position).toString();
                filterList();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        brandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBrand = parent.getItemAtPosition(position).toString();
                filterList();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadScannedProducts() {
        userScannedProductsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                scannedProducts.clear();
                categoriesSet.clear();
                brandsSet.clear();

                for (DataSnapshot productSnapshot : dataSnapshot.getChildren()) {
                    String name = productSnapshot.child("product_name").getValue(String.class);
                    String imageUrl = productSnapshot.child("image_url").getValue(String.class);
                    String barcode = productSnapshot.child("barcode").getValue(String.class);
                    String nutriGrade = productSnapshot.child("nutriGrade").getValue(String.class);
                    Integer nutriScore = productSnapshot.child("nutriScore").getValue(Integer.class);
                    if (nutriScore == null) {
                        nutriScore = 0;
                    }
                    ArrayList<String> categoryList = new ArrayList<>();
                    for (DataSnapshot categorySnapshot : productSnapshot.child("categories").getChildren()) {
                        String category = categorySnapshot.getValue(String.class);
                        if (category != null) {
                            categoryList.add(category);
                            categoriesSet.add(category);
                        }
                    }
                    ArrayList<String> brandList = new ArrayList<>();
                    for (DataSnapshot brandSnapshot : productSnapshot.child("brands").getChildren()) {
                        String brand = brandSnapshot.getValue(String.class);
                        if (brand != null) {
                            brandList.add(brand);
                            brandsSet.add(brand);
                        }
                    }
                    ArrayList<String> countries = new ArrayList<>();
                    for (DataSnapshot countrySnapshot : productSnapshot.child("countries").getChildren()) {
                        String country = countrySnapshot.getValue(String.class);
                        if (country != null) {
                            countries.add(country);
                            countriesSet.add(country);
                        }
                    }
                    ArrayList<String> allergens = new ArrayList<>();
                    for (DataSnapshot allergenSnapshot : productSnapshot.child("allergens").getChildren()) {
                        String allergen = allergenSnapshot.getValue(String.class);
                        if (allergen != null) {
                            allergens.add(allergen);
                            allergensSet.add(allergen);
                        }
                    }
                    ArrayList<HashMap<String, Float>> nutrimentsList = new ArrayList<>();
                    for (DataSnapshot nutrimentSnapshot : productSnapshot.child("nutriments").getChildren()) {
                        HashMap<String, Float> nutrimentMap = new HashMap<>();
                        for (DataSnapshot nutrientData : nutrimentSnapshot.getChildren()) {
                            String key = nutrientData.getKey();
                            Float value = nutrientData.getValue(Float.class);
                            if (key != null && value != null) {
                                nutrimentMap.put(key, value);
                            }
                        }
                        if (!nutrimentMap.isEmpty()) {
                            nutrimentsList.add(nutrimentMap);
                        }
                    }
                    String genericName = productSnapshot.child("generic_name").getValue(String.class);
                    if (name == null || imageUrl == null || barcode == null) continue;
                    ScannedProduct product = new ScannedProduct(name, imageUrl, categoryList, barcode, nutriScore, nutriGrade, brandList, genericName, countries, nutrimentsList, allergens);
                    scannedProducts.add(product);
                }

                filteredList.clear();
                filteredList.addAll(scannedProducts);

                updateCategorySpinner();
                updateBrandSpinner();

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Firebase", "Failed to load scanned products", databaseError.toException());
            }
        });
    }



    private void updateCategorySpinner() {
        List<String> categoryList = new ArrayList<>(categoriesSet);
        categoryList.add(0, "All");
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
    }

    private void updateBrandSpinner() {
        List<String> brandList = new ArrayList<>(brandsSet);
        brandList.add(0, "All");
        ArrayAdapter<String> brandAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, brandList);
        brandAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        brandSpinner.setAdapter(brandAdapter);
    }

    private void filterList() {
        String query = searchBar.getText().toString().toLowerCase();
        filteredList.clear();

        for (ScannedProduct product : scannedProducts) {
            boolean matchesCategory = selectedCategory.equals("All") || containsIgnoreCase(product.getCategories(), selectedCategory);
            boolean matchesBrand = selectedBrand.equals("All") || containsIgnoreCase(product.getBrands(), selectedBrand);
            boolean matchesSearch = product.getName().toLowerCase().contains(query);

            if (matchesCategory && matchesBrand && matchesSearch) {
                filteredList.add(product);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private boolean containsIgnoreCase(List<String> list, String value) {
        for (String item : list) {
            if (item.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void openProductFragment(ScannedProduct product) {
        ProductFragment productFragment = ProductFragment.newInstance(
                product.getName(),
                new ArrayList<>(product.getBrands()),
                product.getGenericName(),
                new ArrayList<>(product.getCategories()),
                new ArrayList<>(product.getCountries()),
                product.getBarcode(),
                product.getImageUrl(),
                new ArrayList<>(product.getAllergens()),
                product.getNutrimentsList(),
                product.getNutriScore(),
                product.getNutriGrade()
        );

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, productFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }


}
