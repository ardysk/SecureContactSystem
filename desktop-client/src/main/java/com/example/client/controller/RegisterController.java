package com.example.client.controller;

import com.example.client.ClientApplication;
import javafx.fxml.FXML;
import javafx.scene.control.Alert; // Potrzebne do Alertu
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class RegisterController {

    @FXML private TextField userField;
    @FXML private TextField emailField;
    @FXML private PasswordField passField;
    @FXML private Label infoLabel;
    @FXML private TextField addressField;
    @FXML private TextField ageField;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_URL = "http://localhost:8000/api/auth/register";

    @FXML
    public void onRegister() {
        String username = userField.getText();
        String password = passField.getText();
        String email = emailField.getText();
        String address = addressField.getText();
        String ageText = ageField.getText();

        // 1. Walidacja pustych pól
        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || address.isEmpty() || ageText.isEmpty()) {
            infoLabel.setText("Wypełnij wszystkie pola!");
            infoLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // 2. Walidacja Wieku (musi być liczbą i >= 18)
        int age;
        try {
            age = Integer.parseInt(ageText);
            if (age < 18) {
                infoLabel.setText("Rejestracja tylko dla osób pełnoletnich (18+)!");
                infoLabel.setStyle("-fx-text-fill: red;");
                return;
            }
        } catch (NumberFormatException e) {
            infoLabel.setText("Wiek musi być liczbą!");
            infoLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // 3. Walidacja formatu Email
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            infoLabel.setText("Niepoprawny format email!");
            infoLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // 4. Zaawansowana Walidacja Hasła
        // Wymaga: min 8 znaków, 1 duża, 1 mała, 1 cyfra, 1 znak specjalny
        String passRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

        if (!password.matches(passRegex)) {
            // Wyświetlamy Alert, bo komunikat jest długi
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Słabe hasło");
            alert.setHeaderText("Hasło nie spełnia wymagań bezpieczeństwa");
            alert.setContentText("Hasło musi zawierać:\n- Minimum 8 znaków\n- Przynajmniej jedną dużą literę\n- Przynajmniej jedną małą literę\n- Przynajmniej jedną cyfrę\n- Znak specjalny (@#$%^&+=!)");
            alert.showAndWait();
            return;
        }

        try {
            // Budowanie JSONa (Zmieniono na String, Object aby wiek był intem)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("username", username);
            requestBody.put("password", password);
            requestBody.put("email", email);
            requestBody.put("role", "USER"); // Domyślna rola
            requestBody.put("address", address);
            requestBody.put("age", age); // Przekazujemy int

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Strzał do API
            restTemplate.postForEntity(API_URL, entity, String.class);

            infoLabel.setText("Sukces! Konto utworzone. Poczekaj na aktywację przez Admina.");
            infoLabel.setStyle("-fx-text-fill: green;");

            // Czyścimy pola
            userField.clear();
            passField.clear();
            emailField.clear();
            addressField.clear();
            ageField.clear();

        } catch (HttpClientErrorException e) {
            infoLabel.setText("Błąd: " + e.getResponseBodyAsString());
            infoLabel.setStyle("-fx-text-fill: red;");
        } catch (Exception e) {
            infoLabel.setText("Błąd połączenia: " + e.getMessage());
            infoLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void onBack() {
        ClientApplication.changeScene("login-view.fxml", "Logowanie", 400, 500);
    }
}