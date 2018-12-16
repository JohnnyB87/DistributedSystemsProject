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
import java.util.Set;
import java.util.regex.Pattern;

public class MyMediaPlayerController {

    private AnchorPane anchorPane;
    @FXML private Button selectButton;
    @FXML private MediaPlayerTableView<FileInfo> clientTable;
    @FXML private MediaPlayerTableView<FileInfo> serverTable;
    @FXML private Button playButton;
    @FXML private Button downloadButton;
    @FXML private Button uploadButton;
    @FXML private TextField serverIpTxtBox;

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
    private DataOutputStream out;
    private DataInputStream in;
    private String filePath;
    private FileInfo fileInfoGlobal;


    @FXML
    private void initialize(){

        this.localFolder = new MyMediaPlayer();
        this.sharedFolder = this.localFolder.getMonitor();
//        serverTable.getItems().addAll(this.sharedFolder.getNames());

        clientTableSelected();
        serverTableSelected();
        startServer();

        sharedThread = new Thread(this.sharedFolder);
        sharedThread.start();
    }

    private void startServer(){
        new Thread(()->{
            try {
                System.out.println("Server started");
                serverSocket = new ServerSocket(SOCKET_PORT_NO);
                System.out.println("Waiting for connection");
                socket = serverSocket.accept();
                System.out.println("Successful connection");
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                while(true){
                    int actionCode = in.readInt();
                    System.out.println(actionCode);
                    switch (actionCode){
                        case 0:
                            sharedFolder.receiveFile(socket);
                            break;
                        case 1:
                            System.out.println("Downloading data...");
                            sharedFolder.sendFile(fileInfoGlobal, socket);
                            break;
                        default:
                            break;
                    }
                    Thread.sleep(3000);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("Server Thread interrupted.");
                e.printStackTrace();
            }

        }).start();
    }

    public void connectButtonPressed() {
        System.out.println("Connecting to server");
        String ipAddress = this.serverIpTxtBox.getText();
        boolean isValidIpAddress = validateIpAddress(ipAddress.trim());
        if(isValidIpAddress){
            if(localFolder.connectToServer(ipAddress)) {
                serverTable.getItems().addAll(this.sharedFolder.getNames());
            }
            else
                new Alert(Alert.AlertType.ERROR, "Connection Failed").show();
        }else{
            new Alert(Alert.AlertType.ERROR, "Invalid Ip Address Entered").show();
        }
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
                FileInfo fileInfo = this.clientTable.getSelectionModel().getSelectedItems().get(0);
                    this.localFolder.uploadFile(fileInfo);
                    System.out.println("FOLDER CHANGED: " + this.sharedFolder.checkForChange());
                    if(this.sharedFolder.checkForChange())
                        serverTable.getItems().add(fileInfo);
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
                fileInfoGlobal = this.serverTable.getSelectionModel().getSelectedItems().get(0);

                this.localFolder.downLoadFile(fileInfoGlobal);
                if (this.localFolder.checkForChange())
                        clientTable.getItems().add(fileInfoGlobal);
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
