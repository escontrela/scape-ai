package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.davidpe.scapeai.persistence.repository.PersistedAssetPreviewRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PersistedAssetPreviewServiceTest {

  @Test
  void shouldRenderCompactMazePreview() {
    PersistedAssetPreviewRepository repository =
        (type, assetId) ->
            Optional.of(
                Map.of(
                    "id", 8L,
                    "name", "maze-neon",
                    "rows_count", 4,
                    "cols_count", 6,
                    "difficulty_score", 0.7,
                    "layout", "######\n#....#\n#.##.#\n######"));
    PersistedAssetPreviewService service = new PersistedAssetPreviewService(repository);

    var preview = service.render(PersistedAssetType.MAZE, "8", 1000);

    assertTrue(preview.isPresent());
    assertTrue(preview.get().content().contains("TYPE=MAZE"));
    assertTrue(preview.get().content().contains("dims=4x6"));
    assertTrue(preview.get().content().contains("layout:"));
    assertEquals(false, preview.get().truncated());
  }

  @Test
  void shouldTruncateLargePayload() {
    PersistedAssetPreviewRepository repository =
        (type, assetId) ->
            Optional.of(
                Map.ofEntries(
                    Map.entry("id", 99L),
                    Map.entry("training_session_id", "session-x"),
                    Map.entry("maze_id", 2L),
                    Map.entry("policy_id", "heuristic-baseline"),
                    Map.entry("success", 0),
                    Map.entry("steps", 120),
                    Map.entry("elapsed_millis", 9999L),
                    Map.entry("total_reward", -88.5),
                    Map.entry("collisions", 50),
                    Map.entry("discovered_cells", 40),
                    Map.entry("final_distance_to_exit", 10),
                    Map.entry("terminal_reason", "TIMEOUT"),
                    Map.entry("timeout_reached", 1),
                    Map.entry("trajectory_path", "X".repeat(900)),
                    Map.entry("replay_debug_metadata", "{\"very\":\"" + "x".repeat(900) + "\"}"),
                    Map.entry("created_at_epoch_millis", 1234L)));
    PersistedAssetPreviewService service = new PersistedAssetPreviewService(repository);

    var preview = service.render(PersistedAssetType.TRAINING_RUN, "99", 220);

    assertTrue(preview.isPresent());
    assertTrue(preview.get().truncated());
    assertTrue(preview.get().content().endsWith("..."));
  }
}
