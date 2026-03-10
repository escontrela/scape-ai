package com.davidpe.scapeai.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.application.PolicyInferenceTrace;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationState;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CompositeMovementPolicyTest {

  @Test
  void shouldUseFallbackWhenPrimaryConfidenceIsBelowThreshold() {
    TraceablePrimaryPolicy lowConfidencePrimary =
        new TraceablePrimaryPolicy(
            MoveDirection.RIGHT,
            new PolicyInferenceTrace("djl-primary", 0.20, 1, false, null));
    CompositeMovementPolicy composite =
        new CompositeMovementPolicy("djl-composite", lowConfidencePrimary, context -> MoveDirection.DOWN, 0.60);

    MoveDirection decision = composite.chooseNextMove(contextForOpenMaze());

    assertEquals(MoveDirection.DOWN, decision);
    assertTrue(composite.latestInferenceTrace().isPresent());
    assertTrue(composite.latestInferenceTrace().orElseThrow().fallbackApplied());
    assertEquals("LOW_CONFIDENCE", composite.latestInferenceTrace().orElseThrow().fallbackReason());
  }

  @Test
  void shouldUsePrimaryWhenValidAndAboveThreshold() {
    TraceablePrimaryPolicy strongPrimary =
        new TraceablePrimaryPolicy(
            MoveDirection.RIGHT,
            new PolicyInferenceTrace("djl-primary", 0.95, 1, false, null));
    CompositeMovementPolicy composite =
        new CompositeMovementPolicy("djl-composite", strongPrimary, context -> MoveDirection.DOWN, 0.60);

    MoveDirection decision = composite.chooseNextMove(contextForOpenMaze());

    assertEquals(MoveDirection.RIGHT, decision);
    assertTrue(composite.latestInferenceTrace().isPresent());
    assertTrue(!composite.latestInferenceTrace().orElseThrow().fallbackApplied());
  }

  private SpatialContext contextForOpenMaze() {
    boolean[][] walls = new boolean[3][3];
    MazeDefinition maze = new MazeDefinition(3, 3, walls, new GridPosition(1, 1), new GridPosition(2, 2));
    return new SpatialContext(maze, SimulationState.initial(new GridPosition(1, 1)));
  }

  private static final class TraceablePrimaryPolicy implements MovementPolicy, InferenceTraceProvider {

    private final MoveDirection direction;
    private final PolicyInferenceTrace trace;

    private TraceablePrimaryPolicy(MoveDirection direction, PolicyInferenceTrace trace) {
      this.direction = direction;
      this.trace = trace;
    }

    @Override
    public MoveDirection chooseNextMove(SpatialContext context) {
      return direction;
    }

    @Override
    public Optional<PolicyInferenceTrace> latestInferenceTrace() {
      return Optional.of(trace);
    }
  }
}
