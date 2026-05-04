package com.bootcamp.flowcheck.domain.auth.repository;

import com.bootcamp.flowcheck.domain.auth.entity.Pm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PmRepository extends JpaRepository<Pm, Long> {
    Optional<Pm> findByEmail(String email);
}
