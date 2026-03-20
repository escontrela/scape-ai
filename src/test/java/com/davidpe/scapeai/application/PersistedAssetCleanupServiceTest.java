package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.persistence.repository.PersistedAssetCleanupRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PersistedAssetCleanupServiceTest {

  @Test
  void shouldBlockDeleteWhenDependenciesExist() {
    PersistedAssetCleanupRepository repository =
        new StubCleanupRepository() {
          @Override
          public long countTrainingRunsByMazeId(long mazeId) {
            return 2;
          }

          @Override
          public Optional<String> findMazeNameById(long mazeId) {
            return Optional.of("maze-a");
          }
        };
    PersistedAssetCleanupService service = new PersistedAssetCleanupService(repository);

    var result = service.delete(List.of(new PersistedAssetRef(PersistedAssetType.MAZE, "1")));

    assertEquals(0, result.deleted().size());
    assertEquals(1, result.blocked().size());
    assertTrue(result.blocked().get(0).validationMessage().contains("dependencies"));
  }

  @Test
  void shouldSupportMultiSelectionAndPartialSuccess() {
    StubCleanupRepository repository = new StubCleanupRepository();
    repository.failDeletes.add("TRAINING_RUN:33");
    PersistedAssetCleanupService service = new PersistedAssetCleanupService(repository);

    var result =
        service.delete(
            List.of(
                new PersistedAssetRef(PersistedAssetType.TRAINING_RUN, "11"),
                new PersistedAssetRef(PersistedAssetType.TRAINING_RUN, "33"),
                new PersistedAssetRef(PersistedAssetType.EXPERIENCE_TRANSITION, "44")));

    assertEquals(2, result.deleted().size());
    assertEquals(0, result.blocked().size());
    assertEquals(1, result.failed().size());
    assertEquals("33", result.failed().get(0).ref().assetId());
    assertFalse(result.failed().get(0).eligible());
  }

  private static class StubCleanupRepository implements PersistedAssetCleanupRepository {
    private final Set<String> failDeletes = new HashSet<>();

    @Override
    public Optional<String> findMazeNameById(long mazeId) {
      return Optional.empty();
    }

    @Override
    public long countTrainingRunsByMazeId(long mazeId) {
      return 0;
    }

    @Override
    public long countMazeCoverageByMazeId(long mazeId) {
      return 0;
    }

    @Override
    public long countTrainingSessionsByMazeRef(String mazeRef) {
      return 0;
    }

    @Override
    public long countTrainingRunsBySessionId(String sessionId) {
      return 0;
    }

    @Override
    public long countTrainingSessionsByPresetId(long presetId) {
      return 0;
    }

    @Override
    public long countExplorationBudgetsByPresetId(long presetId) {
      return 0;
    }

    @Override
    public boolean deleteByTypeAndId(PersistedAssetType type, String assetId) {
      return !failDeletes.contains(type.name() + ":" + assetId);
    }
  }
}
