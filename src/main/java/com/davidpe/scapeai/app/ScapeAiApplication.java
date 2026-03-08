package com.davidpe.scapeai.app;

import com.davidpe.scapeai.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public final class ScapeAiApplication extends Application {

  private final MainWindow mainWindow = new MainWindow();

  @Override
  public void start(Stage primaryStage) {
    mainWindow.show(primaryStage);
  }

  public static void main(String[] args) {
    launch(args);
  }
}
