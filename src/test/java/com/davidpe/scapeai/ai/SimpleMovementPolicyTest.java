package com.davidpe.scapeai.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationState;
import java.util.LinkedHashSet;
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
        new MazeDefinition(3, 3, new boolean[3][3], new GridPosition(1, 1), new GridPosition(0, 2));

    Set<GridPosition> visited = new LinkedHashSet<>();
    visited.add(new GridPosition(1, 1));
    visited.add(new GridPosition(1, 2));
    visited.add(new GridPosition(1, 1));
    SimulationState state = new SimulationState(new GridPosition(1, 2), Set.copyOf(visited), 0, false);

    MoveDirection decision = policy.chooseNextMove(new SpatialContext(maze, state));

    assertEquals(MoveDirection.UP, decision);
  }
}
