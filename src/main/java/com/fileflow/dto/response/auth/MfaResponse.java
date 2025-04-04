package com.fileflow.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for MFA setup
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaResponse {

    private String secret;
    private String qrCodeUrl;
    private String recoveryCodes;
}