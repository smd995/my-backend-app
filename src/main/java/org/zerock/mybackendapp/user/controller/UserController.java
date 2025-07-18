package org.zerock.mybackendapp.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.mybackendapp.user.domain.User;
import org.zerock.mybackendapp.user.service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        try{
            log.info("=== 사용자 생성 요청: {} ===", request);

            String username = request.get("username");
            String email = request.get("email");

            // 필수 필드 검증
            if(username == null || email == null) {
                log.warn("필수 필드 누락: username={}, email={}", username, email);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "username과 email은 필수입니다"));
            }

            User user = userService.createUser(username, email);
            log.info("사용자 생성 성공: id={}, username={}", user.getId(), user.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch(IllegalArgumentException e) {
            log.error("사용자 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch(Exception e) {
            log.error("예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "내부 서버 오류"));
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("=== 모든 사용자 조회 요청 ===");

        List<User> users = userService.getAllUsers();
        log.info("사용자 조회 완료: {} 명", users.size());

        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable("id") Long id) {
        log.info("=== ID로 사용자 조회: {} ===", id);

        return userService.getUserById(id)
                .map(user -> {
                    log.info("사용자 조회 성공: {}", user.getUsername());
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    log.warn("사용자를 찾을 수 없음: ID={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable("username") String username) {
        log.info("=== 사용자명으로 조회: {} ===", username);

        return userService.getUserByUsername(username)
                .map(user -> {
                    log.info("사용자 조회 성공: {}", user.getEmail());
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    log.warn("사용자를 찾을 수 없음: username={}", username);
                    return ResponseEntity.notFound().build();
                });
    }


}
