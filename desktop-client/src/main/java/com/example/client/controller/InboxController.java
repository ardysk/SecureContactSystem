package com.example.client.controller;

import com.example.client.session.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class InboxController {
    @FXML private ListView<String> emailList;
    @FXML private Label subjectLabel;
    @FXML private Label fromLabel;
    @FXML private TextArea contentArea;

    private List<Map<String, String>> fetchedEmails;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_URL = "http://localhost:8000/api/notifications/inbox?username=";

    @FXML
    public void initialize() {
        loadEmails();

        // Obsługa kliknięcia w listę
        emailList.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() >= 0) {
                showEmailDetails(newValue.intValue());
            }
        });
    }

    private void loadEmails() {
        try {
            String user = UserSession.getInstance().getUsername();
            var response = restTemplate.exchange(
                    API_URL + user,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, String>>>() {}
            );

            fetchedEmails = response.getBody();
            if (fetchedEmails != null) {
                emailList.setItems(FXCollections.observableArrayList(
                        fetchedEmails.stream().map(m -> m.get("subject")).toList()
                ));
            }
        } catch (Exception e) {
            emailList.getItems().add("Błąd pobierania: " + e.getMessage());
        }
    }

    private void showEmailDetails(int index) {
        Map<String, String> email = fetchedEmails.get(index);
        subjectLabel.setText(email.get("subject"));
        fromLabel.setText("Od: " + email.get("from"));
        contentArea.setText(email.get("content"));
    }
}