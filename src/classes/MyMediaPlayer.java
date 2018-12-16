package classes;

import interfaces.RemoteInterface;
import interfaces.Viewer;
import javafx.scene.control.Alert;

import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.nio.file.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

public class MyMediaPlayer implements Runnable, Viewer {

    //--------------------------------
    //      ATTRIBUTES
    //--------------------------------
    private final Monitor MONITOR;
    private File localFolder;
    private ArrayList<FileInfo> names;
    private boolean isChanged;
    private String folderPath;
    private static int SOCKET_PORT_NO = 1234;
    private RemoteInterface remoteInterface;

    //--------------------------------
    //      CONSTRUCTORS
    //--------------------------------
    public MyMediaPlayer() {
        this.MONITOR = Monitor.getInstance();
    }

    //--------------------------------
    //      GETTERS
    //--------------------------------
    public File getLocalFolder() {
        return this.localFolder;
    }

    public Monitor getMonitor() {
        return MONITOR;
    }

    //--------------------------------
    //      SETTERS
    //--------------------------------
    public void setLocalFolder(File localFolder) {
        this.folderPath = localFolder.getAbsolutePath();
        this.localFolder = localFolder;
    }

    public void setFileInfo(ArrayList<FileInfo> names) {
        this.names = names;
    }

    //--------------------------------
    //      IMPLEMENTED METHODS
    //--------------------------------

    /**
     * Method implemented from the Runnable Interface
     * Thread that monitors the associated directory for changes
     */
    @Override
    public void run() {
        watchDirectory();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method implemented from the Viewer Interface
     * @return an ArrayList of FileInfo objects
     */
    @Override
    public ArrayList<FileInfo> getNames() {
        return names;
    }

    /**
     * Method implemented from the Viewer Interface
     * @return a boolean stating if the associated folder has been changed
     */
    @Override
    public boolean checkForChange() {
        // if it has been changed
        if(isChanged) {
            // reset the global boolean variable to false and return true
            isChanged = false;
            return true;
        }
        return false;
    }

    //--------------------------------
    //      EXTRA FUNCTIONALITY
    //--------------------------------

    /**
     * Method that reads the folder contents
     * and converts them to FileInfo objects
     * and appends them to an ArrayList
     */
    public void folderItemsToArrayList() {
        // convert folder contents into an array
        String[] array = this.localFolder.list();
        names = new ArrayList<>();

        // check if the array is not null
        if (array != null) {
            // loop through the array
            for (String s : array) {
                // create a new file from the string
                File file = new File(this.folderPath + File.separator + s);

                // get file name and type from string
                String fileName = s.substring(0, s.lastIndexOf("."));
                String fileType = s.substring(s.lastIndexOf(".") + 1);

                // create a new FileInfo object
                FileInfo fileInfo = new FileInfo(this.folderPath, fileName, fileType, (int)file.length());
                // add FileInfo object to the ArrayList
                this.names.add(fileInfo);
            }
        }
    }

    /**
     * Method used to connect the client to the server using RMI
     * creates the needed interface using the lookup method
     *
     * @return true if it was able to connect successfully
     */
    public boolean connectToServer(){
        try {
            remoteInterface = (RemoteInterface) Naming.lookup("rmi://localhost:1234/johnsRMI");
            return true;
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            new Alert(Alert.AlertType.ERROR, "Connection Failed").show();
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Method that allows the client to upload a given FileInfo object to the server
     * Converts the file into a byte[] and sends it to the server via sockets
     *
     * @param file FileInfo object to be uploaded
     */
    public void uploadFile(FileInfo file){
        if(file != null && !MONITOR.fileExists(file)) {
            try {
                System.out.println("Server side sending data");
                // set the file infos bytes
                file.setBytes(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                // upload to server calling the servers interface
                remoteInterface.uploadFile(file);
                System.out.println("Data sent");
            } catch (Exception e) {
                System.err.println("uploadFile(): File does not exist!");
                e.printStackTrace();
                System.exit(0);
            }
        }
        else
            System.out.println("File already exists in this Folder.");
    }

    /**
     * Method that allows the client to download a file from the server via sockets
     *
     * @param file FileInfo of file to be downloaded
     */
    public void downLoadFile(FileInfo file){
        System.out.println("Inside downloadFile()");
        if(file != null && !this.fileExists(file)) {
            try{
                FileInfo fileInfo = remoteInterface.downloadFile(file);
                String fileName = fileInfo.getName() + "." + fileInfo.getType();
                Path path = Paths.get(folderPath + File.separator + fileName);
                Files.write(path, fileInfo.getBytes());
                fileInfo.setLocation(folderPath);
                this.addFile(fileInfo);
                this.isChanged = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Download Complete");
        }
        else
            System.out.println("File already exists in this Folder.");
    }

    /**
     * method that allows you to add a new file to the names array list
     *
     * @param file FileInfo object that gets added to the ArrayList
     */
    private void addFile(FileInfo file) {
        this.names.add(file);
    }

    /**
     * Method to check if a file already exist in the folder
     * loops through the names ArrayList comparing the FileInfo object
     * with each item in the ArrayList until it finds a match
     * or finishes the loop
     *
     * @param file FileInfo object to check
     * @return true if the file already exists
     */
    public boolean fileExists(FileInfo file){
        for(FileInfo f : this.names){
            if(file.compareTo(f) == 0)
                return true;
        }
        return false;
    }

    /**
     * Method used to watch the associated folder
     * uses the WatchService class to monitor the folder for any additions or deletions
     * adds or removes the associated FileInfo object to/from the names ArrayList
     * changes the isChanged boolean value to true if there's a change
     */
    private void watchDirectory(){
        Path path = Paths.get(this.folderPath);
        FileSystem fs = FileSystems.getDefault();
        try {
            WatchService service = fs.newWatchService();
            path.register(service, ENTRY_CREATE, ENTRY_DELETE);
            WatchKey key;
            do {
                key = service.take();
                for (WatchEvent event : key.pollEvents()) {
                    WatchEvent.Kind kind = event.kind();
                    String fileName = event.context().toString();

                    FileInfo file = FileInfo.createFileInfo(this.folderPath, fileName);

                    if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
                        isChanged = true;
                        System.out.println("File Created:" + fileName);
                        this.addFile(file);
                    } else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
                        isChanged = true;
                        this.names.remove(file);
                        System.out.println("File deleted: " + fileName);
                    }
                }

            } while (key.reset());
        }catch(InterruptedException ie){
            Thread.currentThread().interrupt();
            System.out.println("InterruptedException: --> Class: MyMediaPlayer --> watchDirectory()");
        }catch (IOException ioe) {
            System.out.println("IOException: --> Class: MyMediaPlayer --> Method: watchDirectory()");
        }
    }

}
