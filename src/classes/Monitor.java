package classes;

import interfaces.Viewer;

import java.io.File;
import java.util.ArrayList;

public class Monitor implements Viewer {

    private String folderName;
    private String folderPath;
    private File folder;
    private ArrayList<FileInfo> names;

    //---------------------------
    //      CONSTRUCTORS
    //---------------------------
    public Monitor() {
        this.folderName = "Shared Folder";
        String userHome = System.getProperty("user.home") + File.separator + "Desktop";
        this.folderPath = userHome + File.separator + this.folderName;
        this.folder = new File(this.folderPath);
        this.names = new ArrayList<>();
        if (!this.folder.exists()) {
            this.folder.mkdir();
        }
        else{
            System.out.println("Already Exists");
            populateArray();
        }
    }

    //---------------------------
    //      GETTERS
    //---------------------------
    public String getFolderName() {
        return folderName;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public File getFolder() {
        return folder;
    }

    //---------------------------
    //      SETTERS
    //---------------------------
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public void setFolder(File folder) {
        this.folder = folder;
    }

    //---------------------------
    //      IMPLEMENTED METHODS
    //---------------------------
    @Override
    public ArrayList<FileInfo> getNames() {
        return this.names;
    }

    @Override
    public boolean openFile(String name){
        try {
            if (this.names != null) {
                for (FileInfo fileName : this.names)
                    if (name.equalsIgnoreCase(fileName.getName()))
                        return true;
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
        String[] newFiles = this.folder.list();
        return newFiles != null && newFiles.length != this.names.size();
    }

    //---------------------------
    //      EXTRA FUNCTIONALITY
    //---------------------------
    public void populateArray() {
        String[] array = this.folder.list();
        this.names.clear();

        for(String s : array){
            File file = new File(this.folderPath + File.separator + s);

            String fileName = s.substring(0, s.lastIndexOf("."));
            String fileType = s.substring(s.lastIndexOf(".") + 1);

            FileInfo fileInfo = new FileInfo(this.folderPath, fileName, fileType, file.length()/1024.0);
            names.add(fileInfo);
        }
    }

    public void addFile(FileInfo file){
        this.names.add(file);
    }

    public boolean fileExists(FileInfo file){
        for(FileInfo f : this.names){
            if(file.compareTo(f) == 0)
                return true;
        }
        return false;
    }
}
