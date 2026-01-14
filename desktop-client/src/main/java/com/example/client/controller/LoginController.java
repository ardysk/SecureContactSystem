package com.example.client.controller;

import com.example.client.ClientApplication;
import com.example.client.session.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    // Łączymy się z Gatewayem
    private final String AUTH_URL = "http://localhost:8000/api/auth/login";
    private final RestTemplate restTemplate = new RestTemplate();

    @FXML
    public void onLogin() {
        // 1. Pobieranie danych z pól
        String username = usernameField.getText();
        String password = passwordField.getText();

        // 2. Walidacja wstępna (puste pola)
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Wpisz login i hasło!");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        statusLabel.setText("Logowanie...");
        statusLabel.setStyle("-fx-text-fill: black;");

        try {
            // 3. Tworzymy mapę z danymi do wysłania
            Map<String, String> request = new HashMap<>();
            request.put("username", username);
            request.put("password", password);

            // 4. Strzał do API
            ResponseEntity<Map> response = restTemplate.postForEntity(AUTH_URL, request, Map.class);

            // 5. Sprawdzenie sukcesu (200 OK)
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();

                // 6. ZAPISUJEMY SESJĘ (Singleton)
                UserSession session = UserSession.getInstance();
                session.setUsername(username);

                if (body != null) {
                    session.setRole((String) body.get("role"));

                    // Bezpieczne rzutowanie ID
                    if (body.get("userId") instanceof Number) {
                        session.setId(((Number) body.get("userId")).longValue());
                    }

                    // Zapisujemy email do sesji (ważne dla poczty!)
                    if (body.get("email") != null) {
                        session.setEmail((String) body.get("email"));
                    }
                }

                System.out.println("Zalogowano pomyślnie jako: " + username);
                ClientApplication.changeScene("dashboard-view.fxml", "SECS Dashboard - " + username, 1000, 700);

            } else {
                statusLabel.setText("Błąd serwera: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException.Unauthorized e) {
            // Kod 401 - Złe hasło lub login
            statusLabel.setText("Niepoprawny login lub hasło!");
            statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        } catch (HttpClientErrorException.Forbidden e) {
            // Kod 403 - Konto zablokowane/nieaktywne
            statusLabel.setText("Twoje konto jest nieaktywne! Skontaktuj się z administratorem.");
            statusLabel.setStyle("-fx-text-fill: red;");

        } catch (ResourceAccessException e) {
            // Błąd połączenia (np. Docker nie działa)
            statusLabel.setText("Brak połączenia z serwerem! Sprawdź Gateway.");
            statusLabel.setStyle("-fx-text-fill: red;");

        } catch (Exception e) {
            // Inne błędy
            e.printStackTrace();
            statusLabel.setText("Wystąpił błąd: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void onGoToRegister() {
        ClientApplication.changeScene("register-view.fxml", "Rejestracja", 500, 600); // Zwiększyłem lekko wysokość okna
    }
}