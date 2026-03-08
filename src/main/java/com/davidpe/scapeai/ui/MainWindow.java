package com.davidpe.scapeai.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public final class MainWindow {

  public void show(Stage stage) {
    Label message = new Label("hello AI");
    message.setFont(Font.font(24));

    StackPane root = new StackPane(message);
    root.setPadding(new Insets(24));
    root.setAlignment(Pos.CENTER);

    Scene scene = new Scene(root, 640, 480);
    stage.setTitle("Scape AI");
    stage.setScene(scene);
    stage.show();
  }
}
