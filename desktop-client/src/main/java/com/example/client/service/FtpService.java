package com.example.client.service;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FtpService {

    private final String server = "localhost";
    private final int port = 21;
    private final String user = "admin";
    private final String pass = "admin";
    private final RestTemplate restTemplate = new RestTemplate();

    public List<String> listFiles() {
        FTPClient ftpClient = new FTPClient();
        try {
            connect(ftpClient);
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

    public boolean uploadFile(File localFile, String username) {
        FTPClient ftpClient = new FTPClient();
        try (InputStream inputStream = new FileInputStream(localFile)) {
            connect(ftpClient);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            boolean success = ftpClient.storeFile(localFile.getName(), inputStream);

            if (success) {
                try {
                    String auditMsg = "FTP_UPLOAD|" + username + "|Wgrano plik: " + localFile.getName();
                    restTemplate.postForObject("http://localhost:8000/api/audit/external", auditMsg, String.class);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
            return success;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            disconnect(ftpClient);
        }
    }

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

    private void connect(FTPClient ftpClient) throws IOException {
        ftpClient.connect(server, port);
        boolean login = ftpClient.login(user, pass);
        if (!login) {
            throw new IOException("Błąd logowania FTP");
        }
        ftpClient.enterLocalPassiveMode();
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