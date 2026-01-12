package com.example.client.controller;

import com.example.client.ClientApplication;
import com.example.client.model.UserDto;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class ContactsController {

    @FXML private TextField searchField;
    @FXML private TableView<UserDto> contactsTable;
    @FXML private TableColumn<UserDto, String> colName;
    @FXML private TableColumn<UserDto, String> colEmail;
    @FXML private TableColumn<UserDto, Void> colAction; // Nowa kolumna na przycisk

    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_URL = "http://localhost:8000/api/auth/users";

    // Lista do filtrowania
    private FilteredList<UserDto> filteredData;

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Konfiguracja przycisku "Wyślij Maila"
        addButtonToTable();

        refreshContacts();

        // Obsługa szukania
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (filteredData != null) {
                filteredData.setPredicate(user -> {
                    if (newValue == null || newValue.isEmpty()) return true;
                    String lowerCaseFilter = newValue.toLowerCase();

                    if (user.getUsername().toLowerCase().contains(lowerCaseFilter)) return true;
                    if (user.getEmail().toLowerCase().contains(lowerCaseFilter)) return true;
                    return false;
                });
            }
        });
    }

    private void addButtonToTable() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Napisz");

            {
                btn.setStyle("-fx-background-color: #0f62fe; -fx-text-fill: white; -fx-font-size: 10px;");
                btn.setOnAction(event -> {
                    UserDto user = getTableView().getItems().get(getIndex());
                    // Przekierowanie do poczty z wypełnionym adresem
                    ClientApplication.loadMailViewWithRecipient(user.getEmail(), "");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btn);
            }
        });
    }

    @FXML
    public void refreshContacts() {
        try {
            var response = restTemplate.exchange(API_URL, HttpMethod.GET, null, new ParameterizedTypeReference<List<UserDto>>() {});
            List<UserDto> users = response.getBody();

            if (users != null) {
                var observableList = FXCollections.observableArrayList(users);
                filteredData = new FilteredList<>(observableList, p -> true);
                contactsTable.setItems(filteredData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}