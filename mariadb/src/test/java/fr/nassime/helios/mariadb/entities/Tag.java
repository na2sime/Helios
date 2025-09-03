package fr.nassime.helios.mariadb.entities;

import fr.nassime.helios.api.annotations.*;
import fr.nassime.helios.api.annotations.enums.GenerationType;
import fr.nassime.helios.api.annotations.enums.PersistenceType;
import lombok.Data;

import java.util.List;

/**
 * Test entity representing a Tag for MariaDB tests.
 */
@Persistable(name = "tags", type = PersistenceType.SQL)
@Table(name = "tags")
@Data
public class Tag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "color")
    private String color;
    
    @ManyToMany(mappedBy = "tags")
    private List<Post> posts;
    
    public Tag() {
    }
    
    public Tag(String name) {
        this.name = name;
    }
    
    public Tag(String name, String description, String color) {
        this.name = name;
        this.description = description;
        this.color = color;
    }
}