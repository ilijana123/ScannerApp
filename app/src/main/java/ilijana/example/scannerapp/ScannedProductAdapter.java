package ilijana.example.scannerapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.List;

public class ScannedProductAdapter extends RecyclerView.Adapter<ScannedProductAdapter.ViewHolder> {

    private Context context;
    private List<ScannedProduct> scannedProducts;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(ScannedProduct product);
    }

    public ScannedProductAdapter(Context context, List<ScannedProduct> scannedProducts, OnItemClickListener listener) {
        this.context = context;
        this.scannedProducts = scannedProducts;
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_scanned_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScannedProduct product = scannedProducts.get(position);

        holder.productName.setText(product.getName());

        String imageUrl = product.getImageUrl();
        Log.d("Image URL", imageUrl);

        if (imageUrl == null || imageUrl.isEmpty()) {
            holder.productImage.setImageResource(R.drawable.image);
        } else {
            Picasso.get().load(imageUrl)
                    .placeholder(R.drawable.image)
                    .error(R.drawable.image)
                    .into(holder.productImage);
        }
        int nutriScore = product.getNutriScore();
        holder.healthScore.setText(String.valueOf(nutriScore)+"/100");

        switch (product.getNutriGrade().toLowerCase()) {
            case "a": holder.healthScore.setBackgroundResource(R.drawable.badge_green); break;
            case "b": holder.healthScore.setBackgroundResource(R.drawable.badge_light_green); break;
            case "c": holder.healthScore.setBackgroundResource(R.drawable.badge_yellow); break;
            case "d": holder.healthScore.setBackgroundResource(R.drawable.badge_orange); break;
            case "e": holder.healthScore.setBackgroundResource(R.drawable.badge_red); break;
            default: holder.healthScore.setBackgroundResource(R.drawable.badge_gray); break;
        }
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductActivity.class);
            intent.putExtra("barcode", product.getBarcode());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return scannedProducts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, healthScore;

        public ViewHolder(View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            healthScore = itemView.findViewById(R.id.health_score);
        }
    }
}
