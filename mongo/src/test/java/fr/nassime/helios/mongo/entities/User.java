package fr.nassime.helios.mongo.entities;

import fr.nassime.helios.api.annotations.Document;
import fr.nassime.helios.api.annotations.Field;
import fr.nassime.helios.api.annotations.Id;

/**
 * Test entity for MongoDB User document.
 */
@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    @Field(name = "username")
    private String username;
    
    @Field(name = "email")
    private String email;
    
    @Field(name = "age")
    private Integer age;
    
    @Field(name = "active")
    private Boolean active;
    
    // No-arg constructor
    public User() {
    }
    
    // Constructor with fields
    public User(String username, String email, Integer age, Boolean active) {
        this.username = username;
        this.email = email;
        this.age = age;
        this.active = active;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Integer getAge() {
        return age;
    }
    
    public void setAge(Integer age) {
        this.age = age;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", age=" + age +
                ", active=" + active +
                '}';
    }
}