package com.feri.artistictransform;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StylePagerAdapter extends RecyclerView.Adapter<StylePagerAdapter.StyleViewHolder> {
    private static final String TAG = "StylePagerAdapter";
    private List<Bitmap> styles = new ArrayList<>();

    public void setStyles(List<Bitmap> newStyles) {
        Log.d(TAG, "Updating styles, size: " + (newStyles != null ? newStyles.size() : 0));
        if (newStyles != null) {
            this.styles.clear();
            this.styles.addAll(newStyles);
            notifyDataSetChanged();
            Log.d(TAG, "Styles updated successfully");
        }
    }

    @NonNull
    @Override
    public StyleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "Creating new ViewHolder");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_style_full, parent, false);
        return new StyleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StyleViewHolder holder, int position) {
        Log.d(TAG, "Binding ViewHolder at position: " + position);
        holder.bind(styles.get(position));
    }

    @Override
    public int getItemCount() {
        return styles.size();
    }

    static class StyleViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;

        StyleViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.styleResultImageView);
        }

        void bind(Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}
