package com.example.contact_service.component;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;

@Component
public class ProtocolListener {

    // --- SERWER UDP (np. do odbierania logów) ---
    @PostConstruct
    public void startUdpServer() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(9999)) {
                byte[] buffer = new byte[1024];
                System.out.println(" [UDP] Serwer nasłuchuje na porcie 9999...");
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println(" [UDP LOG] Odebrano: " + received);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // --- SERWER TCP (np. do sprawdzania statusu) ---
    @PostConstruct
    public void startTcpServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(8888)) {
                System.out.println(" [TCP] Serwer nasłuchuje na porcie 8888...");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    String greeting = in.readLine();
                    if ("STATUS".equals(greeting)) {
                        out.println("OK - SYSTEM DZIALA");
                    } else {
                        out.println("NIEZNANA KOMENDA");
                    }
                    clientSocket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}