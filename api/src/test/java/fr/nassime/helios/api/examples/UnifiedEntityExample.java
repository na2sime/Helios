package fr.nassime.helios.api.examples;

import fr.nassime.helios.api.annotations.*;
import fr.nassime.helios.api.annotations.enums.PersistenceType;
import fr.nassime.helios.api.annotations.enums.FetchType;
import fr.nassime.helios.api.annotations.enums.GenerationType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Example demonstrating the unified @Persistable annotation usage.
 * This shows how a single annotation can work for different storage backends.
 */
public class UnifiedEntityExample {
    
    /**
     * User entity with AUTO persistence type.
     * Helios will choose the best available storage backend.
     */
    @Persistable(name = "users", type = PersistenceType.AUTO)
    public static class User {
        
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private String id; // Works for both SQL (Long) and MongoDB (String/ObjectId)
        
        @Column(name = "username")
        private String username;
        
        @Column(name = "email")
        private String email;
        
        @Column(name = "age")
        private Integer age;
        
        @Column(name = "active")
        private Boolean active = true;
        
        @Column(name = "created_at")
        private LocalDateTime createdAt;
        
        @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
        private List<Post> posts;
        
        // Constructors, getters, setters...
        public User() {}
        
        public User(String username, String email) {
            this.username = username;
            this.email = email;
            this.createdAt = LocalDateTime.now();
        }
        
        // Standard getters/setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public List<Post> getPosts() { return posts; }
        public void setPosts(List<Post> posts) { this.posts = posts; }
    }
    
    /**
     * Post entity forced to SQL storage.
     * Good for complex queries and ACID compliance.
     */
    @Persistable(name = "posts", type = PersistenceType.SQL, schema = "content")
    public static class Post {
        
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @Column(name = "title")
        private String title;
        
        @Column(name = "content")
        private String content;
        
        @Column(name = "published")
        private Boolean published = false;
        
        @Column(name = "created_at")
        private LocalDateTime createdAt;
        
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "author_id")
        private User author;
        
        // Constructors, getters, setters...
        public Post() {}
        
        public Post(String title, String content, User author) {
            this.title = title;
            this.content = content;
            this.author = author;
            this.createdAt = LocalDateTime.now();
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Boolean getPublished() { return published; }
        public void setPublished(Boolean published) { this.published = published; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public User getAuthor() { return author; }
        public void setAuthor(User author) { this.author = author; }
    }
    
    /**
     * Analytics entity forced to MongoDB storage.
     * Good for large volumes of semi-structured data.
     */
    @Persistable(name = "analytics", type = PersistenceType.DOCUMENT, database = "metrics")
    public static class Analytics {
        
        @Id
        private String id; // MongoDB ObjectId
        
        @Field(name = "event_type")
        private String eventType;
        
        @Field(name = "user_id")
        private String userId;
        
        @Field(name = "metadata")
        private Object metadata; // Flexible document structure
        
        @Field(name = "timestamp")
        private LocalDateTime timestamp;
        
        // Constructors, getters, setters...
        public Analytics() {}
        
        public Analytics(String eventType, String userId, Object metadata) {
            this.eventType = eventType;
            this.userId = userId;
            this.metadata = metadata;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public Object getMetadata() { return metadata; }
        public void setMetadata(Object metadata) { this.metadata = metadata; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * Product entity demonstrating flexible field mapping.
     * Can work with both SQL and document databases.
     */
    @Persistable(name = "products", type = PersistenceType.AUTO)
    public static class Product {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private String id;

        @Column(name = "name")
        @Field(name = "name")
        private String name;

        @Column(name = "description")
        @Field(name = "description")
        private String description;

        @Column(name = "price")
        @Field(name = "price")
        private Double price;

        @Column(name = "stock")
        @Field(name = "stock")
        private Integer stock;

        // Constructors, getters, setters...
        public Product() {}

        public Product(String name, Double price) {
            this.name = name;
            this.price = price;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }

        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
    }
}