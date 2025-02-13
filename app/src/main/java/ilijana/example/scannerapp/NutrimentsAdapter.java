package ilijana.example.scannerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;

public class NutrimentsAdapter extends RecyclerView.Adapter<NutrimentsAdapter.NutrientViewHolder> {

    private final ArrayList<HashMap<String, Float>> nutrimentsList;
    private boolean isEditable = false;

    public NutrimentsAdapter(ArrayList<HashMap<String, Float>> nutrimentsList, boolean isEditable) {
        this.nutrimentsList = nutrimentsList;
        this.isEditable = isEditable;
    }

    @NonNull
    @Override
    public NutrientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nutriment, parent, false);
        return new NutrientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NutrientViewHolder holder, int position) {
        HashMap<String, Float> nutriment = nutrimentsList.get(position);
        for (String key : nutriment.keySet()) {
            holder.nutrientLabel.setText(key + ":");
            holder.nutrientValue.setText(String.valueOf(nutriment.get(key)));
        }

        holder.nutrientValue.setEnabled(isEditable);
    }

    @Override
    public int getItemCount() {
        return nutrimentsList.size();
    }

    public void setEditable(boolean editable) {
        this.isEditable = editable;
        notifyDataSetChanged();
    }

    public static class NutrientViewHolder extends RecyclerView.ViewHolder {
        TextView nutrientLabel, nutrientValue;

        public NutrientViewHolder(View itemView) {
            super(itemView);
            nutrientLabel = itemView.findViewById(R.id.nutrient_label);
            nutrientValue = itemView.findViewById(R.id.nutrient_value);
        }
    }
}
