package com.judicator.gateway.modules.identity.dto.authentication.request;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationRequest(@NotBlank String username, @NotBlank String password) {}
