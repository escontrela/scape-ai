package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.ExperienceTransitionEntity;
import com.davidpe.scapeai.persistence.repository.ExperienceReplayRepository;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationState;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class AsyncExperienceTransitionRecorder implements ExperienceTransitionRecorder {

  /** Bounded queue – drops oldest tasks when full to prevent unbounded memory growth. */
  private static final int MAX_QUEUED_TRANSITIONS = 2_048;

  private final ExperienceReplayRepository repository;
  private final ExecutorService executor;

  public AsyncExperienceTransitionRecorder(ExperienceReplayRepository repository) {
    this.repository = repository;
    this.executor =
        new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_QUEUED_TRANSITIONS),
            runnable -> {
              Thread thread = new Thread(runnable, "experience-transition-writer");
              thread.setDaemon(true);
              return thread;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy());
  }

  @Override
  public void recordTransition(
      SimulationState previousState,
      MoveDirection action,
      double reward,
      SimulationState nextState) {
    ExperienceTransitionEntity transition =
        new ExperienceTransitionEntity(
            null,
            summarize(previousState),
            action.name(),
            reward,
            summarize(nextState),
            System.currentTimeMillis());
    executor.execute(() -> repository.save(transition));
  }

  @PreDestroy
  public void shutdown() {
    executor.shutdownNow();
  }

  private String summarize(SimulationState state) {
    GridPosition position = state.agentPosition();
    return "pos="
        + position.row()
        + ":"
        + position.col()
        + "|visited="
        + state.visitedCells().size()
        + "|invalid="
        + state.invalidAttempts()
        + "|exit="
        + state.exitReached();
  }
}
