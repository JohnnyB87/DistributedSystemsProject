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

    private static Monitor instance;
    private static final String FOLDER_PATH = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "Shared Folder";
    private static File folder;
    private static ArrayList<FileInfo> names;
    private static boolean isChanged;


    private Socket clientSocket;
    public static int SOCKET_PORT_NO = 1234;
    private static ServerSocket serverSocket;
    private OutputStream out;
    private BufferedReader in;
    private String filePath;
    private FileInfo fileInfo;
    //---------------------------
    //      CONSTRUCTORS
    //---------------------------

    public static Monitor getInstance(){
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
    public boolean openFile(String name){
        try {
            if (names != null) {
                for (FileInfo fileName : names)
                    if (name.equalsIgnoreCase(fileName.getName())) {
                        filePath = fileName.getAbsolutePath();
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
    public byte getByte() {
        return 0;
    }

    @Override
    public boolean closeFile(String name) {
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
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    //---------------------------
    //      EXTRA FUNCTIONALITY
    //---------------------------
    public void sendFile(FileInfo file) {
        try {
            if(serverSocket.isClosed() || serverSocket == null) {
                serverSocket = new ServerSocket(SOCKET_PORT_NO);
            }
            System.out.println("new server socket: " + serverSocket.toString());
            clientSocket = serverSocket.accept();
            System.out.println("Sending to client...");
            //handle file read
            System.out.println("SendFile(): "+file.getAbsolutePath());
            File myFile = new File(file.getAbsolutePath());
//            byte[] bytes;
            if(myFile.length() > Integer.MAX_VALUE){
                System.out.println("File too large");
                System.exit(0);
            }
            byte[] bytes = new byte[(int) myFile.length()];

            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            //bis.read(bytes, 0, bytes.length);

            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(bytes, 0, bytes.length);

            //handle file send over socket
            OutputStream os = clientSocket.getOutputStream();

            //Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(myFile.getName());
            dos.writeLong(bytes.length);
            dos.write(bytes, 0, bytes.length);
            dos.flush();
            clientSocket.close();
            serverSocket.close();
            System.out.println("File "+file.getName()+" sent to client.");
        } catch (Exception e) {
            System.err.println("sendFile(): File does not exist!");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void receiveFile(FileInfo fileInfo){
        if(fileInfo != null && !this.fileExists(fileInfo)) {
            new Thread(() -> {try {
                if(serverSocket == null || serverSocket.isClosed()) {
                    serverSocket = new ServerSocket(SOCKET_PORT_NO);
                }
                if(clientSocket == null || clientSocket.isClosed())
                    clientSocket = serverSocket.accept();
                System.out.println("Server Receiving...");
                int bytes;
                DataInputStream clientData = new DataInputStream(clientSocket.getInputStream());

                String fileName = clientData.readUTF();
                System.out.println("File Path: " + fileName);
                OutputStream output = new FileOutputStream(FOLDER_PATH + File.separator + fileName);
                long size = clientData.readLong();
                byte[] buffer = new byte[1024];
                while (size > 0 && (bytes = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    System.out.println("INSIDE while Loop");
                    output.write(buffer, 0, bytes);
                    size -= bytes;
                }
                System.out.println("EXIT while Loop");
                output.close();
                clientData.close();

                System.out.println("File " + fileInfo.getAbsolutePath() + " received from Client.");
                System.out.println("Receiving Finished");
                clientSocket.close();
                Thread.sleep(2000);
//                serverSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (InterruptedException e) {
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
                        this.addFile(fileInfo);
                    } else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
                        isChanged = true;
                        System.out.println("File deleted: " + fileName);
                    }
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
