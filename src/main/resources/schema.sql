CREATE TABLE IF NOT EXISTS mazes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  rows_count INTEGER NOT NULL,
  cols_count INTEGER NOT NULL,
  layout TEXT NOT NULL,
  difficulty_score REAL NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_mazes_name ON mazes(name);

CREATE TABLE IF NOT EXISTS training_runs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  maze_id INTEGER NOT NULL,
  policy_id TEXT,
  policy_snapshot TEXT,
  success INTEGER NOT NULL,
  steps INTEGER NOT NULL,
  elapsed_millis INTEGER NOT NULL,
  total_reward REAL NOT NULL,
  collisions INTEGER NOT NULL,
  discovered_cells INTEGER NOT NULL,
  final_distance_to_exit INTEGER NOT NULL,
  net_progress REAL NOT NULL DEFAULT 0,
  maze_coverage_ratio REAL NOT NULL DEFAULT 0,
  q1_coverage REAL NOT NULL DEFAULT 0,
  q2_coverage REAL NOT NULL DEFAULT 0,
  q3_coverage REAL NOT NULL DEFAULT 0,
  q4_coverage REAL NOT NULL DEFAULT 0,
  left_side_coverage REAL NOT NULL DEFAULT 0,
  right_side_coverage REAL NOT NULL DEFAULT 0,
  path_entropy REAL NOT NULL DEFAULT 0,
  created_at_epoch_millis INTEGER NOT NULL,
  FOREIGN KEY (maze_id) REFERENCES mazes(id)
);


CREATE TABLE IF NOT EXISTS training_presets (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  episodes INTEGER NOT NULL,
  timeout_millis INTEGER NOT NULL,
  policy TEXT NOT NULL,
  seed INTEGER
);

CREATE TABLE IF NOT EXISTS experience_transitions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  state_summary TEXT NOT NULL,
  action TEXT NOT NULL,
  reward REAL NOT NULL,
  next_state_summary TEXT NOT NULL,
  created_at_epoch_millis INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS maze_policy_coverage (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  maze_id INTEGER NOT NULL,
  policy_id TEXT NOT NULL,
  solved INTEGER NOT NULL DEFAULT 0,
  updated_at_epoch_millis INTEGER NOT NULL,
  FOREIGN KEY (maze_id) REFERENCES mazes(id),
  UNIQUE(maze_id, policy_id)
);

CREATE TABLE IF NOT EXISTS exploration_budgets (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  preset_id INTEGER NOT NULL,
  policy_id TEXT NOT NULL,
  initial_budget INTEGER NOT NULL,
  consume_per_episode INTEGER NOT NULL,
  remaining_budget INTEGER NOT NULL,
  updated_at_epoch_millis INTEGER NOT NULL,
  UNIQUE(preset_id, policy_id)
);
