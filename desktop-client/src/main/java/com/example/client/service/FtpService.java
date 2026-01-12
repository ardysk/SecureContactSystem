package com.example.client.service;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FtpService {

    // Dane zgodne z docker-compose.yml (kontener scs-ftp)
    private final String server = "localhost";
    private final int port = 21;
    private final String user = "admin";
    private final String pass = "admin";

    // 1. Pobieranie listy plików
    public List<String> listFiles() {
        FTPClient ftpClient = new FTPClient();
        try {
            connect(ftpClient);
            // Pobieramy listę i zamieniamy tablicę obiektów na listę nazw (String)
            return Arrays.stream(ftpClient.listFiles())
                    .map(file -> file.getName())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.singletonList("Błąd połączenia FTP: " + e.getMessage());
        } finally {
            disconnect(ftpClient);
        }
    }

    // 2. Wysyłanie pliku (Upload)
    public boolean uploadFile(File localFile) {
        FTPClient ftpClient = new FTPClient();
        try (InputStream inputStream = new FileInputStream(localFile)) {
            connect(ftpClient);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE); // Ważne dla zdjęć/pdf itp.
            return ftpClient.storeFile(localFile.getName(), inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            disconnect(ftpClient);
        }
    }

    // 3. Pobieranie pliku (Download)
    public boolean downloadFile(String remoteFileName, File localDestination) {
        FTPClient ftpClient = new FTPClient();
        try (OutputStream outputStream = new FileOutputStream(localDestination)) {
            connect(ftpClient);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            return ftpClient.retrieveFile(remoteFileName, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            disconnect(ftpClient);
        }
    }

    // --- Metody pomocnicze (Prywatne) ---

    private void connect(FTPClient ftpClient) throws IOException {
        ftpClient.connect(server, port);
        boolean login = ftpClient.login(user, pass);
        if (!login) {
            throw new IOException("Nieudane logowanie do FTP (sprawdź login/hasło)");
        }
        ftpClient.enterLocalPassiveMode(); // KLUCZOWE dla działania z Dockerem!
    }

    private void disconnect(FTPClient ftpClient) {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}