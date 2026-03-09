package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.ExperienceTransitionEntity;
import com.davidpe.scapeai.persistence.repository.ExperienceReplayRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BalancedExperienceReplaySampler {

  private static final int MAX_PAGES = 4;
  private final ExperienceReplayRepository experienceReplayRepository;

  public BalancedExperienceReplaySampler(ExperienceReplayRepository experienceReplayRepository) {
    this.experienceReplayRepository = experienceReplayRepository;
  }

  public List<BalancedReplaySample> sampleRecent(int batchSize) {
    int safeBatchSize = Math.max(1, batchSize);
    List<ExperienceTransitionEntity> recent = loadRecentPool(safeBatchSize);
    if (recent.isEmpty()) {
      return List.of();
    }

    Map<ExperienceReplayOutcome, ArrayDeque<BalancedReplaySample>> buckets =
        new EnumMap<>(ExperienceReplayOutcome.class);
    for (ExperienceReplayOutcome outcome : ExperienceReplayOutcome.values()) {
      buckets.put(outcome, new ArrayDeque<>());
    }
    for (ExperienceTransitionEntity transition : recent) {
      ExperienceReplayOutcome outcome = classify(transition);
      buckets.get(outcome).addLast(new BalancedReplaySample(transition, outcome));
    }

    int perCategoryTarget = Math.max(1, safeBatchSize / ExperienceReplayOutcome.values().length);
    List<BalancedReplaySample> batch = new ArrayList<>();
    for (ExperienceReplayOutcome outcome : ExperienceReplayOutcome.values()) {
      addFromBucket(batch, buckets.get(outcome), perCategoryTarget, safeBatchSize);
    }
    for (ExperienceReplayOutcome outcome : ExperienceReplayOutcome.values()) {
      addFromBucket(batch, buckets.get(outcome), safeBatchSize, safeBatchSize);
    }
    return List.copyOf(batch);
  }

  ExperienceReplayOutcome classify(ExperienceTransitionEntity transition) {
    String nextStateSummary =
        transition.nextStateSummary() == null
            ? ""
            : transition.nextStateSummary().toLowerCase(Locale.ROOT);
    if (nextStateSummary.contains("exit=true") || nextStateSummary.contains("success")) {
      return ExperienceReplayOutcome.SUCCESS;
    }
    if (transition.reward() <= -0.5) {
      return ExperienceReplayOutcome.COLLISION;
    }
    return ExperienceReplayOutcome.TIMEOUT;
  }

  private List<ExperienceTransitionEntity> loadRecentPool(int batchSize) {
    int pageSize = Math.max(batchSize * 3, 24);
    List<ExperienceTransitionEntity> pool = new ArrayList<>();
    for (int page = 0; page < MAX_PAGES; page++) {
      List<ExperienceTransitionEntity> chunk = experienceReplayRepository.findRecent(page, pageSize);
      if (chunk.isEmpty()) {
        break;
      }
      pool.addAll(chunk);
      if (chunk.size() < pageSize) {
        break;
      }
    }
    return pool;
  }

  private void addFromBucket(
      List<BalancedReplaySample> batch,
      ArrayDeque<BalancedReplaySample> bucket,
      int maxItems,
      int maxBatchSize) {
    int added = 0;
    while (added < maxItems && batch.size() < maxBatchSize && !bucket.isEmpty()) {
      batch.add(bucket.removeFirst());
      added++;
    }
  }
}
