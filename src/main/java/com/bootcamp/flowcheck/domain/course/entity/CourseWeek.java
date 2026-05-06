package com.bootcamp.flowcheck.domain.course.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "COURSE_WEEK")
@Getter
@NoArgsConstructor
public class CourseWeek {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "lecture_count", nullable = false)
    private Integer lectureCount;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public CourseWeek(Course course, Integer weekNumber, Integer lectureCount) {
        this.course = course;
        this.weekNumber = weekNumber;
        this.lectureCount = lectureCount;
    }
}
