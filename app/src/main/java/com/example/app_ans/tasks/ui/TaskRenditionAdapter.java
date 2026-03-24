package com.example.app_ans.tasks.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_ans.R;
import com.example.app_ans.tasks.persistence.PendingRendition;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskRenditionAdapter extends RecyclerView.Adapter<TaskRenditionAdapter.RenditionViewHolder> {

    private final List<PendingRendition> items = new ArrayList<>();

    public void setItems(List<PendingRendition> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RenditionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_rendition, parent, false);
        return new RenditionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RenditionViewHolder holder, int position) {
        PendingRendition item = items.get(position);

        holder.tvRenditionDate.setText(formatDate(item.createdAt));
        holder.tvRenditionTotal.setText(extractTotal(item.renditionJson));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatDate(long millis) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            return sdf.format(new Date(millis));
        } catch (Exception e) {
            return "--/--/----";
        }
    }

    private String extractTotal(String renditionJson) {
        try {
            JsonObject jsonObject = JsonParser.parseString(renditionJson).getAsJsonObject();
            long totalReported = jsonObject.has("total_reported")
                    ? jsonObject.get("total_reported").getAsLong()
                    : 0L;

            NumberFormat format = NumberFormat.getNumberInstance(new Locale("es", "CO"));
            return "$" + format.format(totalReported);
        } catch (Exception e) {
            return "$0";
        }
    }

    static class RenditionViewHolder extends RecyclerView.ViewHolder {
        TextView tvRenditionDate;
        TextView tvRenditionTotal;

        public RenditionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRenditionDate = itemView.findViewById(R.id.tv_rendition_date);
            tvRenditionTotal = itemView.findViewById(R.id.tv_rendition_total);
        }
    }
}