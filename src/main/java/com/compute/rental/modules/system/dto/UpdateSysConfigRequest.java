package com.compute.rental.modules.system.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSysConfigRequest(
        @NotBlank
        String configValue,

        String configDesc
) {
}
