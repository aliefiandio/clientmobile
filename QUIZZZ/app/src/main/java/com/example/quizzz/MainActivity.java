package com.example.quizzz;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private StorageReference storageRef;
    private DatabaseReference dbRef;
    private FirebaseRecyclerAdapter adapter;

    private AccessTokenTracker accessTokenTracker;
    private ProfileTracker profileTracker;

    private CallbackManager mCallbackManager;
    private String TAG = "TAG_LOG";

    private RelativeLayout crudLayout;
    private EditText nameInput;
    private Button addBtn, selectPhotoBtn, cancelUpdateBtn;
    private ImageView previewPhoto;
    private RecyclerView restoRv;
    private ProgressBar progressBar;

    private Context context = MainActivity.this;
    private static final int PICK_IMAGE_REQUEST = 234;
    private Uri filePath;
    private String idRecord = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(context);
        FirebaseApp.initializeApp(context);
        setContentView(R.layout.activity_main);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("images");
        dbRef = FirebaseDatabase.getInstance().getReference("restaurant");

        // View
        crudLayout = findViewById(R.id.crud_layout);
        nameInput = findViewById(R.id.name_input);
        addBtn = findViewById(R.id.add_btn);
        cancelUpdateBtn = findViewById(R.id.cancel_btn);
        selectPhotoBtn = findViewById(R.id.select_photo_button);
        previewPhoto = findViewById(R.id.preview_photo);
        restoRv = findViewById(R.id.restaurant_rv);
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.INVISIBLE);

        // Facebook
        mCallbackManager = CallbackManager.Factory.create();
        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {
            }
        };
        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(
                    Profile oldProfile,
                    Profile currentProfile) {
            }
        };
        accessTokenTracker.startTracking();
        profileTracker.startTracking();
        LoginButton loginButton = findViewById(R.id.login_fb_btn);
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                progressBar.setVisibility(View.VISIBLE);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
                updateUI(null);
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
                updateUI(null);
                showToast("Facebook failed");
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginManager.getInstance().logOut();
            }
        });

        FirebaseRecyclerOptions<Restaurant> options =
                new FirebaseRecyclerOptions.Builder<Restaurant>()
                        .setQuery(dbRef, Restaurant.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<Restaurant, RestaurantHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RestaurantHolder restaurantHolder, int i, @NonNull final Restaurant restaurant) {
                restaurantHolder.cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, MenuActivity.class);
                        intent.putExtra("KEY_RESTAURANT", restaurant.getId());
                        startActivity(intent);
                    }
                });
                restaurantHolder.nameField.setText(restaurant.getName());

                if (!restaurant.getImage().equals("")) {
                    Glide.with(context).load(restaurant.getImage()).into(restaurantHolder.imgBtn);
                }

                restaurantHolder.editBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        addBtn.setText("Update");
                        cancelUpdateBtn.setVisibility(View.VISIBLE);

                        idRecord = restaurant.getId();
                        filePath = Uri.parse(restaurant.getImage());
                        nameInput.setText(restaurant.getName());
                        Glide.with(context).load(restaurant.getImage()).into(previewPhoto);
                    }
                });

                restaurantHolder.deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog.setTitle("You sure?");
                        alertDialog.setMessage(restaurant.getName() + " will be deleted.");
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES, DELETE",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        storageRef.child(restaurant.getId() + ".jpg").delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    dbRef.child(restaurant.getId()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()) {
                                                                showToast("Deleted");
                                                            }
                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    }
                                });
                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    }
                });
            }

            @Override
            public RestaurantHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_restaurant, parent, false);

                return new RestaurantHolder(view);
            }
        };


        // Adpter Recyclerview untuk me-list data
        restoRv.setLayoutManager(new LinearLayoutManager(context));
        restoRv.setAdapter(adapter);

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(nameInput.getText().toString()) && filePath != null) {
                    uploadImage();
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    showToast("Please fill the blank");
                }
            }
        });
        selectPhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileChooser();
            }
        });
        cancelUpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetForm();
            }
        });
    }

    public class RestaurantHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView nameField;
        ImageView editBtn, deleteBtn, imgBtn;

        RestaurantHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.item_restaurant_card);
            nameField = itemView.findViewById(R.id.item_restaurant_name);
            editBtn = itemView.findViewById(R.id.ic_edit);
            deleteBtn = itemView.findViewById(R.id.ic_delete);
            imgBtn = itemView.findViewById(R.id.ic_image);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mCallbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                previewPhoto.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        accessTokenTracker.stopTracking();
        profileTracker.stopTracking();
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    private void updateUI(@Nullable FirebaseUser user) {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (user != null && isLoggedIn) {
//            crudLayout.setVisibility(View.VISIBLE);
        } else {
            mAuth.signOut();
//            crudLayout.setVisibility(View.GONE);
        }
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a Picture"), PICK_IMAGE_REQUEST);
    }

    private void uploadImage() {
        if (idRecord.equals("")) {
            idRecord = dbRef.push().getKey();
        }

        UploadTask uploadTask = storageRef.child(idRecord + ".jpg").putFile(filePath);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                showToast("Storage success");
                storageRef.child(idRecord + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        uploadDatabase(uri.toString());
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showToast("Url failed");
                        Log.i("storage", e.getMessage());
                    }
                });
            }


        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showToast("Storage failed");
                Log.e("storage", e.getMessage());
            }
        });
    }

    private void uploadDatabase(String url) {
        Restaurant restaurant = new Restaurant(idRecord, nameInput.getText().toString(), url);
        if (idRecord != null) {
            dbRef.child(idRecord).setValue(restaurant).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        showToast("Db success");
                        resetForm();
                    } else {
                        showToast("Db failed");
                    }
                    progressBar.setVisibility(View.INVISIBLE);
                }
            });
        }
    }

    private void showToast(String string) {
        Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
    }

    private void resetForm() {
        addBtn.setText("Add");
        cancelUpdateBtn.setVisibility(View.GONE);

        nameInput.setText("");
        previewPhoto.setImageResource(0);

        filePath = null;
        idRecord = "";
        progressBar.setVisibility(View.INVISIBLE);
    }

}
