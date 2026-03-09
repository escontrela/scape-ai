package com.davidpe.scapeai.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationState;
import java.util.Random;
import org.junit.jupiter.api.Test;

class EpsilonGreedyMovementPolicyDecoratorTest {

  @Test
  void shouldExploreWhenEpsilonIsOne() {
    MovementPolicy delegate = context -> MoveDirection.UP;
    EpsilonGreedyMovementPolicyDecorator decorator =
        new EpsilonGreedyMovementPolicyDecorator(delegate, 1.0, () -> new Random(42L));
    MazeDefinition maze =
        new MazeDefinition(3, 3, new boolean[3][3], new GridPosition(1, 1), new GridPosition(2, 2));
    SpatialContext context = new SpatialContext(maze, SimulationState.initial(new GridPosition(1, 1)));

    MoveDirection chosen = decorator.chooseNextMove(context);

    assertTrue(chosen != null);
    assertTrue(decorator.lastDecisionExploration());
  }

  @Test
  void shouldExploitDelegateWhenEpsilonIsZero() {
    MovementPolicy delegate = context -> MoveDirection.LEFT;
    EpsilonGreedyMovementPolicyDecorator decorator =
        new EpsilonGreedyMovementPolicyDecorator(delegate, 0.0, () -> new Random(42L));
    MazeDefinition maze =
        new MazeDefinition(3, 3, new boolean[3][3], new GridPosition(1, 1), new GridPosition(2, 2));
    SpatialContext context = new SpatialContext(maze, SimulationState.initial(new GridPosition(1, 1)));

    MoveDirection chosen = decorator.chooseNextMove(context);

    assertEquals(MoveDirection.LEFT, chosen);
    assertEquals(false, decorator.lastDecisionExploration());
  }

  @Test
  void shouldValidateEpsilonRange() {
    MovementPolicy delegate = context -> MoveDirection.LEFT;

    assertThrows(
        IllegalArgumentException.class,
        () -> new EpsilonGreedyMovementPolicyDecorator(delegate, -0.1, () -> new Random(1L)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new EpsilonGreedyMovementPolicyDecorator(delegate, 1.1, () -> new Random(1L)));
  }
}
