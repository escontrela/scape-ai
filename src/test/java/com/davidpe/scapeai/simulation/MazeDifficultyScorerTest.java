package com.davidpe.scapeai.simulation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MazeDifficultyScorerTest {

  @Test
  void shouldAssignHigherScoreToMoreComplexMaze() {
    MazeDifficultyScorer scorer = new MazeDifficultyScorer();
    boolean[][] simpleWalls = new boolean[4][4];
    MazeDefinition simple =
        new MazeDefinition(4, 4, simpleWalls, new GridPosition(0, 0), new GridPosition(3, 3));

    boolean[][] complexWalls = new boolean[10][10];
    for (int row = 1; row < 9; row++) {
      complexWalls[row][4] = true;
      complexWalls[row][6] = true;
    }
    complexWalls[5][4] = false;
    complexWalls[7][6] = false;
    MazeDefinition complex =
        new MazeDefinition(10, 10, complexWalls, new GridPosition(0, 0), new GridPosition(9, 9));

    assertTrue(scorer.score(complex) > scorer.score(simple));
  }
}
