package com.example.islboggle.ui.map;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.example.islboggle.R;
import com.example.islboggle.data.Level;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.NodeViewHolder> {

    private final List<Level> levels;
    private final OnLevelClickListener listener;

    public interface OnLevelClickListener {
        void onLevelClick(Level level);
    }

    public LevelAdapter(List<Level> levels, OnLevelClickListener listener) {
        this.levels = levels;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_level_node, parent, false);
        return new NodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NodeViewHolder holder, int position) {
        Level level = levels.get(position);
        holder.levelText.setText(String.valueOf(level.id));
        
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) holder.nodeCard.getLayoutParams();
        float bias = (position % 2 == 0) ? 0.3f : 0.7f;
        params.horizontalBias = bias;
        holder.nodeCard.setLayoutParams(params);

        if (level.isUnlocked) {
            holder.nodeCard.setCardBackgroundColor(Color.parseColor("#4CAF50")); // Green
            holder.levelText.setTextColor(Color.WHITE);
            holder.lockIcon.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> listener.onLevelClick(level));
            holder.nodeCard.setAlpha(1.0f);
        } else {
            holder.nodeCard.setCardBackgroundColor(Color.parseColor("#555555")); // Grey
            holder.levelText.setTextColor(Color.parseColor("#888888"));
            holder.lockIcon.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(null); // Disabled
            holder.nodeCard.setAlpha(0.6f);
        }
    }

    @Override
    public int getItemCount() {
        return levels.size();
    }

    static class NodeViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView nodeCard;
        TextView levelText;
        ImageView lockIcon;

        NodeViewHolder(View itemView) {
            super(itemView);
            nodeCard = itemView.findViewById(R.id.nodeCard);
            levelText = itemView.findViewById(R.id.levelNumberText);
            lockIcon = itemView.findViewById(R.id.lockIcon);
        }
    }
}
