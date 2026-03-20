package com.davidpe.scapeai.ui;

final class DashboardScaleTokens {
  private final double dpiStep;
  private final double fontScale;
  private final double spacingScale;
  private final double layoutScale;

  private DashboardScaleTokens(double dpiStep, double fontScale, double spacingScale, double layoutScale) {
    this.dpiStep = dpiStep;
    this.fontScale = fontScale;
    this.spacingScale = spacingScale;
    this.layoutScale = layoutScale;
  }

  static DashboardScaleTokens defaultTokens() {
    return forOutputScale(1.0);
  }

  static DashboardScaleTokens forOutputScale(double outputScale) {
    double clamped = Math.max(1.0, Math.min(1.5, outputScale));
    double step;
    if (clamped >= 1.375) {
      step = 1.5;
    } else if (clamped >= 1.125) {
      step = 1.25;
    } else {
      step = 1.0;
    }
    double relative = step - 1.0;
    double font = 1.0 + (relative * 0.92);
    double spacing = 1.0 + (relative * 0.80);
    double layout = 1.0 + (relative * 0.76);
    return new DashboardScaleTokens(step, font, spacing, layout);
  }

  double dpiStep() {
    return dpiStep;
  }

  double fontScale() {
    return fontScale;
  }

  double spacingScale() {
    return spacingScale;
  }

  double layoutScale() {
    return layoutScale;
  }
}
