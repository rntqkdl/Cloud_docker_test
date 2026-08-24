package com.skala.dashboard.repository;

import com.skala.dashboard.entity.QuestProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface QuestProgressRepository extends JpaRepository<QuestProgress, Long> {
    Optional<QuestProgress> findByUserId(String userId);
}
