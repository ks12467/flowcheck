package com.bootcamp.flowcheck.domain.progress.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LearningProgressSubmitRequest {

    @NotBlank(message = "수강생 이름은 필수입니다.")
    private String studentName;

    @NotNull(message = "코스 주차 ID는 필수입니다.")
    private Long courseWeekId;

    private Integer lectureNumber;
    private String status;
    private Integer assignmentProgress;
    private Boolean tilWritten;

    @NotNull(message = "난이도 점수는 필수입니다.")
    @Min(value = 1, message = "난이도는 1 이상이어야 합니다.")
    @Max(value = 5, message = "난이도는 5 이하여야 합니다.")
    private Integer conditionScore;

    @Min(value = 1, message = "이해도는 1 이상이어야 합니다.")
    @Max(value = 5, message = "이해도는 5 이하여야 합니다.")
    private Integer understandingScore;

    @Min(value = 1, message = "몰입도는 1 이상이어야 합니다.")
    @Max(value = 5, message = "몰입도는 5 이하여야 합니다.")
    private Integer immersionScore;

    private String comment;
}
