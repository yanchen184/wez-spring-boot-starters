package com.company.common.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Challenge for citizen certificate authentication")
public record CertChallengeResponse(
        @Schema(description = "Unique challenge ID (pass back in login request)")
        String challengeId,

        @Schema(description = "Random nonce to be signed by the citizen certificate's private key")
        String nonce
) {}
