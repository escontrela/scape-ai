package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.TrainingRunEntity;
import java.util.List;

public interface TrainingRunRepository {

  TrainingRunEntity save(TrainingRunEntity run);

  List<TrainingRunEntity> findByMazeId(long mazeId);

  List<TrainingRunEntity> findRecentByMazeId(long mazeId, int limit);
}
