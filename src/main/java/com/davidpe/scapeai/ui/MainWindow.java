package com.davidpe.scapeai.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public final class MainWindow {

  public void show(Stage stage) {
    Label message = new Label("Scape AI bootstrap ready");
    message.setFont(Font.font("Consolas", 22));
    message.setTextFill(Color.web("#7ef9ff"));

    StackPane root = new StackPane(message);
    root.setPadding(new Insets(24));
    root.setAlignment(Pos.CENTER);
    root.setStyle("-fx-background-color: linear-gradient(to bottom, #0a0f1f, #05070f);");

    Scene scene = new Scene(root, 960, 640);
    stage.setTitle("Scape AI");
    stage.setScene(scene);
    stage.show();
  }
}
