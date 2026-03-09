package com.davidpe.scapeai.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationState;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SimpleMovementPolicyTest {

  @Test
  void shouldChooseMoveThatGetsCloserToExit() {
    SimpleMovementPolicy policy = new SimpleMovementPolicy();
    MazeDefinition maze =
        new MazeDefinition(4, 4, new boolean[4][4], new GridPosition(0, 0), new GridPosition(0, 3));
    SimulationState state = SimulationState.initial(new GridPosition(0, 0));

    MoveDirection decision = policy.chooseNextMove(new SpatialContext(maze, state));

    assertEquals(MoveDirection.RIGHT, decision);
  }

  @Test
  void shouldAvoidImmediateLoopUsingRecentHistory() {
    SimpleMovementPolicy policy = new SimpleMovementPolicy(4);
    MazeDefinition maze =
        new MazeDefinition(3, 4, new boolean[3][4], new GridPosition(1, 1), new GridPosition(1, 3));
    SimulationState state =
        new SimulationState(
            new GridPosition(1, 1),
            Set.of(new GridPosition(1, 1), new GridPosition(1, 2)),
            0,
            false);

    SpatialContext context =
        new SpatialContext(
            maze,
            state,
            List.of(
                new GridPosition(1, 1),
                new GridPosition(1, 2),
                new GridPosition(1, 1),
                new GridPosition(1, 2)),
            MoveDirection.LEFT,
            3);
    MoveDirection decision = policy.chooseNextMove(context);

    assertEquals(MoveDirection.UP, decision);
  }
}
