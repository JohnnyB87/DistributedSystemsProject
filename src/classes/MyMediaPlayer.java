package classes;

import interfaces.Viewer;
import javafx.scene.control.Alert;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
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
    private DataOutputStream out;
    private DataInputStream in;

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
     * Method used to connect the client to the server using an ip address
     * creates the needed socket and input/output streams
     *
     * @param ipAddress the ip address of a server
     * @return true if it was able to connect successfully
     */
    public boolean connectToServer(String ipAddress){
        try {
            Socket connectToServerSocket = new Socket(ipAddress, SOCKET_PORT_NO);
            in = new DataInputStream(connectToServerSocket.getInputStream());
            out = new DataOutputStream(connectToServerSocket.getOutputStream());
            System.out.println("Connection Success");
            return true;
        }catch(IOException e){
            new Alert(Alert.AlertType.ERROR, "Connection Failed").show();
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
        // check if the file is not null or that it doesn't already exist on the server
        if(file != null && !MONITOR.fileExists(file)) {
            try{
                System.out.println("UPLOAD STARTED");
                // create a new file object using the FileInfo's absolute path
                File myFile = new File(file.getAbsolutePath());
                // create a new byte array the size of the file to be sent
                byte[] bytes = new byte[(int) myFile.length()];

                // create a new InputStream using the created file
                FileInputStream fis = new FileInputStream(myFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                DataInputStream dis = new DataInputStream(bis);
                // read the input stream into the byte[]
                dis.readFully(bytes, 0, bytes.length);

                //handle file send over socket
                // write an int to the output stream to notify that it's uploading
                out.writeInt(0);
                // pass the files name using the writeUTF method
                out.writeUTF(myFile.getName());
                // pass the files length over sockets
                out.writeLong(bytes.length);
                // write the byte[] to the output stream
                out.write(bytes, 0, bytes.length);
                // flush the output stream
                out.flush();

                System.out.println("File "+file.getName()+" sent to client.");
            } catch (Exception e) {
                System.err.println("File does not exist!");
                e.printStackTrace();
            }

        }
        else
            System.out.println("File already exists in this Folder.");
    }

    /**
     * Method that allows the client to download a file from the server via sockets
     *
     * @param file
     */
    public void downLoadFile(FileInfo file){
        // if statement to check if the file already exists in the folder
        if(file != null && !this.fileExists(file)) {
            try {
                System.out.println("Client Downloading...");
                int bytesRead;
                // write int to output stream to notify server that it needs to send a file
                out.writeInt(1);
                // reads the files name using readUTF method
                String fileName = in.readUTF();
                // create new output stream telling it where to write the data
                OutputStream output = new FileOutputStream(folderPath + File.separator + fileName);
                // read in the files length
                long size = in.readLong();
                byte[] buffer = new byte[1024];
                // loop that writes the the input streams byte[] to the output streams location
                // loops until the whole file has been read
                while (size > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    // write the bytes to the associated location
                    output.write(buffer, 0, bytesRead);
                    size -= bytesRead;
                }

                output.close();
                System.out.println("File "+file.getName()+" received from Server.");
                System.out.println("Downloading Finished");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
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
