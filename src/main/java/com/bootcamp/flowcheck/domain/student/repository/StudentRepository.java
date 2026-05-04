package com.bootcamp.flowcheck.domain.student.repository;

import com.bootcamp.flowcheck.domain.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findAllByTrack_Id(Long trackId);

    @Query("""
            SELECT s FROM Student s
            WHERE s.track.id = :trackId
            AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    List<Student> findByTrackIdAndKeyword(@Param("trackId") Long trackId, @Param("keyword") String keyword);

    boolean existsByTrack_IdAndEmail(Long trackId, String email);

    Optional<Student> findByIdAndTrack_Id(Long id, Long trackId);

    List<Student> findAllByTrack_IdAndName(Long trackId, String name);

    long countByTrack_Id(Long trackId);
}
