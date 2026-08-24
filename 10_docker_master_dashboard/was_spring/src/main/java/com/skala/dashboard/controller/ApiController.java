package com.skala.dashboard.controller;

import com.skala.dashboard.entity.QuestProgress;
import com.skala.dashboard.entity.StudyNote;
import com.skala.dashboard.repository.QuestProgressRepository;
import com.skala.dashboard.repository.StudyNoteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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

    @Value("${OLLAMA_HOST:http://host.docker.internal:11434}")
    private String ollamaHost;

    public ApiController(QuestProgressRepository progressRepo, StudyNoteRepository noteRepo, DataSource dataSource) {
        this.progressRepo = progressRepo;
        this.noteRepo = noteRepo;
        this.dataSource = dataSource;
        this.restTemplate = new RestTemplate();
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

    // 7. 🍄 AI 펫 "도키 (Docky)" sLLM (qwen2.5) 실시간 상황 반응 & Q&A 엔드포인트
    @PostMapping("/ai/pet/chat")
    public ResponseEntity<Map<String, Object>> petChat(@RequestBody Map<String, Object> payload) {
        String question = (String) payload.getOrDefault("question", "도커가 왜 필요한지 비유로 알려줘!");
        String contextInfo = (String) payload.getOrDefault("context", "사용자가 도커 실습을 진행 중입니다.");
        String modelName = (String) payload.getOrDefault("model", "qwen2.5");

        Map<String, Object> res = new HashMap<>();
        res.put("petName", "도키 (Docky)");
        res.put("mood", "happy");

        String prompt = String.format(
            "너는 비전공자 학생(4반 G124 안성민)을 항상 따라다니며 도커와 클라우드를 친절하고 쉽게 알려주는 귀여운 닌텐도 아기 버섯 AI 펫 '도키(Docky)'야!\n" +
            "말투는 항상 밝고 친절하게 '~해요!', '~랍니다!', '~해봐요!' 체를 써줘.\n" +
            "어려운 IT 용어(포트, 패킷, 볼륨, 프록시)는 일상생활(아파트, 택배, 게임기, 식당 지배인) 비유를 곁들여 비전공자가 듣자마자 단번에 이해할 수 있게 2~4문장으로 핵심만 쏙쏙 답해줘.\n" +
            "[현재 학생 상황]: %s\n" +
            "[학생 질문]: %s",
            contextInfo, question
        );

        // 1. 로컬 sLLM (qwen2.5) Ollama 호출 시도
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("model", modelName);
            req.put("prompt", prompt);
            req.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);

            ResponseEntity<Map> ollamaRes = restTemplate.exchange(
                ollamaHost + "/api/generate",
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (ollamaRes.getStatusCode().is2xxSuccessful() && ollamaRes.getBody() != null) {
                String aiText = (String) ollamaRes.getBody().get("response");
                if (aiText != null && !aiText.trim().isEmpty()) {
                    res.put("reply", aiText.trim());
                    res.put("source", "sLLM (qwen2.5 live)");
                    return ResponseEntity.ok(res);
                }
            }
        } catch (Exception e) {
            // Ollama 미가동 시 내장 스마트 지식베이스 폴백 (무중단 작동)
        }

        // 2. Fallback 내장 스마트 지식베이스
        String fallbackReply = generateSmartFallbackReply(question, contextInfo);
        res.put("reply", fallbackReply);
        res.put("source", "내장 스마트 지식베이스");
        return ResponseEntity.ok(res);
    }

    private String generateSmartFallbackReply(String question, String contextInfo) {
        String q = question.toLowerCase();
        if (q.contains("포트") || q.contains("포워딩") || q.contains("8080")) {
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
