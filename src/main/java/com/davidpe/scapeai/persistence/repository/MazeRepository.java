package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.MazeEntity;
import java.util.List;
import java.util.Optional;

public interface MazeRepository {

  MazeEntity save(MazeEntity maze);

  MazeEntity upsertByName(MazeEntity maze);

  Optional<MazeEntity> findById(long id);

  List<MazeEntity> findAllOrderByDifficulty(boolean ascending);
}
