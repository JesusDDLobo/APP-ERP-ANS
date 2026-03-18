package com.example.app_ans.tasks.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_ans.R;
import com.example.app_ans.databinding.FragmentCoordinatorTasksBinding;
import com.example.app_ans.tasks.model.CoordinatorSubWorkOrder;

import java.util.List;

public class CoordinatorTasksFragment extends Fragment {

    private FragmentCoordinatorTasksBinding binding;
    private TaskViewModel viewModel;
    private CoordinatorSubWorkOrderAdapter adapter;

    public static CoordinatorTasksFragment newInstance() {
        return new CoordinatorTasksFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCoordinatorTasksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        setupRecyclerView();
        setupSearch();
        setupObservers();

        viewModel.loadCoordinatorSubWorkOrders();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadCoordinatorSubWorkOrders();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupRecyclerView() {
        adapter = new CoordinatorSubWorkOrderAdapter(this::onSubWorkOrderClick);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };

        binding.rvCoordinatorSubWorkOrders.setLayoutManager(layoutManager);
        binding.rvCoordinatorSubWorkOrders.setNestedScrollingEnabled(false);
        binding.rvCoordinatorSubWorkOrders.setHasFixedSize(false);
        binding.rvCoordinatorSubWorkOrders.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.searchCoordinatorTasks.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (adapter != null) {
                    adapter.filter(query);
                    requestRecyclerRelayout();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.filter(newText);
                    requestRecyclerRelayout();
                }
                return true;
            }
        });
    }

    private void setupObservers() {
        viewModel.getCoordinatorSubWorkOrders().observe(getViewLifecycleOwner(), this::renderSubWorkOrders);

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) return;
            binding.progressCoordinatorTasks.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });
    }

    private void renderSubWorkOrders(List<CoordinatorSubWorkOrder> items) {
        if (binding == null) return;

        if (adapter != null) {
            adapter.setItems(items);
        }

        boolean isEmpty = items == null || items.isEmpty();
        binding.layoutCoordinatorEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvCoordinatorSubWorkOrders.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        requestRecyclerRelayout();
    }

    private void requestRecyclerRelayout() {
        if (binding == null) return;

        binding.rvCoordinatorSubWorkOrders.post(() -> {
            if (binding != null) {
                binding.rvCoordinatorSubWorkOrders.requestLayout();
            }
        });
    }

    private void onSubWorkOrderClick(CoordinatorSubWorkOrder item) {
        if (item == null || !isAdded()) return;

        Bundle args = new Bundle();
        args.putInt("sub_work_order_id", item.getId());
        args.putString("sub_work_order_public_id", item.getPublicId());
        args.putString("sub_work_order_name", item.getName());

        SubWorkOrderTasksFragment fragment = new SubWorkOrderTasksFragment();
        fragment.setArguments(args);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.coordinator_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}