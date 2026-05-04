package com.bootcamp.flowcheck.domain.track.dto;

import com.bootcamp.flowcheck.domain.track.entity.Track;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class TrackResponse {

    private Long trackId;
    private String name;
    private String courseType;
    private Integer generation;
    private LocalDate startDate;
    private LocalDate endDate;
    private long studentCount;

    public static TrackResponse of(Track track, long studentCount) {
        return new TrackResponse(
                track.getId(),
                track.getName(),
                track.getCourseType(),
                track.getGeneration(),
                track.getStartDate(),
                track.getEndDate(),
                studentCount
        );
    }
}
