package com.skala.dashboard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quest_progress")
public class QuestProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false, length = 50)
    private String userId;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(name = "current_level")
    private Integer currentLevel;

    @Column(name = "completed_labs", columnDefinition = "TEXT")
    private String completedLabs;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Integer getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(Integer currentLevel) { this.currentLevel = currentLevel; }
    public String getCompletedLabs() { return completedLabs; }
    public void setCompletedLabs(String completedLabs) { this.completedLabs = completedLabs; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
