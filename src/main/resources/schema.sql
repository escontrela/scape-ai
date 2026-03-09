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
  created_at_epoch_millis INTEGER NOT NULL,
  FOREIGN KEY (maze_id) REFERENCES mazes(id)
);

ALTER TABLE training_runs ADD COLUMN IF NOT EXISTS policy_id TEXT;
ALTER TABLE training_runs ADD COLUMN IF NOT EXISTS policy_snapshot TEXT;
ALTER TABLE mazes ADD COLUMN IF NOT EXISTS difficulty_score REAL NOT NULL DEFAULT 0;

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
