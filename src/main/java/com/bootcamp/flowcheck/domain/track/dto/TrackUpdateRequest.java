package com.bootcamp.flowcheck.domain.track.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class TrackUpdateRequest {

    @NotBlank(message = "과정 분류는 필수입니다.")
    private String courseType;

    @NotNull(message = "기수는 필수입니다.")
    @Positive(message = "기수는 양수여야 합니다.")
    private Integer generation;

    private LocalDate startDate;
    private LocalDate endDate;

    public String buildName() {
        return courseType + " " + generation + "기";
    }
}
