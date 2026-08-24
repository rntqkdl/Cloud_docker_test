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
        factory.setReadTimeout(45000);
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

    // 7. 🍄 AI 펫 "도키 (Docky)" sLLM (qwen2.5) Chat API (반복 방지 & 정밀 가이드 탑재)
    @PostMapping("/ai/pet/chat")
    public ResponseEntity<Map<String, Object>> petChat(@RequestBody Map<String, Object> payload) {
        String question = (String) payload.getOrDefault("question", "내가 뭐부터 공부해야 할까?");
        String contextInfo = (String) payload.getOrDefault("context", "사용자가 도커 학습 로드맵을 확인 중입니다.");
        String modelName = (String) payload.getOrDefault("model", "qwen2.5:1.5b");

        Map<String, Object> res = new HashMap<>();
        res.put("petName", "도키 (Docky)");
        res.put("mood", "happy");

        String systemPrompt =
            "너는 비전공자 학생(4반 G124 안성민)을 가르쳐주는 친절한 닌텐도 아기 버섯 AI 펫 '도키(Docky)'야.\n" +
            "규칙:\n" +
            "1. 부드럽고 자연스러운 존댓말(~해요, ~답니다, ~해봐요)로 말해.\n" +
            "2. 똑같은 말을 반복하지 마.\n" +
            "3. 2~3문장으로 짧고 명쾌하게 답변해.\n\n" +
            "추천 학습 순서:\n" +
            "1단계: STAGE 01 hello-world (생명주기, Exit 0) 및 STAGE 02 Nginx (-p 8080:80 포트포워딩)\n" +
            "2단계: STAGE 03 Dockerfile 및 STAGE 04 볼륨 (-v 영속화)\n" +
            "3단계: STAGE 06~07 Compose 및 STAGE 08 스케일아웃\n" +
            "4단계: STAGE 09 멀티스테이지 다이어트\n\n" +
            "학생이 뭐부터 공부할지 물어보면 반드시 '1단계의 STAGE 01 hello-world와 STAGE 02 Nginx 포트포워딩'부터 시작하라고 밝게 권해줘.";

        // 1. Ollama /api/chat 호출 (repeat_penalty, temperature, num_predict 제어)
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("model", modelName);
            req.put("stream", false);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", question));
            req.put("messages", messages);

            Map<String, Object> options = new HashMap<>();
            options.put("temperature", 0.4);
            options.put("top_p", 0.9);
            options.put("repeat_penalty", 1.25);
            options.put("num_predict", 180);
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
                        res.put("source", "sLLM (" + modelName + " live)");
                        return ResponseEntity.ok(res);
                    }
                }
            }
        } catch (Exception e) {
            // Ollama 지연/오류 시 내장 지식베이스 Fallback
        }

        // 2. Fallback 내장 스마트 지식베이스
        String fallbackReply = generateSmartFallbackReply(question, contextInfo);
        res.put("reply", fallbackReply);
        res.put("source", "내장 스마트 지식베이스");
        return ResponseEntity.ok(res);
    }

    private String generateSmartFallbackReply(String question, String contextInfo) {
        String q = question.toLowerCase();
        if (q.contains("뭐부터") || q.contains("어디서") || q.contains("학습 순서") || q.contains("공부해야")) {
            return "🍄 성민님! 가장 먼저 **1단계(입문)**부터 시작하는 것을 강력 추천해요!\n1. **STAGE 01 (hello-world)**: 컨테이너의 생명주기와 Exit 0 종료 코드를 확인하고,\n2. **STAGE 02 (Nginx)**: 브라우저와 통신하는 `-p 8080:80` 포트포워딩을 연습해 보세요!\n기초가 잡히면 2단계 Dockerfile과 볼륨으로 나아가시면 된답니다 🍄✨";
        } else if (q.contains("포트") || q.contains("포워딩") || q.contains("8080")) {
            return "🍄 포트포워딩은 **아파트 인터폰 텔레포트**예요! 내 Mac 아파트의 8088번 인터폰을 누르면 컨테이너 안쪽 80번 문으로 손님을 쏙 연결해 주는 멋진 통로랍니다!";
        } else if (q.contains("볼륨") || q.contains("volume") || q.contains("삭제")) {
            return "🍄 도커 볼륨(-v)은 **외장 세이브 메모리카드**예요! 게임기 본체(컨테이너)를 부수고 새로 사도 외장 메모리카드만 꽂으면 내 소중한 레벨과 아이템(DB 데이터)이 100% 부활한답니다!";
        } else if (q.contains("컴포즈") || q.contains("compose")) {
            return "🍄 도커 컴포즈는 **파티 일괄 소환 계약서**예요! [Nginx 문지기 + Spring Boot 주방 + PostgreSQL 금고] 3인 파티를 `compose.yaml` 한 장으로 1초 만에 딱 맞추어 소환해 준답니다!";
        } else if (q.contains("이미지") && q.contains("컨테이너")) {
            return "🍄 이미지는 **절대 안 변하는 게임 롬팩(설계도)**이고, 컨테이너는 전원을 켜서 **실제 플레이 중인 신나는 게임 화면(프로세스)**이랍니다!";
        } else if (q.contains("다이어트") || q.contains("멀티스테이지")) {
            return "🍄 멀티스테이지 빌드는 **공사장 크레인 철거** 기술이에요! 무거운 빌드 툴(Maven, npm)은 1단계에서 버리고 2단계에는 쏙 가벼운 완성품(185MB)만 입주시키는 원리랍니다!";
        } else {
            return "🍄 안녕 성민님! 도키가 항상 곁에서 지켜보고 있어요. 지금 보시는 화면의 코드나 개념 중 궁금한 점이 생기면 언제든 질문해 주세요! 쉽고 재미있게 풀어드릴게요 🍄✨";
        }
    }
}
