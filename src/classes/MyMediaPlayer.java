package classes;

import javafx.scene.control.Alert;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.ArrayList;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

public class MyMediaPlayer implements Runnable{

    //--------------------------------
    //      ATTRIBUTES
    //--------------------------------
    private final Monitor MONITOR;
    private File localFolder;
    private ArrayList<FileInfo> fileInfo;
    private boolean isChanged;
    private String folderPath;
    private Socket connectToServer;
    private static int SOCKET_PORT_NO = 1234;
    private ObjectOutputStream out;
    private BufferedReader in;
    private String ipAddress;

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

    public ArrayList<FileInfo> getFileInfo() {
        return fileInfo;
    }

    //--------------------------------
    //      SETTERS
    //--------------------------------
    public void setLocalFolder(File localFolder) {
        this.folderPath = localFolder.getAbsolutePath();
        this.localFolder = localFolder;
    }

    public void setFileInfo(ArrayList<FileInfo> fileInfo) {
        this.fileInfo = fileInfo;
    }

    //--------------------------------
    //      IMPLEMENTED METHODS
    //--------------------------------
    @Override
    public void run() {

        watchDirectory();
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    //--------------------------------
    //      EXTRA FUNCTIONALITY
    //--------------------------------
    public void folderItemsToArrayList() {
        String path = this.localFolder.getAbsolutePath();
        String[] array = this.localFolder.list();
        fileInfo = new ArrayList<>();

        if (array != null) {
            for (String s : array) {
                File file = new File(path + File.separator + s);

                String fileName = s.substring(0, s.lastIndexOf("."));
                String fileType = s.substring(s.lastIndexOf(".") + 1);

                FileInfo fileInfo = new FileInfo(path, fileName, fileType, (int)file.length() );
                this.fileInfo.add(fileInfo);

            }
        }
    }

    private void copyFile(FileInfo file, String source, String destination){
        try {
            String fileName = file.getName() + "." + file.getType();
            String sourcePath = String.format("%s%s", source, File.separator + fileName);
            String destinationPath = String.format("%s%s",destination, File.separator + fileName);

            Files.copy(Paths.get(sourcePath), Paths.get(destinationPath));

            System.out.println("Successful Copy\nSource: " + sourcePath);
            System.out.println("Destination: " + destinationPath);
        }catch(IOException ioe){
            System.out.println("IOException: Class --> MyMediaPlayer --> copyFile()");
        }
    }

    public void uploadFile(FileInfo file){

        if(file != null && !MONITOR.fileExists(file)) {
            if(connectToServer.isClosed()){
                try {
                    connectToServer = new Socket(this.ipAddress, SOCKET_PORT_NO);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("UPLOAD STARTED");
            File f = new File(file.getAbsolutePath());
            MONITOR.setFileInfo(FileInfo.createFileInfo(f.getPath(), f.getName()));

            new Thread(() -> {
                try{
                    //handle file read
                    System.out.println(file.getAbsolutePath());
                    File myFile = new File(file.getAbsolutePath());
                    byte[] bytes = new byte[(int) myFile.length()];

                    System.out.println("byte[] created");

                    FileInputStream fis = new FileInputStream(myFile);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    //bis.read(bytes, 0, bytes.length);

                    DataInputStream dis = new DataInputStream(bis);
                    dis.readFully(bytes, 0, bytes.length);
                    //handle file send over socket
                    OutputStream os = connectToServer.getOutputStream();

                    //Sending file name and file size to the server
                    DataOutputStream dos = new DataOutputStream(os);
                    dos.writeUTF(myFile.getName());
                    dos.writeLong(bytes.length);
                    dos.write(bytes, 0, bytes.length);
                    dos.flush();
                    connectToServer.close();
                    System.out.println("File "+file.getName()+" sent to client.");
                    Thread.sleep(2000);
                } catch (Exception e) {
                    System.err.println("File does not exist!");
                    e.printStackTrace();
                }
            }).start();

        }
        else
            System.out.println("File already exists in this Folder.");
    }

    public void downLoadFile(FileInfo file){
        if(file != null && !this.fileExists(file)) {
            try {
                System.out.println("Client Downloading...");
                int bytesRead;
                if(connectToServer.isClosed()){
                    connectToServer = new Socket(this.ipAddress, SOCKET_PORT_NO);
                }
                Thread.sleep(3000);
                DataInputStream clientData = new DataInputStream(connectToServer.getInputStream());

                String fileName = clientData.readUTF();
                System.out.println("File Path: " + fileName);
                OutputStream output = new FileOutputStream(folderPath + File.separator + fileName);
                long size = clientData.readLong();
                byte[] buffer = new byte[1024];
                while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    output.write(buffer, 0, bytesRead);
                    size -= bytesRead;
                }

                output.close();
                connectToServer.close();

                System.out.println("File "+file.getName()+" received from Server.");
                System.out.println("Downloading Finished");
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (InterruptedException e) {
                System.out.println("downloadFile(): Thread.sleep()");
                e.printStackTrace();
            }
        }
        else
            System.out.println("File already exists in this Folder.");
    }

    public boolean checkForChange() {
        if(isChanged) {
            isChanged = false;
            return true;
        }
        return false;
    }

    private void addFile(FileInfo file) {
        this.fileInfo.add(file);
    }

    public boolean fileExists(FileInfo file){
        for(FileInfo f : this.fileInfo){
            if(file.compareTo(f) == 0)
                return true;
        }
        return false;
    }

    private void watchDirectory(){
        Path path = Paths.get(this.folderPath);
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

                    FileInfo file = FileInfo.createFileInfo(this.folderPath, fileName);

                    if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
                        isChanged = true;
                        System.out.println("File Created:" + fileName);
                        this.addFile(file);
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

    public boolean connectToServer(String ipAddress){
        try {
            this.ipAddress = ipAddress;
            connectToServer = new Socket(this.ipAddress, SOCKET_PORT_NO);
            System.out.println("Connection Success");
            return true;
        }catch(IOException e){
            new Alert(Alert.AlertType.ERROR, "Connection Failed").show();
        }
        return false;
    }

}
