package com.artSight.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "artworks")
@Data
public class Artwork {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String artist;
    private String dateCreated;
    private String medium;
    private String culture;
    private String department;

    @Column(length = 2000)
    private String description;

    @Column(length = 1000)
    private String imageUrl;

    @Column(unique = true)
    private String sourceId; // original ID from Met/WikiArt, for dedup
}