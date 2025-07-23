package org.zerock.mybackendapp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class RegisterRequest {

    @NotBlank(message = "사용자명은 필수입니다")
    @Size(max = 50, message = "사용자명은 50자를 초과할 수 없습니다")
    private String username;

    @NotBlank(message = "이메일은 필수입니다")
    @Size(message = "유효한 이메일 형식이여야 합니다")
    private String email;

    @NotBlank(message = "패스워드는 필수입니다")
    @Size(min = 6, message = "패스워드는 최소 6자 이상이어야 합니다")
    private String password;

    public static RegisterRequest of(String username, String email, String password) {
        log.info("회원가입 요청 DTO 생성: username={}, email={}", username, email);
        return new RegisterRequest(username, email, password);
    }

}
