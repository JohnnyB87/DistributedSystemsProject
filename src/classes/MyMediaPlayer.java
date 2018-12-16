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

    public void uploadFile(FileInfo file){

        if(file != null && !MONITOR.fileExists(file)) {
                try{
                    System.out.println("UPLOAD STARTED");
                    System.out.println(file.getAbsolutePath());
                    File myFile = new File(file.getAbsolutePath());
                    MONITOR.setFileInfo(FileInfo.createFileInfo(MONITOR.getFolderPath(), myFile.getName()));
                    byte[] bytes = new byte[(int) myFile.length()];

                    System.out.println("byte[] created");

                    FileInputStream fis = new FileInputStream(myFile);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    //bis.read(bytes, 0, bytes.length);

                    DataInputStream dis = new DataInputStream(bis);
                    dis.readFully(bytes, 0, bytes.length);
                    //handle file send over socket

                    out.writeInt(0);
                    out.writeUTF(myFile.getName());
                    out.writeLong(bytes.length);
                    out.write(bytes, 0, bytes.length);
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

    public void downLoadFile(FileInfo file){
        System.out.println("Inside downloadFile()");
        if(file != null && !this.fileExists(file)) {
            System.out.println("File not null");
                try {
                    System.out.println("Client Downloading...");
                    int bytesRead;
                    out.writeInt(1);
                    String fileName = in.readUTF();
                    System.out.println("File Path: " + fileName);
                    OutputStream output = new FileOutputStream(folderPath + File.separator + fileName);
                    long size = in.readLong();
                    byte[] buffer = new byte[1024];
                    while (size > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
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

}
