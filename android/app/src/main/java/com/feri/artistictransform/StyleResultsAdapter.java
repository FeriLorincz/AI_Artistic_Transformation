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

public class StyleResultsAdapter extends RecyclerView.Adapter<StyleResultsAdapter.StyleViewHolder>{

    private List<Bitmap> styleResults = new ArrayList<>();
    private static final String TAG = "StyleResultsAdapter";

    @NonNull
    @Override
    public StyleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_style_result, parent, false);

        // Setăm dimensiunea itemului la jumătate din lățimea parentului
        int width = parent.getMeasuredWidth() / 2;
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = width;
        lp.height = width; // Păstrăm aspect ratio pătratic
        view.setLayoutParams(lp);

        return new StyleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StyleViewHolder holder, int position) {
        Bitmap bitmap = styleResults.get(position);
        holder.imageView.setImageBitmap(bitmap);
        Log.d(TAG, "Binding image at position " + position);
    }

    @Override
    public int getItemCount() {
        return styleResults.size();
    }

    public void updateResults(List<Bitmap> newResults) {
        Log.d(TAG, "Updating results with " + newResults.size() + " items");
        this.styleResults = newResults;
        notifyDataSetChanged();
    }

    static class StyleViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        StyleViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.styleResultImageView);
        }
    }
}
