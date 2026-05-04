package com.bootcamp.flowcheck.domain.track.dto;

import com.bootcamp.flowcheck.domain.track.entity.Track;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class TrackDetailResponse {

    private Long trackId;
    private String name;
    private String courseType;
    private Integer generation;
    private LocalDate startDate;
    private LocalDate endDate;
    private long studentCount;
    private List<PmResponse> pms;

    public static TrackDetailResponse of(Track track, long studentCount, List<PmResponse> pms) {
        return new TrackDetailResponse(
                track.getId(),
                track.getName(),
                track.getCourseType(),
                track.getGeneration(),
                track.getStartDate(),
                track.getEndDate(),
                studentCount,
                pms
        );
    }
}
