package ilijana.example.scannerapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.UUID;

public class EditProfileActivity extends AppCompatActivity {

    EditText editName, editEmail, editUsername, editPassword;
    Button saveButton;
    String nameUser = "";
    String emailUser = "";
    String usernameUser = "";
    DatabaseReference reference;
    private Button btnSelect, btnUpload;
    private ImageView imageView;
    private Uri filePath;
    private final int PICK_IMAGE_REQUEST = 22;
    FirebaseStorage storage;
    StorageReference storageReference;
    private String imageUrl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        reference = FirebaseDatabase.getInstance().getReference("users");
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editUsername = findViewById(R.id.editUsername);
        saveButton = findViewById(R.id.saveButton);
        showData();
        saveButton.setOnClickListener(view -> {
            boolean anyChanges = false;
            if (isNameChanged()) anyChanges = true;
            if (isEmailChanged()) anyChanges = true;
            if (isUsernameChanged()) anyChanges = true;

            if (anyChanges) {
                Toast.makeText(EditProfileActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                goToUserActivity();
            } else {
                Toast.makeText(EditProfileActivity.this, "No Changes Found", Toast.LENGTH_SHORT).show();
                goToUserActivity();
            }
        });

        btnSelect = findViewById(R.id.btnChoose);
        btnUpload = findViewById(R.id.btnUpload);
        imageView = findViewById(R.id.imgView);
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectImage();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });
        FirebaseDatabase firebaseDatabase
                = FirebaseDatabase.getInstance();

        DatabaseReference databaseReference
                = firebaseDatabase.getReference();

        DatabaseReference getImage
                = databaseReference.child("images/"
                + usernameUser);
        loadImage();

    }

    private boolean isNameChanged() {
        String newName = editName.getText().toString().trim();
        if (!newName.equals(nameUser)) {
            reference.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("name").setValue(newName)
                    .addOnSuccessListener(aVoid -> {
                        nameUser = newName;
                        Toast.makeText(EditProfileActivity.this, "Name updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditProfileActivity.this, "Error updating name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            return true;
        }
        return false;
    }

    private boolean isEmailChanged() {
        String newEmail = editEmail.getText().toString().trim();
        if (!newEmail.equals(emailUser)) {
            reference.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("email").setValue(newEmail)
                    .addOnSuccessListener(aVoid -> {
                        emailUser = newEmail;
                        Toast.makeText(EditProfileActivity.this, "Email updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditProfileActivity.this, "Error updating email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            return true;
        }
        return false;
    }

    private boolean isUsernameChanged() {
        String newUsername = editUsername.getText().toString().trim();
        if (!newUsername.equals(usernameUser)) {
            reference.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("username").setValue(newUsername)
                    .addOnSuccessListener(aVoid -> {
                        usernameUser = newUsername;
                        Toast.makeText(EditProfileActivity.this, "Username updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditProfileActivity.this, "Error updating username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            return true;
        }
        return false;
    }


    public void showData() {
        Intent intent = getIntent();
        String uid = intent.getStringExtra("uid");

        if (uid == null || uid.trim().isEmpty()) {
            Toast.makeText(this, "Error: User ID is missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    nameUser = dataSnapshot.child("name").getValue(String.class);
                    emailUser = dataSnapshot.child("email").getValue(String.class);
                    usernameUser = dataSnapshot.child("username").getValue(String.class);

                    editName.setText(nameUser != null ? nameUser : "");
                    editEmail.setText(emailUser != null ? emailUser : "");
                    editUsername.setText(usernameUser != null ? usernameUser : "");

                } else {
                    Toast.makeText(EditProfileActivity.this, "User not found in database!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(EditProfileActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void SelectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadImage() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        if (filePath == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading...");
        progressDialog.show();

        StorageReference ref = storageReference.child("images/" + uid);

        ref.putFile(filePath)
                .addOnSuccessListener(taskSnapshot -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditProfileActivity.this, "Image Uploaded Successfully!", Toast.LENGTH_SHORT).show();

                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();

                        reference.child(uid).child("imageUrl").setValue(downloadUrl)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(EditProfileActivity.this, "Profile Image Updated!", Toast.LENGTH_SHORT).show();
                                    imageUrl = downloadUrl;
                                    loadImage();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(EditProfileActivity.this, "Failed to update image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(EditProfileActivity.this, "Failed to retrieve image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditProfileActivity.this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("UPLOAD_ERROR", "Error uploading file", e);
                }).addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    progressDialog.setMessage("Uploaded " + (int) progress + "%");
                });
    }


    private void loadImage() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("LOAD_IMAGE", "User not logged in");
            return;
        }

        String uid = currentUser.getUid();
        DatabaseReference userImageRef = reference.child(uid).child("imageUrl");

        userImageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    imageUrl = dataSnapshot.getValue(String.class);
                    Log.d("LOAD_IMAGE", "Image URL Retrieved: " + imageUrl);

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Picasso.get()
                                .load(imageUrl)
                                .placeholder(R.drawable.profile_icon)
                                .error(R.drawable.profile_icon)
                                .into(imageView, new com.squareup.picasso.Callback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d("LOAD_IMAGE", "Image Loaded Successfully!");
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Log.e("LOAD_IMAGE", "Error loading image: " + e.getMessage(), e);
                                        Toast.makeText(EditProfileActivity.this, "Error loading image", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Log.e("LOAD_IMAGE", "Retrieved URL is empty");
                        imageView.setImageResource(R.drawable.profile_icon);
                    }
                } else {
                    Log.e("LOAD_IMAGE", "No image found in database");
                    imageView.setImageResource(R.drawable.profile_icon);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("LOAD_IMAGE", "Database Error: " + databaseError.getMessage());
                Toast.makeText(EditProfileActivity.this, "Database Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToUserActivity() {
        if (usernameUser == null || usernameUser.trim().isEmpty()) {
            Log.e("NAVIGATION", "Skipping navigation: username is missing!");
            return;
        }

        Intent intent = new Intent(EditProfileActivity.this, UserActivity.class);
        intent.putExtra("username", usernameUser);
        intent.putExtra("name", nameUser);
        intent.putExtra("email", emailUser);
        startActivity(intent);
        finish();
    }
}
