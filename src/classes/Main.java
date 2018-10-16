package classes;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        primaryStage.setTitle("My Media Player");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("../resources/MyMediaPlayerWindow.fxml"));
        Pane mainWindow = loader.load();

        StackPane layout = new StackPane();

        layout.getChildren().add(mainWindow);
        Scene scene = new Scene(layout,650,325);

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
