package com.example.allergydetector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class AllergyAdapter extends RecyclerView.Adapter<AllergyAdapter.AllergyViewHolder> {

    private List<String> allergyList;

    public interface OnRecyclerViewSelectItem<T> {
        void deleteAllergy(int position, T t);
    }

    private OnRecyclerViewSelectItem<String> recyclerViewSelectItem;

    public AllergyAdapter(List<String> allergyList, OnRecyclerViewSelectItem<String> recyclerViewSelectItem) {
        this.allergyList = allergyList;
        this.recyclerViewSelectItem = recyclerViewSelectItem;
    }


    @NonNull
    @Override
    public AllergyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.single_allergy_item, parent, false);
        return new AllergyAdapter.AllergyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AllergyViewHolder holder, int position) {
        String allergy =allergyList.get(position);
        holder.allergyItem.setText(allergy);
    }

    @Override
    public int getItemCount() {
        return allergyList.size();
    }

    class AllergyViewHolder extends RecyclerView.ViewHolder{
        private ImageView ic_minus;
        private TextView allergyItem;

        public AllergyViewHolder(@NonNull View itemView) {
            super(itemView);

            allergyItem = itemView.findViewById(R.id.allergyItem);
            ic_minus = itemView.findViewById(R.id.ic_minus);

            ic_minus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (recyclerViewSelectItem != null) {
                        recyclerViewSelectItem.deleteAllergy(getLayoutPosition(), allergyList.get(getLayoutPosition()));
                    }
                }
            });
        }
    }
}
