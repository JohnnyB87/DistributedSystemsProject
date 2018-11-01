package controllers;

import classes.FileInfo;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

public class MyMediaPlayerPopupController {

    @FXML private Label labelPopup;
    @FXML private Button pauseButton;
    @FXML private Button playButton;
    @FXML private Button quitButton;
    @FXML private MediaView mediaView;
    @FXML private AnchorPane anchorPane;
    @FXML private HBox mediaBar;
    private Slider timeSlider;
    private Duration duration;

    private MediaPlayer mediaPlayer;
    private FileInfo fileToPlay;

    MyMediaPlayerPopupController(FileInfo fileToPlay) {
        this.fileToPlay = fileToPlay;
    }

    @FXML
    private void initialize(){
        this.playButton.setDisable(true);
        this.playButton.setOnAction(e->playButtonPressed());
        this.pauseButton.setOnAction(e->pauseButtonPressed());
        this.quitButton.setOnAction(e->quitButtonPressed());

        playFile();
    }

    void setLabelPopupText(String text) {
        this.labelPopup.setText("Now Playing " + text);
    }

    private void pauseButtonPressed() {
        this.playButton.setDisable(false);
        this.pauseButton.setDisable(true);
        this.mediaPlayer.pause();
    }

    private void playButtonPressed() {
        this.pauseButton.setDisable(false);
        this.playButton.setDisable(true);
        this.mediaPlayer.play();
    }

    public void quitButtonPressed() {
        this.mediaPlayer.stop();
        Stage stage = (Stage)this.playButton.getScene().getWindow();
        stage.close();
    }

    private void playFile(){
        if(fileToPlay !=  null) {
            String name = fileToPlay.getName();
            String type = fileToPlay.getType();
            File file = new File(fileToPlay.getLocation() + File.separator + name + "." + type);
            if (type.equalsIgnoreCase("mp3") || type.equalsIgnoreCase("mp4")) {
                Media hit = new Media(file.toURI().toString());
                mediaPlayer = new MediaPlayer(hit);
                mediaPlayer.play();
                mediaView.setMediaPlayer(mediaPlayer);
                mediaView.setPreserveRatio(true);
                if(type.equalsIgnoreCase("mp3")){
                    System.out.println("MP3 playing");
                    this.anchorPane.prefHeight(200);
                    this.mediaView.setFitHeight(0);
                }
            }
        }
    }
}
