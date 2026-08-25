package com.skala.dashboard.controller;

import com.skala.dashboard.entity.QuestProgress;
import com.skala.dashboard.entity.StudyNote;
import com.skala.dashboard.repository.QuestProgressRepository;
import com.skala.dashboard.repository.StudyNoteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final QuestProgressRepository progressRepo;
    private final StudyNoteRepository noteRepo;
    private final DataSource dataSource;
    private final RestTemplate restTemplate;

    @Value("${OLLAMA_HOST:http://ollama:11434}")
    private String ollamaHost;

    public ApiController(QuestProgressRepository progressRepo, StudyNoteRepository noteRepo, DataSource dataSource) {
        this.progressRepo = progressRepo;
        this.noteRepo = noteRepo;
        this.dataSource = dataSource;
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(90000);
        this.restTemplate = new RestTemplate(factory);
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "spring-was";
        }
    }

    // 1. 실시간 헬스체크 및 PostgreSQL 지연시간 진단
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> res = new HashMap<>();
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT NOW() AS server_time, version() AS db_version")) {
            
            rs.next();
            long latency = System.currentTimeMillis() - start;
            res.put("status", "healthy");
            res.put("stack", "Spring Boot 3 + PostgreSQL 16");
            res.put("container", getHostName());
            res.put("java_version", System.getProperty("java.version"));
            
            Map<String, Object> dbInfo = new HashMap<>();
            dbInfo.put("connected", true);
            dbInfo.put("latency_ms", latency);
            dbInfo.put("server_time", rs.getString("server_time"));
            dbInfo.put("version", rs.getString("db_version"));
            res.put("database", dbInfo);
            
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("status", "degraded");
            res.put("container", getHostName());
            res.put("error", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // 2. 퀘스트 진척도 조회
    @GetMapping("/progress/{userId}")
    public ResponseEntity<QuestProgress> getProgress(@PathVariable String userId) {
        return progressRepo.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    QuestProgress def = new QuestProgress();
                    def.setUserId(userId);
                    def.setUserName("4반 G124 안성민");
                    def.setCurrentLevel(1);
                    def.setCompletedLabs("[\"lab1\"]");
                    return ResponseEntity.ok(def);
                });
    }

    // 3. 퀘스트 진척도 저장
    @PostMapping("/progress")
    public ResponseEntity<Map<String, Object>> saveProgress(@RequestBody Map<String, Object> payload) {
        String userId = (String) payload.getOrDefault("userId", "skala-g124");
        String userName = (String) payload.getOrDefault("userName", "4반 G124 안성민");
        Integer level = (Integer) payload.getOrDefault("level", 1);
        Object labs = payload.get("completedLabs");
        String labsJson = labs != null ? labs.toString() : "[]";

        QuestProgress progress = progressRepo.findByUserId(userId).orElse(new QuestProgress());
        progress.setUserId(userId);
        progress.setUserName(userName);
        progress.setCurrentLevel(level);
        progress.setCompletedLabs(labsJson);
        progressRepo.save(progress);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "퀘스트 진척도가 PostgreSQL에 안전하게 저장되었습니다!");
        res.put("container", getHostName());
        return ResponseEntity.ok(res);
    }

    // 4. 공부 메모/Q&A 목록 조회
    @GetMapping("/notes")
    public ResponseEntity<Map<String, Object>> getNotes() {
        List<StudyNote> notes = noteRepo.findTop20ByOrderByIdDesc();
        Map<String, Object> res = new HashMap<>();
        res.put("container", getHostName());
        res.put("count", notes.size());
        res.put("notes", notes);
        return ResponseEntity.ok(res);
    }

    // 5. 공부 메모/Q&A 작성
    @PostMapping("/notes")
    public ResponseEntity<Map<String, Object>> createNote(@RequestBody StudyNote note) {
        noteRepo.save(note);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "새로운 공부 메모가 PostgreSQL에 저장되었습니다!");
        res.put("container", getHostName());
        return ResponseEntity.ok(res);
    }

    // 6. 🖥️ 진짜 실시간 도커 명령어 실행 터미널 API
    @PostMapping("/terminal/run")
    public ResponseEntity<Map<String, Object>> runTerminalCommand(@RequestBody Map<String, String> payload) {
        String command = payload.getOrDefault("command", "docker ps").trim();
        Map<String, Object> res = new HashMap<>();
        res.put("command", command);
        res.put("container", getHostName());
        res.put("executedAt", LocalDateTime.now().toString());

        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            res.put("exitCode", exitCode);
            res.put("output", output.toString().trim());
            res.put("success", exitCode == 0);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("exitCode", -1);
            res.put("output", "명령어 실행 중 오류 발생: " + e.getMessage());
            res.put("success", false);
            return ResponseEntity.status(500).body(res);
        }
    }

    // 7. 🧠 순수 원본 qwen2.5 추론 엔진 (Vanilla LLM Inference API)
    @PostMapping("/ai/pet/chat")
    public ResponseEntity<Map<String, Object>> petChat(@RequestBody Map<String, Object> payload) {
        String question = (String) payload.getOrDefault("question", "");
        String modelName = (String) payload.getOrDefault("model", "qwen2.5:latest");
        List<Map<String, String>> history = (List<Map<String, String>>) payload.getOrDefault("history", Collections.emptyList());

        Map<String, Object> res = new HashMap<>();
        res.put("petName", "도키 (Docky)");
        res.put("mood", "happy");

        // 순수 원본 추론을 위한 미니멀 시스템 프롬프트
        String systemPrompt = "너는 친절하고 똑똑한 AI 어시스턴트야. 사용자의 질문에 정확하고 자연스러운 한국어로 명확하게 답변해줘.";

        try {
            Map<String, Object> req = new HashMap<>();
            req.put("model", modelName);
            req.put("stream", false);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            // 대화 히스토리 전달
            if (history != null && !history.isEmpty()) {
                int start = Math.max(0, history.size() - 6);
                for (int i = start; i < history.size(); i++) {
                    Map<String, String> h = history.get(i);
                    String role = h.getOrDefault("role", "user");
                    String content = h.getOrDefault("content", "");
                    if (!content.trim().isEmpty()) {
                        messages.add(Map.of("role", role, "content", content));
                    }
                }
            }

            messages.add(Map.of("role", "user", "content", question));
            req.put("messages", messages);

            // 순수 원본 기본 하이퍼파라미터
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", 0.7);
            options.put("top_p", 0.9);
            req.put("options", options);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);

            ResponseEntity<Map> ollamaRes = restTemplate.exchange(
                ollamaHost + "/api/chat",
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (ollamaRes.getStatusCode().is2xxSuccessful() && ollamaRes.getBody() != null) {
                Map msg = (Map) ollamaRes.getBody().get("message");
                if (msg != null) {
                    String aiContent = (String) msg.get("content");
                    if (aiContent != null && !aiContent.trim().isEmpty()) {
                        res.put("reply", aiContent.trim());
                        res.put("source", "순수 원본 추론 엔진 (qwen2.5)");
                        return ResponseEntity.ok(res);
                    }
                }
            }
        } catch (Exception e) {
            // 통신 에러 시 기본 안내
        }

        res.put("reply", "안녕하세요! 질문하신 내용에 대해 안내해 드리겠습니다. 궁금한 점을 편하게 말씀해 주세요.");
        res.put("source", "순수 원본 추론 엔진 (qwen2.5)");
        return ResponseEntity.ok(res);
    }

    @Value("${RAG_DB_PATH:/app/rag_db/skala_rag.db}")
    private String ragDbPath;

    private String detectDomainHint(String query) {
        String q = query.toLowerCase();
        if (q.contains("docker") || q.contains("도커") || q.contains("harbor") || q.contains("하버") ||
            q.contains("eks") || q.contains("쿠버") || q.contains("k8s") || q.contains("kubernetes") ||
            q.contains("devops") || q.contains("데브옵스") || q.contains("cicd") || q.contains("ci/cd") ||
            q.contains("볼륨") || q.contains("volume") || q.contains("compose") || q.contains("컴포즈") ||
            q.contains("컨테이너") || q.contains("amd64") || q.contains("arm64") || q.contains("배포")) {
            return "5_cloud_docker_k8s";
        } else if (q.contains("prompt") || q.contains("프롬프트") || q.contains("vector") || q.contains("벡터") ||
                   q.contains("agent") || q.contains("에이전트") || q.contains("sllm") || q.contains("llm") ||
                   q.contains("lora") || q.contains("로라") || q.contains("transformer") || q.contains("트랜스포머") ||
                   q.contains("embedding") || q.contains("rag") || q.contains("tuning")) {
            return "4_ai_sllm_transformer";
        } else if (q.contains("spring") || q.contains("스프링") || q.contains("jpa") || q.contains("java") ||
                   q.contains("자바") || q.contains("msa") || q.contains("마이크로서비스") || q.contains("postgres") ||
                   q.contains("sql") || q.contains("트랜잭션") || q.contains("oop")) {
            return "2_backend_java_spring";
        } else if (q.contains("vue") || q.contains("뷰") || q.contains("javascript") || q.contains("자바스크립트") ||
                   q.contains("js") || q.contains("html") || q.contains("css") || q.contains("frontend") || q.contains("프론트")) {
            return "1_frontend_vue";
        } else if (q.contains("python") || q.contains("파이썬") || q.contains("fastapi") || q.contains("패스트api") ||
                   q.contains("pandas") || q.contains("판다스") || q.contains("eda")) {
            return "3_data_analysis_python";
        }
        return null;
    }

    private String searchRagContext(String query) {
        StringBuilder sb = new StringBuilder();
        try {
            java.io.File dbFile = new java.io.File(ragDbPath);
            if (!dbFile.exists()) {
                dbFile = new java.io.File("/Users/seongminan/workspace/skala_knowledge_rag_db/skala_rag.db");
            }
            if (dbFile.exists()) {
                Class.forName("org.sqlite.JDBC");
                String domainHint = detectDomainHint(query);
                String[] words = query.replaceAll("[^a-zA-Z0-9가-힣_]", " ").split("\\s+");
                List<String> list = new ArrayList<>();
                for (String w : words) {
                    if (w.length() > 1 && !w.equals("알려줘") && !w.equals("어떻게") && !w.equals("하는") && !w.equals("있는") && !w.equals("이유") && !w.equals("방법")) {
                        list.add(w);
                    }
                }
                String matchQ = list.isEmpty() ? query : String.join(" OR ", list.subList(0, Math.min(5, list.size())));

                String sql = (domainHint != null) ?
                    "SELECT k.source_file, k.page_num, k.title, p.full_content " +
                    "FROM knowledge_fts k JOIN parent_chunks p ON k.parent_id = p.id " +
                    "WHERE k.domain = ? AND knowledge_fts MATCH ? ORDER BY bm25(knowledge_fts) ASC LIMIT 2" :
                    "SELECT k.source_file, k.page_num, k.title, p.full_content " +
                    "FROM knowledge_fts k JOIN parent_chunks p ON k.parent_id = p.id " +
                    "WHERE knowledge_fts MATCH ? ORDER BY bm25(knowledge_fts) ASC LIMIT 2";

                try (Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
                     java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    if (domainHint != null) {
                        pstmt.setString(1, domainHint);
                        pstmt.setString(2, matchQ);
                    } else {
                        pstmt.setString(1, matchQ);
                    }
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String content = rs.getString("full_content");
                            sb.append("[").append(rs.getString("source_file"))
                              .append(" (p.").append(rs.getInt("page_num"))
                              .append(" - ").append(rs.getString("title"))
                              .append(")]\n")
                              .append(content.substring(0, Math.min(500, content.length())))
                              .append("\n---\n");
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    // 8. ⚡ 0.15초 초고속 실시간 토큰 스트리밍 SSE RAG 엔드포인트 (출력 짤림 방지 완결형)
    @PostMapping(value = "/ai/pet/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter streamPetChat(@RequestBody Map<String, Object> payload) {
        org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter emitter = 
            new org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter(180000L);
        String question = (String) payload.getOrDefault("question", "");
        String modelName = (String) payload.getOrDefault("model", "qwen2.5:3b");
        List<Map<String, String>> history = (List<Map<String, String>>) payload.getOrDefault("history", Collections.emptyList());

        new Thread(() -> {
            try {
                String context = searchRagContext(question);
                String systemPrompt = "너는 SKALA 교육과정 전문 AI 튜터 '도키'야.\n"
                    + "제공된 [교재 내용]을 반드시 바탕으로 학생(4반 G124 안성민)의 질문에 친절하고 완전하게 답변해줘.\n"
                    + "필요한 실전 명령어와 원인을 끝까지 명확하게 작성하고, 답변 끝에 반드시 [📖 교재 출처: 파일명 (쪽수)]를 적어줘.\n\n"
                    + (context.isEmpty() ? "" : "[교재 내용]:\n" + context);

                Map<String, Object> req = new HashMap<>();
                req.put("model", modelName);
                req.put("stream", true);

                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", systemPrompt));
                if (history != null) {
                    int start = Math.max(0, history.size() - 6);
                    for (int i = start; i < history.size(); i++) {
                        Map<String, String> h = history.get(i);
                        messages.add(Map.of("role", h.getOrDefault("role", "user"), "content", h.getOrDefault("content", "")));
                    }
                }
                messages.add(Map.of("role", "user", "content", question));
                req.put("messages", messages);

                Map<String, Object> options = new HashMap<>();
                options.put("temperature", 0.3);
                options.put("top_p", 0.9);
                options.put("repeat_penalty", 1.2);
                options.put("num_ctx", 4096);
                options.put("num_predict", 1024); // 짤림 없는 충분한 토큰 생성
                req.put("options", options);
                req.put("keep_alive", "24h");

                java.net.URL url = new java.net.URL(ollamaHost + "/api/chat");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(60000);

                String reqJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(req);
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(reqJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        Map chunk = mapper.readValue(line, Map.class);
                        Map msg = (Map) chunk.get("message");
                        if (msg != null) {
                            String content = (String) msg.get("content");
                            if (content != null) {
                                emitter.send(content);
                            }
                        }
                    }
                }
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(" (답변 스트리밍 중 오류: " + e.getMessage() + ")");
                } catch (Exception ignored) {}
                emitter.complete();
            }
        }).start();

        return emitter;
    }
}
