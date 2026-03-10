package com.davidpe.scapeai.ai;

import com.davidpe.scapeai.application.PolicyInferenceTrace;
import com.davidpe.scapeai.simulation.MoveDirection;
import java.util.Optional;

public class CompositeMovementPolicy implements MovementPolicy, InferenceTraceProvider {

  private final String policyId;
  private final MovementPolicy primaryPolicy;
  private final MovementPolicy fallbackPolicy;
  private final double confidenceThreshold;
  private volatile PolicyInferenceTrace latestTrace;

  public CompositeMovementPolicy(
      String policyId,
      MovementPolicy primaryPolicy,
      MovementPolicy fallbackPolicy,
      double confidenceThreshold) {
    this.policyId = policyId == null || policyId.isBlank() ? "composite-policy" : policyId;
    this.primaryPolicy = java.util.Objects.requireNonNull(primaryPolicy, "primaryPolicy must not be null");
    this.fallbackPolicy = java.util.Objects.requireNonNull(fallbackPolicy, "fallbackPolicy must not be null");
    this.confidenceThreshold = Math.max(0.0, Math.min(1.0, confidenceThreshold));
  }

  @Override
  public MoveDirection chooseNextMove(SpatialContext context) {
    long startedAt = System.nanoTime();
    try {
      MoveDirection primaryDecision = primaryPolicy.chooseNextMove(context);
      Optional<PolicyInferenceTrace> primaryTrace = latestPrimaryTrace();
      double confidence = primaryTrace.map(PolicyInferenceTrace::confidence).orElse(1.0);
      if (primaryDecision == null) {
        return chooseFallback(context, startedAt, confidence, "NULL_PRIMARY_DECISION");
      }
      if (!isValid(primaryDecision, context)) {
        return chooseFallback(context, startedAt, confidence, "INVALID_PRIMARY_MOVE");
      }
      if (confidence < confidenceThreshold) {
        return chooseFallback(context, startedAt, confidence, "LOW_CONFIDENCE");
      }
      latestTrace =
          new PolicyInferenceTrace(
              policyId,
              confidence,
              elapsedMillis(startedAt),
              false,
              primaryTrace.map(PolicyInferenceTrace::fallbackReason).orElse(null));
      return primaryDecision;
    } catch (RuntimeException primaryFailure) {
      return chooseFallback(context, startedAt, 0.0, "PRIMARY_EXCEPTION");
    }
  }

  @Override
  public Optional<PolicyInferenceTrace> latestInferenceTrace() {
    return Optional.ofNullable(latestTrace);
  }

  private MoveDirection chooseFallback(
      SpatialContext context, long startedAt, double confidence, String reason) {
    MoveDirection fallbackDecision = fallbackPolicy.chooseNextMove(context);
    latestTrace =
        new PolicyInferenceTrace(
            policyId, confidence, elapsedMillis(startedAt), true, reason);
    return fallbackDecision;
  }

  private Optional<PolicyInferenceTrace> latestPrimaryTrace() {
    if (primaryPolicy instanceof InferenceTraceProvider provider) {
      return provider.latestInferenceTrace();
    }
    return Optional.empty();
  }

  private boolean isValid(MoveDirection direction, SpatialContext context) {
    var target = context.simulationState().agentPosition().move(direction);
    return context.maze().isInside(target) && !context.maze().isWall(target);
  }

  private long elapsedMillis(long startedAt) {
    return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
  }
}
