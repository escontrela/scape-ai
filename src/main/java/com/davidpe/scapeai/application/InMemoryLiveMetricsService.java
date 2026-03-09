package com.davidpe.scapeai.application;

import jakarta.annotation.PreDestroy;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class InMemoryLiveMetricsService implements LiveMetricsService {

  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "live-metrics-updater");
            thread.setDaemon(true);
            return thread;
          });
  private final List<java.util.function.Consumer<LiveEpisodeMetrics>> listeners =
      new CopyOnWriteArrayList<>();
  private final List<java.util.function.Consumer<List<TrainingTimelineEntry>>> timelineListeners =
      new CopyOnWriteArrayList<>();
  private final Deque<TrainingTimelineEntry> recentTimeline = new ArrayDeque<>();
  private final AtomicInteger steps = new AtomicInteger(0);
  private final AtomicInteger collisions = new AtomicInteger(0);
  private final AtomicLong elapsedMillis = new AtomicLong(0L);
  private volatile double accumulatedReward = 0.0;
  private volatile long episodeStartedAt = 0L;
  private volatile ScheduledFuture<?> ticker;
  private volatile SimulationSpeed simulationSpeed = SimulationSpeed.NORMAL;
  private volatile boolean episodeActive;

  @Override
  public synchronized void startEpisode() {
    if (episodeActive && steps.get() > 0) {
      completeEpisode();
    }
    resetSnapshot();
    episodeStartedAt = System.currentTimeMillis();
    episodeActive = true;
    publish(snapshot());
    restartTicker();
  }

  @Override
  public synchronized void pauseEpisode() {
    stopTicker();
    publish(snapshot());
  }

  @Override
  public synchronized void resetEpisode() {
    stopTicker();
    episodeActive = false;
    resetSnapshot();
    publish(snapshot());
  }

  @Override
  public synchronized void completeEpisode() {
    if (!episodeActive) {
      return;
    }
    stopTicker();
    LiveEpisodeMetrics metrics = snapshot();
    recentTimeline.addFirst(
        new TrainingTimelineEntry(
            classifyEpisode(metrics), metrics.accumulatedReward(), metrics.elapsedMillis()));
    while (recentTimeline.size() > 12) {
      recentTimeline.removeLast();
    }
    episodeActive = false;
    publish(metrics);
    publishTimeline();
  }

  @Override
  public synchronized void setSimulationSpeed(SimulationSpeed speed) {
    if (speed == null || speed == simulationSpeed) {
      return;
    }
    simulationSpeed = speed;
    if (ticker != null) {
      restartTicker();
    }
  }

  @Override
  public SimulationSpeed simulationSpeed() {
    return simulationSpeed;
  }

  @Override
  public void subscribe(java.util.function.Consumer<LiveEpisodeMetrics> listener) {
    listeners.add(listener);
    listener.accept(snapshot());
  }

  @Override
  public void subscribeTimeline(java.util.function.Consumer<List<TrainingTimelineEntry>> listener) {
    timelineListeners.add(listener);
    listener.accept(List.copyOf(recentTimeline));
  }

  @PreDestroy
  public synchronized void shutdown() {
    stopTicker();
    scheduler.shutdownNow();
  }

  private void tick() {
    int tickStep = steps.incrementAndGet();
    if (tickStep % 5 == 0) {
      collisions.incrementAndGet();
      accumulatedReward -= 0.8;
    } else {
      accumulatedReward += 0.2;
    }
    elapsedMillis.set(Math.max(0L, System.currentTimeMillis() - episodeStartedAt));
    publish(snapshot());
  }

  private synchronized void stopTicker() {
    if (ticker != null) {
      ticker.cancel(false);
      ticker = null;
    }
  }

  private synchronized void restartTicker() {
    stopTicker();
    long period = simulationSpeed.metricsTickMillis();
    ticker = scheduler.scheduleAtFixedRate(this::tick, period, period, TimeUnit.MILLISECONDS);
  }

  private synchronized void resetSnapshot() {
    steps.set(0);
    collisions.set(0);
    elapsedMillis.set(0);
    accumulatedReward = 0.0;
  }

  private LiveEpisodeMetrics snapshot() {
    return new LiveEpisodeMetrics(steps.get(), collisions.get(), accumulatedReward, elapsedMillis.get());
  }

  private void publish(LiveEpisodeMetrics metrics) {
    for (java.util.function.Consumer<LiveEpisodeMetrics> listener : listeners) {
      listener.accept(metrics);
    }
  }

  private void publishTimeline() {
    List<TrainingTimelineEntry> snapshot = List.copyOf(recentTimeline);
    for (java.util.function.Consumer<List<TrainingTimelineEntry>> listener : timelineListeners) {
      listener.accept(snapshot);
    }
  }

  private TrainingTimelineStatus classifyEpisode(LiveEpisodeMetrics metrics) {
    if (metrics.collisions() >= Math.max(3, metrics.steps() / 3)) {
      return TrainingTimelineStatus.COLLISION_STALL;
    }
    if (metrics.accumulatedReward() > 0.0) {
      return TrainingTimelineStatus.SUCCESS;
    }
    return TrainingTimelineStatus.TIMEOUT;
  }
}
