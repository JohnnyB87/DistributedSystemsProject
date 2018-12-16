package classes;

import java.io.File;

public class FileInfo implements Comparable<FileInfo>{

    private String absolutePath;
    private String location;
    private String name;
    private String type;
    private int size;

    //---------------------------
    //      CONSTRUCTORS
    //---------------------------

    private FileInfo(){}

    FileInfo(String location, String name, String type, int size){
        this.location = location;
        this.name = name;
        this.type = type;
        this.size = size;
        setAbsolutePath();
    }

    //---------------------------
    //      GETTERS
    //---------------------------

    public String getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    String getAbsolutePath() {
        return absolutePath;
    }

    //---------------------------
    //      SETTERS
    //---------------------------

    private void setLocation(String location) {
        this.location = location;
        setAbsolutePath();
    }

    private void setName(String name) {
        this.name = name;
    }

    private void setType(String type) {
        this.type = type;
    }

    private void setSize(int size) {
        this.size = size;
    }

    private void setAbsolutePath(){
        this.absolutePath = this.location + File.separator + this.name + "." + this.type;
    }

    //---------------------------
    //      EXTRA FUNCTIONALITY
    //---------------------------

    @Override
    public int compareTo(FileInfo file) {
        return this.name.equalsIgnoreCase(file.name) && this.type.equalsIgnoreCase(file.getType())? 0 :
                this.name.compareToIgnoreCase(file.name) == 1 && this.type.compareToIgnoreCase(file.getType()) == 1 ? 1 : -1;
    }

    public String toString(){
        return String.format("%s.%s", this.name, this.type);
    }

    static FileInfo createFileInfo(String path, String fileName){
        File f = new File(path + File.separator + fileName);
        FileInfo file = new FileInfo();
        file.setName(fileName.substring(0, fileName.lastIndexOf(".")));
        file.setType(fileName.substring(fileName.lastIndexOf(".") + 1));
        file.setLocation(path);
        file.setAbsolutePath();
        file.setSize((int)f.length());

        return file;
    }
}
