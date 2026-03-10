package com.davidpe.scapeai.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.simulation.GridPosition;
import com.davidpe.scapeai.simulation.MazeDefinition;
import com.davidpe.scapeai.simulation.MoveDirection;
import com.davidpe.scapeai.simulation.SimulationState;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SpatialContextTest {

  @Test
  void shouldExposeExpandedContractFields() {
    boolean[][] walls = {
      {false, true, false},
      {false, false, false},
      {true, false, false}
    };
    MazeDefinition maze =
        new MazeDefinition(3, 3, walls, new GridPosition(1, 1), new GridPosition(1, 2));
    SimulationState state =
        new SimulationState(new GridPosition(1, 1), Set.of(new GridPosition(1, 1)), 0, false);

    SpatialContext context = new SpatialContext(maze, state, MoveDirection.LEFT, 3);

    assertEquals(SpatialContext.NeighborCell.WALL, context.localNeighborhood().get(MoveDirection.UP));
    assertEquals(SpatialContext.NeighborCell.OPEN, context.localNeighborhood().get(MoveDirection.DOWN));
    assertEquals(SpatialContext.NeighborCell.OPEN, context.localNeighborhood().get(MoveDirection.LEFT));
    assertEquals(SpatialContext.NeighborCell.EXIT, context.localNeighborhood().get(MoveDirection.RIGHT));
    assertEquals(false, context.validActionMask().get(MoveDirection.UP));
    assertEquals(true, context.validActionMask().get(MoveDirection.DOWN));
    assertEquals(true, context.validActionMask().get(MoveDirection.LEFT));
    assertEquals(true, context.validActionMask().get(MoveDirection.RIGHT));
    assertEquals(MoveDirection.LEFT, context.previousDirection());
    assertEquals(3, context.noProgressStreak());
  }

  @Test
  void shouldRemainCompatibleWithLegacyConstructor() {
    MazeDefinition maze =
        new MazeDefinition(2, 2, new boolean[2][2], new GridPosition(0, 0), new GridPosition(1, 1));
    SimulationState state = SimulationState.initial(new GridPosition(0, 0));

    SpatialContext context = new SpatialContext(maze, state);

    assertNull(context.previousDirection());
    assertEquals(0, context.noProgressStreak());
    assertTrue(context.canMove(MoveDirection.RIGHT));
    assertTrue(context.canMove(MoveDirection.DOWN));
    assertEquals(SpatialContext.NeighborCell.WALL, context.localNeighborhood().get(MoveDirection.UP));
  }
}
