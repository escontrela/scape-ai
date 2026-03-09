CREATE TABLE IF NOT EXISTS mazes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  rows_count INTEGER NOT NULL,
  cols_count INTEGER NOT NULL,
  layout TEXT NOT NULL
);

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

CREATE TABLE IF NOT EXISTS training_presets (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  episodes INTEGER NOT NULL,
  timeout_millis INTEGER NOT NULL,
  policy TEXT NOT NULL,
  seed INTEGER
);
