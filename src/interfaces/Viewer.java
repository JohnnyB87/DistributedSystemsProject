package interfaces;

import java.util.ArrayList;

public interface Viewer {

    ArrayList getNames();// returns the names of all the files in folder1
    boolean openFile(String name);// opens a file called name
    byte getByte();//Gets a byte from the currently open file or any mechanism to read file for download
    boolean closeFile(String name);// closes the open file
    boolean checkForChange();// sets a bool if a file has been added


}
