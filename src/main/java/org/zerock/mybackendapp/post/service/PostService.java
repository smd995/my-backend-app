package org.zerock.mybackendapp.post.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.mybackendapp.post.domain.Post;
import org.zerock.mybackendapp.post.repository.PostRepository;
import org.zerock.mybackendapp.user.domain.User;
import org.zerock.mybackendapp.user.service.UserService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;

    @Transactional
    public Post createPost(String title, String content, Long authorId) {
        log.info("게시글 생성 요청: title={}, authorId={}", title, authorId);

        // 작성자 조회
        User author = userService.getUserById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("작성자를 찾을 수 없습니다: " + authorId));

        //도메인 객체 생성 및 저장
        Post post = Post.of(title, content, author);
        Post savedPost = postRepository.save(post);

        log.info("게시글 생성 완료: id={}, title={}, author={}",
                savedPost.getId(), savedPost.getTitle(), author.getUsername());
        return savedPost;
    }

    public List<Post> getAllPosts() {
        log.info("모든 게시글 조회 요청");
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        log.info("게시글 조회 완료: {} 개", posts.size());
        return posts;
    }

    public Optional<Post> getPostById(Long id){
        log.info("ID로 게시글 조회: {}", id);
        Optional<Post> post = postRepository.findById(id);
        if(post.isPresent()) {
            log.info("게시글 조회 성공: {}", post.get());
        } else {
            log.warn("게시글을 찾을 수 없음: ID={}", id);
        }

        return post;
    }

    public List<Post> getPostsByAuthor(Long authorId) {
        log.info("작성자별 게시글 조회: authorId={}", authorId);

        User author = userService.getUserById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("작성자를 찾을 수 없습니다: " + authorId));

        List<Post> posts = postRepository.findByAuthorOrderByCreatedAtDesc(author);
        log.info("작성자 {}의 게시글 조회 완료: {} 개", author.getUsername(), posts.size());

        return posts;
    }

    public List<Post> searchPostsByTitle(String keyword) {
        log.info("제목으로 게시글 검색: keyword={}", keyword);
        List<Post> posts = postRepository.findByTitleContaining(keyword);
        log.info("검색 결과: {} 개", posts.size());
        return posts;
    }

    @Transactional
    public Post updatePost(Long postId, String title, String content, Long userId) {
        log.info("게시글 업데이트 요청: postId={}, userId={}", postId, userId);

        // 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        //  사용자 조회
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 권한 확인
        if(!post.isAuthoredBy(user)) {
            log.warn("게시글 수정 권한 없음: postId={}, userId={}", postId, userId);
            throw new IllegalArgumentException("게시글을 수정할 권한이 없습니다.");
        }

        post.updateTitle(title);
        post.updateContent(content);

        Post updatedPost = postRepository.save(post);
        log.info("게시글 업데이트 완료: id={}, title={}", updatedPost.getId(), updatedPost.getTitle());

        return updatedPost;
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        log.info("게시글 삭제 요청: postId={}, userId={}", postId, userId);

        // 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        //  사용자 조회
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 권한 확인
        if(!post.isAuthoredBy(user)) {
            log.warn("게시글 삭제 권한 없음: postId={}, userId={}", postId, userId);
            throw new IllegalArgumentException("게시글을 삭제할 권한이 없습니다.");
        }

        postRepository.deleteById(postId);
        log.info("게시글 삭제 완료: postId={}", postId);
    }
}
