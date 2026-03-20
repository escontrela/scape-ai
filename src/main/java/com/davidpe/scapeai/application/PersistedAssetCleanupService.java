package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.repository.PersistedAssetCleanupRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PersistedAssetCleanupService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PersistedAssetCleanupService.class);

  private final PersistedAssetCleanupRepository repository;

  public PersistedAssetCleanupService(PersistedAssetCleanupRepository repository) {
    this.repository = repository;
  }

  public List<PersistedAssetDeletionCandidate> evaluate(List<PersistedAssetRef> refs) {
    if (refs == null || refs.isEmpty()) {
      return List.of();
    }
    return refs.stream().map(this::validate).toList();
  }

  public PersistedAssetCleanupResult delete(List<PersistedAssetRef> refs) {
    if (refs == null || refs.isEmpty()) {
      return new PersistedAssetCleanupResult(List.of(), List.of(), List.of());
    }
    List<PersistedAssetRef> deleted = new ArrayList<>();
    List<PersistedAssetDeletionCandidate> blocked = new ArrayList<>();
    List<PersistedAssetDeletionCandidate> failed = new ArrayList<>();
    for (PersistedAssetRef ref : refs) {
      PersistedAssetDeletionCandidate candidate = validate(ref);
      if (!candidate.eligible()) {
        blocked.add(candidate);
        LOGGER.warn(
            "Asset deletion blocked type={} id={} reason={}",
            ref.type(),
            ref.assetId(),
            candidate.validationMessage());
        continue;
      }
      boolean removed = repository.deleteByTypeAndId(ref.type(), ref.assetId());
      if (removed) {
        deleted.add(ref);
        LOGGER.info("Asset deleted type={} id={}", ref.type(), ref.assetId());
      } else {
        failed.add(
            new PersistedAssetDeletionCandidate(
                ref, false, "Delete operation failed or asset not found."));
        LOGGER.error("Asset delete failed type={} id={}", ref.type(), ref.assetId());
      }
    }
    return new PersistedAssetCleanupResult(deleted, blocked, failed);
  }

  private PersistedAssetDeletionCandidate validate(PersistedAssetRef ref) {
    if (ref == null || ref.type() == null || ref.assetId() == null || ref.assetId().isBlank()) {
      return new PersistedAssetDeletionCandidate(
          new PersistedAssetRef(PersistedAssetType.TRAINING_RUN, "unknown"),
          false,
          "Asset reference is invalid.");
    }
    try {
      return switch (ref.type()) {
        case MAZE -> validateMaze(ref);
        case TRAINING_SESSION -> validateTrainingSession(ref);
        case TRAINING_PRESET -> validateTrainingPreset(ref);
        case TRAINING_RUN, EXPERIENCE_TRANSITION, MAZE_POLICY_COVERAGE, EXPLORATION_BUDGET ->
            new PersistedAssetDeletionCandidate(ref, true, "Eligible.");
      };
    } catch (NumberFormatException ex) {
      return new PersistedAssetDeletionCandidate(ref, false, "Asset id is not numeric.");
    }
  }

  private PersistedAssetDeletionCandidate validateMaze(PersistedAssetRef ref) {
    long mazeId = Long.parseLong(ref.assetId().trim());
    long runs = repository.countTrainingRunsByMazeId(mazeId);
    long coverage = repository.countMazeCoverageByMazeId(mazeId);
    Optional<String> mazeName = repository.findMazeNameById(mazeId);
    long sessions = mazeName.map(repository::countTrainingSessionsByMazeRef).orElse(0L);
    if (runs > 0 || coverage > 0 || sessions > 0) {
      return new PersistedAssetDeletionCandidate(
          ref,
          false,
          "Maze has dependencies: trainingRuns="
              + runs
              + ", coverage="
              + coverage
              + ", sessions="
              + sessions
              + ".");
    }
    return new PersistedAssetDeletionCandidate(ref, true, "Eligible.");
  }

  private PersistedAssetDeletionCandidate validateTrainingSession(PersistedAssetRef ref) {
    long runs = repository.countTrainingRunsBySessionId(ref.assetId().trim());
    if (runs > 0) {
      return new PersistedAssetDeletionCandidate(
          ref, false, "Training session has dependent runs: " + runs + ".");
    }
    return new PersistedAssetDeletionCandidate(ref, true, "Eligible.");
  }

  private PersistedAssetDeletionCandidate validateTrainingPreset(PersistedAssetRef ref) {
    long presetId = Long.parseLong(ref.assetId().trim());
    long sessions = repository.countTrainingSessionsByPresetId(presetId);
    long budgets = repository.countExplorationBudgetsByPresetId(presetId);
    if (sessions > 0 || budgets > 0) {
      return new PersistedAssetDeletionCandidate(
          ref,
          false,
          "Training preset has dependencies: sessions="
              + sessions
              + ", explorationBudgets="
              + budgets
              + ".");
    }
    return new PersistedAssetDeletionCandidate(ref, true, "Eligible.");
  }
}
