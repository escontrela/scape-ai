package com.davidpe.scapeai.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SingleStepSimulationEngineTest {

  private final SingleStepSimulationEngine engine = new SingleStepSimulationEngine();

  @Test
  void shouldMoveAgentWhenTargetCellIsWalkable() {
    MazeDefinition maze = mazeWithExitAt(0, 2, new boolean[3][3]);
    SimulationState start = SimulationState.initial(new GridPosition(0, 0));

    SimulationStepResult result = engine.step(start, maze, MoveDirection.RIGHT);

    assertTrue(result.moved());
    assertFalse(result.collision());
    assertEquals(new GridPosition(0, 1), result.state().agentPosition());
    assertTrue(result.state().visitedCells().contains(new GridPosition(0, 1)));
    assertEquals(0, result.state().invalidAttempts());
    assertFalse(result.state().exitReached());
  }

  @Test
  void shouldRegisterInvalidAttemptWhenTargetCellIsWall() {
    boolean[][] walls = new boolean[3][3];
    walls[0][1] = true;
    MazeDefinition maze = mazeWithExitAt(0, 2, walls);
    SimulationState start = SimulationState.initial(new GridPosition(0, 0));

    SimulationStepResult result = engine.step(start, maze, MoveDirection.RIGHT);

    assertFalse(result.moved());
    assertTrue(result.collision());
    assertEquals(new GridPosition(0, 0), result.state().agentPosition());
    assertEquals(1, result.state().invalidAttempts());
    assertEquals(1, result.state().visitedCells().size());
  }

  @Test
  void shouldMarkExitAsReachedWhenAgentArrivesToExitCell() {
    MazeDefinition maze = mazeWithExitAt(0, 1, new boolean[3][3]);
    SimulationState start = SimulationState.initial(new GridPosition(0, 0));

    SimulationStepResult result = engine.step(start, maze, MoveDirection.RIGHT);

    assertTrue(result.moved());
    assertTrue(result.state().exitReached());
  }

  private MazeDefinition mazeWithExitAt(int row, int col, boolean[][] walls) {
    return new MazeDefinition(3, 3, walls, new GridPosition(row, col));
  }
}
