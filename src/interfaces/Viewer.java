package interfaces;

import java.util.ArrayList;

public interface Viewer {

    ArrayList getNames();// returns the names of all the files in folder
    boolean checkForChange();// sets a bool if a file has been added

}
