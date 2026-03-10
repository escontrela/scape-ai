package com.davidpe.scapeai.ai.infrastructure.djl;

import com.davidpe.scapeai.ai.InferenceTraceProvider;
import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.ai.SpatialContext;
import com.davidpe.scapeai.application.PolicyInferenceTrace;
import com.davidpe.scapeai.simulation.MoveDirection;
import java.util.Optional;

public class DjlMovementPolicyAdapter implements MovementPolicy, InferenceTraceProvider {

  private final DjlDirectionPredictor predictor;
  private final MovementPolicy fallbackPolicy;
  private volatile PolicyInferenceTrace latestTrace;

  public DjlMovementPolicyAdapter(DjlDirectionPredictor predictor, MovementPolicy fallbackPolicy) {
    this.predictor = predictor;
    this.fallbackPolicy = fallbackPolicy;
  }

  @Override
  public MoveDirection chooseNextMove(SpatialContext context) {
    long startedAt = System.nanoTime();
    try {
      MoveDirection predicted = predictor.predictNext(context);
      if (predicted == null) {
        return useFallback(context, startedAt, "NULL_PREDICTION");
      }

      if (!context.canMove(predicted)) {
        return useFallback(context, startedAt, "INVALID_MOVE");
      }
      latestTrace = buildTrace(1.0, startedAt, false, null);
      return predicted;
    } catch (RuntimeException predictionFailure) {
      return useFallback(context, startedAt, "PREDICTION_FAILURE");
    }
  }

  @Override
  public Optional<PolicyInferenceTrace> latestInferenceTrace() {
    return Optional.ofNullable(latestTrace);
  }

  private MoveDirection useFallback(SpatialContext context, long startedAt, String reason) {
    MoveDirection fallbackDecision = fallbackPolicy.chooseNextMove(context);
    latestTrace = buildTrace(0.0, startedAt, true, reason);
    return fallbackDecision;
  }

  private PolicyInferenceTrace buildTrace(
      double confidence, long startedAtNanos, boolean fallbackApplied, String fallbackReason) {
    long latencyMillis = Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000);
    return new PolicyInferenceTrace(
        "djl-adapter", confidence, latencyMillis, fallbackApplied, fallbackReason);
  }
}
