package org.zerock.mybackendapp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class TokenRefreshRequest {

    @NotBlank(message = "리프레시 토큰은 필수입니다")
    private String refreshToken;

    public static TokenRefreshRequest of(final String refreshToken) {
        log.info("토큰 갱신 요청 DTO 생성: refreshToken 길이={}", refreshToken.length());
        return new TokenRefreshRequest(refreshToken);
    }

}
