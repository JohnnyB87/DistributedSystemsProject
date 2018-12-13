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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

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
    private Thread sharedThread;
    private Thread localThread;
    private static final Pattern PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");


    private ServerSocket serverSocket;
    private Socket socket;
    private static final int SOCKET_PORT_NO = 1234;
    private OutputStream out;
    private InputStream in;
    private String filePath;
    private String ipAddress;


    @FXML
    private void initialize(){

        this.localFolder = new MyMediaPlayer();
        this.sharedFolder = this.localFolder.getMonitor();
//        serverTable.getItems().addAll(this.sharedFolder.getNames());

        clientTableSelected();
        serverTableSelected();

        sharedThread = new Thread(this.sharedFolder);
        sharedThread.start();
    }

    public void connectButtonPressed() {
        System.out.println("Connecting to server");
        String ipAddress = this.serverIpTxtBox.getText();
        boolean isValidIpAddress = validateIpAddress(ipAddress.trim());
        if(isValidIpAddress){
            if(localFolder.connectToServer(ipAddress)) {
                this.ipAddress = ipAddress;
                serverTable.getItems().addAll(this.sharedFolder.getNames());
            }
            else
                new Alert(Alert.AlertType.ERROR, "Connection Failed").show();
        }else{
            new Alert(Alert.AlertType.ERROR, "Invalid Ip Address Entered").show();
        }
    }

    private void startServer(){
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(SOCKET_PORT_NO);
                socket = serverSocket.accept();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void quitButtonPressed() {
        stopThreads();
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
            localThread = new Thread(this.localFolder);
            localThread.start();
        }catch(Exception e){
            e.printStackTrace();
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
        System.out.println(this.localFolder);
        if(this.localFolder.getLocalFolder() != null){
            int selected = this.clientTable.getSelectionModel().getSelectedIndex();
            System.out.println("Index: " + selected);
            if (selected > -1) {
                System.out.println("Index: " + this.clientTable.getSelectionModel().getFocusedIndex());
                FileInfo file = this.clientTable.getSelectionModel().getSelectedItems().get(0);
                this.localFolder.uploadFile(file);
                this.sharedFolder.receiveFile(file);
                if(this.sharedFolder.checkForChange())
                    serverTable.getItems().add(file);
            } else {
                System.out.println("Select item to upload");
                new Alert(Alert.AlertType.ERROR, "No file selected").show();
            }
        }else {
            System.out.println("Select folder first");
            new Alert(Alert.AlertType.ERROR, "No folder selected").show();
        }
    }

    public void downloadButtonPressed() {
        if(this.localFolder.getLocalFolder() != null){
            int selected = this.serverTable.getSelectionModel().getSelectedIndex();
            System.out.println("Index: " + selected);
            if (selected > -1) {
                FileInfo file = this.serverTable.getSelectionModel().getSelectedItems().get(0);
//                if(this.localFolder.getConnectToServer().isClosed())
//                    this.localFolder.connectToServer(this.ipAddress);
                this.sharedFolder.sendFile(file);
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
                if (newSelection != null && this.localFolder.fileExists(newSelection)) {
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
        stage.setOnCloseRequest(event -> {
            myController.quitButtonPressed();
        });
        stage.showAndWait();
    }

    private void stopThreads(){
        if(this.localThread != null)
            localThread.interrupt();
        sharedThread.interrupt();
    }

    private boolean validateIpAddress(String ipAddress){
        return PATTERN.matcher(ipAddress).matches();
    }
}
