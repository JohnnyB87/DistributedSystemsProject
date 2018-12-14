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

    private Socket clientSocket;
    private static int SOCKET_PORT_NO = 1234;
    private static ServerSocket serverSocket;
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
            try {
                serverSocket = new ServerSocket(SOCKET_PORT_NO);
            } catch (IOException e) {
                e.printStackTrace();
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
    public boolean openFile(String name) {
        try {
            if (names != null) {
                for (FileInfo fileName : names)
                    if (name.equalsIgnoreCase(fileName.getName())) {
                        String filePath = fileName.getAbsolutePath();
                        fileInfo = fileName;
                        return true;
                    }
            }
        }catch (NullPointerException npe) {
            System.out.println("Null Pointer Exception --> Class: Monitor --> Method: openFile(String)");
        }
        return false;
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
     *
     * @param file Takes in a FileInfo parameter of the file to be sent
     */
    public void sendFile(FileInfo file) {
        new Thread(()->{
            try {
                // check if the server socket is exists
                if(serverSocket.isClosed() || serverSocket == null) {
                    // if not create a new server socket
                    serverSocket = new ServerSocket(SOCKET_PORT_NO);
                }
                System.out.println("new server socket: " + serverSocket.toString());
                // get the clients socket
                clientSocket = serverSocket.accept();
                System.out.println("Sending to client...");
                //handle file read
                System.out.println("SendFile(): "+file.getAbsolutePath());
                // create a new FILE OBJECT  to be sent over the stream
                File myFile = new File(file.getAbsolutePath());
//            byte[] bytes;
//                if(myFile.length() > Integer.MAX_VALUE){
//                    System.out.println("File too large");
//                    System.exit(0);
//                }
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
                // write the UTF to the output stream
                dos.writeUTF(myFile.getName());
                // give the output stream the length of the byte array
                dos.writeLong(bytes.length);
                // write the byte[] to the output stream
                dos.write(bytes, 0, bytes.length);
                // flush output stream and close sockets
                dos.flush();
                clientSocket.close();
                serverSocket.close();
                System.out.println("File "+file.getName()+" sent to client.");
                // get thread to sleep for 2 seconds
                Thread.sleep(2000);
            } catch (Exception e) {
                System.err.println("sendFile(): File does not exist!");
                e.printStackTrace();
                System.exit(0);
            }
        }).start();

    }

    /**
     * Method used for receiving a file from the client
     *
     * @param fileInfo Takes in a FileInfo parameter of the file to be sent
     */
    public void receiveFile(FileInfo fileInfo){
        // check if the file is not null and if the server already has it
        if(fileInfo != null && !this.fileExists(fileInfo)) {
            new Thread(() -> {
            try {
                // check if sockets exist or are open
                if(serverSocket == null || serverSocket.isClosed()) {
                    serverSocket = new ServerSocket(SOCKET_PORT_NO);
                }
                if(clientSocket == null || clientSocket.isClosed())
                    clientSocket = serverSocket.accept();
                System.out.println("Server Receiving...");
                int bytes;
                // create a new data input stream using the clients socket
                DataInputStream clientData = new DataInputStream(clientSocket.getInputStream());

                // read the UTF from the clients socket using the input stream
                String fileName = clientData.readUTF();
                System.out.println("File Path: " + fileName);
                // create a new output stream
                OutputStream output = new FileOutputStream(new File(FOLDER_PATH + File.separator + fileName));
                long size = clientData.readLong();
                byte[] buffer = new byte[1024];
                while (size > 0 && (bytes = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    output.write(buffer, 0, bytes);
                    size -= bytes;
                }

                output.close();
                clientData.close();

                FileInfo fi = FileInfo.createFileInfo(FOLDER_PATH, fileInfo.getName()+"."+fileInfo.getType());
                this.addFile(fi);
                System.out.println("File " + fileInfo.getAbsolutePath() + " received from Client.");
                System.out.println("File " + fi.getAbsolutePath() + " saved to server.");
                System.out.println("Receiving Finished");
                clientSocket.close();
                //                serverSocket.close();
                Thread.sleep(2000);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            catch (InterruptedException e) {
                System.out.println("Receive file: Interrupted Threads");
                e.printStackTrace();
            }
            }).start();

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
