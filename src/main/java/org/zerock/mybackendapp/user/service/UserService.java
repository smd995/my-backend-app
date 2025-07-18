package org.zerock.mybackendapp.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.mybackendapp.user.domain.User;
import org.zerock.mybackendapp.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User createUser(String username, String email) {
        log.info("사용자 생성 요청: username={}, email={}", username, email);

        // 중복 검증
        if(userRepository.existsByUsername(username)) {
            log.warn("이미 존재하는 사용자명: {}", username);
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다: " + username);
        }

        if(userRepository.existsByEmail(email)) {
            log.warn("이미 존재하는 이메일: {}", email);
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + email);
        }

        // 도메인 객체 생성 및 저장
        User user = User.of(username, email);
        User savedUser = userRepository.save(user);

        log.info("사용자 생성 완료: id={}, username={}", savedUser.getId(), savedUser.getUsername());

        return savedUser;
    }

    public List<User> getAllUsers() {
        log.info("모든 사용자 조회 요청");
        List<User> users = userRepository.findAll();
        log.info("사용자 조회 완료: {} 명", users.size());
        return users;
    }

    public Optional<User> getUserById(Long id) {
        log.info("ID로 사용자 조회: {}", id);
        Optional<User> user = userRepository.findById(id);
        if(user.isPresent()) {
            log.info("사용자 조회 성공: {}", user.get());
        } else {
            log.warn("사용자를 찾을 수 없음: ID={}", id);
        }
        return user;
    }

    public Optional<User> getUserByUsername(String username) {
        log.info("사용자명으로 조회: {}", username);
        Optional<User> user = userRepository.findByUsername(username);
        if(user.isPresent()) {
            log.info("사용자 조회 성공: {}", user.get());
        } else {
            log.info("사용자를 찾을 수 없음: username={}", username);
        }
        return user;
    }
}
