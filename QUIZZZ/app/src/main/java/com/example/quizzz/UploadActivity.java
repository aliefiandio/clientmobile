package com.example.quizzz;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import com.bumptech.glide.Glide;
import com.example.quizzz.networking.ServiceApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UploadActivity extends AppCompatActivity {

    private EditText textInput;
    private TextView textOutput;
    private Button selectBtn, uploadBtn, resetBtn;
    private ImageView imgInput, imgOutput;
    private ProgressBar progressBar;

    private Uri selectedImg;
    private Context context = UploadActivity.this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        initView();

        initOnclick();
    }

    private void initView() {
        textInput = findViewById(R.id.text_input);
        textOutput = findViewById(R.id.text_output);
        selectBtn = findViewById(R.id.select_img_btn);
        uploadBtn = findViewById(R.id.upload_btn);
        resetBtn = findViewById(R.id.reset_btn);
        imgInput = findViewById(R.id.image_input);
        imgOutput = findViewById(R.id.image_output);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initOnclick() {
        selectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //opening file chooser
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, 100);
            }
        });

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                //calling the upload file method after choosing the file
                String fileName = textInput.getText().toString();
                if (!TextUtils.isEmpty(fileName) && selectedImg != null) {
                    uploadFile(selectedImg, fileName);
                } else {
                    Toast.makeText(getApplicationContext(), "Isi semua", Toast.LENGTH_SHORT).show();
                }
            }
        });
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textInput.setText("");
                textOutput.setText("");
                imgInput.setImageResource(0);
                imgOutput.setImageResource(0);
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            //the image URI
            selectedImg = data.getData();

            Glide.with(context).load(selectedImg).into(imgInput);
            Toast.makeText(getApplicationContext(), selectedImg.toString(), Toast.LENGTH_SHORT).show();

        }
    }

    private void uploadFile(Uri fileUri, String name) {


        //creating a file
        File file = new File(getRealPathFromURI(fileUri));

        //creating request body for file
        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(fileUri)), file);
        RequestBody nameBody = RequestBody.create(MediaType.parse("text/plain"), name);

        //The gson builder
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();


        //creating retrofit object
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ServiceApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        //creating our api
        ServiceApi api = retrofit.create(ServiceApi.class);

        //creating a call and calling the upload image method
        Call<HashMap<String, String>> call = api.convertImage(requestFile, nameBody);

        //finally performing the call
        call.enqueue(new Callback<HashMap<String, String>>() {
            @Override
            public void onResponse(Call<HashMap<String, String>> call, Response<HashMap<String, String>> response) {
                if (response.body() == null) {
                    Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
                } else {

                    textOutput.setText(response.body().get("text"));
                    Glide.with(context).load(ServiceApi.BASE_URL + response.body().get("image")).into(imgOutput);
                }
                progressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onFailure(Call<HashMap<String, String>> call, Throwable t) {
                Log.d("RESPONSEEEEEE", t.getMessage());
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }
}
