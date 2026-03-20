package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.persistence.MazeEntity;
import com.davidpe.scapeai.persistence.TrainingRunEntity;
import com.davidpe.scapeai.persistence.TrainingRunReplayDiagnosticEntity;
import com.davidpe.scapeai.persistence.repository.MazeRepository;
import com.davidpe.scapeai.persistence.repository.TrainingRunRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RecentRunsComparisonServiceTest {

  @Test
  void shouldReturnLastTenRowsSortedByDate() {
    InMemoryMazeRepository mazeRepository = new InMemoryMazeRepository();
    InMemoryTrainingRunRepository trainingRunRepository = new InMemoryTrainingRunRepository();
    RecentRunsComparisonService service =
        new RecentRunsComparisonService(mazeRepository, trainingRunRepository, 1.10);

    List<RecentRunComparisonRow> rows = service.recentRuns("Neon Gate", RecentRunsSortOption.BY_DATE);

    assertEquals(10, rows.size());
    assertTrue(rows.get(0).createdAtEpochMillis() > rows.get(9).createdAtEpochMillis());
  }

  @Test
  void shouldAllowSortingByReward() {
    InMemoryMazeRepository mazeRepository = new InMemoryMazeRepository();
    InMemoryTrainingRunRepository trainingRunRepository = new InMemoryTrainingRunRepository();
    RecentRunsComparisonService service =
        new RecentRunsComparisonService(mazeRepository, trainingRunRepository, 1.10);

    List<RecentRunComparisonRow> rows = service.recentRuns("Neon Gate", RecentRunsSortOption.BY_REWARD);

    assertEquals(10, rows.size());
    assertTrue(rows.get(0).reward() >= rows.get(9).reward());
  }

  @Test
  void shouldAllowSortingByNetProgress() {
    InMemoryMazeRepository mazeRepository = new InMemoryMazeRepository();
    InMemoryTrainingRunRepository trainingRunRepository = new InMemoryTrainingRunRepository();
    RecentRunsComparisonService service =
        new RecentRunsComparisonService(mazeRepository, trainingRunRepository, 1.10);

    List<RecentRunComparisonRow> rows =
        service.recentRuns("Neon Gate", RecentRunsSortOption.BY_NET_PROGRESS);

    assertEquals(10, rows.size());
    assertTrue(rows.get(0).netProgress() >= rows.get(9).netProgress());
  }

  @Test
  void shouldAllowSortingByTrainingHealthIndex() {
    InMemoryMazeRepository mazeRepository = new InMemoryMazeRepository();
    InMemoryTrainingRunRepository trainingRunRepository = new InMemoryTrainingRunRepository();
    RecentRunsComparisonService service =
        new RecentRunsComparisonService(mazeRepository, trainingRunRepository, 1.10);

    List<RecentRunComparisonRow> rows =
        service.recentRuns("Neon Gate", RecentRunsSortOption.BY_HEALTH_INDEX);

    assertEquals(10, rows.size());
    assertTrue(rows.get(0).trainingHealthIndex() >= rows.get(9).trainingHealthIndex());
  }

  @Test
  void shouldFlagLowEntropyRunsWhenBelowThreshold() {
    InMemoryMazeRepository mazeRepository = new InMemoryMazeRepository();
    InMemoryTrainingRunRepository trainingRunRepository = new InMemoryTrainingRunRepository();
    RecentRunsComparisonService service =
        new RecentRunsComparisonService(mazeRepository, trainingRunRepository, 1.10);

    List<RecentRunComparisonRow> rows = service.recentRuns("Neon Gate", RecentRunsSortOption.BY_DATE);

    assertTrue(rows.stream().anyMatch(RecentRunComparisonRow::lowEntropyAlert));
    assertTrue(rows.stream().anyMatch(RecentRunComparisonRow::healthRegression));
  }

  private static final class InMemoryMazeRepository implements MazeRepository {

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
      return List.of(new MazeEntity(1L, "Neon Gate", 10, 10, "....", 3.0));
    }
  }

  private static final class InMemoryTrainingRunRepository implements TrainingRunRepository {

    private final List<TrainingRunEntity> rows = seedRows();

    @Override
    public TrainingRunEntity save(TrainingRunEntity run) {
      return run;
    }

    @Override
    public List<TrainingRunEntity> findByMazeId(long mazeId) {
      return rows;
    }

    @Override
    public List<TrainingRunEntity> findRecentByMazeId(long mazeId, int limit) {
      return rows.stream().limit(limit).toList();
    }

    @Override
    public List<TrainingRunEntity> findByTrainingSessionId(String trainingSessionId) {
      return rows.stream()
          .filter(row -> java.util.Objects.equals(row.trainingSessionId(), trainingSessionId))
          .toList();
    }

    @Override
    public List<TrainingRunEntity> findSuccessfulByTrainingSessionId(String trainingSessionId, int limit) {
      return rows.stream()
          .filter(row -> java.util.Objects.equals(row.trainingSessionId(), trainingSessionId))
          .filter(row -> row.success() && "EXIT_REACHED".equals(row.terminalReason()))
          .limit(Math.max(1, limit))
          .toList();
    }

    @Override
    public Optional<TrainingRunEntity> findById(long trainingRunId) {
      return rows.stream().filter(row -> row.id() == trainingRunId).findFirst();
    }

    @Override
    public List<CellVisitFrequency> findAccumulatedCellVisitsByMazeId(long mazeId, int limit) {
      return List.of();
    }

    @Override
    public Optional<TrainingRunReplayDiagnosticEntity> findReplayDiagnosticByTrainingRunId(
        long trainingRunId) {
      return rows.stream()
          .filter(row -> row.id() == trainingRunId)
          .findFirst()
          .map(
              row ->
                  new TrainingRunReplayDiagnosticEntity(
                      row.id(), row.createdAtEpochMillis(), row.replayDebugMetadata()));
    }

    @Override
    public List<TrainingRunReplayDiagnosticEntity> findRecentReplayDiagnostics(int limit) {
      return rows.stream()
          .limit(limit)
          .map(
              row ->
                  new TrainingRunReplayDiagnosticEntity(
                      row.id(), row.createdAtEpochMillis(), row.replayDebugMetadata()))
          .toList();
    }

    private static List<TrainingRunEntity> seedRows() {
      List<TrainingRunEntity> generated = new ArrayList<>();
      for (int i = 0; i < 12; i++) {
        generated.add(
            new TrainingRunEntity(
                (long) (i + 1),
                "session-" + (i / 3),
                1L,
                "heuristic-baseline",
                "{}",
                "reward-v1",
                i % 2 == 0,
                10 + i,
                1_000L + i,
                i * 0.5,
                i % 3,
                5 + i,
                2,
                12 - i,
                Math.min(1.0, 0.15 + (0.05 * i)),
                0.10 * i,
                0.10 * i,
                0.10 * i,
                0.10 * i,
                Math.max(0.0, 0.9 - (0.05 * i)),
                Math.min(1.0, 0.1 + (0.05 * i)),
                1.5 - (0.08 * i),
                "[{\"milestone\":\"FINAL\"}]",
                "{\"seed\":20260309}",
                "0,0R",
                "0:0:1",
                i % 4 == 0 ? "TIMEOUT" : (i % 2 == 0 ? "EXIT_REACHED" : "ABORTED"),
                i % 4 == 0,
                75.0 - (i * 3.2),
                TrainingHealthIndexFormula.FORMULA_VERSION,
                1_700_000_000_000L + i));
      }
      generated.sort(
          java.util.Comparator.comparingLong(TrainingRunEntity::createdAtEpochMillis).reversed());
      return generated;
    }
  }
}
