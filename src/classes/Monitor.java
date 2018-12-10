package classes;

import interfaces.Viewer;

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

    private static ServerSocket serverSocket;
    private Socket socket;
    public static int SOCKET_PORT_NO = 1234;
    private OutputStream out;
    private InputStream in;
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
            try {
                serverSocket = new ServerSocket(SOCKET_PORT_NO);
            } catch (IOException e) {
                e.printStackTrace();
            }
//            serverSocket = new ServerSocket(SOCKET_PORT_NO);
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
//        try {
////            serverSocket = new ServerSocket(SOCKET_PORT_NO);
//            receiveFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        System.out.println("RUN(): " + fileInfo==null);
        if(fileInfo != null) {
            try {
                socket = serverSocket.accept();
                System.out.println("receiveFile() running");
                String fileName = fileInfo.getName() + "." + fileInfo.getType();
                in = socket.getInputStream();
                out = new FileOutputStream(FOLDER_PATH + File.separator + fileName);
                byte[] bytes = new byte[8192];

                System.out.println("START WHILE LOOP");
                int count;
                while ((count = in.read(bytes)) > 0) {
                    System.out.println("INSIDE WHILE LOOP");
                    out.write(bytes, 0, count);
                }

                System.out.println("END WHILE LOOP");
                out.close();
                in.close();
                System.out.println("receiveFile() stopped");
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        watchDirectory();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //---------------------------
    //      EXTRA FUNCTIONALITY
    //---------------------------
    public void sendFile() {
        try {
//            serverSocket = new ServerSocket(SOCKET_PORT_NO);
            socket = serverSocket.accept();
            in = socket.getInputStream();
            out = new FileOutputStream(filePath);

            byte[] bytes = new byte[8192];
            int count;

            while ((count = in.read(bytes)) > 0) {
                out.write(bytes, 0, count);
            }

            out.close();
            in.close();
            socket.close();
//            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveFile(FileInfo fileInfo){
//            int current = 0;
//            try {
//                socket = serverSocket.accept();
//                System.out.println("receiveFile() running");
//                String fileName = fileInfo.getName() + "." + fileInfo.getType();
//                in = socket.getInputStream();
//                out = new FileOutputStream(FOLDER_PATH + File.separator + fileName);
//                byte[] bytes = new byte[8192];
//
//                System.out.println("START WHILE LOOP");
//                int count;
//                while ((count = in.read(bytes)) > 0) {
//                    System.out.println("INSIDE WHILE LOOP");
//                    out.write(bytes, 0, count);
//                }
//
//                System.out.println("END WHILE LOOP");
//                out.close();
//                in.close();
//                System.out.println("receiveFile() stopped");
//                socket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

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
