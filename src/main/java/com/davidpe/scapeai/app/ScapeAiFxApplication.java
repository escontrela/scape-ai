package com.davidpe.scapeai.app;

import com.davidpe.scapeai.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class ScapeAiFxApplication extends Application {

  private ConfigurableApplicationContext context;

  @Override
  public void init() {
    context =
        new SpringApplicationBuilder(ScapeAiSpringBoot.class)
            .headless(false)
            .run(getParameters().getRaw().toArray(new String[0]));
  }

  @Override
  public void start(Stage primaryStage) {
    context.getBean(MainWindow.class).show(primaryStage);
  }

  @Override
  public void stop() {
    if (context != null) {
      context.close();
    }
  }
}
