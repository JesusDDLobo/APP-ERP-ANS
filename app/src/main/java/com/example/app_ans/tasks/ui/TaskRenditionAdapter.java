package com.example.app_ans.tasks.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_ans.R;
import com.example.app_ans.tasks.persistence.PendingRendition;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskRenditionAdapter extends RecyclerView.Adapter<TaskRenditionAdapter.GroupViewHolder> {

    public interface OnRenditionClickListener {
        void onRenditionClick(PendingRendition rendition);
    }

    private final List<RenditionGroup> groups = new ArrayList<>();
    private final OnRenditionClickListener listener;

    public TaskRenditionAdapter(OnRenditionClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<PendingRendition> newItems) {
        groups.clear();

        if (newItems != null && !newItems.isEmpty()) {
            Map<String, RenditionGroup> groupedMap = new LinkedHashMap<>();

            for (PendingRendition item : newItems) {
                String dateKey = formatDate(item.createdAt);

                RenditionGroup group = groupedMap.get(dateKey);
                if (group == null) {
                    group = new RenditionGroup();
                    group.date = dateKey;
                    groupedMap.put(dateKey, group);
                }

                group.items.add(item);
                group.total += extractTotalValue(item.renditionJson);
            }

            groups.addAll(groupedMap.values());
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rendition_task_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        RenditionGroup group = groups.get(position);

        holder.tvBadgeCount.setText(String.valueOf(group.items.size()));
        holder.tvTaskTitle.setText(group.date);
        holder.tvTaskCode.setText(group.items.size() == 1
                ? "1 rendición"
                : group.items.size() + " rendiciones");
        holder.tvTaskTotal.setText(formatCurrency(group.total));

        holder.expandableContainer.setVisibility(group.expanded ? View.VISIBLE : View.GONE);
        holder.ivArrow.setRotation(group.expanded ? 180f : 0f);

        holder.recyclerRenditions.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.recyclerRenditions.setAdapter(new RenditionChildAdapter(group.items));

        // IMPORTANTE: el header ya NO despliega
        holder.headerContainer.setOnClickListener(null);
        holder.headerContainer.setClickable(false);
        holder.headerContainer.setFocusable(false);

        // SOLO la flecha despliega
        holder.ivArrow.setOnClickListener(v -> {
            group.expanded = !group.expanded;

            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    private String formatDate(long millis) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            return sdf.format(new Date(millis));
        } catch (Exception e) {
            return "--/--/----";
        }
    }

    private String formatTime(long millis) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(millis));
        } catch (Exception e) {
            return "--:--";
        }
    }

    private long extractTotalValue(String renditionJson) {
        try {
            JsonObject jsonObject = JsonParser.parseString(renditionJson).getAsJsonObject();
            return jsonObject.has("total_reported")
                    ? jsonObject.get("total_reported").getAsLong()
                    : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String formatCurrency(long value) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("es", "CO"));
        return "$" + format.format(value);
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadgeCount;
        TextView tvTaskTitle;
        TextView tvTaskCode;
        TextView tvTaskTotal;
        ImageView ivArrow;
        View headerContainer;
        View expandableContainer;
        RecyclerView recyclerRenditions;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadgeCount = itemView.findViewById(R.id.tv_badge_count);
            tvTaskTitle = itemView.findViewById(R.id.tv_task_title);
            tvTaskCode = itemView.findViewById(R.id.tv_task_code);
            tvTaskTotal = itemView.findViewById(R.id.tv_task_total);
            ivArrow = itemView.findViewById(R.id.iv_arrow);
            headerContainer = itemView.findViewById(R.id.header_container);
            expandableContainer = itemView.findViewById(R.id.expandable_container);
            recyclerRenditions = itemView.findViewById(R.id.recycler_renditions);
        }
    }

    private static class RenditionGroup {
        String date;
        long total;
        boolean expanded = false;
        List<PendingRendition> items = new ArrayList<>();
    }

    private class RenditionChildAdapter extends RecyclerView.Adapter<RenditionChildAdapter.ChildViewHolder> {

        private final List<PendingRendition> childItems;

        RenditionChildAdapter(List<PendingRendition> childItems) {
            this.childItems = childItems != null ? childItems : new ArrayList<>();
        }

        @NonNull
        @Override
        public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task_rendition, parent, false);
            return new ChildViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
            PendingRendition item = childItems.get(position);

            holder.tvRenditionDate.setText("Rendición " + (position + 1) + " • " + formatTime(item.createdAt));
            holder.tvRenditionTotal.setText(formatCurrency(extractTotalValue(item.renditionJson)));

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRenditionClick(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return childItems.size();
        }

        class ChildViewHolder extends RecyclerView.ViewHolder {
            TextView tvRenditionDate;
            TextView tvRenditionTotal;

            ChildViewHolder(@NonNull View itemView) {
                super(itemView);
                tvRenditionDate = itemView.findViewById(R.id.tv_rendition_date);
                tvRenditionTotal = itemView.findViewById(R.id.tv_rendition_total);
            }
        }
    }
}