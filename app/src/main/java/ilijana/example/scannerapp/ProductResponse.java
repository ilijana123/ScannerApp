package ilijana.example.scannerapp;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.gson.annotations.SerializedName;

public class ProductResponse implements Result {
    @SerializedName("product")
    private Product product;

    public Product getProduct() {
        return product;
    }

    @NonNull
    @Override
    public Status getStatus() {
        return null;
    }

    public static class Product {
        @SerializedName("nutriscore_score")
        private int nutriScore;

        @SerializedName("nutriscore_grade")
        private String nutriGrade;

        public int getNutriScore() {
            return nutriScore;
        }

        public String getNutriGrade() {
            return nutriGrade;
        }
    }
}

