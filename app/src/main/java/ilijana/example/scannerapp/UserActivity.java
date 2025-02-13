package ilijana.example.scannerapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.CameraSelector;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.camera.core.Camera;
import android.widget.Button;

public class UserActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    String username, name, email, password;
    private PreviewView previewView;
    private TextView barcodeText;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private ToneGenerator toneGen1;
    private String barcodeData;
    String intentData = "";
    Button scan;
    Button authButton;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_user);
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Intent intent = new Intent(UserActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }

            String uid = currentUser.getUid();

            toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            drawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.nav_view);

            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            navigationView.setNavigationItemSelectedListener(this);

            View headerView = navigationView.getHeaderView(0);
            TextView navUsername = headerView.findViewById(R.id.nav_username);
            ImageView navProfileImage = headerView.findViewById(R.id.profile_image);

            loadProfileImage(navProfileImage);

            Intent intent = getIntent();
            username = intent.getStringExtra("username");
            name = intent.getStringExtra("name");
            email = intent.getStringExtra("email");
            password = intent.getStringExtra("password");

            barcodeText = findViewById(R.id.barcode_text);
            previewView = findViewById(R.id.preview_view);
            barcodeText = findViewById(R.id.barcode_text);
            scan = findViewById(R.id.scan);

            barcodeScanner = BarcodeScanning.getClient();
            cameraExecutor = Executors.newSingleThreadExecutor();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                startCamera();
            }
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String updatedUsername = dataSnapshot.child("username").getValue(String.class);
                        String profileImageUrl = dataSnapshot.child("imageUrl").getValue(String.class);

                        if (updatedUsername != null) {
                            navUsername.setText(updatedUsername);
                        }

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Picasso.get().load(profileImageUrl)
                                    .placeholder(R.drawable.profile_icon)
                                    .error(R.drawable.profile_icon)
                                    .into(navProfileImage);
                        } else {
                            navProfileImage.setImageResource(R.drawable.profile_icon);
                        }
                    } else {
                        Log.e("Firebase", "User does not exist in the database.");
                        Toast.makeText(UserActivity.this, "User does not exist in database", Toast.LENGTH_SHORT).show();
                        navUsername.setText("Guest");
                        navProfileImage.setImageResource(R.drawable.profile_icon);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("Firebase", "Database Error: " + databaseError.getMessage());
                    Toast.makeText(UserActivity.this, "Failed to load user data!", Toast.LENGTH_SHORT).show();
                }
            });

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                Log.d("Permissions", "Camera permission already granted");
                startCamera();
            }
        }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            Toast.makeText(this, "Home Clicked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_profile) {
            Intent editProfileIntent = new Intent(this, EditProfileActivity.class);
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                editProfileIntent.putExtra("uid", currentUser.getUid());
            }
            startActivity(editProfileIntent);
        } else if(id == R.id.nav_add_allergies) {
            Intent addAllergensIntent = new Intent(this, AddAllergiesActivity.class);
            startActivity(addAllergensIntent);
        } else if (id == R.id.scanned_products) {
            Intent scannedProductsIntent = new Intent(this, ScannedProductsActivity.class);
            startActivity(scannedProductsIntent);
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(UserActivity.this, LoginActivity.class));
            finish();
        }

        drawerLayout.closeDrawers();
        return true;
    }


    private void loadProfileImage(ImageView imageView) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference userImageRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("imageUrl");

            userImageRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String imageUrl = dataSnapshot.getValue(String.class);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Picasso.get()
                                    .load(imageUrl)
                                    .placeholder(R.drawable.profile_icon)
                                    .error(R.drawable.profile_icon)
                                    .into(imageView);
                        }
                    } else {
                        imageView.setImageResource(R.drawable.profile_icon);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(UserActivity.this, "Failed to load profile image!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ProcessCameraProvider cameraProvider = getIntent().getParcelableExtra("camera_provider");
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }


    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    Log.d("CameraX", "Image received for analysis");
                    scanBarcode(image);
                });

                Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                Log.d("CameraX", "Camera started successfully");

            } catch (Exception e) {
                Log.e("CameraX", "Use case binding failed", e);
            }
        }, ActivityCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void scanBarcode(ImageProxy image) {
        if (image.getImage() == null) {
            Log.e("MLKit", "Image is null, skipping scan");
            image.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees());

        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes.isEmpty()) {
                        Log.d("MLKit", "No barcode detected");
                        return;
                    }

                    for (Barcode barcode : barcodes) {
                        String scannedData = barcode.getRawValue();
                        barcodeText.setText(scannedData);
                        Log.d("MLKit", "Scanned Barcode: " + scannedData);

                        runOnUiThread(() -> {
                            scan.setOnClickListener(v -> {
                                if (scannedData != null && !scannedData.isEmpty()) {
                                    Intent intent = new Intent(UserActivity.this, ProductActivity.class);
                                    intent.putExtra("barcode", scannedData);
                                    startActivity(intent);
                                }
                            });
                        });
                    }
                })
                .addOnFailureListener(e -> Log.e("MLKit", "Barcode scanning failed", e))
                .addOnCompleteListener(task -> {
                    image.close();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permissions", "Camera permission granted, starting camera");
                startCamera();
            } else {
                Log.e("Permissions", "Camera permission denied!");
                Toast.makeText(this, "Camera permission is required!", Toast.LENGTH_LONG).show();
            }
        }
    }
}