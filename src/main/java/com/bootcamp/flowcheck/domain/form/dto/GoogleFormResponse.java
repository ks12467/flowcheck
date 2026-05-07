package com.bootcamp.flowcheck.domain.form.dto;

import com.bootcamp.flowcheck.domain.form.entity.GoogleForm;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GoogleFormResponse {

    private Long formId;
    private String name;
    private String formUrl;
    private String spreadsheetId;
    private String scoreColumnHeader;
    private Long trackId;
    private String trackName;

    public static GoogleFormResponse of(GoogleForm f) {
        return new GoogleFormResponse(
                f.getId(),
                f.getName(),
                f.getFormUrl(),
                f.getSpreadsheetId(),
                f.getScoreColumnHeader(),
                f.getTrack().getId(),
                f.getTrack().getName()
        );
    }
}
