package controllers;

import classes.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.*;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyMediaPlayerController {

    private AnchorPane anchorPane;
    @FXML private Button refreshButton;
    @FXML private Button selectButton;
    @FXML private MediaPlayerTableView<FileInfo> clientTable;
    @FXML private MediaPlayerTableView<FileInfo> serverTable;
    @FXML private Button playButton;
    @FXML private Button quitButton;
    @FXML private Button downloadButton;
    @FXML private Button uploadButton;
    @FXML private TextField serverIpTxtBox;
    @FXML private Button connectButton;
    private Monitor sharedFolder;
    private MyMediaPlayer localFolder;
    private Alert alert;
    private boolean clientIsSelected = false;
    private boolean serverIsSelected = false;

    @FXML
    private void initialize(){

        this.localFolder = new MyMediaPlayer();
        this.sharedFolder = this.localFolder.getMonitor();
        serverTable.getItems().addAll(this.sharedFolder.getNames());

        clientTableSelected();
        serverTableSelected();

        ExecutorService application = Executors.newCachedThreadPool();
        application.execute( this.sharedFolder );
        application.shutdown();

    }

    public void connectButtonPressed() {
        System.out.println("Hello");
    }

    public void quitButtonPressed() {
        Platform.exit();
    }

    public void selectButtonPressed() {
        try {
            this.clientTable.getItems().clear();
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedFolder = directoryChooser.showDialog(this.selectButton.getScene().getWindow());

            this.localFolder.setLocalFolder(selectedFolder);
            this.localFolder.folderItemsToArrayList();
            this.clientTable.getItems().addAll(this.localFolder.getFileInfo());
        }catch(Exception e){
            System.out.println("Folder not selected.");
            alert = new Alert(Alert.AlertType.ERROR, "No folder selected");
            alert.show();
        }

    }

    public void playButtonPressed() {
        if(serverIsSelected || clientIsSelected) {
            try {

                FileInfo s = serverIsSelected ? serverTable.getSelectionModel().getSelectedItem()
                        : clientTable.getSelectionModel().getSelectedItem();

                String name = s != null ? s.getName() : "NULL";
//                String type = s != null ? s.getType() : "NULL";
//                File file = new File(sharedFolder.getFolderPath() + File.separator + name + "." + type);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("../resources/MyMediaPlayerPopupWindow.fxml"));

                MyMediaPlayerPopupController myController = new MyMediaPlayerPopupController(s);
                loader.setController(myController);
                this.anchorPane = loader.load();
                myController.setLabelPopupText(name);

                createNewStage(myController);
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadButtonPressed() {
        if(this.localFolder.getLocalFolder() != null){
            int selected = this.serverTable.getSelectionModel().getSelectedIndex();
            System.out.println("Index: " + selected);
            if (selected > -1) {
                System.out.println("Index: " + this.clientTable.getSelectionModel().getFocusedIndex());
                FileInfo file = this.clientTable.getSelectionModel().getSelectedItems().get(0);
                this.localFolder.uploadFile(file);
                if(this.sharedFolder.checkForChange())
                    serverTable.getItems().add(file);
            } else {
                System.out.println("Select item to upload");
                alert = new Alert(Alert.AlertType.ERROR, "No file selected");
                alert.show();
            }
        }else {
            System.out.println("Select folder first");
            alert = new Alert(Alert.AlertType.ERROR, "No folder selected");
            alert.show();
        }
    }

    public void downloadButtonPressed() {
        if(this.localFolder.getLocalFolder() != null){
            int selected = this.serverTable.getSelectionModel().getSelectedIndex();
            System.out.println("Index: " + selected);
            if (selected > -1) {
                FileInfo file = this.serverTable.getSelectionModel().getSelectedItems().get(0);
                this.localFolder.downLoadFile(file);
                if (this.localFolder.checkForChange())
                    clientTable.getItems().add(file);
            } else {
                System.out.println("Select item to download");
                alert = new Alert(Alert.AlertType.ERROR, "No file selected");
                alert.show();
            }
        }else {
            System.out.println("Select folder first");
            alert = new Alert(Alert.AlertType.ERROR, "No folder selected");
            alert.show();
        }
    }

    public void refreshButtonPressed() {
        if(this.sharedFolder.checkForChange()) {
            serverTable.getItems().clear();
            serverTable.getItems().addAll(this.sharedFolder.getNames());
        }
    }

    public void serverTableSelected() {
        this.serverTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            this.clientIsSelected = false;
            this.serverIsSelected = true;
            this.uploadButton.setDisable(true);
            if(this.localFolder.getLocalFolder() != null) {
                if (this.localFolder.fileExists(newSelection)) {
                    this.playButton.setDisable(false);
                    this.downloadButton.setDisable(true);
                }
                else {
                    this.playButton.setDisable(true);
                    this.downloadButton.setDisable(false);
                }
            }
            else {
                System.out.println("Select folder first");
                alert = new Alert(Alert.AlertType.ERROR, "No folder selected");
                alert.show();
            }
        });
    }

    public void clientTableSelected() {
        this.clientTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            this.clientIsSelected = true;
            this.serverIsSelected = false;
            this.playButton.setDisable(false);
            this.downloadButton.setDisable(true);
            if(this.sharedFolder.getFolder() != null) {
                if (this.sharedFolder.fileExists(newSelection))
                    this.uploadButton.setDisable(true);
                else
                    this.uploadButton.setDisable(false);
            }
        });
    }

    private void createNewStage(MyMediaPlayerPopupController myController){
        String title = "Media Player";

        StackPane sp = new StackPane();
        sp.getChildren().add(this.anchorPane);

        Scene scene = new Scene(sp);
        Stage stage = new Stage();

        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.initOwner(this.playButton.getScene().getWindow());
        stage.setOnCloseRequest(event ->
            myController.quitButtonPressed()
        );
        stage.showAndWait();
    }

}
