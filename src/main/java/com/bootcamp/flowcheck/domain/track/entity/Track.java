package com.bootcamp.flowcheck.domain.track.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TRACK")
@Getter
@NoArgsConstructor
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "course_type", length = 50)
    private String courseType;

    @Column(name = "generation")
    private Integer generation;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Track(String name, String description, String courseType, Integer generation,
                 LocalDate startDate, LocalDate endDate, LocalDateTime createdAt) {
        this.name = name;
        this.description = description;
        this.courseType = courseType;
        this.generation = generation;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
    }

    public void update(String name, String courseType, Integer generation,
                       LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.courseType = courseType;
        this.generation = generation;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
