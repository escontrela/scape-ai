package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.scapeai.persistence.MazeEntity;
import com.davidpe.scapeai.persistence.TrainingRunEntity;
import com.davidpe.scapeai.persistence.TrainingRunReplayDiagnosticEntity;
import com.davidpe.scapeai.persistence.repository.MazeRepository;
import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import com.davidpe.scapeai.simulation.GridPosition;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PersistentMazeHeatmapServiceTest {

  @Test
  void shouldResolveMazeByNameAndLoadAccumulatedHeatmap() {
    PersistentMazeHeatmapService service =
        new PersistentMazeHeatmapService(new StubMazeRepository(), new StubTrainingRunRepository());

    List<CellVisitFrequency> heatmap = service.loadAccumulatedHeatmap("Neon Gate", 8);

    assertEquals(2, heatmap.size());
    assertEquals(5, heatmap.get(0).visits());
    assertEquals(new GridPosition(0, 1), heatmap.get(1).position());
  }

  private static final class StubMazeRepository implements MazeRepository {

    @Override
    public MazeEntity save(MazeEntity maze) {
      return maze;
    }

    @Override
    public MazeEntity upsertByName(MazeEntity maze) {
      return maze;
    }

    @Override
    public Optional<MazeEntity> findById(long id) {
      return Optional.empty();
    }

    @Override
    public List<MazeEntity> findAllOrderByDifficulty(boolean ascending) {
      return List.of(new MazeEntity(7L, "Neon Gate", 10, 10, "....", 33.0));
    }
  }

  private static final class StubTrainingRunRepository implements TrainingRunRepository {

    @Override
    public TrainingRunEntity save(TrainingRunEntity run) {
      return run;
    }

    @Override
    public List<TrainingRunEntity> findByMazeId(long mazeId) {
      return List.of();
    }

    @Override
    public List<TrainingRunEntity> findRecentByMazeId(long mazeId, int limit) {
      return List.of();
    }

    @Override
    public List<TrainingRunEntity> findByTrainingSessionId(String trainingSessionId) {
      return List.of();
    }

    @Override
    public List<CellVisitFrequency> findAccumulatedCellVisitsByMazeId(long mazeId, int limit) {
      return List.of(
          new CellVisitFrequency(new GridPosition(0, 0), 5),
          new CellVisitFrequency(new GridPosition(0, 1), 2));
    }

    @Override
    public Optional<TrainingRunReplayDiagnosticEntity> findReplayDiagnosticByTrainingRunId(
        long trainingRunId) {
      return Optional.empty();
    }

    @Override
    public List<TrainingRunReplayDiagnosticEntity> findRecentReplayDiagnostics(int limit) {
      return List.of();
    }
  }
}
