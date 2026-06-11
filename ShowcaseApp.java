import javafx.application.Application;
import javafx.stage.Stage;
 
public class ShowcaseApp extends Application {
 
    @Override
    public void start(Stage primaryStage) {
        MainScene mainScene = new MainScene(primaryStage);
        mainScene.show();
    }
 
    public static void main(String[] args) {
        launch(args);
    }
}