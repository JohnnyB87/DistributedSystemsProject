package classes;

import java.io.File;
import java.io.IOException;
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

                FileInfo fileInfo = new FileInfo(path, fileName, fileType, file.length() / 1024.0);
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
            String source = file.getLocation();
            String destination = this.MONITOR.getFolderPath();
            this.copyFile(file, source, destination);
        }
        else
            System.out.println("File already exists in this Folder.");
    }

    public void downLoadFile(FileInfo file){
        if(file != null && !this.fileExists(file)) {
            String source = this.MONITOR.getFolderPath();
            String destination = this.folderPath;
            this.copyFile(file, source, destination);
            this.addFile(file);
            this.isChanged = true;
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
}
