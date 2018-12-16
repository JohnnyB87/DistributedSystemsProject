package classes;

import interfaces.Viewer;
import jdk.net.SocketFlow;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.ArrayList;

import static java.nio.file.StandardWatchEventKinds.*;

public class Monitor implements Viewer, Runnable {

    //---------------------------
    //      ATTRIBUTES
    //---------------------------
    private static Monitor instance;
    private static final String FOLDER_PATH = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "Shared Folder";
    private static File folder;
    private static ArrayList<FileInfo> names;
    private static boolean isChanged;

    private FileInfo fileInfo;
    //---------------------------
    //      CONSTRUCTORS
    //---------------------------

    static Monitor getInstance(){
        if(instance == null) {
            instance = new Monitor();
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

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    //---------------------------
    //      IMPLEMENTED METHODS
    //---------------------------
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

    @Override
    public void run() {

        watchDirectory();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //---------------------------
    //      EXTRA FUNCTIONALITY
    //---------------------------

    /**
     * Method used for sending a file from the server to the client
     */
    public void sendFile(FileInfo fileInfo, Socket clientSocket) {
            try {
                System.out.println("Server side sending data");
                //handle file read
                // create a new FILE OBJECT  to be sent over the stream
                String fileName = fileInfo.getName() + "." + fileInfo.getType();
                File myFile = new File(FOLDER_PATH + File.separator + fileName);
                // crate empty byte array the size of the file
                byte[] bytes = new byte[(int) myFile.length()];

                // create a new file input stream using the file
                FileInputStream fis = new FileInputStream(myFile);
                // create a buffered input steam using the file input stream
                BufferedInputStream bis = new BufferedInputStream(fis);
                // create a data input stream to add more functionality
                DataInputStream dis = new DataInputStream(bis);
                // read the file into the byte[] using the data input stream
                dis.readFully(bytes, 0, bytes.length);

                //handle file send over socket
                // get the clients output stream
                OutputStream os = clientSocket.getOutputStream();

                //Sending file name and file size to the server
                // using a data output stream
                DataOutputStream dos = new DataOutputStream(os);
//                dos.writeInt(1);
                // write the UTF to the output stream
                dos.writeUTF(myFile.getName());
                // give the output stream the length of the byte array
                dos.writeLong(bytes.length);
                // write the byte[] to the output stream
                dos.write(bytes, 0, bytes.length);
                // flush output stream and close sockets
                dos.flush();
                System.out.println("Data sent");
            } catch (Exception e) {
                System.err.println("sendFile(): File does not exist!");
                e.printStackTrace();
                System.exit(0);
            }

    }

    /**
     * Method used for receiving a file from the client
     *
     */
    public void receiveFile(Socket clientSocket){
        // check if the file is not null and if the server already has it
        if(fileInfo != null && !this.fileExists(fileInfo)) {
            try {
                int bytes;
                // create a new data input stream using the clients socket
                DataInputStream clientData = new DataInputStream(clientSocket.getInputStream());
                // read the UTF from the clients socket using the input stream
                String fileName = clientData.readUTF();
                // create a new output stream
                OutputStream output = new FileOutputStream(new File(FOLDER_PATH + File.separator + fileName));

                long size = clientData.readLong();
                byte[] buffer = new byte[1024];
                while (size > 0 && (bytes = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    output.write(buffer, 0, bytes);
                    size -= bytes;
                }

                output.close();

                FileInfo fi = FileInfo.createFileInfo(FOLDER_PATH, fileInfo.getName()+"."+fileInfo.getType());
                this.addFile(fi);
                //                serverSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

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

    private void addFile(FileInfo file){
        names.add(file);
    }

    public boolean fileExists(FileInfo file){
        for(FileInfo f : names){
            if(file.compareTo(f) == 0)
                return true;
        }
        return false;
    }

    private void watchDirectory(){
        Path path = Paths.get(FOLDER_PATH);
        FileSystem fs = FileSystems.getDefault();
        try {
            WatchService service = fs.newWatchService();
            path.register(service, ENTRY_CREATE, ENTRY_DELETE);
            WatchKey key;
            do {
                key = service.take();
//                System.out.println(key.pollEvents());
                for (WatchEvent event : key.pollEvents()) {
                    WatchEvent.Kind kind = event.kind();
                    String fileName = event.context().toString();

                    fileInfo = FileInfo.createFileInfo(FOLDER_PATH, fileName);

                    if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
                        isChanged = true;
                        System.out.println("File Created:" + fileName);
//                        this.addFile(fileInfo);
                    }
//                    else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
//                        isChanged = true;
//                        System.out.println("File deleted: " + fileName);
//                    }
                    System.out.println(isChanged);
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
