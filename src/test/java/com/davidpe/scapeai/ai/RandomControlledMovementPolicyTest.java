package com.davidpe.scapeai.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationState;
import org.junit.jupiter.api.Test;

class RandomControlledMovementPolicyTest {

  @Test
  void shouldProduceDeterministicMoveSequenceWithSameSeed() {
    RandomControlledMovementPolicy first = new RandomControlledMovementPolicy(42L);
    RandomControlledMovementPolicy second = new RandomControlledMovementPolicy(42L);
    MazeDefinition maze =
        new MazeDefinition(3, 3, new boolean[3][3], new GridPosition(1, 1), new GridPosition(2, 2));
    SpatialContext context = new SpatialContext(maze, SimulationState.initial(new GridPosition(1, 1)));

    MoveDirection firstMove = first.chooseNextMove(context);
    MoveDirection secondMove = second.chooseNextMove(context);

    assertEquals(firstMove, secondMove);
  }
}
