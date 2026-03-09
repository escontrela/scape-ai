package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.persistence.MazeEntity;
import com.davidpe.scapeai.persistence.TrainingRunEntity;
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
        new RecentRunsComparisonService(mazeRepository, trainingRunRepository);

    List<RecentRunComparisonRow> rows = service.recentRuns("Neon Gate", RecentRunsSortOption.BY_DATE);

    assertEquals(10, rows.size());
    assertTrue(rows.get(0).createdAtEpochMillis() > rows.get(9).createdAtEpochMillis());
  }

  @Test
  void shouldAllowSortingByReward() {
    InMemoryMazeRepository mazeRepository = new InMemoryMazeRepository();
    InMemoryTrainingRunRepository trainingRunRepository = new InMemoryTrainingRunRepository();
    RecentRunsComparisonService service =
        new RecentRunsComparisonService(mazeRepository, trainingRunRepository);

    List<RecentRunComparisonRow> rows = service.recentRuns("Neon Gate", RecentRunsSortOption.BY_REWARD);

    assertEquals(10, rows.size());
    assertTrue(rows.get(0).reward() >= rows.get(9).reward());
  }

  @Test
  void shouldAllowSortingByNetProgress() {
    InMemoryMazeRepository mazeRepository = new InMemoryMazeRepository();
    InMemoryTrainingRunRepository trainingRunRepository = new InMemoryTrainingRunRepository();
    RecentRunsComparisonService service =
        new RecentRunsComparisonService(mazeRepository, trainingRunRepository);

    List<RecentRunComparisonRow> rows =
        service.recentRuns("Neon Gate", RecentRunsSortOption.BY_NET_PROGRESS);

    assertEquals(10, rows.size());
    assertTrue(rows.get(0).netProgress() >= rows.get(9).netProgress());
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

    private static List<TrainingRunEntity> seedRows() {
      List<TrainingRunEntity> generated = new ArrayList<>();
      for (int i = 0; i < 12; i++) {
        generated.add(
            new TrainingRunEntity(
                (long) (i + 1),
                1L,
                "heuristic-baseline",
                "{}",
                i % 2 == 0,
                10 + i,
                1_000L + i,
                i * 0.5,
                i % 3,
                5 + i,
                2,
                12 - i,
                1_700_000_000_000L + i));
      }
      generated.sort(
          java.util.Comparator.comparingLong(TrainingRunEntity::createdAtEpochMillis).reversed());
      return generated;
    }
  }
}
