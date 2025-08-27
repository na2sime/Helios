package fr.nassime.helios.mongo.entities;

import fr.nassime.helios.api.annotations.Document;
import fr.nassime.helios.api.annotations.Field;
import fr.nassime.helios.api.annotations.Id;

import java.math.BigDecimal;

/**
 * Test entity for MongoDB Product document.
 */
@Document(collection = "products")
public class Product {
    
    @Id
    private String id;
    
    @Field(name = "name")
    private String name;
    
    @Field(name = "description")
    private String description;
    
    @Field(name = "price")
    private BigDecimal price;
    
    @Field(name = "category")
    private String category;
    
    @Field(name = "inStock")
    private Boolean inStock;
    
    // No-arg constructor
    public Product() {
    }
    
    // Constructor with fields
    public Product(String name, String description, BigDecimal price, String category, Boolean inStock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.inStock = inStock;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Boolean getInStock() {
        return inStock;
    }
    
    public void setInStock(Boolean inStock) {
        this.inStock = inStock;
    }
    
    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", inStock=" + inStock +
                '}';
    }
}