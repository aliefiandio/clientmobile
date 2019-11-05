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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;

public class MenuActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private FirebaseRecyclerAdapter adapter;

    private EditText nameInput, priceInput;
    private Button addBtn, selectPhotoBtn, cancelUpdateBtn;
    private ImageView previewPhoto;
    private RecyclerView menuRv;
    private ProgressBar progressBar;

    private Context context = MenuActivity.this;
    private static final int PICK_IMAGE_REQUEST = 234;
    private Uri filePath;
    private String idRecord = "";
    private String id_restaurant = "";
    private String id_menu = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        id_restaurant = getIntent().getStringExtra("KEY_RESTAURANT");
        databaseReference = FirebaseDatabase.getInstance().getReference("menu").child(id_restaurant);
        storageReference = FirebaseStorage.getInstance().getReference("images");

        nameInput = findViewById(R.id.name_input);
        priceInput = findViewById(R.id.price_input);
        addBtn = findViewById(R.id.add_btn);
        cancelUpdateBtn = findViewById(R.id.cancel_btn);
        selectPhotoBtn = findViewById(R.id.select_photo_button);
        previewPhoto = findViewById(R.id.preview_photo);
        menuRv = findViewById(R.id.menu_rv);
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.INVISIBLE);

        FirebaseRecyclerOptions<Menu> options =
                new FirebaseRecyclerOptions.Builder<Menu>()
                        .setQuery(databaseReference, Menu.class)
                        .build();
        adapter = new FirebaseRecyclerAdapter<Menu, MenuHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MenuHolder menuHolder, int i, @NonNull final Menu menu) {
                menuHolder.cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, DetailMenuActivity.class);
                        intent.putExtra("MENU_NAME", menu.getName());
                        intent.putExtra("MENU_PRICE", menu.getPrice());
                        intent.putExtra("MENU_IMAGE", menu.getImage());
                        startActivity(intent);
                    }
                });
                menuHolder.nameField.setText(menu.getName());
                menuHolder.priceField.setText("" + menu.getPrice());

                if (!menu.getImage().equals("")) {
                    Glide.with(context).load(menu.getImage()).into(menuHolder.imgBtn);
                }

                menuHolder.editBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        addBtn.setText("Update");
                        cancelUpdateBtn.setVisibility(View.VISIBLE);

                        idRecord = menu.getId();
                        nameInput.setText(menu.getName());
                        priceInput.setText(String.valueOf(menu.getPrice()));
                        Glide.with(context).load(menu.getImage()).into(previewPhoto);
                    }
                });

                menuHolder.deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog alertDialog = new AlertDialog.Builder(MenuActivity.this).create();
                        alertDialog.setTitle("You sure?");
                        alertDialog.setMessage(menu.getName() + " will be deleted.");
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES, DELETE",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        storageReference.child(menu.getId() + ".jpg").delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    databaseReference.child(menu.getId()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
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

            @NonNull
            @Override
            public MenuHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_menu, parent, false);

                return new MenuHolder(view);
            }
        };

        menuRv.setLayoutManager(new LinearLayoutManager(context));
        menuRv.setAdapter(adapter);

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

    public class MenuHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView nameField, priceField;
        ImageView editBtn, deleteBtn, imgBtn;

        MenuHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.item_menu_card);
            nameField = itemView.findViewById(R.id.item_menu_name);
            priceField = itemView.findViewById(R.id.item_menu_price);
            editBtn = itemView.findViewById(R.id.ic_edit);
            deleteBtn = itemView.findViewById(R.id.ic_delete);
            imgBtn = itemView.findViewById(R.id.ic_image);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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

    private void uploadImage() {
        if (idRecord.equals("")) {
            idRecord = databaseReference.push().getKey();
        }

        UploadTask uploadTask = storageReference.child(idRecord + ".jpg").putFile(filePath);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                showToast("Storage success");
                storageReference.child(idRecord + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
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
        Menu menu = new Menu(idRecord, nameInput.getText().toString(), Integer.parseInt(priceInput.getText().toString()), url);
        if (idRecord != null) {
            databaseReference.child(idRecord).setValue(menu).addOnCompleteListener(new OnCompleteListener<Void>() {
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

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a Picture"), PICK_IMAGE_REQUEST);
    }

    private void resetForm() {
        addBtn.setText("Add");
        cancelUpdateBtn.setVisibility(View.GONE);

        nameInput.setText("");
        priceInput.setText("" + 0);
        previewPhoto.setImageResource(0);

        filePath = null;
        idRecord = "";
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
