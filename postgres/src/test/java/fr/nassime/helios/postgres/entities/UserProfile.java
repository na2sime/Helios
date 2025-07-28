package fr.nassime.helios.postgres.entities;

import fr.nassime.helios.api.annotations.*;
import fr.nassime.helios.api.annotations.enums.FetchType;
import fr.nassime.helios.api.annotations.enums.GenerationType;
import lombok.Data;

/**
 * Test entity representing a UserProfile (OneToOne relationship).
 */
@Entity
@Table(name = "user_profiles")
@Data
public class UserProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "bio")
    private String bio;
    
    @Column(name = "website")
    private String website;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    public UserProfile() {}
    
    public UserProfile(String bio, User user) {
        this.bio = bio;
        this.user = user;
    }
}