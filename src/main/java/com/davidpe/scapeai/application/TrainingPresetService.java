package com.davidpe.scapeai.application;

import java.util.List;
import java.util.Optional;

public interface TrainingPresetService {

  TrainingPreset save(TrainingPresetDraft preset);

  List<TrainingPreset> list();

  Optional<TrainingPreset> load(long id);

  Optional<TrainingPreset> apply(long id);

  Optional<TrainingPreset> activePreset();
}
