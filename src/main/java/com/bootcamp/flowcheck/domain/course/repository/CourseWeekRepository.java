package com.bootcamp.flowcheck.domain.course.repository;

import com.bootcamp.flowcheck.domain.course.entity.CourseWeek;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseWeekRepository extends JpaRepository<CourseWeek, Long> {

    boolean existsByCourse_IdAndWeekNumber(Long courseId, Integer weekNumber);
}
