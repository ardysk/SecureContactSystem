package com.example.client;

import atlantafx.base.theme.PrimerLight; // Styl jasny, nowoczesny
import com.example.client.controller.DashboardController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApplication extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // 1. Ładujemy nowoczesny motyw (AtlantaFX)
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // 2. Ładujemy ekran logowania
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("/fxml/login-view.fxml"));

        // Ustawiamy rozmiar okna
        Scene scene = new Scene(fxmlLoader.load(), 400, 500);

        stage.setTitle("SECS v4.0 - Login");
        stage.setScene(scene);
        stage.setResizable(false); // Blokujemy zmianę rozmiaru przy logowaniu
        stage.show();
    }

    // Metoda pomocnicza do zmiany ekranów (np. po zalogowaniu)
    public static void changeScene(String fxmlFile, String title, int width, int height) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("/fxml/" + fxmlFile));
            Scene scene = new Scene(fxmlLoader.load(), width, height);
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.setResizable(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void loadMailViewWithRecipient(String recipient, String bodyContent) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("/fxml/dashboard-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1000, 700);

            // Pobieramy kontroler dashboardu, żeby załadować w nim widok poczty
            DashboardController dashboardController = fxmlLoader.getController();
            // Musimy "wymusić" załadowanie widoku poczty
            dashboardController.loadView("mail-view.fxml");

            // To jest trochę tricky w JavaFX przy zagnieżdżonych widokach.
            // Prostsza opcja: Użyjmy statycznego pola w MailControllerze, które on odczyta przy starcie via initialize().
            com.example.client.controller.MailController.preloadRecipient = recipient;
            com.example.client.controller.MailController.preloadBody = bodyContent;

            // Przełączamy scenę na Dashboard (który w initialize załaduje mail-view jeśli klikniesz,
            // ale my chcemy to zrobić automatycznie.
            // UPROSZCZENIE: Po prostu zmieńmy scenę na dashboard, a użytkownik kliknie "Poczta".
            // ALE LEPIEJ: Zrobimy w MailControllerze odczyt statycznych pól.

            primaryStage.setScene(scene);
            dashboardController.onMailClick(); // Symulujemy kliknięcie

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        launch();
    }
}