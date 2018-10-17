package classes;

import interfaces.Viewer;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

import static java.nio.file.StandardWatchEventKinds.*;

public class Monitor implements Viewer, Runnable {

    private final String FOLDERPATH = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "Shared Folder";
    private File folder;
    private ArrayList<FileInfo> names;
    private boolean isChanged;

    //---------------------------
    //      CONSTRUCTORS
    //---------------------------
    public Monitor() {
        this.folder = new File(this.FOLDERPATH);
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
    public String getFolderPath() {
        return FOLDERPATH;
    }

    public File getFolder() {
        return folder;
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
        if(isChanged) {
            isChanged = false;
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        watchDirectory();
    }

    //---------------------------
    //      EXTRA FUNCTIONALITY
    //---------------------------
    public void populateArray() {
        String[] array = this.folder.list();
        this.names.clear();

        for(String s : array){
            File file = new File(this.FOLDERPATH + File.separator + s);

            String fileName = s.substring(0, s.lastIndexOf("."));
            String fileType = s.substring(s.lastIndexOf(".") + 1);

            FileInfo fileInfo = new FileInfo(this.FOLDERPATH, fileName, fileType, file.length()/1024.0);
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

    public void watchDirectory(){
        Path path = Paths.get(this.FOLDERPATH);
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

//                    System.out.printf("%nkind=%s, count=%d, context=%s Context type=%s%n%n ",
//                            kind,
//                            event.count(), event.context(),
//                            ((Path) event.context()).getClass());

                    FileInfo file = new FileInfo();
                    file.setName(fileName.substring(0, fileName.lastIndexOf(".")));
                    file.setType(fileName.substring(fileName.lastIndexOf(".") + 1));
                    file.setLocation(this.FOLDERPATH);

                    if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
                        isChanged = true;
                        System.out.println("File Created:" + fileName);
                        this.addFile(file);
//                    } else if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind)) {
//                        isChanged = true;
//                        System.out.println("New path modified: " + fileName);
                    }else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
                        isChanged = true;
                        System.out.println("Path deleted: " + fileName);
                    }

                    System.out.println(isChanged);
                }

            } while (key.reset());

        } catch (IOException | InterruptedException ioe) {
            ioe.printStackTrace();
        }
    }

}
