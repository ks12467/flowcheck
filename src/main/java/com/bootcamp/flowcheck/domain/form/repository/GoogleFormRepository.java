package com.bootcamp.flowcheck.domain.form.repository;

import com.bootcamp.flowcheck.domain.form.entity.GoogleForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GoogleFormRepository extends JpaRepository<GoogleForm, Long> {

    List<GoogleForm> findAllByTrack_Id(Long trackId);

    @Query("SELECT f FROM GoogleForm f JOIN FETCH f.track WHERE f.track.id IN :trackIds ORDER BY f.track.id, f.id")
    List<GoogleForm> findAllByTrackIdIn(@Param("trackIds") List<Long> trackIds);
}
