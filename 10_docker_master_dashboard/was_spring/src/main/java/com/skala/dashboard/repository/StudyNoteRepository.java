package com.skala.dashboard.repository;

import com.skala.dashboard.entity.StudyNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudyNoteRepository extends JpaRepository<StudyNote, Long> {
    List<StudyNote> findTop20ByOrderByIdDesc();
}
