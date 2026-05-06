package com.bootcamp.flowcheck.domain.track.entity;

import com.bootcamp.flowcheck.domain.auth.entity.Pm;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "PM_TRACK")
@Getter
@NoArgsConstructor
public class PmTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pm_id", nullable = false)
    private Pm pm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(length = 50)
    private String role;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public PmTrack(Pm pm, Track track, String role) {
        this.pm = pm;
        this.track = track;
        this.role = role != null ? role : "MANAGER";
    }
}
