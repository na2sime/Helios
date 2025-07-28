package fr.nassime.helios.postgres.entities;

import fr.nassime.helios.api.annotations.*;
import fr.nassime.helios.api.annotations.enums.FetchType;
import fr.nassime.helios.api.annotations.enums.GenerationType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Test entity representing a Comment.
 */
@Entity
@Table(name = "comments")
@Data
public class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "content", nullable = false)
    private String content;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;
    
    public Comment() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Comment(String content, Post post, User author) {
        this();
        this.content = content;
        this.post = post;
        this.author = author;
    }
}