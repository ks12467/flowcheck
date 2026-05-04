package com.bootcamp.flowcheck.domain.student.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class StudentBulkCreateRequest {

    @NotEmpty(message = "수강생 목록은 비어있을 수 없습니다.")
    @Valid
    private List<StudentCreateRequest> students;
}
