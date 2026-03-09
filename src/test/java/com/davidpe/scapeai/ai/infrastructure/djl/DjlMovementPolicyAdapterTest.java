package com.davidpe.scapeai.ai.infrastructure.djl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.scapeai.ai.MovementPolicy;
import com.davidpe.scapeai.ai.SpatialContext;
import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationState;
import org.junit.jupiter.api.Test;

class DjlMovementPolicyAdapterTest {

  @Test
  void shouldUsePredictedMoveWhenItIsValid() {
    MovementPolicy fallback = context -> MoveDirection.UP;
    DjlMovementPolicyAdapter adapter = new DjlMovementPolicyAdapter(context -> MoveDirection.RIGHT, fallback);
    SpatialContext context = contextForOpenMaze();

    MoveDirection decision = adapter.chooseNextMove(context);

    assertEquals(MoveDirection.RIGHT, decision);
  }

  @Test
  void shouldFallbackWhenPredictionIsInvalidOrFails() {
    MovementPolicy fallback = context -> MoveDirection.DOWN;
    SpatialContext context = contextForOpenMaze();

    DjlMovementPolicyAdapter invalidPredictionAdapter =
        new DjlMovementPolicyAdapter(any -> MoveDirection.LEFT, fallback);
    DjlMovementPolicyAdapter failingAdapter =
        new DjlMovementPolicyAdapter(
            any -> {
              throw new IllegalStateException("model unavailable");
            },
            fallback);

    assertEquals(MoveDirection.DOWN, invalidPredictionAdapter.chooseNextMove(context));
    assertEquals(MoveDirection.DOWN, failingAdapter.chooseNextMove(context));
  }

  private SpatialContext contextForOpenMaze() {
    boolean[][] walls = new boolean[3][3];
    walls[1][0] = true;
    MazeDefinition maze = new MazeDefinition(3, 3, walls, new GridPosition(1, 1), new GridPosition(2, 2));
    return new SpatialContext(maze, SimulationState.initial(new GridPosition(1, 1)));
  }
}
