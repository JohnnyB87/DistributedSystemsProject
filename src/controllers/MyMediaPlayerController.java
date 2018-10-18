package controllers;

import classes.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.TouchEvent;
import javafx.stage.DirectoryChooser;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javazoom.jl.decoder.JavaLayerException;

public class MyMediaPlayerController {

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

    @FXML
    private void initialize(){

        this.localFolder = new MyMediaPlayer();
        this.sharedFolder = this.localFolder.getMonitor();
        serverTable.getItems().addAll(this.sharedFolder.getNames());
        ExecutorService application = Executors.newCachedThreadPool();
        application.execute( this.sharedFolder );
        application.shutdown();

    }

    public void connectButtonPressed(ActionEvent actionEvent) {

        System.out.println("Hello");
    }

    public void quitButtonPressed(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void selectButtonPressed(ActionEvent actionEvent) {
//        FileChooser fileChooser = new FileChooser();
//        fileChooser.showOpenDialog(this.selectButton.getScene().getWindow());
        try {
            this.clientTable.getItems().clear();
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedFolder = directoryChooser.showDialog(this.selectButton.getScene().getWindow());

            this.localFolder.setLocalFolder(selectedFolder);
            this.localFolder.folderItemsToArrayList();
            this.clientTable.getItems().addAll(this.localFolder.getFileInfo());
        }catch(Exception e){
            System.out.println("Folder not selected.");
        }

    }

    public void playButtonPressed(ActionEvent actionEvent) {

//        new Thread() {
//            try {
//
//                FileInfo s = serverTable.getSelectionModel().getSelectedItem();
//                File file = new File (sharedFolder.getFolderPath() + File.separator + s.getName()+"."+s.getType());
//                FileInputStream fis = new FileInputStream(file);
//                BufferedInputStream bis = new BufferedInputStream(fis);
//                try{
//
//                        Player player = new Player(bis);
//                        player.play();
//
//                }catch(JavaLayerException ex){System.out.println("Player problem");}
//
//
//
//            } catch (IOException e){System.out.println("File io problem: " + e);}
//        }.start();
    }

    public void uploadButtonPressed(ActionEvent actionEvent) {
        System.out.println("Index: " + this.clientTable.getSelectionModel().getFocusedIndex());
        FileInfo file = this.clientTable.getSelectionModel().getSelectedItems().get(0);
        this.localFolder.uploadFile(file);
        if(this.sharedFolder.checkForChange()) {
            serverTable.getItems().add(file);
//            this.sharedFolder.addFile(file);
        }
    }

    public void downloadButtonPressed(ActionEvent actionEvent) {
        System.out.println("Index: " + this.serverTable.getSelectionModel().getFocusedIndex());
        FileInfo file = this.serverTable.getSelectionModel().getSelectedItems().get(0);
        this.localFolder.downLoadFile(file);
//        if(this.localFolder.checkForChange())
//            clientTable.getItems().add(file);
    }

    public void refreshButtonPressed(ActionEvent actionEvent) {
        serverTable.getItems().clear();
//        this.sharedFolder.populateArray();
        serverTable.getItems().addAll(this.sharedFolder.getNames());
    }

//    public void checkIfFileExists() {
//        this.playButton.setDisable(true);
//        this.serverTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
//            if (this.localFolder.fileExists(newSelection)) {
//                this.playButton.setDisable(false);
//            }
//            System.out.println(newSelection.toString());
//        });
//    }
}
