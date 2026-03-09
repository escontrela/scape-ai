package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.persistence.ExperienceTransitionEntity;
import com.davidpe.scapeai.persistence.repository.ExperienceReplayRepository;
import java.util.ArrayList;
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
  }
}
