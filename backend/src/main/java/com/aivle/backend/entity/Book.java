package com.aivle.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(nullable = false)
    private String author;

    @NotBlank
    @Lob @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Lob @Column(columnDefinition = "LONGTEXT")
    private String coverImageUrl;

    private Integer likes;

    private Integer views;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column
    private String createdBy;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> tags = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.likes == null) this.likes = 0;
        if (this.views == null) this.views = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}