package com.example.quizzz;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class DetailMenuActivity extends AppCompatActivity {

    private String id_menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_menu);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

//        id_menu = getIntent().getStringExtra("KEY_MENU");
        String name = getIntent().getStringExtra("MENU_NAME");
        int price = getIntent().getIntExtra("MENU_PRICE", 0);
        String image = getIntent().getStringExtra("MENU_IMAGE");

        ImageView imageView = findViewById(R.id.detail_menu_image);
        TextView nameText = findViewById(R.id.detail_menu_name_text);
        TextView priceText = findViewById(R.id.detail_menu_price_text);

        nameText.setText(name);
        priceText.setText("" + price);
        if (!image.equals("")) {
            Glide.with(DetailMenuActivity.this).load(image).into(imageView);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
