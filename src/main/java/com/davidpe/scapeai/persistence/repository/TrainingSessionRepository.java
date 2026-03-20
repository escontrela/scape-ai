package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.TrainingSessionEntity;
import java.util.List;
import java.util.Optional;

public interface TrainingSessionRepository {

  TrainingSessionEntity save(TrainingSessionEntity session);

  Optional<TrainingSessionEntity> findById(String sessionId);

  List<TrainingSessionEntity> findRecent(int limit);

  void markEnded(String sessionId, long endedAtEpochMillis);
}
