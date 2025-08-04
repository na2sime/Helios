package fr.nassime.helios.mariadb.entities;

import fr.nassime.helios.api.annotations.*;
import fr.nassime.helios.api.annotations.enums.GenerationType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Test entity representing a User for MariaDB tests.
 */
@Entity
@Table(name = "users")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "username", nullable = false, unique = true)
    private String username;
    
    @Column(name = "email", nullable = false)
    private String email;
    
    @Column(name = "full_name")
    private String fullName;
    
    @Column(name = "age")
    private Integer age;
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "author")
    private List<Post> posts;
    
    @OneToOne(mappedBy = "user")
    private UserProfile profile;
    
    public User() {
        this.createdAt = LocalDateTime.now();
    }
    
    public User(String username, String email) {
        this();
        this.username = username;
        this.email = email;
    }
}