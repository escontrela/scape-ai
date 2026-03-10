package com.davidpe.scapeai.application;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TrainingSessionConfigValidator {

  private static final Duration MIN_TIMEOUT = Duration.ofSeconds(1);
  private static final Duration MAX_TIMEOUT = Duration.ofMinutes(30);

  public List<TrainingSessionConfigValidationError> validate(TrainingSessionConfig config) {
    List<TrainingSessionConfigValidationError> errors = new ArrayList<>();
    if (config == null) {
      errors.add(
          new TrainingSessionConfigValidationError(
              TrainingSessionConfigErrorCode.CONFIG_REQUIRED,
              "Training session config is required."));
      return List.copyOf(errors);
    }

    if (config.version() != TrainingSessionConfig.VERSION_1) {
      errors.add(
          new TrainingSessionConfigValidationError(
              TrainingSessionConfigErrorCode.UNSUPPORTED_VERSION,
              "Unsupported training session config version: " + config.version()));
    }
    if (isBlank(config.mazeId())) {
      errors.add(
          new TrainingSessionConfigValidationError(
              TrainingSessionConfigErrorCode.MAZE_ID_REQUIRED, "Select a maze before starting."));
    }
    if (isBlank(config.policyId())) {
      errors.add(
          new TrainingSessionConfigValidationError(
              TrainingSessionConfigErrorCode.POLICY_ID_REQUIRED, "Select a movement policy."));
    }
    if (config.timeout() == null) {
      errors.add(
          new TrainingSessionConfigValidationError(
              TrainingSessionConfigErrorCode.TIMEOUT_REQUIRED, "Training timeout is required."));
    } else if (config.timeout().compareTo(MIN_TIMEOUT) < 0
        || config.timeout().compareTo(MAX_TIMEOUT) > 0) {
      errors.add(
          new TrainingSessionConfigValidationError(
              TrainingSessionConfigErrorCode.TIMEOUT_OUT_OF_RANGE,
              "Training timeout must be between 1 second and 30 minutes."));
    }
    if (config.difficultyTarget() == null) {
      errors.add(
          new TrainingSessionConfigValidationError(
              TrainingSessionConfigErrorCode.DIFFICULTY_TARGET_REQUIRED,
              "Select a target difficulty before starting."));
    }
    return List.copyOf(errors);
  }

  public void ensureValid(TrainingSessionConfig config) {
    List<TrainingSessionConfigValidationError> errors = validate(config);
    if (!errors.isEmpty()) {
      throw new TrainingSessionConfigValidationException(errors);
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
