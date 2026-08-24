package com.skala.dashboard.controller;

import com.skala.dashboard.entity.QuestProgress;
import com.skala.dashboard.entity.StudyNote;
import com.skala.dashboard.repository.QuestProgressRepository;
import com.skala.dashboard.repository.StudyNoteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public ApiController(QuestProgressRepository progressRepo, StudyNoteRepository noteRepo, DataSource dataSource) {
        this.progressRepo = progressRepo;
        this.noteRepo = noteRepo;
        this.dataSource = dataSource;
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

    // 6. 🖥️ 진짜 실시간 도커 명령어 실행 터미널 API (Real Live Execution Engine)
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
}
