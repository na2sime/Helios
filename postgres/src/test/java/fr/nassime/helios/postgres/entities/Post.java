package fr.nassime.helios.postgres.entities;

import fr.nassime.helios.api.annotations.*;
import fr.nassime.helios.api.annotations.enums.FetchType;
import fr.nassime.helios.api.annotations.enums.GenerationType;
import fr.nassime.helios.api.annotations.enums.PersistenceType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Test entity representing a Post.
 */
@Persistable(name = "posts", type = PersistenceType.SQL)
@Data
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "content", nullable = false)
    private String content;
    
    @Column(name = "published", nullable = false)
    private Boolean published = false;
    
    @Column(name = "view_count")
    private Integer viewCount = 0;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "author_id")
    private Long authorId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;
    
    @OneToMany(mappedBy = "post")
    private List<Comment> comments;
    
    @ManyToMany
    @JoinTable(
        name = "post_tags",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;
    
    public Post() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Post(String title, String content, User author) {
        this();
        this.title = title;
        this.content = content;
        this.author = author;
        this.authorId = author != null ? author.getId() : null;
    }
}