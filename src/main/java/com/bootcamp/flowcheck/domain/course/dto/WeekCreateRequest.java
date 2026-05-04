package com.bootcamp.flowcheck.domain.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WeekCreateRequest {

    @NotNull(message = "주차 번호는 필수입니다.")
    @Min(value = 1, message = "주차 번호는 1 이상이어야 합니다.")
    private Integer weekNumber;

    @NotNull(message = "강의 수는 필수입니다.")
    @Min(value = 1, message = "강의 수는 1 이상이어야 합니다.")
    private Integer lectureCount;
}
