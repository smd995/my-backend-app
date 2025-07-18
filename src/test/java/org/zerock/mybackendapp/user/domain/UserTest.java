package org.zerock.mybackendapp.user.domain;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.org.hibernate.SQL=DEBUG"
})
@Slf4j
@DisplayName("User Domain 테스트")
class UserTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("유효한 사용자 생성 테스트")
    void createValidUser() {
        log.info("=== 유효한 사용자 생성 테스트 시작 ===");

        // Given
        String username = "testuser";
        String email = "test@example.com";

        // When
        User user = User.of(username, email);

        log.info("사용자 생성: username={}, email={}", username, email);

        // Then
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getId()).isNull();

        log.info("=== 유효한 사용자 생성 테스트 완료 ===");
    }

    @Test
    @DisplayName("사용자 엔티티 저장 테스트")
    void saveUser() {
        log.info("=== 사용자 엔티티 저장 테스트 시작 ===");

        // Given
        User user = User.of("saveuser", "save@example.com");

        // When
        User savedUser = entityManager.persistAndFlush(user);

        log.info("사용자 저장 완료: id={}, username={}", savedUser.getId(), savedUser.getUsername());

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isEqualTo(savedUser.getUpdatedAt());

        log.info("=== 사용자 엔티티 저장 테스트 완료 ===");
    }

    @Test
    @DisplayName("사용자 정보 업데이트 테스트")
    void updateUser() throws InterruptedException {
        log.info("=== 사용자 정보 업데이트 테스트 시작 ===");

        // Given
        User user = User.of("updateuser", "update@example.com");
        User savedUser = entityManager.persistAndFlush(user);
        LocalDateTime originalUpdatedAt = savedUser.getUpdatedAt();

        log.info("초기 저장: updatedAt={}", originalUpdatedAt);

        Thread.sleep(100); // 시간 차이 생성

        // When
        savedUser.updateEmail("newemail@example.com");
        User updatedUser = entityManager.persistAndFlush(savedUser);

        log.info("업데이트 후: updatedAt={}", updatedUser.getUpdatedAt());

        // Then
        assertThat(updatedUser.getEmail()).isEqualTo("newemail@example.com");
        assertThat(updatedUser.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updatedUser.getCreatedAt()).isEqualTo(savedUser.getCreatedAt());

        log.info("=== 사용자 정보 업데이트 테스트 완료 ===");
    }

    @Test
    @DisplayName("잘못된 이메일 형식 검증 테스트")
    void validateInvalidEmail() {
        log.info("=== 잘못된 이메일 형식 검증 테스트 시작 ===");

        // When & Then
        assertThatThrownBy(() -> User.of("testuser", "invalid-email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 이메일 형식");

        log.info("잘못된 이메일 형식 검증 완료");
        log.info("=== 잘못된 이메일 형식 검증 테스트 완료 ===");
    }

    @Test
    @DisplayName("빈 사용자명 검증 테스트")
    void validateEmptyUsername() {
        log.info("=== 빈 사용자명 검증 테스트 시작 ===");

        // When & Then
        assertThatThrownBy(() -> User.of("", "test@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자명은 필수");

        assertThatThrownBy(() -> User.of(null, "test@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자명은 필수");

        log.info("빈 사용자명 검증 완료");
        log.info("=== 빈 사용자명 검증 테스트 완료 ===");
    }

    @Test
    @DisplayName("사용자명 길이 제한 검증 테스트")
    void validateUsernameLengthLimit() {
        log.info("=== 사용자명 길이 제한 검증 테스트 시작 ===");

        // Given
        String longUsername = "a".repeat(51); // 50자 초과

        // When & Then
        assertThatThrownBy(() -> User.of(longUsername, "test@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자명은 50자를 초과할 수 없습니다");

        log.info("사용자명 길이 제한 검증 완료");
        log.info("=== 사용자명 길이 제한 검증 테스트 완료 ===");
    }


}
