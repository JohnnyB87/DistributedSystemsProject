package classes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class MyMediaPlayer implements Runnable{

    //--------------------------------
    //      ATTRIBUTES
    //--------------------------------
    private final Monitor MONITOR;
    private File localFolder;
    private ArrayList<FileInfo> fileInfo;
    private boolean changed;

    //--------------------------------
    //      CONSTRUCTORS
    //--------------------------------
    public MyMediaPlayer() {
        this.MONITOR = Main.getMonitor();
    }

    //--------------------------------
    //      GETTERS
    //--------------------------------
    public File getLocalFolder() {
        return localFolder;
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

    }

    //--------------------------------
    //      EXTRA FUNCTIONALITY
    //--------------------------------
    public void folderItemsToArrayList(){
        String path = this.localFolder.getAbsolutePath();
        String[] array = this.localFolder.list();
        fileInfo = new ArrayList<>();

        for(String s : array){
            File file = new File(path + File.separator + s);

            String fileName = s.substring(0, s.lastIndexOf("."));
            String fileType = s.substring(s.lastIndexOf(".") + 1);

            FileInfo fileInfo = new FileInfo(path, fileName, fileType, file.length()/1024.0);
            this.fileInfo.add(fileInfo);

        }
    }

    public void copyFile(FileInfo file, String source, String destination){
        try {
            String fileName = file.getName() + "." + file.getType();
            source = String.format("%s%s", source, File.separator + fileName);
            destination = destination + File.separator + fileName;

            Files.copy(Paths.get(source), Paths.get(destination));

            System.out.println("Successful Copy\nSource: " + source);
            System.out.println("Destination: " + destination);
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
            String destination = file.getLocation();
            this.copyFile(file, source, destination);
            this.addFile(file);
        }
        else
            System.out.println("File already exists in this Folder.");
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



}
