package com.davidpe.scapeai.persistence.repository;

import com.davidpe.scapeai.persistence.TrainingPresetEntity;
import java.util.List;
import java.util.Optional;

public interface TrainingPresetRepository {

  TrainingPresetEntity save(TrainingPresetEntity preset);

  List<TrainingPresetEntity> findAll();

  Optional<TrainingPresetEntity> findById(long id);
}
