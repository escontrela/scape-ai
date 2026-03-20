package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.application.CellVisitFrequency;
import com.davidpe.scapeai.persistence.TrainingRunEntity;
import com.davidpe.scapeai.persistence.TrainingRunReplayDiagnosticEntity;
import java.util.List;
import java.util.Optional;

public interface TrainingRunRepository {

  TrainingRunEntity save(TrainingRunEntity run);

  List<TrainingRunEntity> findByMazeId(long mazeId);

  List<TrainingRunEntity> findRecentByMazeId(long mazeId, int limit);

  List<TrainingRunEntity> findByTrainingSessionId(String trainingSessionId);

  Optional<TrainingRunEntity> findById(long trainingRunId);

  List<CellVisitFrequency> findAccumulatedCellVisitsByMazeId(long mazeId, int limit);

  Optional<TrainingRunReplayDiagnosticEntity> findReplayDiagnosticByTrainingRunId(long trainingRunId);

  List<TrainingRunReplayDiagnosticEntity> findRecentReplayDiagnostics(int limit);
}
