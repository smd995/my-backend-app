package org.zerock.mybackendapp.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.mybackendapp.post.domain.Post;
import org.zerock.mybackendapp.post.repository.PostRepository;
import org.zerock.mybackendapp.post.service.PostService;
import org.zerock.mybackendapp.user.domain.User;
import org.zerock.mybackendapp.user.repository.UserRepository;
import org.zerock.mybackendapp.user.service.UserService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
@Transactional
@Slf4j
@DisplayName("Post 전체 통합 테스트")
class PostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private User testAuthor;

    @BeforeEach
    void setUp() {
        log.info("=== 통합 테스트 데이터 초기화 ===");
        postRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트용 작성자 생성
        testAuthor = userService.createUser("postAuthor", "post@example.com");
        log.info("테스트 작성자 생성: {}", testAuthor);
    }

    @Test
    @DisplayName("Post 도메인 전체 플로우 테스트 (User-Post 관계)")
    void postDomainFullFlowTest() throws Exception {
        log.info("=== Post 도메인 전체 플로우 테스트 시작 ===");

        // 1. API로 게시글 생성
        String title = "통합테스트 게시글";
        String content = "통합테스트 내용입니다.";
        Map<String, Object> createRequest = Map.of(
                "title", title,
                "content", content,
                "authorId", testAuthor.getId()
        );

        String response = mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.author.username").value("postAuthor"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Post createdPost = objectMapper.readValue(response, Post.class);
        log.info("API로 게시글 생성 완료: {}", createdPost);

        // 2. Service로 게시글 조회
        Post servicePost = postService.getPostById(createdPost.getId()).orElseThrow();
        log.info("Service로 조회한 게시글: {}", servicePost);

        // 3. Repository로 직접 조회
        Post repoPost = postRepository.findById(createdPost.getId()).orElseThrow();
        log.info("Repository로 조회한 게시글: {}", repoPost);

        // 4. User-Post 관계 확인
        assertThat(servicePost.getAuthor().getId()).isEqualTo(testAuthor.getId());
        assertThat(servicePost.getAuthor().getUsername()).isEqualTo("postAuthor");

        // 5. 도메인 로직 테스트
        String newContent = "업데이트된 내용";
        servicePost.updateContent(newContent);
        Post updatedPost = postRepository.save(servicePost);
        log.info("도메인 로직으로 내용 업데이트: {}", updatedPost);

        // 6. 검증
        assertThat(createdPost.getId()).isEqualTo(servicePost.getId());
        assertThat(servicePost.getId()).isEqualTo(repoPost.getId());
        assertThat(updatedPost.getContent()).isEqualTo(newContent);
        assertThat(updatedPost.getAuthor().getId()).isEqualTo(testAuthor.getId());

        log.info("=== Post 도메인 전체 플로우 테스트 완료 ===");
    }

    @Test
    @DisplayName("User-Post 관계 통합 테스트")
    void userPostRelationshipTest() throws Exception {
        log.info("=== User-Post 관계 통합 테스트 시작 ===");

        // 1. 추가 사용자 생성
        User author2 = userService.createUser("author2", "author2@example.com");
        User author3 = userService.createUser("author3", "author3@example.com");

        // 2. 각 사용자별로 게시글 생성
        Post post1 = postService.createPost("첫 번째 게시글", "내용1", testAuthor.getId());
        Post post2 = postService.createPost("두 번째 게시글", "내용2", testAuthor.getId());
        Post post3 = postService.createPost("다른 사용자 게시글", "내용3", author2.getId());

        log.info("게시글 3개 생성 완료");

        // 3. 작성자별 게시글 조회 테스트
        List<Post> author1Posts = postService.getPostsByAuthor(testAuthor.getId());
        List<Post> author2Posts = postService.getPostsByAuthor(author2.getId());
        List<Post> author3Posts = postService.getPostsByAuthor(author3.getId());

        assertThat(author1Posts).hasSize(2);
        assertThat(author2Posts).hasSize(1);
        assertThat(author3Posts).hasSize(0);

        log.info("작성자별 게시글 수: author1={}, author2={}, author3={}",
                author1Posts.size(), author2Posts.size(), author3Posts.size());

        // 4. API로 작성자별 조회 테스트
        mockMvc.perform(get("/api/posts/author/{authorId}", testAuthor.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].author.username").value("postAuthor"))
                .andExpect(jsonPath("$[1].author.username").value("postAuthor"));

        log.info("=== User-Post 관계 통합 테스트 완료 ===");
    }

    @Test
    @DisplayName("게시글 권한 검증 통합 테스트")
    void postAuthorizationTest() throws Exception {
        log.info("=== 게시글 권한 검증 통합 테스트 시작 ===");

        // 1. 다른 사용자 생성
        User otherUser = userService.createUser("otheruser", "other@example.com");

        // 2. testAuthor가 게시글 생성
        Post post = postService.createPost("권한 테스트 게시글", "원본 내용", testAuthor.getId());
        log.info("게시글 생성: 작성자={}, 게시글ID={}", testAuthor.getUsername(), post.getId());

        // 3. 작성자가 수정 - 성공해야 함
        Map<String, Object> updateRequest = Map.of(
                "title", "수정된 제목",
                "content", "수정된 내용",
                "userId", testAuthor.getId()
        );

        mockMvc.perform(put("/api/posts/" + post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목"));

        log.info("작성자 본인의 게시글 수정 성공");

        // 4. 다른 사용자가 수정 시도 - 실패해야 함
        Map<String, Object> unauthorizedRequest = Map.of(
                "title", "권한 없는 수정",
                "content", "권한 없는 내용",
                "userId", otherUser.getId()
        );

        mockMvc.perform(put("/api/posts/" + post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unauthorizedRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("게시글을 수정할 권한이 없습니다."));

        log.info("다른 사용자의 게시글 수정 시도 실패 - 권한 검증 성공");

        // 5. 다른 사용자가 삭제 시도 - 실패해야 함
        Map<String, Object> deleteRequest = Map.of("userId", otherUser.getId());

        mockMvc.perform(delete("/api/posts/" + post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("게시글을 삭제할 권한이 없습니다."));

        log.info("다른 사용자의 게시글 삭제 시도 실패 - 권한 검증 성공");

        log.info("=== 게시글 권한 검증 통합 테스트 완료 ===");
    }

    @Test
    @DisplayName("게시글 검색 통합 테스트")
    void postSearchTest() throws Exception {
        log.info("=== 게시글 검색 통합 테스트 시작 ===");

        // 1. 검색용 게시글들 생성
        postService.createPost("Spring Boot 완벽 가이드", "Spring Boot 내용", testAuthor.getId());
        postService.createPost("Spring Security 튜토리얼", "Spring Security 내용", testAuthor.getId());
        postService.createPost("React 기초부터 심화까지", "React 내용", testAuthor.getId());
        postService.createPost("Vue.js 입문", "Vue.js 내용", testAuthor.getId());

        log.info("검색용 게시글 4개 생성 완료");

        // 2. "Spring" 키워드 검색
        mockMvc.perform(get("/api/posts/search")
                        .param("keyword", "Spring"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Spring Boot 완벽 가이드"))
                .andExpect(jsonPath("$[1].title").value("Spring Security 튜토리얼"));

        log.info("'Spring' 키워드 검색 완료");

        // 3. "기초" 키워드 검색
        mockMvc.perform(get("/api/posts/search")
                        .param("keyword", "기초"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("React 기초부터 심화까지"));

        log.info("'기초' 키워드 검색 완료");

        // 4. 없는 키워드 검색
        mockMvc.perform(get("/api/posts/search")
                        .param("keyword", "없는키워드"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        log.info("존재하지 않는 키워드 검색 완료");

        log.info("=== 게시글 검색 통합 테스트 완료 ===");
    }

    @Test
    @DisplayName("API 전체 CRUD 통합 테스트")
    void apiCrudIntegrationTest() throws Exception {
        log.info("=== API 전체 CRUD 통합 테스트 시작 ===");

        // 1. CREATE - 게시글 생성
        Map<String, Object> createRequest = Map.of(
                "title", "CRUD 테스트 게시글",
                "content", "CRUD 테스트 내용",
                "authorId", testAuthor.getId()
        );

        String createResponse = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Post createdPost = objectMapper.readValue(createResponse, Post.class);
        Long postId = createdPost.getId();
        log.info("CREATE 완료: {}", createdPost);

        // 2. READ - 모든 게시글 조회
        mockMvc.perform(get("/api/posts"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("CRUD 테스트 게시글"));

        log.info("READ ALL 완료");

        // 3. READ - ID로 게시글 조회
        mockMvc.perform(get("/api/posts/" + postId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("CRUD 테스트 게시글"))
                .andExpect(jsonPath("$.content").value("CRUD 테스트 내용"))
                .andExpect(jsonPath("$.author.username").value("postAuthor"));

        log.info("READ BY ID 완료");

        // 4. UPDATE - 게시글 수정
        Map<String, Object> updateRequest = Map.of(
                "title", "수정된 CRUD 테스트",
                "content", "수정된 CRUD 내용",
                "userId", testAuthor.getId()
        );

        mockMvc.perform(put("/api/posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 CRUD 테스트"))
                .andExpect(jsonPath("$.content").value("수정된 CRUD 내용"));

        log.info("UPDATE 완료");

        // 5. DELETE - 게시글 삭제
        Map<String, Object> deleteRequest = Map.of("userId", testAuthor.getId());

        mockMvc.perform(delete("/api/posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글이 삭제되었습니다"));

        log.info("DELETE 완료");

        // 6. 삭제 확인
        mockMvc.perform(get("/api/posts/" + postId))
                .andDo(print())
                .andExpect(status().isNotFound());

        log.info("DELETE 검증 완료");

        // 7. DB 직접 검증
        long postCount = postRepository.count();
        assertThat(postCount).isEqualTo(0);

        log.info("DB 검증 완료: 게시글 수 = {}", postCount);
        log.info("=== API 전체 CRUD 통합 테스트 완료 ===");
    }
}
