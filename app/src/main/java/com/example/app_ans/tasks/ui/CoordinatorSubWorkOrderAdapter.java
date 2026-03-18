package com.example.app_ans.tasks.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_ans.R;
import com.example.app_ans.tasks.model.CoordinatorSubWorkOrder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CoordinatorSubWorkOrderAdapter extends RecyclerView.Adapter<CoordinatorSubWorkOrderAdapter.GroupViewHolder> {

    public interface OnSubWorkOrderClickListener {
        void onSubWorkOrderClick(CoordinatorSubWorkOrder item);
    }

    private final OnSubWorkOrderClickListener listener;
    private final List<GroupItem> groupedItems = new ArrayList<>();
    private List<CoordinatorSubWorkOrder> fullList = new ArrayList<>();
    private String lastQuery = "";

    public CoordinatorSubWorkOrderAdapter(OnSubWorkOrderClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<CoordinatorSubWorkOrder> items) {
        this.fullList = items != null ? new ArrayList<>(items) : new ArrayList<>();
        if (lastQuery != null && !lastQuery.isEmpty()) {
            filter(lastQuery);
        } else {
            buildGroups(fullList);
        }
    }

    public void filter(String query) {
        lastQuery = query != null ? query.trim() : "";

        if (lastQuery.isEmpty()) {
            buildGroups(fullList);
            return;
        }

        String q = lastQuery.toLowerCase(Locale.ROOT);
        List<CoordinatorSubWorkOrder> filtered = new ArrayList<>();

        for (CoordinatorSubWorkOrder item : fullList) {
            if (contains(item.getPublicId(), q)
                    || contains(item.getWorkOrderPublicId(), q)
                    || contains(item.getName(), q)
                    || contains(item.getCoordinatorName(), q)
                    || contains(item.getStatus(), q)
                    || contains(item.getCreatedAt(), q)) {
                filtered.add(item);
            }
        }

        buildGroups(filtered);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void buildGroups(List<CoordinatorSubWorkOrder> items) {
        groupedItems.clear();

        Map<String, List<CoordinatorSubWorkOrder>> groupedMap = new LinkedHashMap<>();

        for (CoordinatorSubWorkOrder item : items) {
            String key = item.getWorkOrderPublicId();
            if (key == null || key.trim().isEmpty()) {
                key = "Sin OT";
            }

            if (!groupedMap.containsKey(key)) {
                groupedMap.put(key, new ArrayList<>());
            }

            groupedMap.get(key).add(item);
        }

        for (Map.Entry<String, List<CoordinatorSubWorkOrder>> entry : groupedMap.entrySet()) {
            GroupItem group = new GroupItem();
            group.workOrderKey = entry.getKey();
            group.items = entry.getValue();
            group.expanded = false;
            groupedItems.add(group);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_coordinator_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        holder.bind(groupedItems.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return groupedItems.size();
    }

    static class GroupItem {
        String workOrderKey;
        List<CoordinatorSubWorkOrder> items = new ArrayList<>();
        boolean expanded;
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        private final TextView countView;
        private final TextView titleView;
        private final TextView keyView;
        private final ImageView arrowView;
        private final LinearLayout headerView;
        private final LinearLayout itemsContainer;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            countView = itemView.findViewById(R.id.coordinator_group_count);
            titleView = itemView.findViewById(R.id.coordinator_group_title);
            keyView = itemView.findViewById(R.id.coordinator_group_key);
            arrowView = itemView.findViewById(R.id.coordinator_group_arrow);
            headerView = itemView.findViewById(R.id.coordinator_group_header);
            itemsContainer = itemView.findViewById(R.id.coordinator_group_items_container);
        }

        void bind(GroupItem group, OnSubWorkOrderClickListener listener) {
            countView.setText(String.valueOf(group.items.size()));
            titleView.setText("Subórdenes");
            keyView.setText(group.workOrderKey);

            itemsContainer.removeAllViews();
            itemsContainer.setVisibility(group.expanded ? View.VISIBLE : View.GONE);
            arrowView.setRotation(group.expanded ? 180f : 0f);

            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());

            for (CoordinatorSubWorkOrder item : group.items) {
                View childView = inflater.inflate(
                        R.layout.item_coordinator_sub_work_order,
                        itemsContainer,
                        false
                );

                bindSubWorkOrderItem(childView, item, listener);
                itemsContainer.addView(childView);
            }

            headerView.setOnClickListener(v -> {
                group.expanded = !group.expanded;
                itemsContainer.setVisibility(group.expanded ? View.VISIBLE : View.GONE);
                arrowView.animate().rotation(group.expanded ? 180f : 0f).setDuration(180).start();
            });
        }

        private void bindSubWorkOrderItem(View itemView, CoordinatorSubWorkOrder item, OnSubWorkOrderClickListener listener) {
            TextView tvPublicId = itemView.findViewById(R.id.tv_sub_work_order_id);
            TextView tvName = itemView.findViewById(R.id.tv_sub_work_order_name);
            TextView tvCreatedAt = itemView.findViewById(R.id.tv_sub_work_order_created_at);
            TextView tvWorkOrderId = itemView.findViewById(R.id.tv_sub_work_order_work_order);
            TextView tvCoordinator = itemView.findViewById(R.id.tv_sub_work_order_coordinator);
            TextView tvStatus = itemView.findViewById(R.id.tv_sub_work_order_status);

            tvPublicId.setText(item.getPublicId() != null ? item.getPublicId() : "Sin ID");
            tvName.setText(item.getName() != null ? item.getName() : "Sin nombre");
            tvCreatedAt.setText(item.getCreatedAt() != null ? item.getCreatedAt() : "Sin fecha");
            tvWorkOrderId.setText(item.getWorkOrderPublicId() != null ? item.getWorkOrderPublicId() : "Sin OT");
            tvCoordinator.setText(item.getCoordinatorName() != null ? item.getCoordinatorName() : "Sin coordinador");
            tvStatus.setText(item.getStatus() != null ? item.getStatus() : "Sin estado");

            String status = item.getStatus() != null
                    ? item.getStatus().trim().toLowerCase(Locale.ROOT)
                    : "";

            if (status.equals("en proceso")) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_chip_orange);
                tvStatus.setTextColor(
                        androidx.core.content.ContextCompat.getColor(
                                itemView.getContext(),
                                android.R.color.white
                        )
                );
            } else {
                com.example.app_ans.core.ui.StatusUtils.applyStatusColor(tvStatus, item.getStatus());
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubWorkOrderClick(item);
                }
            });
        }
    }
}