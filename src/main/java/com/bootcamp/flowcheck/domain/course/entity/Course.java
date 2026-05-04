package com.bootcamp.flowcheck.domain.course.entity;

import com.bootcamp.flowcheck.domain.track.entity.Track;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "COURSE")
@Getter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weekNumber ASC")
    private List<CourseWeek> weeks = new ArrayList<>();

    @Builder
    public Course(Track track, String name) {
        this.track = track;
        this.name = name;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void replaceWeeks(List<CourseWeek> newWeeks) {
        this.weeks.clear();
        this.weeks.addAll(newWeeks);
    }
}
