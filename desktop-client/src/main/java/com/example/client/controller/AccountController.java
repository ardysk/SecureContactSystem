package com.example.client.controller;

import com.example.client.ClientApplication;
import com.example.client.session.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

public class AccountController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private Label statusLabel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String AUTH_API = "http://localhost:8000/api/auth";

    @FXML
    public void initialize() {
        // Ładowanie danych z sesji (można też pobrać świeże z API)
        UserSession session = UserSession.getInstance();
        usernameField.setText(session.getUsername());
        emailField.setText(session.getEmail());

        // Tutaj można by dorobić endpoint GET /user/{id} w Auth Service, żeby pobrać imię/nazwisko
        // Na potrzeby demo zostawiamy puste do edycji lub pobieramy jeśli mamy w sesji
    }

    @FXML
    public void saveChanges() {
        try {
            Long userId = UserSession.getInstance().getId();
            Map<String, String> updates = new HashMap<>();
            updates.put("firstName", firstNameField.getText());
            updates.put("lastName", lastNameField.getText());

            restTemplate.put(AUTH_API + "/user/" + userId, updates);

            statusLabel.setText("Dane zaktualizowane!");
            statusLabel.setStyle("-fx-text-fill: green;");
        } catch (Exception e) {
            statusLabel.setText("Błąd zapisu: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void deleteAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Czy na pewno chcesz usunąć konto? Tej operacji nie można cofnąć.", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            try {
                Long userId = UserSession.getInstance().getId();
                restTemplate.delete(AUTH_API + "/user/" + userId);

                UserSession.getInstance().cleanUserSession();
                ClientApplication.changeScene("login-view.fxml", "Logowanie", 400, 500);
            } catch (Exception e) {
                statusLabel.setText("Błąd usuwania: " + e.getMessage());
            }
        }
    }
}