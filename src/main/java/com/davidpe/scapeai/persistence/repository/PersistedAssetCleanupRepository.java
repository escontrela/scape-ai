package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.application.PersistedAssetType;
import java.util.Optional;

public interface PersistedAssetCleanupRepository {

  Optional<String> findMazeNameById(long mazeId);

  long countTrainingRunsByMazeId(long mazeId);

  long countMazeCoverageByMazeId(long mazeId);

  long countTrainingSessionsByMazeRef(String mazeRef);

  long countTrainingRunsBySessionId(String sessionId);

  long countTrainingSessionsByPresetId(long presetId);

  long countExplorationBudgetsByPresetId(long presetId);

  boolean deleteByTypeAndId(PersistedAssetType type, String assetId);
}
