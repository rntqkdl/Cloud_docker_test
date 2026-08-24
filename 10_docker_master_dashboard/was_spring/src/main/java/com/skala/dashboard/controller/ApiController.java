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

    // 7. 🍄 AI 펫 "도키 (Docky)" Multi-Turn 대화 문맥 기억 Chat API
    @PostMapping("/ai/pet/chat")
    public ResponseEntity<Map<String, Object>> petChat(@RequestBody Map<String, Object> payload) {
        String question = (String) payload.getOrDefault("question", "내가 뭐부터 공부해야 할까?");
        String contextInfo = (String) payload.getOrDefault("context", "사용자가 도커 학습 로드맵을 확인 중입니다.");
        String modelName = (String) payload.getOrDefault("model", "qwen2.5:latest");
        List<Map<String, String>> history = (List<Map<String, String>>) payload.getOrDefault("history", Collections.emptyList());

        Map<String, Object> res = new HashMap<>();
        res.put("petName", "도키 (Docky)");
        res.put("mood", "happy");

        String systemPrompt =
            "너는 비전공자 학생(4반 G124 안성민)을 항상 따라다니며 도커와 클라우드를 친절하게 가르쳐주는 닌텐도 아기 버섯 AI 펫 '도키(Docky)'야.\n" +
            "[대화 원칙]:\n" +
            "1. 말투: 친절하고 다정한 존댓말('~해요!', '~랍니다!', '~해 보세요!')을 써줘.\n" +
            "2. 문맥 파악: 이전 대화 내용을 반드시 기억하고, 학생이 '그걸 왜 해야 해?', '그게 뭔데?'라고 물어보면 바로 이전 대화에서 언급한 주제(hello-world, 포트포워딩, 볼륨 등)에 대해 '왜 필요한지 이유와 장점'을 쉽고 명쾌하게 설명해줘.\n" +
            "3. 일상 비유: 어려운 용어는 게임, 택배, 아파트, 식당 비유를 활용해 2~3문장으로 짧고 똑똑하게 답해줘.\n" +
            "4. 문맥 지식:\n" +
            "   - 1단계(hello-world/Nginx): 컨테이너가 켜지고 꺼지는 생명주기(Exit 0)와 외부 포트 연결(-p 8080:80)을 모르면 뒤의 DB나 3-Tier를 연결할 수 없기 때문에 가장 먼저 필수적으로 배워야 합니다.\n" +
            "   - 2단계(Dockerfile/볼륨): 컨테이너를 부숴도 DB 데이터를 살리는 외장 세이브팩(-v)과 나만의 이미지 패키징.\n" +
            "   - 3단계(Compose/스케일아웃): 3-Tier 파티 일괄 소환과 고가용성 무중단 로드밸런싱.\n" +
            "   - 4단계(다이어트): 1.78GB 이미지를 185MB로 줄이는 멀티스테이지 기술.";

        // 1. Ollama /api/chat 호출 (멀티턴 히스토리 전달)
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("model", modelName);
            req.put("stream", false);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            // 최근 6개 대화 히스토리 주입 (문맥 기억)
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

            Map<String, Object> options = new HashMap<>();
            options.put("temperature", 0.4);
            options.put("top_p", 0.9);
            options.put("repeat_penalty", 1.25);
            options.put("num_predict", 220);
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
                        res.put("source", "sLLM (qwen2.5 live)");
                        return ResponseEntity.ok(res);
                    }
                }
            }
        } catch (Exception e) {
            // Ollama 미가동/지연 시 스마트 Fallback
        }

        // 2. Fallback 내장 스마트 지식베이스
        String fallbackReply = generateSmartFallbackReply(question, contextInfo, history);
        res.put("reply", fallbackReply);
        res.put("source", "내장 스마트 지식베이스");
        return ResponseEntity.ok(res);
    }

    private String generateSmartFallbackReply(String question, String contextInfo, List<Map<String, String>> history) {
        String q = question.toLowerCase();
        
        // 이전 질문 문맥 체크
        String lastAssistantMsg = "";
        if (history != null && !history.isEmpty()) {
            for (int i = history.size() - 1; i >= 0; i--) {
                if ("assistant".equals(history.get(i).get("role"))) {
                    lastAssistantMsg = history.get(i).getOrDefault("content", "").toLowerCase();
                    break;
                }
            }
        }

        if (q.contains("왜 해야") || q.contains("왜해야") || q.contains("이유") || q.contains("왜 배워")) {
            if (lastAssistantMsg.contains("1단계") || lastAssistantMsg.contains("hello-world") || lastAssistantMsg.contains("nginx")) {
                return "🍄 1단계(hello-world & Nginx)를 먼저 해야 하는 이유는 **컨테이너의 전원 켜고 끄는 법(생명주기)**과 **외부에서 내 컨테이너로 들어오는 문(포트포워딩)**을 먼저 알아야, 뒤에 나오는 복잡한 DB나 3-Tier 백엔드를 연결할 수 있기 때문이랍니다!";
            }
            return "🍄 도커를 배우면 '내 컴퓨터에선 잘 되는데 서버에선 안 돌아가는' 환경 불일치 문제를 100% 없애고, 어디서나 똑같이 1초 만에 프로그램을 실행할 수 있기 때문이에요!";
        } else if (q.contains("뭐부터") || q.contains("어디서") || q.contains("학습 순서") || q.contains("공부해야")) {
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
