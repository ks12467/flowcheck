package com.bootcamp.flowcheck.domain.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudentCreateRequest {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    private String email;
    private String phone;
}
