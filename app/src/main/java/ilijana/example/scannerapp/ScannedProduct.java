package ilijana.example.scannerapp;

import java.io.Serializable;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ScannedProduct implements Serializable {
    private String name, genericName, imageUrl, barcode, nutriGrade;
    private int nutriScore;
    private ArrayList<String> brands, categories, countries, allergens;
    private ArrayList<HashMap<String, Float>> nutrimentsList;

    public ScannedProduct(String name, String imageUrl, ArrayList<String> categories, String barcode, int nutriScore, String nutriGrade, ArrayList<String> brands, String genericName, ArrayList<String> countries, ArrayList<HashMap<String, Float>> nutrimentsList, ArrayList<String> allergens) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.categories = categories;
        this.barcode = barcode;
        this.nutriScore = nutriScore;
        this.nutriGrade = nutriGrade;
        this.brands = brands;
        this.genericName = genericName;
        this.countries = countries;
        this.nutrimentsList = nutrimentsList;
        this.allergens = allergens;
    }

    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public ArrayList<String> getCategories() { return categories; }
    public String getBarcode() { return barcode; }
    public int getNutriScore() { return nutriScore; }
    public String getNutriGrade() { return nutriGrade; }
    public ArrayList<String> getBrands() { return brands; }

    public ArrayList<HashMap<String, Float>> getNutrimentsList() {
        return nutrimentsList;
    }
    public ArrayList<String>  getAllergens(){
        return allergens;
    }
    public ArrayList<String> getCountries(){
        return countries;
    }

    public String getGenericName() {
        return genericName;
    }
}
