package com.pixelpen.whereitwent;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AccountsOverviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts_overview);

        RecyclerView recycler = findViewById(R.id.recycler_accounts);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new AccountsOverviewAdapter());

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }
}
