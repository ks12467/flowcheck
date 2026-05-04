package com.bootcamp.flowcheck.domain.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CourseUpdateRequest {

    @NotBlank(message = "강의 이름은 필수입니다.")
    private String name;

    @Valid
    private List<WeekRequest> weeks;
}
