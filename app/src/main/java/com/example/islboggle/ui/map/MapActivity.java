package com.example.islboggle.ui.map;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.islboggle.R;
import com.example.islboggle.ui.game.GameActivity;

public class MapActivity extends AppCompatActivity {

    private MapViewModel viewModel;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        viewModel = new ViewModelProvider(this).get(MapViewModel.class);
        
        viewModel.getLevels().observe(this, levels -> {
            LevelAdapter adapter = new LevelAdapter(levels, level -> {
                Intent intent = new Intent(MapActivity.this, GameActivity.class);
                intent.putExtra("LEVEL_ID", level.id);
                intent.putExtra("TIME_LIMIT", level.timeLimitMs);
                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshLevels();
    }
}
