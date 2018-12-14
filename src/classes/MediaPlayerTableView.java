package classes;
/**
 * Custom class that creates a 3 columned table with specific column names
 * Table is used to display file information
 */

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class MediaPlayerTableView<S> extends TableView<S> {

    //-----------------------------
    //      ATTRIBUTES
    //-----------------------------
    private TableColumn name;
    private TableColumn type;
    private TableColumn size;

    //-----------------------------
    //      CONSTRUCTORS
    //-----------------------------
    public MediaPlayerTableView() {
        super();

        double width = 225;
        double height = 200;

        this.setPrefWidth(width);
        this.setPrefHeight(height);

        this.name = new TableColumn("Name");
        this.name.setPrefWidth(width/3);

        this.type = new TableColumn("Type");
        this.type.setPrefWidth(width/3);

        this.size = new TableColumn("Size(KB)");
        this.size.setPrefWidth(width/3);

        this.name.setCellValueFactory(new PropertyValueFactory<>("Name"));
        this.type.setCellValueFactory(new PropertyValueFactory<>("Type"));
        this.size.setCellValueFactory(new PropertyValueFactory<>("Size"));

        this.getColumns().addAll(name, type, size);
    }

    //-----------------------------
    //      GETTERS
    //-----------------------------
    public TableColumn getName() {
        return name;
    }

    public TableColumn getType() {
        return type;
    }

    public TableColumn getSize() {
        return size;
    }

    //-----------------------------
    //      SETTERS
    //-----------------------------
    public void setType(TableColumn type) {
        this.type = type;
    }

    public void setName(TableColumn name) {
        this.name = name;
    }

    public void setSize(TableColumn size) {
        this.size = size;
    }

}
