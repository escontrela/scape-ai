package com.davidpe.scapeai.application;

public interface SimulationControlService {

  void start();

  void pause();

  void reset();

  void selectMovementPolicy(String policyId);

  String activeMovementPolicy();

  java.util.List<MovementPolicyOption> availableMovementPolicies();

  java.util.List<TrainingPresetOption> availableTrainingPresets();

  void applyTrainingPreset(long presetId);

  Long activeTrainingPresetId();
}
