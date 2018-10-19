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
import java.lang.reflect.InvocationTargetException;
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

        checkIfFileExists();

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

    public void playButtonPressed() {

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
            } else
                System.out.println("Select item to upload");
        }else
            System.out.println("Select folder first");
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
            } else
                System.out.println("Select item to download");
        }else
            System.out.println("Select folder first");
    }

    public void refreshButtonPressed() {
        if(this.sharedFolder.checkForChange()) {
            serverTable.getItems().clear();
            serverTable.getItems().addAll(this.sharedFolder.getNames());
        }
    }

    public void checkIfFileExists() {

        this.serverTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if(this.localFolder.getLocalFolder() != null) {
                if (this.localFolder.fileExists(newSelection))
                    this.playButton.setDisable(true);
                else
                    this.playButton.setDisable(false);
            }
            else
                System.out.println("Select folder first");
        });
    }
}
