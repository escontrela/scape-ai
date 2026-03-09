package com.davidpe.scapeai.application;

import com.davidpe.scapeai.simulation.MazeDefinition;
import java.time.Duration;
import java.util.function.BooleanSupplier;

public interface IterativeEpisodeTrainingService {

  IterativeTrainingSummary train(
      MazeDefinition maze, int episodes, Duration timeout, BooleanSupplier cancellationRequested);
}
