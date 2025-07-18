package org.zerock.mybackendapp.user.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.zerock.mybackendapp.user.domain.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.org.hibernate.SQL=DEBUG"
})
@Slf4j
@DisplayName("User Repository 테스트")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        log.info("=== 테스트 데이터 정리 ===");
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("사용자 저장 및 ID로 조회")
    void saveAndFindById() {
        log.info("=== 사용자 저장 및 ID로 조회 테스트 시작 ===");

        // Given
        User user = User.of("testuser", "test@example.com");

        // When
        User savedUser = userRepository.save(user);
        log.info("사용자 저장: {}", savedUser);

        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");

        log.info("ID로 조회된 사용자: {}", foundUser.get());
        log.info("=== 사용자 저장 및 ID로 조회 테스트 완료 ===");
    }

    @Test
    @DisplayName("사용자명으로 조회")
    void findByUsername() {
        log.info("=== 사용자명으로 조회 테스트 시작 ===");

        // Given
        User user = User.of("finduser", "find@example.com");
        userRepository.save(user);

        // When
        Optional<User> foundUser = userRepository.findByUsername("finduser");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("find@example.com");

        log.info("사용자명으로 조회된 사용자: {}", foundUser.get());
        log.info("=== 사용자명으로 조회 테스트 완료 ===");
    }

    @Test
    @DisplayName("이메일로 조회")
    void findByEmail() {
        log.info("=== 이메일로 조회 테스트 시작 ===");

        // Given
        User user = User.of("emailuser", "email@example.com");
        userRepository.save(user);

        // When
        Optional<User> foundUser = userRepository.findByEmail("email@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("emailuser");

        log.info("이메일로 조회된 사용자: {}", foundUser.get());
        log.info("=== 이메일로 조회 테스트 완료 ===");
    }

    @Test
    @DisplayName("사용자명 존재 여부 확인")
    void existsByUsername() {
        log.info("=== 사용자명 존재 여부 확인 테스트 시작 ===");

        // Given
        User user = User.of("existuser", "exist@example.com");
        userRepository.save(user);

        // When & Then
        assertThat(userRepository.existsByUsername("existuser")).isTrue();
        assertThat(userRepository.existsByUsername("nonexist")).isFalse();

        log.info("사용자명 존재 여부 확인 완료");
        log.info("=== 사용자명 존재 여부 확인 테스트 완료 ===");
    }

    @Test
    @DisplayName("이메일 존재 여부 확인")
    void existsByEmail() {
        log.info("=== 이메일 존재 여부 확인 테스트 시작 ===");

        // Given
        User user = User.of("emailcheck", "emailcheck@example.com");
        userRepository.save(user);

        // When & Then
        assertThat(userRepository.existsByEmail("emailcheck@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexist@example.com")).isFalse();

        log.info("이메일 존재 여부 확인 완료");
        log.info("=== 이메일 존재 여부 확인 테스트 완료 ===");
    }

    @Test
    @DisplayName("사용자명 부분 검색")
    void findByUsernameContaining() {
        log.info("=== 사용자명 부분 검색 테스트 시작 ===");

        // Given
        User user1 = User.of("searchuser1", "searchuser1@example.com");
        User user2 = User.of("searchuser2", "searchuser2@example.com");
        User user3 = User.of("diffrent", "diffrent@example.com");
        userRepository.saveAll(List.of(user1, user2, user3));

        // When
        List<User> searchResults = userRepository.findByUsernameContaining("search");

        // Then
        assertThat(searchResults).hasSize(2);
        assertThat(searchResults).extracting(User::getUsername).containsExactlyInAnyOrder("searchuser1", "searchuser2");

        log.info("검색 결과: {} 개", searchResults.size());
        searchResults.forEach(user -> log.info("  - {}", user));
        log.info("=== 사용자명 부분 검색 테스트 완료 ===");
    }



}
