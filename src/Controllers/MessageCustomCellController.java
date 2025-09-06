package Controllers;

import Models.MessageViewModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.*;

public class MessageCustomCellController extends ListCell<MessageViewModel> {

    @FXML private GridPane root;
    @FXML private ImageView imageView;
    @FXML private Label messageLabel;
    @FXML private Label messageTimeLabel;

    // This will be set by the UserChatController
    public Runnable deleteCallback;

    @Override
    protected void updateItem(MessageViewModel item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        try {
            String fxmlPath;
            if (item.isOutgoing) {
                fxmlPath = item.isImage
                        ? "/Views/outgoing_image_custom_cell_view.fxml"
                        : "/Views/outgoing_message_custom_cell_view.fxml";
            } else {
                fxmlPath = item.isImage
                        ? "/Views/incoming_image_custom_cell_view.fxml"
                        : "/Views/incoming_message_custom_cell_view.fxml";
            }

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
            fxmlLoader.setController(this);
            fxmlLoader.load();

            if (messageLabel != null) {
                messageLabel.setText(item.getMessage());
            }

            if (messageTimeLabel != null) {
                messageTimeLabel.setText(item.getTime());
            }

            if (item.isImage && imageView != null) {
                imageView.setImage(item.getImage());
            }


            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("❌ Delete");
            deleteItem.setOnAction(e -> {
                if (deleteCallback != null) {
                    deleteCallback.run();
                }
            });
            contextMenu.getItems().add(deleteItem);


            if (item.isFile) {
                MenuItem download = new MenuItem("📥Download");
                download.setOnAction(e -> saveFile(item.fileName, item.fileData));

                if (item.fileName.toLowerCase().endsWith(".pdf")) {
                    MenuItem open = new MenuItem("👁️Open PDF");
                    open.setOnAction(e -> openPdf(item.fileName, item.fileData));
                    contextMenu.getItems().add(0, open);
                }
                contextMenu.getItems().add(0, download);
            }

            root.setOnContextMenuRequested(e -> contextMenu.show(root, e.getScreenX(), e.getScreenY()));

            setGraphic(root);

        } catch (IOException e) {
            e.printStackTrace();
            setText("Failed to load message bubble.");
        }
    }


    private void saveFile(String fileName, byte[] data) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialFileName(fileName);
            File file = fileChooser.showSaveDialog(new Stage());
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(data);
                    System.out.println("✅ File saved to: " + file.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openPdf(String fileName, byte[] data) {
        try {
            File temp = File.createTempFile("chatfile_", "_" + fileName);
            temp.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(temp)) {
                fos.write(data);
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(temp);
            } else {
                System.out.println("⚠️ Desktop not supported.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
