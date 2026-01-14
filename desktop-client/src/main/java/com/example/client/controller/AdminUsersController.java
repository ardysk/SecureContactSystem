package com.example.client.controller;

import com.example.client.model.UserDto;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminUsersController {

    @FXML private TableView<UserDto> usersTable;
    @FXML private TableColumn<UserDto, Long> colId;
    @FXML private TableColumn<UserDto, String> colUsername;
    @FXML private TableColumn<UserDto, String> colEmail;
    @FXML private TableColumn<UserDto, String> colRole;
    @FXML private TableColumn<UserDto, Boolean> colActive;
    @FXML private TableColumn<UserDto, Void> colAction;

    private final RestTemplate restTemplate = new RestTemplate();
    // Pamiętaj, że port 8000 to Gateway
    private final String API_URL = "http://localhost:8000/api/auth";

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));

        addEditButtonToTable();
        refreshData();
    }

    @FXML
    public void refreshData() {
        try {
            var response = restTemplate.exchange(
                    API_URL + "/users",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<UserDto>>() {}
            );
            List<UserDto> users = response.getBody();
            if (users != null) {
                usersTable.setItems(FXCollections.observableArrayList(users));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addEditButtonToTable() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edytuj");

            {
                editBtn.setOnAction(event -> {
                    UserDto user = getTableView().getItems().get(getIndex());
                    showEditDialog(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(editBtn);
                }
            }
        });
    }

    private void showEditDialog(UserDto user) {
        Dialog<UserDto> dialog = new Dialog<>();
        dialog.setTitle("Edycja Użytkownika: " + user.getUsername());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField emailField = new TextField(user.getEmail());
        ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList("USER", "ADMIN"));
        roleCombo.setValue(user.getRole());
        CheckBox activeCheck = new CheckBox("Aktywny");
        activeCheck.setSelected(user.isActive());

        VBox content = new VBox(10,
                new Label("Email:"), emailField,
                new Label("Rola:"), roleCombo,
                activeCheck
        );
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                user.setEmail(emailField.getText());
                user.setRole(roleCombo.getValue());
                user.setActive(activeCheck.isSelected());
                return user;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(this::updateUserOnServer);
    }

    private void updateUserOnServer(UserDto user) {
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("email", user.getEmail());
            updates.put("role", user.getRole());
            updates.put("active", user.isActive());

            restTemplate.put(API_URL + "/admin/users/" + user.getId(), updates);
            refreshData();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Błąd: " + e.getMessage());
            alert.show();
        }
    }
}