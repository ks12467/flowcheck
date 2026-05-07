package com.bootcamp.flowcheck.domain.form.entity;

import com.bootcamp.flowcheck.domain.track.entity.Track;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "GOOGLE_FORM")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor
public class GoogleForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "form_url", nullable = false, columnDefinition = "TEXT")
    private String formUrl;

    @Column(name = "spreadsheet_id")
    private String spreadsheetId;

    @Column(name = "score_column_header")
    private String scoreColumnHeader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public GoogleForm(String name, String formUrl, String spreadsheetId,
                      String scoreColumnHeader, Track track, LocalDateTime createdAt) {
        this.name = name;
        this.formUrl = formUrl;
        this.spreadsheetId = spreadsheetId;
        this.scoreColumnHeader = scoreColumnHeader;
        this.track = track;
        this.createdAt = createdAt;
    }

    public void update(String name, String formUrl, String spreadsheetId,
                       String scoreColumnHeader, Track track) {
        this.name = name;
        this.formUrl = formUrl;
        this.spreadsheetId = spreadsheetId;
        this.scoreColumnHeader = scoreColumnHeader;
        this.track = track;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
