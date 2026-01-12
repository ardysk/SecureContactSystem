package com.example.client.controller;

import com.example.client.ClientApplication;
import com.example.client.model.AuditLogDto;
import com.example.client.model.EmailLogDto;
import com.example.client.session.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
    @FXML private StackPane contentArea; // Zamiast BorderPane używamy StackPane z FXML

    // Lista w Dashboardzie (zastępuje wykres)
    @FXML private ListView<String> latestEmailsList;

    // Tabela Audit
    @FXML private TableView<AuditLogDto> auditTable;
    @FXML private TableColumn<AuditLogDto, String> colAction; // Zmieniono nazwę zmiennej zgodnie z FXML
    @FXML private TableColumn<AuditLogDto, String> colWho;    // Zmieniono nazwę zmiennej zgodnie z FXML
    @FXML private TableColumn<AuditLogDto, String> colDetails;// Zmieniono nazwę zmiennej zgodnie z FXML
    @FXML private TableColumn<AuditLogDto, String> colTime;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_GATEWAY = "http://localhost:8000/api";

    @FXML
    public void initialize() {
        // Ustawienie powitania
        String username = UserSession.getInstance().getUsername();
        if (username != null) {
            welcomeLabel.setText("Witaj, " + username);
        }

        // Konfiguracja kolumn tabeli Audit
        // Upewnij się, że AuditLogDto ma odpowiednie gettery
        colAction.setCellValueFactory(new PropertyValueFactory<>("eventType"));
        colWho.setCellValueFactory(new PropertyValueFactory<>("username"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("message"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Ładowanie danych
        loadLatestEmails();
        refreshAuditLogs();
    }

    // --- LOGIKA DASHBOARDU ---

    private void loadLatestEmails() {
        try {
            String email = UserSession.getInstance().getEmail();
            if (email == null) {
                latestEmailsList.getItems().add("Brak adresu email w sesji.");
                return;
            }

            // Pobieramy inbox z notification-service
            var response = restTemplate.exchange(
                    "http://localhost:8000/api/email/inbox?email=" + email,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<EmailLogDto>>() {}
            );

            if (response.getBody() != null) {
                latestEmailsList.getItems().clear();

                // Filtrujemy nieprzeczytane i bierzemy 5 najnowszych
                long count = response.getBody().stream()
                        .filter(e -> !e.isRead())
                        .peek(e -> latestEmailsList.getItems().add(
                                "✉ Od: " + e.getSender() + " | Temat: " + e.getSubject() + " (" + e.getSentAt() + ")"
                        ))
                        .limit(5)
                        .count();

                if (count == 0) {
                    latestEmailsList.getItems().add("Brak nowych wiadomości.");
                }
            }
        } catch (Exception e) {
            latestEmailsList.getItems().add("Błąd pobierania wiadomości: " + e.getMessage());
        }
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

            List<AuditLogDto> logs = response.getBody();
            if (logs != null) {
                auditTable.setItems(FXCollections.observableArrayList(logs));
            }
        } catch (Exception e) {
            System.err.println("Błąd pobierania logów audit: " + e.getMessage());
        }
    }

    // --- NAWIGACJA (Menu Boczne) ---

    @FXML
    public void onContactsClick() {
        loadView("contacts-view.fxml");
    }

    @FXML
    public void onMailClick() {
        loadView("mail-view.fxml");
    }

    @FXML
    public void onFtpClick() {
        loadView("ftp-view.fxml");
    }

    @FXML
    public void onAccountClick() {
        loadView("account-view.fxml");
    }

    @FXML
    public void onLogout() {
        UserSession.getInstance().cleanUserSession();
        ClientApplication.changeScene("login-view.fxml", "Logowanie", 400, 500);
    }

    // --- METODA POMOCNICZA DO PODMIANY EKRANU ---
    public void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Parent view = loader.load();

            // Czyścimy obecny widok i dodajemy nowy
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Nie udało się załadować widoku: " + fxmlFile);
        }
    }
}