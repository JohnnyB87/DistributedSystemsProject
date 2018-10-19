package classes;

public class FileInfo implements Comparable<FileInfo>{

    private String location;
    private String name;
    private String type;
    private double size;

    //---------------------------
    //      CONSTRUCTORS
    //---------------------------
    public FileInfo(){}

    public FileInfo(String location, String name, String type, double size){
        this.location = location;
        this.name = name;
        this.type = type;
        this.size = size;
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

    public double getSize() {
        return size;
    }

    //---------------------------
    //      SETTERS
    //---------------------------

    public void setLocation(String location) {
        this.location = location;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSize(double size) {
        this.size = size;
    }

    @Override
    public int compareTo(FileInfo file) {
        return this.name.equalsIgnoreCase(file.name) && this.type.equalsIgnoreCase(file.getType())? 0 :
                this.name.compareToIgnoreCase(file.name) == 1 && this.type.compareToIgnoreCase(file.getType()) == 1 ? 1 : -1;
    }

    public String toString(){
        return String.format("%s.%s", this.name, this.type);
    }
}
