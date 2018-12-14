package interfaces;

import java.util.ArrayList;

public interface Viewer {

    ArrayList getNames();// returns the names of all the files in folder
    boolean openFile(String name);// opens a file called name
    boolean checkForChange();// sets a bool if a file has been added

}
