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
import org.zerock.mybackendapp.auth.dto.LoginRequest;
import org.zerock.mybackendapp.auth.dto.RegisterRequest;
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
        "jwt.secret=testSecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLongForTesting",
        "jwt.access-token-expiration=3600000",
        "jwt.refresh-token-expiration=604800000"
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

    private String accessToken;
    private User testAuthor;

    @BeforeEach
    void setUp() throws Exception {
        log.info("=== 통합 테스트 데이터 초기화 ===");
        postRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트용 사용자 회원가입
        RegisterRequest registerRequest = RegisterRequest.of("postAuthor", "post@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        log.info("테스트 사용자 회원가입 완료: {}", registerRequest.getUsername());

        // 로그인하여 토큰 획득
        LoginRequest loginRequest = LoginRequest.of("postAuthor", "password123");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var responseMap = objectMapper.readValue(loginResponse, Map.class);
        this.accessToken = (String) responseMap.get("accessToken");

        testAuthor = userRepository.findByUsername("postAuthor").orElseThrow();

        log.info("테스트 사용자 로그인 완료: userId={}, 토큰 길이={}",
                testAuthor.getId(), accessToken.length());
    }

    @Test
    @DisplayName("기존 Post 도메인 전체 플로우 테스트 (JWT 인증 추가)")
    void originalPostDomainFlowWithJWT() throws Exception {
        log.info("=== 기존 Post 도메인 전체 플로우 테스트 시작 (JWT 추가) ===");

        // 기존 테스트에 JWT 인증 추가
        // 1. API로 게시글 생성 (JWT 토큰 사용)
        String title = "통합테스트 게시글";
        String content = "통합테스트 내용입니다.";
        Map<String, Object> createRequest = Map.of(
                "title", title,
                "content", content,
                "authorId", testAuthor.getId()
        );

        String response = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + accessToken) // JWT 토큰 추가
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

        // 2. Service로 게시글 조회 (기존 로직)
        Post servicePost = postService.getPostById(createdPost.getId()).orElseThrow();
        log.info("Service로 조회한 게시글: {}", servicePost);

        // 3. Repository로 직접 조회 (기존 로직)
        Post repoPost = postRepository.findById(createdPost.getId()).orElseThrow();
        log.info("Repository로 조회한 게시글: {}", repoPost);

        // 4. User-Post 관계 확인 (기존 로직)
        assertThat(servicePost.getAuthor().getId()).isEqualTo(testAuthor.getId());
        assertThat(servicePost.getAuthor().getUsername()).isEqualTo("postAuthor");

        // 5. 도메인 로직 테스트 (기존 로직)
        String newContent = "업데이트된 내용";
        servicePost.updateContent(newContent);
        Post updatedPost = postRepository.save(servicePost);
        log.info("도메인 로직으로 내용 업데이트: {}", updatedPost);

        // 6. 검증 (기존 로직)
        assertThat(createdPost.getId()).isEqualTo(servicePost.getId());
        assertThat(servicePost.getId()).isEqualTo(repoPost.getId());
        assertThat(updatedPost.getContent()).isEqualTo(newContent);
        assertThat(updatedPost.getAuthor().getId()).isEqualTo(testAuthor.getId());

        log.info("=== 기존 Post 도메인 전체 플로우 테스트 완료 (JWT 추가) ===");
    }

    @Test
    @DisplayName("User-Post 관계 통합 테스트")
    void userPostRelationshipTest() throws Exception {
        log.info("=== User-Post 관계 통합 테스트 시작 ===");

        // 1. 추가 사용자 생성 및 로그인
        RegisterRequest author2Request = RegisterRequest.of("author2", "author2@example.com", "password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(author2Request)))
                .andExpect(status().isCreated());

        LoginRequest author2Login = LoginRequest.of("author2", "password123");
        String author2Response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(author2Login)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var author2Map = objectMapper.readValue(author2Response, Map.class);
        String author2Token = (String) author2Map.get("accessToken");
        Long author2Id = ((Number) author2Map.get("userId")).longValue();

        // 2. 각 사용자별로 게시글 생성
        Map<String, Object> post1Request = Map.of(
                "title", "첫 번째 게시글",
                "content", "내용1",
                "authorId", testAuthor.getId()
        );
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post1Request)))
                .andExpect(status().isCreated());

        Map<String, Object> post2Request = Map.of(
                "title", "두 번째 게시글",
                "content", "내용2",
                "authorId", testAuthor.getId()
        );
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post2Request)))
                .andExpect(status().isCreated());

        Map<String, Object> post3Request = Map.of(
                "title", "다른 사용자 게시글",
                "content", "내용3",
                "authorId", author2Id
        );
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + author2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post3Request)))
                .andExpect(status().isCreated());

        log.info("게시글 3개 생성 완료");

        // 3. 작성자별 게시글 조회 테스트
        List<Post> author1Posts = postService.getPostsByAuthor(testAuthor.getId());
        List<Post> author2Posts = postService.getPostsByAuthor(author2Id);

        assertThat(author1Posts).hasSize(2);
        assertThat(author2Posts).hasSize(1);

        log.info("작성자별 게시글 수: author1={}, author2={}",
                author1Posts.size(), author2Posts.size());

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
        RegisterRequest otherUserRequest = RegisterRequest.of("otheruser", "other@example.com", "password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherUserRequest)))
                .andExpect(status().isCreated());

        LoginRequest otherLogin = LoginRequest.of("otheruser", "password123");
        String otherResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherLogin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var otherMap = objectMapper.readValue(otherResponse, Map.class);
        String otherToken = (String) otherMap.get("accessToken");
        Long otherUserId = ((Number) otherMap.get("userId")).longValue();

        // 2. testAuthor가 게시글 생성
        Map<String, Object> createRequest = Map.of(
                "title", "권한 테스트 게시글",
                "content", "원본 내용",
                "authorId", testAuthor.getId()
        );
        String postResponse = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Post post = objectMapper.readValue(postResponse, Post.class);
        log.info("게시글 생성: 작성자={}, 게시글ID={}", testAuthor.getUsername(), post.getId());

        // 3. 작성자가 수정 - 성공해야 함
        Map<String, Object> updateRequest = Map.of(
                "title", "수정된 제목",
                "content", "수정된 내용",
                "userId", testAuthor.getId()
        );

        mockMvc.perform(put("/api/posts/" + post.getId())
                        .header("Authorization", "Bearer " + accessToken)
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
                "userId", otherUserId
        );

        mockMvc.perform(put("/api/posts/" + post.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unauthorizedRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("게시글을 수정할 권한이 없습니다."));

        log.info("다른 사용자의 게시글 수정 시도 실패 - 권한 검증 성공");

        // 5. 다른 사용자가 삭제 시도 - 실패해야 함
        Map<String, Object> deleteRequest = Map.of("userId", otherUserId);

        mockMvc.perform(delete("/api/posts/" + post.getId())
                        .header("Authorization", "Bearer " + otherToken)
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
        String[] titles = {
                "Spring Boot 완벽 가이드",
                "Spring Security 튜토리얼",
                "React 기초부터 심화까지",
                "Vue.js 입문"
        };

        for (String title : titles) {
            Map<String, Object> request = Map.of(
                    "title", title,
                    "content", title + " 내용",
                    "authorId", testAuthor.getId()
            );
            mockMvc.perform(post("/api/posts")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

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
                        .header("Authorization", "Bearer " + accessToken)
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
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 CRUD 테스트"));

        log.info("UPDATE 완료");

        // 5. DELETE - 게시글 삭제
        Map<String, Object> deleteRequest = Map.of("userId", testAuthor.getId());

        mockMvc.perform(delete("/api/posts/" + postId)
                        .header("Authorization", "Bearer " + accessToken)
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

    @Test
    @DisplayName("인증 없이 공개 API 접근 테스트")
    void publicApiAccessTest() throws Exception {
        log.info("=== 인증 없이 공개 API 접근 테스트 시작 ===");

        // 먼저 JWT 토큰으로 게시글 생성
        Map<String, Object> createRequest = Map.of(
                "title", "공개 조회용 게시글",
                "content", "누구나 조회 가능한 게시글",
                "authorId", testAuthor.getId()
        );

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        log.info("테스트용 게시글 생성 완료");

        // 1. 토큰 없이 게시글 목록 조회
        mockMvc.perform(get("/api/posts"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("공개 조회용 게시글"));

        log.info("토큰 없이 게시글 목록 조회 성공");

        // 2. 토큰 없이 게시글 검색
        mockMvc.perform(get("/api/posts/search")
                        .param("keyword", "공개"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("공개 조회용 게시글"));

        log.info("토큰 없이 게시글 검색 성공");

        log.info("=== 인증 없이 공개 API 접근 테스트 완료 ===");
    }

    @Test
    @DisplayName("인증 없이 보호된 API 접근 실패 테스트")
    void protectedApiAccessFailTest() throws Exception {
        log.info("=== 인증 없이 보호된 API 접근 실패 테스트 시작 ===");

        // 1. 토큰 없이 게시글 생성 시도
        Map<String, Object> createRequest = Map.of(
                "title", "무단 생성 게시글",
                "content", "토큰 없이 생성 시도",
                "authorId", testAuthor.getId()
        );

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        log.info("토큰 없이 게시글 생성 차단 확인");

        // 2. 유효하지 않은 토큰으로 게시글 생성 시도
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        log.info("유효하지 않은 토큰으로 게시글 생성 차단 확인");

        log.info("=== 인증 없이 보호된 API 접근 실패 테스트 완료 ===");
    }
}
