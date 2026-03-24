package com.example.app_ans.tasks.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_ans.databinding.ItemRenditionRowBinding;
import com.example.app_ans.tasks.model.Rendition;

import java.util.ArrayList;
import java.util.List;

public class RenditionItemAdapter extends RecyclerView.Adapter<RenditionItemAdapter.RenditionViewHolder> {

    private final List<Rendition> items = new ArrayList<>();

    public void submitList(List<Rendition> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RenditionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRenditionRowBinding binding = ItemRenditionRowBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new RenditionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RenditionViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RenditionViewHolder extends RecyclerView.ViewHolder {

        private final ItemRenditionRowBinding binding;

        public RenditionViewHolder(ItemRenditionRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Rendition item) {
            binding.tvRenditionDate.setText(item.getDate());
            binding.tvRenditionAmount.setText(item.getAmount());

            // opcional (si luego quieres manejar hora)
            if (binding.tvRenditionTime != null) {
                binding.tvRenditionTime.setText("");
            }
        }
    }
}