package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.davidpe.scapeai.simulation.GridPosition;
import java.util.List;
import org.junit.jupiter.api.Test;

class EpisodeTrajectoryCodecTest {

  @Test
  void shouldEncodeAndDecodeTrajectoryWithRelativeAndAbsoluteSegments() {
    List<GridPosition> trajectory =
        List.of(
            new GridPosition(0, 0),
            new GridPosition(0, 1),
            new GridPosition(1, 1),
            new GridPosition(1, 1),
            new GridPosition(4, 4),
            new GridPosition(4, 5));

    String encoded = EpisodeTrajectoryCodec.encode(trajectory);
    List<GridPosition> decoded = EpisodeTrajectoryCodec.decode(encoded);

    assertEquals(trajectory, decoded);
  }
}
