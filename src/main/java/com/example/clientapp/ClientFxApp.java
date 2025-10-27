package com.example.clientapp;

import com.example.clientapp.config.ApplicationContextProvider;
import com.example.clientapp.controller.MainScreenController;
import com.example.clientapp.util.KioskLockUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class ClientFxApp extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(ClientApp.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LockScreen.fxml"));
        loader.setControllerFactory(ApplicationContextProvider.getContext()::getBean);

        Scene scene = new Scene(loader.load());

        stage.setScene(scene);
        stage.setTitle("FunFactor POS Client");
/*
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setFullScreen(true);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setOnCloseRequest(Event::consume);
*/

        // Safe controller injection
        Object controller = loader.getController();
        if (controller instanceof MainScreenController mainController) {
            mainController.setStage(stage);
        }

        // Enable kiosk
//       KioskLockUtil.enableKioskMode(stage, scene);

        stage.show();
    }

    @Override
    public void stop() {
        try {
            if (springContext != null) springContext.close();
            Platform.exit();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
