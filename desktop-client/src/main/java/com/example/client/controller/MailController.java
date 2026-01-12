package com.example.client.controller;

import com.example.client.model.EmailLogDto;
import com.example.client.session.UserSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MailController {

    // --- POLA STATYCZNE (Dla ClientApplication) ---
    public static String preloadRecipient = "";
    public static String preloadBody = "";

    // Pola FXML
    @FXML private TextField toField;
    @FXML private TextField subjectField;
    @FXML private TextArea bodyField;
    @FXML private ComboBox<String> ftpAttachmentCombo;
    @FXML private TextField filterField; // Pole filtrowania

    @FXML private TabPane tabPane;
    @FXML private Tab inboxTab;
    @FXML private Tab adminTab;

    // Tabela Odbiorcza
    @FXML private TableView<EmailLogDto> inboxTable;
    @FXML private TableColumn<EmailLogDto, String> colInboxFrom;
    @FXML private TableColumn<EmailLogDto, String> colInboxSubject;
    @FXML private TableColumn<EmailLogDto, String> colInboxDate;
    @FXML private TableColumn<EmailLogDto, String> colInboxBody;

    // Tabela Wysłane
    @FXML private TableView<EmailLogDto> sentTable;
    @FXML private TableColumn<EmailLogDto, String> colSentTo;
    @FXML private TableColumn<EmailLogDto, String> colSentSubject;
    @FXML private TableColumn<EmailLogDto, String> colSentDate;

    // Tabela Admina
    @FXML private TableView<EmailLogDto> adminTable;
    @FXML private TableColumn<EmailLogDto, String> colAdminSender;
    @FXML private TableColumn<EmailLogDto, String> colAdminRecipient;
    @FXML private TableColumn<EmailLogDto, String> colAdminSubject;
    @FXML private TableColumn<EmailLogDto, String> colAdminDate;

    @FXML private Button deleteBtn;

    // Listy danych (Master Data) do filtrowania
    private ObservableList<EmailLogDto> masterInboxData = FXCollections.observableArrayList();
    private ObservableList<EmailLogDto> masterSentData = FXCollections.observableArrayList();
    private ObservableList<EmailLogDto> masterAdminData = FXCollections.observableArrayList();

    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_URL = "http://localhost:8000/api/email";

    @FXML
    public void initialize() {
        // Preload danych
        if (!preloadRecipient.isEmpty()) {
            toField.setText(preloadRecipient);
            preloadRecipient = "";
        }
        if (!preloadBody.isEmpty()) {
            bodyField.setText(preloadBody);
            preloadBody = "";
        }

        setupColumns();
        setupRowFactories();
        setupClickListeners();
        setupFiltering();

        // Ukryj zakładkę Admina jeśli user nie jest ADMIN
        if (!"ADMIN".equals(UserSession.getInstance().getRole())) {
            if (adminTab != null) {
                tabPane.getTabs().remove(adminTab);
            }
        }

        // Pokaż deleteBtn tylko dla admina w jego inboxie
        if (deleteBtn != null && "ADMIN".equals(UserSession.getInstance().getRole())) {
            deleteBtn.setVisible(true);
        }

        updateUnreadCount();
        loadFtpFiles();
        loadInbox(); // Załaduj na start
    }

    private void setupColumns() {
        // Inbox
        colInboxFrom.setCellValueFactory(new PropertyValueFactory<>("sender"));
        colInboxSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colInboxDate.setCellValueFactory(new PropertyValueFactory<>("sentAt"));
        colInboxBody.setCellValueFactory(new PropertyValueFactory<>("body"));

        // Sent
        colSentTo.setCellValueFactory(new PropertyValueFactory<>("recipient"));
        colSentSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colSentDate.setCellValueFactory(new PropertyValueFactory<>("sentAt"));

        // Admin
        if (colAdminSender != null) {
            colAdminSender.setCellValueFactory(new PropertyValueFactory<>("sender"));
            colAdminRecipient.setCellValueFactory(new PropertyValueFactory<>("recipient"));
            colAdminSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
            colAdminDate.setCellValueFactory(new PropertyValueFactory<>("sentAt"));
        }
    }

    private void setupRowFactories() {
        // Pogrubienie nieprzeczytanych w Inbox
        inboxTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(EmailLogDto item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (!item.isRead()) {
                    setStyle("-fx-font-weight: bold; -fx-background-color: #e6f3ff;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void setupClickListeners() {
        // Double Click -> Szczegóły (Inbox)
        inboxTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && inboxTable.getSelectionModel().getSelectedItem() != null) {
                showEmailDetails();
            }
        });

        // Double Click -> Szczegóły (Sent)
        sentTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && sentTable.getSelectionModel().getSelectedItem() != null) {
                showDetailsGeneric(sentTable.getSelectionModel().getSelectedItem());
            }
        });

        // Double Click -> Szczegóły (Admin)
        if (adminTable != null) {
            adminTable.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && adminTable.getSelectionModel().getSelectedItem() != null) {
                    showDetailsGeneric(adminTable.getSelectionModel().getSelectedItem());
                }
            });
        }
    }

    private void setupFiltering() {
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilter(newValue);
        });
    }

    // --- LOGIKA FILTROWANIA ---
    private void applyFilter(String filter) {
        String lowerCaseFilter = (filter == null) ? "" : filter.toLowerCase();

        java.util.function.Predicate<EmailLogDto> predicate = email -> {
            if (lowerCaseFilter.isEmpty()) return true;

            boolean matchSubject = email.getSubject() != null && email.getSubject().toLowerCase().contains(lowerCaseFilter);
            boolean matchSender = email.getSender() != null && email.getSender().toLowerCase().contains(lowerCaseFilter);
            boolean matchRecipient = email.getRecipient() != null && email.getRecipient().toLowerCase().contains(lowerCaseFilter);
            boolean matchDate = email.getSentAt() != null && email.getSentAt().toLowerCase().contains(lowerCaseFilter);

            return matchSubject || matchSender || matchRecipient || matchDate;
        };

        inboxTable.setItems(new FilteredList<>(masterInboxData, predicate));
        sentTable.setItems(new FilteredList<>(masterSentData, predicate));
        if (adminTable != null) {
            adminTable.setItems(new FilteredList<>(masterAdminData, predicate));
        }
    }

    // --- POBIERANIE DANYCH ---

    @FXML
    public void loadInbox() {
        try {
            String myEmail = UserSession.getInstance().getEmail();
            if (myEmail == null) return;

            var response = restTemplate.exchange(
                    API_URL + "/inbox?email=" + myEmail,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<EmailLogDto>>() {}
            );

            if (response.getBody() != null) {
                masterInboxData.setAll(response.getBody());
                applyFilter(filterField.getText()); // Odśwież widok z filtrem
            }
            updateUnreadCount();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void loadSent() {
        try {
            String username = UserSession.getInstance().getUsername();
            var response = restTemplate.exchange(
                    API_URL + "/history/" + username,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<EmailLogDto>>() {}
            );
            if (response.getBody() != null) {
                masterSentData.setAll(response.getBody());
                applyFilter(filterField.getText());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void loadAllEmailsAdmin() {
        try {
            var response = restTemplate.exchange(
                    API_URL + "/all",
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<EmailLogDto>>() {}
            );
            if (response.getBody() != null) {
                masterAdminData.setAll(response.getBody());
                applyFilter(filterField.getText());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- AKCJE ---

    @FXML
    public void showEmailDetails() {
        EmailLogDto selected = inboxTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        showDetailsGeneric(selected);

        if (!selected.isRead()) {
            markAsRead(selected.getId());
        }
    }

    private void showDetailsGeneric(EmailLogDto email) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Szczegóły wiadomości");
        alert.setHeaderText("Temat: " + email.getSubject());

        String info = "Od: " + email.getSender() + "\nDo: " + email.getRecipient() + "\nData: " + email.getSentAt() + "\n\n";
        TextArea area = new TextArea(info + email.getBody());
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefSize(500, 400);

        GridPane content = new GridPane();
        content.add(area, 0, 0);
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }

    private void markAsRead(Long id) {
        try {
            restTemplate.put(API_URL + "/" + id + "/read", null);
            loadInbox();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUnreadCount() {
        try {
            String myEmail = UserSession.getInstance().getEmail();
            if (myEmail == null) return;

            Long count = restTemplate.getForObject(API_URL + "/unread-count?email=" + myEmail, Long.class);
            if (count != null && count > 0) {
                inboxTab.setText("Skrzynka Odbiorcza (" + count + ")");
            } else {
                inboxTab.setText("Skrzynka Odbiorcza");
            }
        } catch (Exception e) {}
    }

    @FXML
    public void deleteEmail() {
        EmailLogDto selected = inboxTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        deleteGeneric(selected.getId(), () -> loadInbox());
    }

    @FXML
    public void deleteAdminEmail() {
        EmailLogDto selected = adminTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        deleteGeneric(selected.getId(), () -> loadAllEmailsAdmin());
    }

    private void deleteGeneric(Long id, Runnable refreshAction) {
        try {
            restTemplate.delete(API_URL + "/" + id);
            refreshAction.run();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Usunięto.");
            alert.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Błąd: " + e.getMessage());
            alert.show();
        }
    }

    @FXML
    public void loadFtpFiles() {
        new Thread(() -> {
            FTPClient ftpClient = new FTPClient();
            try {
                ftpClient.connect("localhost", 21);
                boolean login = ftpClient.login("admin", "admin");
                if (login) {
                    ftpClient.enterLocalPassiveMode();
                    FTPFile[] files = ftpClient.listFiles();
                    List<String> fileNames = new ArrayList<>();
                    for (FTPFile f : files) {
                        if (f.isFile()) fileNames.add(f.getName());
                    }
                    javafx.application.Platform.runLater(() -> {
                        if (ftpAttachmentCombo != null)
                            ftpAttachmentCombo.setItems(FXCollections.observableArrayList(fileNames));
                    });
                    ftpClient.logout();
                }
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void sendEmail() {
        String to = toField.getText();
        String subject = subjectField.getText();
        String body = bodyField.getText();
        String sender = UserSession.getInstance().getUsername();

        String attachment = ftpAttachmentCombo.getValue();
        if (attachment != null && !attachment.isEmpty()) {
            body += "\n\n--------------------------------\n" +
                    "[ZAŁĄCZNIK FTP]: ftp://127.0.0.1/" + attachment + "\n" +
                    "(Plik dostępny na firmowym serwerze plików)";
        }

        Map<String, String> request = new HashMap<>();
        request.put("to", to); // Backend rozdzieli po przecinku
        request.put("subject", subject);
        request.put("body", body);
        request.put("sender", sender);

        try {
            restTemplate.postForObject(API_URL + "/send", request, String.class);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Wiadomość wysłana!");
            alert.showAndWait();
            toField.clear();
            subjectField.clear();
            bodyField.clear();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Błąd wysyłania: " + e.getMessage());
            alert.show();
        }
    }
}