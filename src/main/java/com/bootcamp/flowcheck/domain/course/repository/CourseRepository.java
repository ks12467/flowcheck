package com.bootcamp.flowcheck.domain.course.repository;

import com.bootcamp.flowcheck.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.weeks WHERE c.track.id = :trackId ORDER BY c.id")
    List<Course> findAllByTrackIdWithWeeks(@Param("trackId") Long trackId);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.weeks WHERE c.id = :courseId")
    Optional<Course> findByIdWithWeeks(@Param("courseId") Long courseId);

    Optional<Course> findByIdAndTrack_Id(Long id, Long trackId);

    @Query(value = "SELECT * FROM COURSE WHERE id = :id AND deleted_at IS NOT NULL", nativeQuery = true)
    Optional<Course> findDeletedById(@Param("id") Long id);

    @Modifying
    @Query(value = "DELETE FROM COURSE WHERE id = :id", nativeQuery = true)
    void permanentDeleteById(@Param("id") Long id);
}
