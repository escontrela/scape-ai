package com.davidpe.scapeai.application;

import com.davidpe.scapeai.persistence.repository.PersistedAssetPreviewRepository;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PersistedAssetPreviewService {

  private static final int DEFAULT_MAX_CHARS = 1400;

  private final PersistedAssetPreviewRepository repository;

  public PersistedAssetPreviewService(PersistedAssetPreviewRepository repository) {
    this.repository = repository;
  }

  public Optional<PersistedAssetPreview> render(PersistedAssetType type, String assetId) {
    return render(type, assetId, DEFAULT_MAX_CHARS);
  }

  public Optional<PersistedAssetPreview> render(
      PersistedAssetType type, String assetId, int maxChars) {
    int budget = Math.max(120, maxChars);
    Optional<Map<String, Object>> source = repository.findByTypeAndId(type, assetId);
    if (source.isEmpty()) {
      return Optional.empty();
    }
    String raw = switch (type) {
      case MAZE -> renderMaze(source.get());
      case TRAINING_RUN -> renderTrainingRun(source.get());
      case TRAINING_PRESET -> renderPreset(source.get());
      case TRAINING_SESSION -> renderSession(source.get());
      case EXPERIENCE_TRANSITION -> renderTransition(source.get());
      case MAZE_POLICY_COVERAGE -> renderCoverage(source.get());
      case EXPLORATION_BUDGET -> renderBudget(source.get());
    };
    boolean truncated = raw.length() > budget;
    String text = truncated ? raw.substring(0, budget - 3) + "..." : raw;
    return Optional.of(new PersistedAssetPreview(type, assetId, text, truncated));
  }

  private String renderMaze(Map<String, Object> row) {
    String layout = asString(row.get("layout"), "");
    String[] lines = layout.split("\\R");
    StringBuilder compact = new StringBuilder();
    int maxLines = Math.min(lines.length, 12);
    for (int index = 0; index < maxLines; index++) {
      String line = lines[index];
      if (line.length() > 48) {
        compact.append(line, 0, 45).append("...");
      } else {
        compact.append(line);
      }
      compact.append('\n');
    }
    if (lines.length > maxLines) {
      compact.append("...").append('\n');
    }
    return "TYPE=MAZE\n"
        + "id="
        + asLong(row.get("id"), -1)
        + "\nname="
        + asString(row.get("name"), "unknown")
        + "\ndims="
        + asInt(row.get("rows_count"), 0)
        + "x"
        + asInt(row.get("cols_count"), 0)
        + "\ndifficulty="
        + String.format(Locale.US, "%.2f", asDouble(row.get("difficulty_score"), 0.0))
        + "\nlayout:\n"
        + compact;
  }

  private String renderTrainingRun(Map<String, Object> row) {
    return "TYPE=TRAINING_RUN\n"
        + "id="
        + asLong(row.get("id"), -1)
        + "\nsession="
        + asString(row.get("training_session_id"), "-")
        + "\nmazeId="
        + asLong(row.get("maze_id"), -1)
        + "\npolicy="
        + asString(row.get("policy_id"), "unknown")
        + "\nsuccess="
        + asBoolean(row.get("success"))
        + "\nsteps="
        + asInt(row.get("steps"), 0)
        + "\nelapsedMs="
        + asLong(row.get("elapsed_millis"), 0)
        + "\nreward="
        + String.format(Locale.US, "%.2f", asDouble(row.get("total_reward"), 0.0))
        + "\ncollisions="
        + asInt(row.get("collisions"), 0)
        + "\ndiscovered="
        + asInt(row.get("discovered_cells"), 0)
        + "\nfinalDistance="
        + asInt(row.get("final_distance_to_exit"), 0)
        + "\nterminal="
        + asString(row.get("terminal_reason"), "UNKNOWN")
        + "\ntimeout="
        + asBoolean(row.get("timeout_reached"))
        + "\ntrajectory="
        + clampInline(asString(row.get("trajectory_path"), "-"), 180)
        + "\nreplayMeta="
        + clampInline(asString(row.get("replay_debug_metadata"), "{}"), 220)
        + "\ncreatedAtEpochMs="
        + asLong(row.get("created_at_epoch_millis"), 0);
  }

  private String renderPreset(Map<String, Object> row) {
    return "TYPE=TRAINING_PRESET\n"
        + "id="
        + asLong(row.get("id"), -1)
        + "\nepisodes="
        + asInt(row.get("episodes"), 0)
        + "\ntimeoutMs="
        + asLong(row.get("timeout_millis"), 0)
        + "\npolicy="
        + asString(row.get("policy"), "unknown")
        + "\nseed="
        + (row.get("seed") == null ? "AUTO" : asLong(row.get("seed"), 0));
  }

  private String renderSession(Map<String, Object> row) {
    return "TYPE=TRAINING_SESSION\n"
        + "id="
        + asString(row.get("id"), "unknown")
        + "\nmaze="
        + asString(row.get("maze_ref"), "unknown")
        + "\npolicy="
        + asString(row.get("policy_id"), "unknown")
        + "\npreset="
        + (row.get("preset_id") == null ? "-" : asLong(row.get("preset_id"), -1))
        + "\nseed="
        + asLong(row.get("effective_seed"), 0)
        + "\nstartedAtEpochMs="
        + asLong(row.get("started_at_epoch_millis"), 0)
        + "\nendedAtEpochMs="
        + (row.get("ended_at_epoch_millis") == null
            ? "-"
            : Long.toString(asLong(row.get("ended_at_epoch_millis"), 0)));
  }

  private String renderTransition(Map<String, Object> row) {
    return "TYPE=EXPERIENCE_TRANSITION\n"
        + "id="
        + asLong(row.get("id"), -1)
        + "\naction="
        + asString(row.get("action"), "UNKNOWN")
        + "\nreward="
        + String.format(Locale.US, "%.3f", asDouble(row.get("reward"), 0.0))
        + "\nstate="
        + clampInline(asString(row.get("state_summary"), "-"), 240)
        + "\nnextState="
        + clampInline(asString(row.get("next_state_summary"), "-"), 240)
        + "\ncreatedAtEpochMs="
        + asLong(row.get("created_at_epoch_millis"), 0);
  }

  private String renderCoverage(Map<String, Object> row) {
    return "TYPE=MAZE_POLICY_COVERAGE\n"
        + "id="
        + asLong(row.get("id"), -1)
        + "\nmazeId="
        + asLong(row.get("maze_id"), -1)
        + "\npolicy="
        + asString(row.get("policy_id"), "unknown")
        + "\nsolved="
        + asBoolean(row.get("solved"))
        + "\nupdatedAtEpochMs="
        + asLong(row.get("updated_at_epoch_millis"), 0);
  }

  private String renderBudget(Map<String, Object> row) {
    return "TYPE=EXPLORATION_BUDGET\n"
        + "id="
        + asLong(row.get("id"), -1)
        + "\npreset="
        + asLong(row.get("preset_id"), -1)
        + "\npolicy="
        + asString(row.get("policy_id"), "unknown")
        + "\ninitial="
        + asInt(row.get("initial_budget"), 0)
        + "\nconsumePerEpisode="
        + asInt(row.get("consume_per_episode"), 0)
        + "\nremaining="
        + asInt(row.get("remaining_budget"), 0)
        + "\nupdatedAtEpochMs="
        + asLong(row.get("updated_at_epoch_millis"), 0);
  }

  private static String asString(Object value, String fallback) {
    if (value == null) {
      return fallback;
    }
    String text = value.toString();
    return text.isBlank() ? fallback : text;
  }

  private static long asLong(Object value, long fallback) {
    return value instanceof Number number ? number.longValue() : fallback;
  }

  private static int asInt(Object value, int fallback) {
    return value instanceof Number number ? number.intValue() : fallback;
  }

  private static double asDouble(Object value, double fallback) {
    return value instanceof Number number ? number.doubleValue() : fallback;
  }

  private static boolean asBoolean(Object value) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    if (value instanceof Number number) {
      return number.intValue() != 0;
    }
    return false;
  }

  private static String clampInline(String value, int maxLength) {
    String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
    if (normalized.length() <= maxLength) {
      return normalized;
    }
    return normalized.substring(0, maxLength - 3) + "...";
  }
}
