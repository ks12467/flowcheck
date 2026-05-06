package com.bootcamp.flowcheck.domain.progress.entity;

import com.bootcamp.flowcheck.domain.course.entity.CourseWeek;
import com.bootcamp.flowcheck.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "LEARNING_PROGRESS")
@Getter
@NoArgsConstructor
public class LearningProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_week_id", nullable = false)
    private CourseWeek courseWeek;

    @Column(name = "lecture_number")
    private Integer lectureNumber;

    @Column(length = 20)
    private String status;

    @Column(name = "assignment_progress")
    private Integer assignmentProgress;

    @Column(name = "til_written")
    private Boolean tilWritten;

    @Column(name = "condition_score")
    private Integer conditionScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public LearningProgress(Student student, CourseWeek courseWeek, Integer lectureNumber,
                            String status, Integer assignmentProgress, Boolean tilWritten,
                            Integer conditionScore, String riskLevel, String comment,
                            LocalDateTime submittedAt) {
        this.student = student;
        this.courseWeek = courseWeek;
        this.lectureNumber = lectureNumber;
        this.status = status;
        this.assignmentProgress = assignmentProgress;
        this.tilWritten = tilWritten;
        this.conditionScore = conditionScore;
        this.riskLevel = riskLevel;
        this.comment = comment;
        this.submittedAt = submittedAt;
    }
}
