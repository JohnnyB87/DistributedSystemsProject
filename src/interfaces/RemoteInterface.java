package interfaces;

import classes.FileInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    FileInfo downloadFile(FileInfo fileInfo) throws RemoteException;
    void uploadFile(FileInfo fileInfo) throws RemoteException;
}
