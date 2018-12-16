package classes;

import interfaces.RemoteInterface;
import interfaces.Viewer;

import java.io.*;
import java.net.Socket;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

public class Monitor extends UnicastRemoteObject implements Viewer, Runnable, RemoteInterface {

    //---------------------------
    //      ATTRIBUTES
    //---------------------------
    private static Monitor instance;
    private static final String FOLDER_PATH = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "Shared Folder";
    private static File folder;
    private static ArrayList<FileInfo> names;
    private static boolean isChanged;

    //---------------------------
    //      CONSTRUCTORS
    //---------------------------

    private Monitor() throws RemoteException {
    }

    static Monitor getInstance(){
        if(instance == null) {
            try {
                instance = new Monitor();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            folder = new File(FOLDER_PATH);
            names = new ArrayList<>();
            if (!folder.exists()) {
                folder.mkdir();
            }else{
                populateArray();
            }
        }
        return instance;
    }

    //---------------------------
    //      GETTERS
    //---------------------------

    String getFolderPath() {
        return FOLDER_PATH;
    }

    public File getFolder() {
        return folder;
    }

    //---------------------------
    //      IMPLEMENTED METHODS
    //---------------------------

    @Override
    public void run() {

        watchDirectory();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ArrayList<FileInfo> getNames() {
        return names;
    }

    @Override
    public boolean checkForChange() {
        if(isChanged) {
            isChanged = false;
            return true;
        }
        return false;
    }

    /**
     * Method used for sending a file from the server to the client
     */
    @Override
    public FileInfo downloadFile(FileInfo fileInfo) {
        try {
            System.out.println("Server side sending data");
            fileInfo.setBytes(Files.readAllBytes(Paths.get(fileInfo.getAbsolutePath())));
            System.out.println("Data sent");
        } catch (Exception e) {
            System.err.println("sendFile(): File does not exist!");
            e.printStackTrace();
            System.exit(0);
        }
        return fileInfo;
    }

    /**
     * Method used for receiving a file from the client
     *
     */
    @Override
    public void uploadFile(FileInfo fileInfo) {
        System.out.println("Uploading File");
        String fileName = fileInfo.getName() + "." + fileInfo.getType();
        Path path = Paths.get(FOLDER_PATH + File.separator + fileName);
        try {
            Files.write(path, fileInfo.getBytes());
            fileInfo.setLocation(FOLDER_PATH);
            this.addFile(fileInfo);
            isChanged = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Upload Complete");
    }
    //---------------------------
    //      EXTRA FUNCTIONALITY
    //---------------------------

    /**
     * Method that reads the folder contents
     * and converts them to FileInfo objects
     * and appends them to an ArrayList
     */
    private static void populateArray() {
        String[] array = folder.list();
        names.clear();
        if(array != null) {
            for (String s : array) {
                File file = new File(FOLDER_PATH + File.separator + s);

                String fileName = s.substring(0, s.lastIndexOf("."));
                String fileType = s.substring(s.lastIndexOf(".") + 1);

                FileInfo fileInfo = new FileInfo(FOLDER_PATH, fileName, fileType, (int)file.length());
                names.add(fileInfo);
            }
        }
    }

    /**
     * method that allows you to add a new file to the names array list
     *
     * @param file FileInfo object that gets added to the ArrayList
     */
    private void addFile(FileInfo file){
        names.add(file);
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
        for(FileInfo f : names){
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
        Path path = Paths.get(FOLDER_PATH);
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

//                    FileInfo fileInfo = FileInfo.createFileInfo(FOLDER_PATH, fileName);
                    if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
                        isChanged = true;
                        System.out.println("File Created:" + fileName);
//                        this.addFile(fileInfo);
                    }
//                    else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
//                        isChanged = true;
//                        System.out.println("File deleted: " + fileName);
//                    }
                }
            } while (key.reset());
        }catch(InterruptedException ie){
            Thread.currentThread().interrupt();
            System.out.println("InterruptedException: --> Class: Monitor --> watchDirectory()");
        }catch (IOException ioe) {
            System.out.println("IOException: --> Class: Monitor --> Method: watchDirectory()");
        }
    }

}
