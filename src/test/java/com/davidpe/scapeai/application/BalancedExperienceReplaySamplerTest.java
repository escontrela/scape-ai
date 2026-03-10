package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.persistence.ExperienceTransitionEntity;
import com.davidpe.scapeai.persistence.repository.ExperienceReplayRepository;
import com.davidpe.scapeai.persistence.repository.ExperienceReplaySamplingStrategy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class BalancedExperienceReplaySamplerTest {

  @Test
  void shouldKeepMinimumBalanceAcrossSuccessTimeoutAndCollision() {
    BalancedExperienceReplaySampler sampler =
        new BalancedExperienceReplaySampler(
            new InMemoryExperienceReplayRepository(
                List.of(
                    transition(1L, "exit=true", 1.0),
                    transition(2L, "exit=true", 0.8),
                    transition(3L, "exit=false", 0.1),
                    transition(4L, "exit=false", 0.0),
                    transition(5L, "exit=false", -0.8),
                    transition(6L, "exit=false", -0.9),
                    transition(7L, "exit=false", -1.2),
                    transition(8L, "exit=false", 0.2))));

    List<BalancedReplaySample> batch = sampler.sampleRecent(6);

    assertEquals(6, batch.size());
    assertTrue(count(batch, ExperienceReplayOutcome.SUCCESS) >= 2);
    assertTrue(count(batch, ExperienceReplayOutcome.TIMEOUT) >= 2);
    assertTrue(count(batch, ExperienceReplayOutcome.COLLISION) >= 2);
  }

  @Test
  void shouldReturnAvailableRowsWhenDatasetIsSmall() {
    BalancedExperienceReplaySampler sampler =
        new BalancedExperienceReplaySampler(
            new InMemoryExperienceReplayRepository(List.of(transition(1L, "exit=true", 1.0))));

    List<BalancedReplaySample> batch = sampler.sampleRecent(4);

    assertEquals(1, batch.size());
    assertEquals(ExperienceReplayOutcome.SUCCESS, batch.get(0).outcome());
  }

  @Test
  void shouldImproveRewardAndNoveltyBenchmarksAgainstUniform() {
    List<ExperienceTransitionEntity> dataset =
        List.of(
            transition(1L, "exit=true;zone=A", 0.2),
            transition(2L, "exit=false;zone=B", -0.1),
            transition(3L, "exit=false;zone=B", -0.2),
            transition(4L, "exit=false;zone=C", 1.6),
            transition(5L, "exit=false;zone=C", -1.7),
            transition(6L, "exit=false;zone=D", 0.3),
            transition(7L, "exit=false;zone=E", -1.5),
            transition(8L, "exit=false;zone=F", 0.1),
            transition(9L, "exit=true;zone=G", 1.9),
            transition(10L, "exit=false;zone=H", 0.2),
            transition(11L, "exit=false;zone=I", 0.2),
            transition(12L, "exit=false;zone=J", -1.8));
    BalancedExperienceReplaySampler sampler =
        new BalancedExperienceReplaySampler(new InMemoryExperienceReplayRepository(dataset));

    List<BalancedReplaySample> uniform =
        sampler.sampleRecent(8, ExperienceReplaySamplingStrategy.UNIFORM);
    List<BalancedReplaySample> rewardAware =
        sampler.sampleRecent(8, ExperienceReplaySamplingStrategy.REWARD_AWARE);
    List<BalancedReplaySample> noveltyAware =
        sampler.sampleRecent(8, ExperienceReplaySamplingStrategy.NOVELTY_AWARE);

    assertTrue(averageAbsoluteReward(rewardAware) >= averageAbsoluteReward(uniform));
    assertTrue(uniqueNextStates(noveltyAware) >= uniqueNextStates(uniform));
  }

  private static int count(List<BalancedReplaySample> batch, ExperienceReplayOutcome outcome) {
    int count = 0;
    for (BalancedReplaySample sample : batch) {
      if (sample.outcome() == outcome) {
        count++;
      }
    }
    return count;
  }

  private static ExperienceTransitionEntity transition(long id, String nextStateSummary, double reward) {
    return new ExperienceTransitionEntity(id, "state-" + id, "RIGHT", reward, nextStateSummary, 1_000L + id);
  }

  private static double averageAbsoluteReward(List<BalancedReplaySample> batch) {
    if (batch.isEmpty()) {
      return 0.0;
    }
    double total = 0.0;
    for (BalancedReplaySample sample : batch) {
      total += Math.abs(sample.transition().reward());
    }
    return total / batch.size();
  }

  private static int uniqueNextStates(List<BalancedReplaySample> batch) {
    HashSet<String> unique = new HashSet<>();
    for (BalancedReplaySample sample : batch) {
      unique.add(sample.transition().nextStateSummary());
    }
    return unique.size();
  }

  private static final class InMemoryExperienceReplayRepository implements ExperienceReplayRepository {

    private final List<ExperienceTransitionEntity> rows;

    private InMemoryExperienceReplayRepository(List<ExperienceTransitionEntity> rows) {
      this.rows = new ArrayList<>(rows);
    }

    @Override
    public ExperienceTransitionEntity save(ExperienceTransitionEntity transition) {
      rows.add(transition);
      return transition;
    }

    @Override
    public List<ExperienceTransitionEntity> findRecent(int page, int pageSize) {
      int offset = Math.max(0, page) * Math.max(1, pageSize);
      if (offset >= rows.size()) {
        return List.of();
      }
      int end = Math.min(rows.size(), offset + Math.max(1, pageSize));
      return List.copyOf(rows.subList(offset, end));
    }

    @Override
    public List<ExperienceTransitionEntity> findRecent(
        int page, int pageSize, ExperienceReplaySamplingStrategy strategy) {
      List<ExperienceTransitionEntity> ordered = new ArrayList<>(rows);
      ExperienceReplaySamplingStrategy effective =
          strategy == null ? ExperienceReplaySamplingStrategy.UNIFORM : strategy;
      switch (effective) {
        case UNIFORM ->
            ordered.sort(
                Comparator.comparingLong(ExperienceTransitionEntity::createdAtEpochMillis)
                    .reversed());
        case REWARD_AWARE ->
            ordered.sort(
                Comparator.comparingDouble(
                        (ExperienceTransitionEntity row) -> Math.abs(row.reward()))
                    .reversed()
                    .thenComparing(
                        Comparator.comparingLong(ExperienceTransitionEntity::createdAtEpochMillis)
                            .reversed()));
        case NOVELTY_AWARE ->
            ordered.sort(
                Comparator.comparingInt(
                        (ExperienceTransitionEntity row) -> row.nextStateSummary() == null ? 0 : row.nextStateSummary().length())
                    .reversed()
                    .thenComparing(
                        Comparator.comparingLong(ExperienceTransitionEntity::createdAtEpochMillis)
                            .reversed()));
      }
      int offset = Math.max(0, page) * Math.max(1, pageSize);
      if (offset >= ordered.size()) {
        return List.of();
      }
      int end = Math.min(ordered.size(), offset + Math.max(1, pageSize));
      return List.copyOf(ordered.subList(offset, end));
    }
  }
}
