package com.davidpe.scapeai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrainingSessionConfigValidatorTest {

  private final TrainingSessionConfigValidator validator = new TrainingSessionConfigValidator();

  @Test
  void shouldReturnTypedErrorsForInvalidConfig() {
    TrainingSessionConfig config =
        new TrainingSessionConfig(99, "", " ", Duration.ofHours(2), null, true, null);

    List<TrainingSessionConfigValidationError> errors = validator.validate(config);

    assertEquals(5, errors.size());
    assertTrue(errors.stream().anyMatch(error -> error.code() == TrainingSessionConfigErrorCode.UNSUPPORTED_VERSION));
    assertTrue(errors.stream().anyMatch(error -> error.code() == TrainingSessionConfigErrorCode.MAZE_ID_REQUIRED));
    assertTrue(errors.stream().anyMatch(error -> error.code() == TrainingSessionConfigErrorCode.POLICY_ID_REQUIRED));
    assertTrue(errors.stream().anyMatch(error -> error.code() == TrainingSessionConfigErrorCode.TIMEOUT_OUT_OF_RANGE));
    assertTrue(
        errors.stream()
            .anyMatch(error -> error.code() == TrainingSessionConfigErrorCode.DIFFICULTY_TARGET_REQUIRED));
  }

  @Test
  void shouldThrowTypedExceptionWhenEnsureValidFails() {
    TrainingSessionConfigValidationException exception =
        assertThrows(TrainingSessionConfigValidationException.class, () -> validator.ensureValid(null));

    assertEquals(1, exception.errors().size());
    assertEquals(TrainingSessionConfigErrorCode.CONFIG_REQUIRED, exception.errors().get(0).code());
  }
}
