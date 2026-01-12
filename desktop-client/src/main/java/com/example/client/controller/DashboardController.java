package com.example.client.controller;

import com.example.client.session.UserSession;
import com.example.client.ClientApplication;
import com.example.client.model.AuditLogDto;
import com.example.client.model.ContactDto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button; // <--- DODANY IMPORT
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

public class DashboardController {

    // --- GŁÓWNY KONTENER (do podmiany widoków) ---
    @FXML private BorderPane mainPane;
    private Parent dashboardContent; // Tu przechowamy oryginalny widok Dashboardu (wykresy)

    @FXML private Label welcomeLabel;
    @FXML private PieChart statsChart;

    // Tabela Audit
    @FXML private TableView<AuditLogDto> auditTable;
    @FXML private TableColumn<AuditLogDto, String> colType;
    @FXML private TableColumn<AuditLogDto, String> colUser;
    @FXML private TableColumn<AuditLogDto, String> colMsg;
    @FXML private TableColumn<AuditLogDto, String> colTime;

    // Przycisk Panelu Admina
    @FXML private Button adminPanelButton;

    private final RestTemplate restTemplate = new RestTemplate();
    // Gateway URL
    private final String API_GATEWAY = "http://localhost:8000/api";

    @FXML
    public void initialize() {
        // 1. Zapamiętujemy oryginalny środek (wykresy i tabele), żeby móc tu wrócić
        if (mainPane != null) {
            dashboardContent = (Parent) mainPane.getCenter();
        }

        // 2. Konfiguracja kolumn tabeli
        colType.setCellValueFactory(new PropertyValueFactory<>("eventType"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colMsg.setCellValueFactory(new PropertyValueFactory<>("message"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // 3. LOGIKA UKRYWANIA PANELU ADMINA
        // Sprawdzamy rolę zalogowanego użytkownika
        String role = UserSession.getInstance().getRole();
        if (!"ADMIN".equals(role)) {
            if (adminPanelButton != null) {
                adminPanelButton.setVisible(false); // Ukryj
                adminPanelButton.setManaged(false); // Nie zajmuj miejsca w układzie
            }
        }

        // 4. Pobranie danych na start
        loadChartData();
        refreshAuditLogs();

        // Ustawienie powitania jeśli user jest w sesji
        if (UserSession.getInstance().getUsername() != null) {
            setUsername(UserSession.getInstance().getUsername());
        }
    }

    public void setUsername(String username) {
        welcomeLabel.setText("Witaj, " + username);
    }

    // --- NAWIGACJA (Side Menu) ---

    @FXML
    private void onDashboardClick() {
        System.out.println("Powrót do Dashboardu");
        if (mainPane != null && dashboardContent != null) {
            mainPane.setCenter(dashboardContent); // Przywracamy wykresy
            refreshAuditLogs(); // Odświeżamy dane przy powrocie
            loadChartData();
        }
    }

    @FXML
    public void onMailClick() { // Public, żeby można było wywołać z zewnątrz
        System.out.println("Przełączanie na Pocztę");
        loadView("mail-view.fxml");
    }

    @FXML
    private void onContactsClick() {
        System.out.println("Przełączanie na Kontakty");
        loadView("contacts-view.fxml");
    }

    @FXML
    private void onFtpClick() {
        System.out.println("Przełączanie na FTP");
        loadView("ftp-view.fxml");
    }

    @FXML
    public void onAdminPanelClick() {
        // Podwójne sprawdzenie bezpieczeństwa (nawet jakby ktoś odkrył przycisk)
        if ("ADMIN".equals(UserSession.getInstance().getRole())) {
            loadView("admin-users-view.fxml");
        } else {
            System.out.println("Brak uprawnień do Panelu Admina!");
        }
    }

    @FXML
    private void onLogoutClick() {
        // Czyścimy sesję
        UserSession.getInstance().cleanUserSession();
        // Powrót do logowania (cała scena)
        ClientApplication.changeScene("login-view.fxml", "Logowanie", 400, 500);
    }

    // --- METODA POMOCNICZA DO PODMIANY EKRANU ---
    // Zmieniona na public, aby ClientApplication mogło jej użyć przy przekierowaniu
    public void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("/fxml/" + fxmlFile));
            Parent view = loader.load();
            mainPane.setCenter(view); // Podmieniamy tylko środek, menu zostaje
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Nie udało się załadować widoku: " + fxmlFile);
        }
    }

    // --- LOGIKA BIZNESOWA (Wykresy i Logi) ---

    private void loadChartData() {
        try {
            // Pobieramy listę użytkowników z Auth Service zamiast z Contact Service
            // (bo teraz to Auth trzyma listę ludzi)
            var response = restTemplate.exchange(
                    "http://localhost:8000/api/auth/users", // Zmieniony URL na Auth Service
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ContactDto>>() {} // ContactDto pasuje polami (age)
            );

            List<ContactDto> contacts = response.getBody();
            if (contacts == null) return;

            long adults = contacts.stream().filter(c -> c.getAge() >= 18).count();
            long minors = contacts.size() - adults;

            ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList(
                    new PieChart.Data("Dorośli (" + adults + ")", adults),
                    new PieChart.Data("Niepełnoletni (" + minors + ")", minors)
            );
            statsChart.setData(chartData);

        } catch (Exception e) {
            System.err.println("Błąd wykresu (może brak połączenia?): " + e.getMessage());
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
            System.err.println("Błąd pobierania logów: " + e.getMessage());
        }
    }
}