package controllers;

import classes.FileInfo;
import classes.MediaPlayerTableView;
import classes.Monitor;
import classes.MyMediaPlayer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.regex.Pattern;

public class MyMediaPlayerController {

    //-----------------------------
    //      GUI ATTRIBUTES
    //-----------------------------
    @FXML private MediaPlayerTableView<FileInfo> clientTable;
    @FXML private MediaPlayerTableView<FileInfo> serverTable;
    @FXML private TextField serverIpTxtBox;
    @FXML private Button downloadButton;
    @FXML private Button connectButton;
    @FXML private Button uploadButton;
    @FXML private Button selectButton;
    @FXML private Button playButton;
    private AnchorPane anchorPane;

    //-------------------------------
    //      ATTRIBUTES
    //-------------------------------
    private boolean clientIsSelected = false;
    private boolean serverIsSelected = false;
    private MyMediaPlayer localFolder;
    private Monitor sharedFolder;
    private Thread sharedThread;
    private Thread localThread;
    private static final Pattern PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    //-------------------------------
    //      SOCKET ATTRIBUTES
    //-------------------------------
    private static final int SOCKET_PORT_NO = 1234;

    //-------------------------------
    //      METHODS
    //-------------------------------

    /**
     * Method that is run automatically when the GUI is created
     * Used to initialise some attributes and onclick listeners
     */
    @FXML
    private void initialize(){
        this.localFolder = new MyMediaPlayer();
        this.sharedFolder = this.localFolder.getMonitor();

        clientTableSelected();
        serverTableSelected();
        startServer();

        sharedThread = new Thread(this.sharedFolder);
        sharedThread.start();
    }

    /**
     * Method that starts the server thread,
     * it's called in the initialize method when the GUI is created.
     * Initialises the socket attributes
     * creates a new ServerSocket using the SOCKET_PORT_NO attribute
     * Uses a switch statement to switch between uploading and downloading content from the server
     */
    private void startServer() {
        try {
            LocateRegistry.createRegistry(SOCKET_PORT_NO);
            Naming.rebind("rmi://localhost:" + SOCKET_PORT_NO + "/johnsRMI", this.sharedFolder);
            System.out.println("Server running...");
        } catch (RemoteException | MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Method to stop all threads associated with the program
     */
    private void stopThreads(){
        if(this.localThread != null)
            localThread.interrupt();
        sharedThread.interrupt();
    }

    /**
     * Method used to validate an IP address
     * uses a regex pattern to compare the IP address
     * @param ipAddress String value of IP address
     * @return boolean indicated if the input string is a valid IP address
     */
    private boolean validateIpAddress(String ipAddress){
        return PATTERN.matcher(ipAddress).matches();
    }

    /**
     * Method used to create a new child stage that plays the folders content
     * @param myController a controller for the the window
     */
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

    //-----------------------------
    //      ONCLICK METHODS
    //-----------------------------

    /**
     * ocClickListener for the connect button
     * Method user to connect the client to the server
     * Gets the input from the serverIpTxtBox and checks if it's a valid IP address
     * confirms if the client successfully connected to the server
     */
    public void connectButtonPressed() {
        System.out.println("Connecting to server");
        this.localFolder.connectToServer();
        serverTable.getItems().clear();
        serverTable.getItems().addAll(this.sharedFolder.getNames());
    }

    /**
     * onClick listener for the quit button
     * Stops the threads that have been started
     * closes GUI
     */
    public void quitButtonPressed() {
        stopThreads();
        System.exit(0);
        Platform.exit();
    }

    /**
     * onClick listener for the select button
     * Opens a Directory Chooser window to allow the user to choose a folder
     * once a folder is selected, converts the contents into FileInfo objects
     * and adds them to a list in the client
     * then populates the clients table using the list created
     * starts the client thread
     */
    public void selectButtonPressed() {
        try {
            // clear the contents of the client table
            this.clientTable.getItems().clear();
            DirectoryChooser directoryChooser = new DirectoryChooser();
            // open the GUI for the directory chooser
            File selectedFolder = directoryChooser.showDialog(this.selectButton.getScene().getWindow());

            // set the clients local folder to the selected item
            this.localFolder.setLocalFolder(selectedFolder);
            // convert folder contents into an ArrayList
            this.localFolder.folderItemsToArrayList();
            // append items to table
            this.clientTable.getItems().addAll(this.localFolder.getNames());
            // start thread
            localThread = new Thread(this.localFolder);
            localThread.start();
        }catch(Exception e){
            e.printStackTrace();
            // create alert window if a folder is selected
            new Alert(Alert.AlertType.ERROR, "No folder selected").show();
        }

    }

    /**
     * onClick listener for the play button
     * checks if one of the tables have been selected
     * checks if the file is playable
     * creates a new window and plays the selected file
     */
    public void playButtonPressed() {
        if(serverIsSelected || clientIsSelected) {
            try {
                // Gets the selected item and stores it as a FileInfo Object
                FileInfo fileInfo = serverIsSelected ? serverTable.getSelectionModel().getSelectedItem()
                        : clientTable.getSelectionModel().getSelectedItem();
                // checks if the file isn't null and that it's either an mp3 or mp4 file
                if(fileInfo != null && (fileInfo.getType().equalsIgnoreCase("mp3")
                        || fileInfo.getType().equalsIgnoreCase("mp4"))) {
                    // variable used to set the new windows label
                    String name = fileInfo.getName() != null ? fileInfo.getName() : "NULL";

                    // create the GUI from an FXML file
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("../resources/MyMediaPlayerPopupWindow.fxml"));
                    // create a controller for the GUI
                    MyMediaPlayerPopupController myController = new MyMediaPlayerPopupController(fileInfo);
                    // set the GUI's controller
                    loader.setController(myController);
                    this.anchorPane = loader.load();
                    myController.setLabelPopupText(name);

                    // create the new window
                    createNewStage(myController);
                }
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * onClick listener for the upload button
     * checks if a folder has been selected by the user
     * checks if the user has selected an item on the table
     * gets the item selected from the client table
     * uploads the content fro the client
     * checks if the server folder has been modified
     * updates the server table to reflect the change
     * creates alert windows if there are any errors
     */
    public void uploadButtonPressed() {
        System.out.println(this.localFolder);
        if(this.localFolder.getLocalFolder() != null){
            int selected = this.clientTable.getSelectionModel().getSelectedIndex();
            System.out.println("Index: " + selected);
            if (selected > -1) {
                System.out.println("Index: " + this.clientTable.getSelectionModel().getFocusedIndex());
                FileInfo fileInfo = this.clientTable.getSelectionModel().getSelectedItems().get(0);
                this.localFolder.uploadFile(fileInfo);
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

    /**
     * onClick listener for the download button
     * checks if a folder has been selected by the user
     * checks if the user has selected an item on the table
     * gets the item selected from the client table
     * download the content to the client
     * checks if the client folder has been modified
     * updates the client table to reflect the change
     * creates alert windows if there are any errors
     */
    public void downloadButtonPressed() {
        if(this.localFolder.getLocalFolder() != null){
            int selected = this.serverTable.getSelectionModel().getSelectedIndex();
            System.out.println("Index: " + selected);
            if (selected > -1) {
                FileInfo fileInfo = this.serverTable.getSelectionModel().getSelectedItems().get(0);
                this.localFolder.downLoadFile(fileInfo);
                if (this.localFolder.checkForChange())
                    clientTable.getItems().add(fileInfo);
            } else {
                System.out.println("Select item to download");
                new Alert(Alert.AlertType.ERROR, "No file selected").show();
            }
        }else {
            System.out.println("Select folder first");
            new Alert(Alert.AlertType.ERROR, "No folder selected").show();
        }
    }

    /**
     * onClick listener for the refresh button
     * used to refresh the server table items
     * clears the list and adds all the items from the servers list
     */
    public void refreshButtonPressed() {
        if(this.sharedFolder.checkForChange()) {
            serverTable.getItems().clear();
            serverTable.getItems().addAll(this.sharedFolder.getNames());
        }
    }

    /**
     * onClick listener for the server table
     * checks the items in the clients folder
     * disables/enables the appropriate buttons
     * alerts user if no folder has been selected
     */
    private void serverTableSelected() {
        this.serverTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            this.clientIsSelected = false;
            this.serverIsSelected = true;
            // disable the upload button
            this.uploadButton.setDisable(true);
            // checks if there's a client folder
            if(this.localFolder.getLocalFolder() != null) {
                // check if the selection is not null and if it exists in the client folder
                if (newSelection != null && this.localFolder.fileExists(newSelection)) {
                    //if it does enable the play button and disable the download button
                    this.playButton.setDisable(false);
                    this.downloadButton.setDisable(true);
                }
                else {
                    // else disable the play button and enable the download button
                    this.playButton.setDisable(true);
                    this.downloadButton.setDisable(false);
                }
            }
            else {
                new Alert(Alert.AlertType.ERROR, "No folder selected").show();
            }
        });
    }

    /**
     * onClick listener for the client table
     * checks the items in the server folder
     * disables/enables the appropriate buttons
     */
    private void clientTableSelected() {
        this.clientTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            this.clientIsSelected = true;
            this.serverIsSelected = false;
            this.playButton.setDisable(false);
            this.downloadButton.setDisable(true);
            if(this.sharedFolder.getFolder() != null) {
                // if file exists in the server disable the upload button else enable it
                if (this.sharedFolder.fileExists(newSelection))
                    this.uploadButton.setDisable(true);
                else
                    this.uploadButton.setDisable(false);
            }
        });
    }

}
