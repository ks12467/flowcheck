package com.bootcamp.flowcheck.domain.student.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BulkCreateResponse {

    private int successCount;
    private int failCount;
}
