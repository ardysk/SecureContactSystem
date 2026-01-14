package com.example.client.controller;

import com.example.client.session.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class ProfileController {
    @FXML private TextField usernameField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private Label statusLabel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_URL = "http://localhost:8000/api/auth/profile/";

    @FXML
    public void initialize() {
        String currentUser = UserSession.getInstance().getUsername();
        usernameField.setText(currentUser);
        loadProfileData(currentUser);
    }

    private void loadProfileData(String username) {
        try {
            // Zakładamy, że API zwraca mapę pól
            Map<String, Object> user = restTemplate.getForObject(API_URL + username, Map.class);
            if (user != null) {
                if(user.get("firstName") != null) firstNameField.setText((String)user.get("firstName"));
                if(user.get("lastName") != null) lastNameField.setText((String)user.get("lastName"));
            }
        } catch (Exception e) {
            statusLabel.setText("Błąd ładowania danych.");
        }
    }

    @FXML
    public void saveChanges() {
        try {
            Map<String, String> update = new HashMap<>();
            update.put("firstName", firstNameField.getText());
            update.put("lastName", lastNameField.getText());

            restTemplate.put(API_URL + UserSession.getInstance().getUsername(), update);
            statusLabel.setText("Zapisano pomyślnie!");
            statusLabel.setStyle("-fx-text-fill: green;");
        } catch (Exception e) {
            statusLabel.setText("Błąd zapisu: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }
}