package classes;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class MonitorDirectory {

    public static void main(String[] args) throws IOException,
            InterruptedException {

        String FOLDERPATH = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "Shared Folder";
        Path path = Paths.get(FOLDERPATH);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

        boolean test = false;
        boolean valid = true;
        do {
            WatchKey watchKey = watchService.take();


            System.out.println(watchKey.pollEvents());
            for (WatchEvent event : watchKey.pollEvents()) {
                WatchEvent.Kind kind = event.kind();
                String fileName = event.context().toString();
                if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
                    test = true;
                    System.out.println("File Created: " + fileName);

                }
                else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
//                    String fileName = event.context().toString();
                    System.out.println("File Deleted:" + fileName);

                }
                System.out.println(test);
            }
            System.out.println(false);
            valid = watchKey.reset();

        } while (valid);

    }
}