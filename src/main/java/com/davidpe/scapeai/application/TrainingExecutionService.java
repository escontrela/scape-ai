package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public interface TrainingExecutionService {

  CompletableFuture<IterativeTrainingSummary> startTraining(
      MazeDefinition maze, int episodes, Duration timeout);

  CompletableFuture<IterativeTrainingSummary> startBatchTraining(
      MazeDefinition maze, int episodesPerBatch, int batches, Duration timeout);

  default CompletableFuture<IterativeTrainingSummary> startBatchTraining(
      MazeDefinition maze,
      int episodesPerBatch,
      int batches,
      Duration timeout,
      TrainingBudget budget) {
    return startBatchTraining(maze, episodesPerBatch, batches, timeout);
  }

  void cancelTraining();
}
