package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.MazeEntity;
import java.util.Optional;

public interface MazeRepository {

  MazeEntity save(MazeEntity maze);

  Optional<MazeEntity> findById(long id);
}
