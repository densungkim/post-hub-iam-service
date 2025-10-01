package com.post.hub.iamservice.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Getter
@Setter
public class Post {

    public static final String ID_FIELD = "id";
    public static final String TITLE_NAME_FIELD = "title";
    public static final String CONTENT_NAME_FIELD = "content";
    public static final String LIKES_NAME_FIELD = "likes";
    public static final String DELETED_FIELD = "deleted";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer likes = 0;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "created_by")
    private String createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime created = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updated = LocalDateTime.now();

}
