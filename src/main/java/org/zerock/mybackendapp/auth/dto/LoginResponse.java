package org.zerock.mybackendapp.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zerock.mybackendapp.user.domain.User;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class LoginResponse {

    private Long userId;
    private String username;
    private String email;
    private String role;
    private String accessToken;
    private String refreshToken;

    public static LoginResponse of(User user, String accessToken, String refreshToken) {
        log.info("로그인 응답 DTO 생성: username={}, userId={}", user.getUsername(), user.getId());
        log.info("토큰 정보: accessToken 길이={}, refreshToken 길이={}",
                accessToken.length(), refreshToken.length());

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                accessToken,
                refreshToken
        );
    }

}
