package org.zerock.mybackendapp.post.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.mybackendapp.post.domain.Post;
import org.zerock.mybackendapp.post.service.PostService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Map<String, Object> request) {
        try {
            log.info("=== 게시글 생성 요청: {} ===", request);

            String title = (String) request.get("title");
            String content = (String) request.get("content");
            Object authorIdObj = request.get("authorId");

            // 필수 필드 검증
            if(title == null || content == null || authorIdObj == null) {
                log.warn("필수 필드 누락: title={}, content={}, authorId={}", title, content, authorIdObj);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "title, content, authorId는 필수입니다."));
            }

            Long authorId = Long.valueOf(authorIdObj.toString());

            Post post = postService.createPost(title, content, authorId);
            log.info("게시글 생성 성공: id={}, title={}", post.getId(), post.getTitle());

            return ResponseEntity.status(HttpStatus.CREATED).body(post);

        } catch (IllegalArgumentException e) {
            log.error("게시글 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "내부 서버 오류"));
        }
    };

    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        log.info("=== 모든 게시글 조회 요청 ===");

        List<Post> posts = postService.getAllPosts();
        log.info("게시글 조회 완료: {} 개", posts.size());

        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable("id") Long id) {
        log.info("=== ID로 게시글 조회: {} ===", id);

        return postService.getPostById(id)
                .map(post -> {
                    log.info("게시글 조회 성공: {}", post.getTitle());
                    return ResponseEntity.ok(post);
                })
                .orElseGet(() -> {
                    log.warn("게시글을 찾을 수 없음: ID={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<Post>> getPostsByAuthorId(@PathVariable("authorId") Long authorId) {
        log.info("=== 작성자별 게시글 조회: {} ===", authorId);

        try {
            List<Post> posts = postService.getPostsByAuthor(authorId);
            log.info("작성자별 게시글 조회 완료: {} 개", posts.size());
            return ResponseEntity.ok(posts);
        } catch (IllegalArgumentException e) {
            log.error("작성자별 게시글 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Post>> searchPostsByTitle(@RequestParam("keyword") String keyword) {
        log.info("=== 제목으로 게시글 검색: {} ===", keyword);

        List<Post> posts = postService.searchPostsByTitle(keyword);
        log.info("검색 결과: {} 개", posts.size());

        return ResponseEntity.ok(posts);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable("id") Long id, @RequestBody Map<String, Object> request) {

        try {
            log.info("=== 게시글 업데이트 요청: id={}, data={} ===", id, request);

            String title = (String) request.get("title");
            String content = (String) request.get("content");
            Object userIdObj = request.get("userId");

            if(title == null || content == null || userIdObj == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "title, content, userId는 필수입니다."));
            }

            Long userId = Long.valueOf(userIdObj.toString());

            Post updatedPost = postService.updatePost(id, title, content, userId);
            log.info("게시글 업데이트 성공: {}", updatedPost);

            return ResponseEntity.ok(updatedPost);
        } catch (IllegalArgumentException e) {
            log.error("게시글 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "내부 서버 오류"));
        }

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id,
                                        @RequestBody Map<String, Object> request) {
        try {
            log.info("=== 게시글 삭제 요청: id={} ===", id);

            Object userIdObj = request.get("userId");
            if (userIdObj == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "userId는 필수입니다"));
            }

            Long userId = Long.valueOf(userIdObj.toString());

            postService.deletePost(id, userId);
            log.info("게시글 삭제 완료: id={}", id);

            return ResponseEntity.ok(Map.of("message", "게시글이 삭제되었습니다"));

        } catch (IllegalArgumentException e) {
            log.error("게시글 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "내부 서버 오류"));
        }
    }


}
