package com.bootcamp.flowcheck.domain.auth.service;

import com.bootcamp.flowcheck.domain.auth.entity.Pm;
import com.bootcamp.flowcheck.domain.auth.repository.PmRepository;
import com.bootcamp.flowcheck.global.exception.BusinessException;
import com.bootcamp.flowcheck.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PmRepository pmRepository;

    @Transactional
    public void invite(String name, String email) {
        if (pmRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_REGISTERED);
        }
        Pm pm = Pm.builder()
                .name(name)
                .email(email)
                .role("PM")
                .createdAt(LocalDateTime.now())
                .build();
        pmRepository.save(pm);
    }
}
