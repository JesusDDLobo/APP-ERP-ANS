package com.example.app_ans.tasks.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_ans.databinding.ItemRenditionTaskGroupBinding;
import com.example.app_ans.tasks.model.RenditionTaskGroup;

import java.util.ArrayList;
import java.util.List;

public class RenditionTaskGroupAdapter extends RecyclerView.Adapter<RenditionTaskGroupAdapter.GroupViewHolder> {

    private final List<RenditionTaskGroup> items = new ArrayList<>();

    public void submitList(List<RenditionTaskGroup> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRenditionTaskGroupBinding binding = ItemRenditionTaskGroupBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new GroupViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {

        private final ItemRenditionTaskGroupBinding binding;
        private final RenditionItemAdapter childAdapter;

        public GroupViewHolder(ItemRenditionTaskGroupBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            childAdapter = new RenditionItemAdapter();
            binding.recyclerRenditions.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));
            binding.recyclerRenditions.setAdapter(childAdapter);

            View.OnClickListener toggle = v -> {
                int position = getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                RenditionTaskGroup item = items.get(position);
                item.setExpanded(!item.isExpanded());
                notifyItemChanged(position);
            };

            binding.headerContainer.setOnClickListener(toggle);
            binding.ivArrow.setOnClickListener(toggle);
        }

        void bind(RenditionTaskGroup item) {

            binding.tvBadgeCount.setText(String.valueOf(item.getRenditionCount()));
            binding.tvTaskTitle.setText(item.getTaskTitle());
            binding.tvTaskCode.setText(item.getTaskCode());
            binding.tvTaskTotal.setText("Total: " + item.getTotalAmount());

            childAdapter.submitList(item.getRenditions());

            boolean expanded = item.isExpanded();
            binding.expandableContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
            binding.ivArrow.setRotation(expanded ? 180f : 0f);
        }
    }
}