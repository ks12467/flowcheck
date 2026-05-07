package com.bootcamp.flowcheck.domain.form.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class GoogleFormRequest {

    @NotBlank(message = "폼 이름을 입력해주세요.")
    private String name;

    @NotNull(message = "트랙을 선택해주세요.")
    private Long trackId;

    @NotBlank(message = "구글폼 URL을 입력해주세요.")
    private String formUrl;

    private String spreadsheetId;
    private String scoreColumnHeader;
}
