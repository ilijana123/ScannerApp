package ilijana.example.scannerapp;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class SimpleListAdapter extends RecyclerView.Adapter<SimpleListAdapter.SimpleListViewHolder> {
    private final ArrayList<String> list;
    private boolean isEditable = false;

    public SimpleListAdapter(ArrayList<String> list, boolean isEditable) {
        this.list = new ArrayList<>(list);
        this.isEditable = isEditable;
    }

    @NonNull
    @Override
    public SimpleListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new SimpleListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleListViewHolder holder, int position) {
        holder.value.setText(list.get(position));
        holder.value.setEnabled(isEditable);

        holder.value.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                list.set(holder.getAdapterPosition(), s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setEditable(boolean editable) {
        this.isEditable = editable;
        notifyDataSetChanged();
    }

    public ArrayList<String> getUpdatedList() {
        return new ArrayList<>(list);
    }

    public static class SimpleListViewHolder extends RecyclerView.ViewHolder {
        TextView value;

        public SimpleListViewHolder(View itemView) {
            super(itemView);
            value = itemView.findViewById(R.id.item_text);
        }
    }
}
