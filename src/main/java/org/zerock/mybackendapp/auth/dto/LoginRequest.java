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
public class LoginRequest {

    @NotBlank(message = "사용자명은 필수입니다")
    private String username;

    @NotBlank(message = "패스워드는 필수입니다")
    private String password;

    public static LoginRequest of(String username, String password) {
        log.info("로그인 요청 DTO 생성: username={}", username);
        return new LoginRequest(username, password);
    }

}
