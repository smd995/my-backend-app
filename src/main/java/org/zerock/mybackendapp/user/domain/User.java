package org.zerock.mybackendapp.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class User {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 기존 정적 팩토리 메서드 (하위 호환성)
    public static User of(String username, String email) {
        return of(username, email, "defaultPassword", UserRole.USER);
    }

    // 새로운 정적 팩토리 메서드 (인증 기능 포함)
    public static User of(String username, String email, String rawPassword, UserRole role) {
        validateUsername(username);
        validateEmail(email);
        validatePassword(rawPassword);
        validateRole(role);

        User user = new User();
        user.username = username;
        user.email = email;
        user.password = rawPassword; // 실제로는 서비스에서 인코딩됨
        user.role = role;

        log.info("새 사용자 도메인 객체 생성: username={}, email={}, role={}", username, email, role);

        return user;
    }

    // 패스워드 인코딩 (서비스 레이어에서 호출)
    public void encodePassword(PasswordEncoder passwordEncoder) {
        log.info("패스워드 인코딩 시작: username={}", this.username);
        this.password = passwordEncoder.encode(this.password);
        log.info("패스워드 인코딩 완료: username={}", this.username);
    }

    // 패스워드 검증
    public boolean matchesPassword(String rawPassword, PasswordEncoder passwordEncoder) {
        log.info("패스워드 검증 시작: username={}", this.username);
        boolean matches = passwordEncoder.matches(rawPassword, this.password);
        log.info("패스워드 검증 결과: username={}, matches={}", this.username, matches);
        return matches;
    }

    // 도메인 로직 - 이메일 업데이트
    public void updateEmail(String newEmail) {
        validateEmail(newEmail);
        log.info("사용자 이메일 업데이트: {} => {}", this.email, newEmail);
        this.email = newEmail;
    }

    // 패스워드 변경
    public void changePassword(String currentPassword, String newPassword, PasswordEncoder passwordEncoder) {
        log.info("패스워드 변경 시도: username={}", this.username);

        if (!matchesPassword(currentPassword, passwordEncoder)) {
            log.warn("현재 패스워드 불일치: username={}", this.username);
            throw new IllegalArgumentException("현재 패스워드가 일치하지 않습니다.");
        }

        validatePassword(newPassword);
        this.password = passwordEncoder.encode(newPassword);
        log.info("패스워드 변경 완료: username={}", this.username);
    }

    // 권한 확인
    public boolean hasRole(UserRole role) {
        boolean hasRole = this.role == role;
        log.info("권한 확인: username={}, requiredRole={}, hasRole={}",
                this.username, role, hasRole);
        return hasRole;
    }

    public boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }

    private static void validateUsername(String username) {
        if(username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자명은 필수입니다.");
        }
        if(username.length() > 50) {
            throw new IllegalArgumentException("사용자명은 50자를 초과할 수 없습니다.");
        }
    }

    private static void validateEmail(String email) {
        if(email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }

        if(!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다.");
        }
    }

    private static void validatePassword(String password) {
        if(password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("패스워드는 필수입니다.");
        }
        if(password.length() < 6) {
            throw new IllegalArgumentException("패스워드는 최소 6자 이상이어야 합니다.");
        }
    }

    private static void validateRole(UserRole role) {
        if(role == null) {
            throw new IllegalArgumentException("사용자 역할은 필수입니다.");
        }
    }

    @PrePersist
    protected void onCreate() {
        log.info("사용자 엔티티 저장: username={]", username);
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        log.info("사용자 엔티티 업데이트: username={}", username);
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', email='%s'}", id, username, email);
    }

    // 사용자 역할 enum
    public enum UserRole {
        USER, ADMIN
    }
}
