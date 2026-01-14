package com.example.client.controller;

import com.example.client.ClientApplication;
import com.example.client.model.AuditLogDto;
import com.example.client.model.EmailLogDto;
import com.example.client.session.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private StackPane contentArea;

    @FXML private ListView<String> latestEmailsList;
    @FXML private TableView<AuditLogDto> auditTable;
    @FXML private TableColumn<AuditLogDto, String> colAction;
    @FXML private TableColumn<AuditLogDto, String> colWho;
    @FXML private TableColumn<AuditLogDto, String> colDetails;
    @FXML private TableColumn<AuditLogDto, String> colTime;

    @FXML private Button adminPanelButton;

    private Node defaultDashboardView;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_GATEWAY = "http://localhost:8000/api";

    @FXML
    public void initialize() {
        if (!contentArea.getChildren().isEmpty()) {
            defaultDashboardView = contentArea.getChildren().get(0);
        }

        String username = UserSession.getInstance().getUsername();
        String role = UserSession.getInstance().getRole();
        if (username != null) {
            welcomeLabel.setText("Witaj, " + username + " [" + role + "]");
        }

        if (!"ADMIN".equals(role)) {
            if (adminPanelButton != null) {
                adminPanelButton.setVisible(false);
                adminPanelButton.setManaged(false);
            }
        }

        colAction.setCellValueFactory(new PropertyValueFactory<>("eventType"));
        colWho.setCellValueFactory(new PropertyValueFactory<>("username"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("message"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Załadowanie danych przy starcie
        loadLatestEmails();
        refreshAuditLogs();
    }

    @FXML
    public void onDashboardClick() {
        if (defaultDashboardView != null) {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(defaultDashboardView);
            // Odświeżanie obu sekcji po kliknięciu w menu
            refreshAuditLogs();
            loadLatestEmails();
        }
    }

    @FXML
    public void onContactsClick() { loadView("contacts-view.fxml"); }

    @FXML
    public void onMailClick() { loadView("mail-view.fxml"); }

    @FXML
    public void onFtpClick() { loadView("ftp-view.fxml"); }

    @FXML
    public void onAccountClick() { loadView("account-view.fxml"); }

    @FXML
    public void onAdminPanelClick() {
        if ("ADMIN".equals(UserSession.getInstance().getRole())) {
            loadView("admin-users-view.fxml");
        }
    }

    @FXML
    public void onLogout() {
        UserSession.getInstance().cleanUserSession();
        ClientApplication.changeScene("login-view.fxml", "Logowanie", 400, 500);
    }

    @FXML
    public void refreshAuditLogs() {
        try {
            var response = restTemplate.exchange(
                    API_GATEWAY + "/audit",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<AuditLogDto>>() {}
            );
            if (response.getBody() != null) {
                auditTable.setItems(FXCollections.observableArrayList(response.getBody()));
            }
        } catch (Exception e) {
            System.err.println("Błąd Audit: " + e.getMessage());
        }
    }

    private void loadLatestEmails() {
        try {
            String email = UserSession.getInstance().getEmail();
            if (email == null) return;

            // Pobieranie maili z API
            var response = restTemplate.exchange(
                    "http://localhost:8000/api/email/inbox?email=" + email,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<EmailLogDto>>() {}
            );

            if (response.getBody() != null) {
                latestEmailsList.getItems().clear();
                // ZMIANA: Usunięto .filter(e -> !e.isRead()), aby pokazywać wszystkie ostatnie wiadomości
                response.getBody().stream()
                        .limit(5)
                        .forEach(e -> latestEmailsList.getItems().add("✉ " + e.getSubject()));
            }
        } catch (Exception e) {
            System.err.println("Błąd ładowania maili w Dashboard: " + e.getMessage());
        }
    }

    public void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Parent view = loader.load();

            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Nie udało się załadować widoku: " + fxmlFile);
        }
    }
}