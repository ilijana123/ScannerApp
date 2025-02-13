package ilijana.example.scannerapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProductActivity extends AppCompatActivity {
    private static final String TAG = "ProductActivity";
    private String barcode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);
        Log.d(TAG, "onCreate() called in ProductActivity");

        barcode = getIntent().getStringExtra("barcode");
        Log.d(TAG, "Received barcode: " + barcode);

        if (barcode != null) {
            fetchProductDetails(barcode);
        } else {
            Toast.makeText(this, "No barcode found!", Toast.LENGTH_SHORT).show();
            finish();
        }

        ProcessCameraProvider cameraProvider = getIntent().getParcelableExtra("camera_provider");

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
    }

    private void fetchProductDetails(String barcode) {
        String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProductActivity.this, "API Request Failed", Toast.LENGTH_SHORT).show());
                Log.e(TAG, "API Request Failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject productJson = new JSONObject(jsonData);
                        JSONObject product = productJson.optJSONObject("product");

                        if (product != null) {
                            String productName = product.optString("product_name", "Unknown");
                            String brandsString = product.optString("brands", "");
                            ArrayList<String> brands = new ArrayList<>();
                            if (!brandsString.isEmpty()) {
                                String[] brandsArray = brandsString.split(",");
                                brands.addAll(Arrays.asList(brandsArray));
                            }

                            String genericName = product.optString("generic_name", "Unknown");

                            JSONArray categoriesArray = product.optJSONArray("categories_hierarchy");
                            ArrayList<String> categories = new ArrayList<>();
                            if (categoriesArray != null) {
                                for (int i = 0; i < categoriesArray.length(); i++) {
                                    String category = categoriesArray.optString(i, "");
                                    if (category.startsWith("en:")) {
                                        category = category.substring(3);
                                        category = capitalize(category);
                                        categories.add(category);
                                    }
                                }
                            }
                            String barcode = product.optString("code", "Unknown");
                            String imageUrl = product.optString("image_url", "");

                            JSONObject nutrimentsObject = product.optJSONObject("nutriments");
                            ArrayList<HashMap<String, Float>> nutrimentsList = new ArrayList<>();
                            if (nutrimentsObject != null) {
                                Iterator<String> keys = nutrimentsObject.keys();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    try {
                                        float value = Float.parseFloat(nutrimentsObject.getString(key));
                                        HashMap<String, Float> nutriment = new HashMap<>();
                                        nutriment.put(key, value);
                                        nutrimentsList.add(nutriment);
                                    } catch (NumberFormatException e) {
                                        Log.w(TAG, "Skipping non-float key: " + key);
                                    }
                                }
                            }
                            JSONArray allergensArray = product.optJSONArray("allergens_tags");
                            Set<String> allergenSet = new LinkedHashSet<>();

                            if (allergensArray != null) {
                                for (int i = 0; i < allergensArray.length(); i++) {
                                    String allergen = allergensArray.optString(i, "").trim();
                                    if (allergen.startsWith("en:")) {
                                        allergen = allergen.substring(3);
                                        allergen = capitalize(allergen);
                                        allergenSet.add(allergen);
                                    }
                                }
                            }

                            ArrayList<String> allergens = new ArrayList<>(allergenSet);

                            JSONArray countriesArray = product.optJSONArray("countries_hierarchy");
                            ArrayList<String> countries = new ArrayList<>();
                            if (countriesArray != null) {
                                for (int i = 0; i < countriesArray.length(); i++) {
                                    String country = countriesArray.getString(i).replace("en:", "");
                                    countries.add(country.substring(0, 1).toUpperCase() + country.substring(1).toLowerCase());
                                }
                            }

                            JSONObject nutriScoreObject = product.optJSONObject("nutriscore");
                            String nutriGrade;
                            int nutriScore;

                            if (nutriScoreObject != null) {
                                JSONObject yearData = nutriScoreObject.optJSONObject("2021");
                                if (yearData != null) {
                                    nutriGrade = yearData.optString("grade", "");
                                    nutriScore = yearData.optInt("score", 0);
                                } else {
                                    nutriGrade = "";
                                    nutriScore = 0;
                                }
                            } else {
                                nutriGrade = "";
                                nutriScore = 0;
                            }

                            runOnUiThread(() -> {
                                View fragmentContainer = findViewById(R.id.fragment_container);
                                if (fragmentContainer == null) {
                                    Log.e(TAG, "fragment_container is NULL. Cannot load ProductFragment.");
                                    return;
                                }

                                Log.d(TAG, "Replacing fragment in container...");

                                ProductFragment productFragment = ProductFragment.newInstance(
                                        productName, brands, genericName, categories, countries, barcode, imageUrl, allergens, nutrimentsList, nutriScore, nutriGrade);

                                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                transaction.replace(R.id.fragment_container, productFragment);
                                transaction.commitAllowingStateLoss();

                                Log.d(TAG, "ProductFragment should now be visible.");
                            });


                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing error: " + e.getMessage());
                    }
                }
            }
        });
    }
    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).replace("-", " ");
    }

}
