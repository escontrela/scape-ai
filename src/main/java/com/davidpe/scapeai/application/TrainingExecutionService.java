package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public interface TrainingExecutionService {

  CompletableFuture<IterativeTrainingSummary> startTraining(
      MazeDefinition maze, int episodes, Duration timeout);

  void cancelTraining();
}
