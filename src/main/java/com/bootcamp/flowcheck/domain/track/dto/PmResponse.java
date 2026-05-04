package com.bootcamp.flowcheck.domain.track.dto;

import com.bootcamp.flowcheck.domain.auth.entity.Pm;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PmResponse {

    private Long pmId;
    private String name;
    private String email;

    public static PmResponse of(Pm pm) {
        return new PmResponse(pm.getId(), pm.getName(), pm.getEmail());
    }
}
