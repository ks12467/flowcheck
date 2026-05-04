package com.bootcamp.flowcheck.domain.student.dto;

import com.bootcamp.flowcheck.domain.progress.entity.LearningProgress;
import com.bootcamp.flowcheck.domain.student.entity.Student;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class StudentResponse {

    private Long studentId;
    private String name;
    private String email;
    private String phone;
    private String riskLevel;
    private Integer latestConditionScore;
    private LocalDateTime latestSubmittedAt;

    public static StudentResponse of(Student student, LearningProgress latestProgress) {
        return new StudentResponse(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getPhone(),
                latestProgress != null ? latestProgress.getRiskLevel() : null,
                latestProgress != null ? latestProgress.getConditionScore() : null,
                latestProgress != null ? latestProgress.getSubmittedAt() : null
        );
    }
}
