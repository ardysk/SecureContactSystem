package com.example.client.controller;

import com.example.client.ClientApplication;
import com.example.client.service.FtpService;
import com.example.client.session.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class FtpController {

    @FXML private ListView<String> fileList;
    @FXML private Label statusLabel;

    private final FtpService ftpService = new FtpService();

    @FXML
    public void initialize() {
        refreshList();
    }

    @FXML
    public void refreshList() {
        statusLabel.setText("Odświeżanie listy plików...");

        new Thread(() -> {
            List<String> files = ftpService.listFiles();

            Platform.runLater(() -> {
                fileList.getItems().setAll(files);
                statusLabel.setText("Lista zaktualizowana: " + files.size() + " plików.");
                statusLabel.setStyle("-fx-text-fill: black;");
            });
        }).start();
    }

    @FXML
    public void uploadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wybierz plik do wysłania na serwer");
        File file = fileChooser.showOpenDialog(statusLabel.getScene().getWindow());

        if (file != null) {
            statusLabel.setText("Wysyłanie: " + file.getName() + "...");

            new Thread(() -> {
                String currentUser = UserSession.getInstance().getUsername();
                boolean success = ftpService.uploadFile(file, currentUser);
                Platform.runLater(() -> {
                    if (success) {
                        statusLabel.setText("Sukces! Plik wysłany.");
                        statusLabel.setStyle("-fx-text-fill: green;");
                        refreshList();
                    } else {
                        statusLabel.setText("Błąd wysyłania pliku.");
                        statusLabel.setStyle("-fx-text-fill: red;");
                    }
                });
            }).start();
        }
    }

    @FXML
    public void onShareFile() {
        String selectedFile = fileList.getSelectionModel().getSelectedItem();
        if (selectedFile == null) return;

        String msg = "Cześć,\n\nUdostępniam Ci plik z serwera FTP: " + selectedFile + "\n\nPozdrawiam.";
        ClientApplication.loadMailViewWithRecipient("", msg);
    }

    @FXML
    public void downloadFile() {
        String selectedFile = fileList.getSelectionModel().getSelectedItem();

        if (selectedFile == null) {
            statusLabel.setText("Wybierz najpierw plik z listy!");
            statusLabel.setStyle("-fx-text-fill: orange;");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz plik jako...");
        fileChooser.setInitialFileName(selectedFile);
        File dest = fileChooser.showSaveDialog(statusLabel.getScene().getWindow());

        if (dest != null) {
            statusLabel.setText("Pobieranie...");

            new Thread(() -> {
                boolean success = ftpService.downloadFile(selectedFile, dest);
                Platform.runLater(() -> {
                    if (success) {
                        statusLabel.setText("Plik pobrany do: " + dest.getAbsolutePath());
                        statusLabel.setStyle("-fx-text-fill: green;");
                    } else {
                        statusLabel.setText("Błąd pobierania pliku.");
                        statusLabel.setStyle("-fx-text-fill: red;");
                    }
                });
            }).start();
        }
    }
}