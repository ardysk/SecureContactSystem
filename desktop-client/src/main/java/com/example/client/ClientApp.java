package com.example.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ClientApp extends JFrame {

    private static final String API_URL = "http://localhost:8080/api/contacts";
    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper mapper = new ObjectMapper();

    private String currentUserRole = "";
    private String currentUserName = "";
    private String currentSessionPassword = "";

    // Flaga do sterowania wylogowaniem
    public boolean loggedOut = false;

    // GUI Components
    private DefaultTableModel contactsModel;
    private DefaultTableModel usersModel; // Nowy model dla tabeli użytkowników
    private JLabel statusLabel;
    private JTextArea logArea;

    private JTextField nameField, emailField, addressField, ageField;
    private JComboBox<String> emailRecipientCombo;
    private JTextField mailSubjectField;
    private JTextArea mailBodyArea;
    private JTextArea inboxArea;

    private List<String> cachedEmails = new ArrayList<>();

    public ClientApp() {
        // Logowanie na starcie konstruktora
        if (!performLoginOrRegister()) {
            System.exit(0);
        }

        setTitle("Secure System v3.0 (" + currentUserName + " [" + currentUserRole + "])");
        setSize(1100, 750);
        // Ważne: DISPOSE zamiast EXIT, żebyśmy mogli obsłużyć wylogowanie w main()
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- GŁÓWNE ZAKŁADKI ---
        JTabbedPane mainTabs = new JTabbedPane();

        if ("ADMIN".equals(currentUserRole)) {
            mainTabs.addTab("Zarządzanie Kontaktami", createContactsPanel());
            mainTabs.addTab("ADMIN: Użytkownicy", createUsersManagementPanel()); // NOWOŚĆ
        }

        mainTabs.addTab("Poczta", createMailPanel());
        mainTabs.addTab("Pliki FTP", createFtpPanel());
        mainTabs.addTab("Ustawienia / Wyloguj", createSettingsPanel());

        add(mainTabs, BorderLayout.CENTER);

        // --- STOPKA ---
        JPanel footer = new JPanel(new BorderLayout());
        statusLabel = new JLabel(" Serwer TCP: Łączenie...");
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.LIGHT_GRAY);
        footer.add(statusLabel, BorderLayout.NORTH);
        logArea = new JTextArea(4, 80);
        logArea.setEditable(false);
        footer.add(new JScrollPane(logArea), BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        startTcpStatusChecker();

        if ("ADMIN".equals(currentUserRole)) {
            refreshContacts();
            refreshUsersList(); // Pobierz listę userów na start
        }
    }

    // --- 1. NOWY PANEL: ZARZĄDZANIE UŻYTKOWNIKAMI (DLA ADMINA) ---
    private JPanel createUsersManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Pasek narzędzi
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefreshUsers = new JButton("Odśwież listę");
        JButton btnResetPass = new JButton("Zresetuj hasło");
        JButton btnDeleteUser = new JButton("Usuń użytkownika");
        btnDeleteUser.setBackground(new Color(255, 100, 100));

        toolBar.add(btnRefreshUsers);
        toolBar.add(btnResetPass);
        toolBar.add(btnDeleteUser);
        panel.add(toolBar, BorderLayout.NORTH);

        // Tabela Użytkowników
        String[] cols = {"ID", "Login", "Rola", "Wymuszona zmiana?"};
        usersModel = new DefaultTableModel(cols, 0);
        JTable usersTable = new JTable(usersModel);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);

        // Logika Przycisków
        btnRefreshUsers.addActionListener(e -> refreshUsersList());

        btnDeleteUser.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row == -1) return;
            Long id = ((Number) usersModel.getValueAt(row, 0)).longValue();
            String login = (String) usersModel.getValueAt(row, 1);

            if (login.equals(currentUserName)) {
                JOptionPane.showMessageDialog(this, "Nie możesz usunąć samego siebie!");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, "Czy na pewno usunąć użytkownika " + login + "?");
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    restTemplate.delete(API_URL + "/auth/users/" + id);
                    log("ADMIN: Usunięto użytkownika " + login);
                    refreshUsersList();
                } catch (Exception ex) {
                    log("Błąd usuwania: " + ex.getMessage());
                }
            }
        });

        btnResetPass.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row == -1) return;
            Long id = ((Number) usersModel.getValueAt(row, 0)).longValue();
            String login = (String) usersModel.getValueAt(row, 1);

            String tempPass = JOptionPane.showInputDialog(this, "Podaj nowe tymczasowe hasło dla " + login + ":");
            if (tempPass != null && !tempPass.isEmpty()) {
                try {
                    Map<String, String> data = new HashMap<>();
                    data.put("id", String.valueOf(id));
                    data.put("tempPassword", tempPass);
                    restTemplate.postForObject(API_URL + "/auth/admin-reset-password", data, String.class);
                    JOptionPane.showMessageDialog(this, "Hasło zmienione. Użytkownik będzie musiał je zmienić przy logowaniu.");
                    refreshUsersList();
                } catch (Exception ex) {
                    log("Błąd resetu: " + ex.getMessage());
                }
            }
        });

        return panel;
    }

    private void refreshUsersList() {
        if (usersModel == null) return;
        try {
            String json = restTemplate.getForObject(API_URL + "/auth/users", String.class);
            List<Map<String, Object>> users = mapper.readValue(json, new TypeReference<List<Map<String, Object>>>(){});

            usersModel.setRowCount(0);
            for (Map<String, Object> u : users) {
                usersModel.addRow(new Object[]{
                        ((Number) u.get("id")).longValue(),
                        u.get("username"),
                        u.get("role"),
                        u.get("changePasswordNextLogin")
                });
            }
            log("ADMIN: Pobrano listę użytkowników.");
        } catch (Exception e) {
            log("Błąd pobierania userów: " + e.getMessage());
        }
    }

    // --- 2. USTAWIENIA I WYLOGOWANIE ---
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0;

        JButton btnChangePass = new JButton("Zmień moje hasło");
        btnChangePass.setPreferredSize(new Dimension(200, 40));
        btnChangePass.addActionListener(e -> showChangePasswordDialog(false));
        panel.add(btnChangePass, gbc);

        gbc.gridy = 1;
        JButton btnLogout = new JButton("WYLOGUJ");
        btnLogout.setBackground(new Color(255, 150, 150));
        btnLogout.setPreferredSize(new Dimension(200, 40));

        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Czy na pewno wylogować?", "Wyloguj", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                this.loggedOut = true; // Sygnał dla pętli w main()
                this.dispose(); // Zamknij to okno
            }
        });
        panel.add(btnLogout, gbc);

        return panel;
    }

    // --- RESZTA KODU (BEZ ZMIAN LOGICZNYCH, TYLKO UI) ---

    private boolean performLoginOrRegister() {
        while (true) {
            JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
            JTextField userField = new JTextField();
            JPasswordField passField = new JPasswordField();
            panel.add(new JLabel("Login:")); panel.add(userField);
            panel.add(new JLabel("Hasło:")); panel.add(passField);

            Object[] options = {"Zaloguj", "Zarejestruj", "Wyjdź"};
            int result = JOptionPane.showOptionDialog(null, panel, "Secure System v3.0",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]);

            if (result == 2 || result == -1) return false;

            String user = userField.getText();
            String pass = new String(passField.getPassword());

            if (result == 1) performRegister(user, pass);
            else if (performApiLogin(user, pass)) return true;
        }
    }

    private void performRegister(String user, String pass) {
        try {
            String json = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", user, pass);
            org.springframework.http.HttpHeaders h = new org.springframework.http.HttpHeaders();
            h.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            restTemplate.postForObject(API_URL + "/auth/register", new org.springframework.http.HttpEntity<>(json, h), String.class);
            JOptionPane.showMessageDialog(null, "Konto utworzone! Możesz się zalogować.");
        } catch (HttpClientErrorException ex) {
            JOptionPane.showMessageDialog(null, "Błąd: " + ex.getResponseBodyAsString());
        } catch (Exception e) { JOptionPane.showMessageDialog(null, "Błąd: " + e.getMessage()); }
    }

    private boolean performApiLogin(String user, String pass) {
        try {
            Map<String, String> creds = new HashMap<>();
            creds.put("username", user); creds.put("password", pass);
            Map response = restTemplate.postForObject(API_URL + "/auth/login", creds, Map.class);

            if (response != null && "OK".equals(response.get("status"))) {
                this.currentUserRole = (String) response.get("role");
                this.currentUserName = user;
                this.currentSessionPassword = pass;

                if ((Boolean) response.get("forceChange")) {
                    JOptionPane.showMessageDialog(null, "Wymuszona zmiana hasła!");
                    if (!showChangePasswordDialog(true)) return false;
                }
                return true;
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(null, "Błąd logowania: " + e.getMessage()); }
        return false;
    }

    private boolean showChangePasswordDialog(boolean forced) {
        JPasswordField pf = new JPasswordField();
        if(JOptionPane.showConfirmDialog(null, new Object[]{"Nowe hasło:", pf}, "Zmiana Hasła", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                Map<String,String> d = new HashMap<>(); d.put("username", currentUserName); d.put("newPassword", new String(pf.getPassword()));
                restTemplate.postForObject(API_URL + "/auth/change-password", d, String.class);
                JOptionPane.showMessageDialog(null, "Hasło zmienione.");
                return true;
            } catch(Exception e) { JOptionPane.showMessageDialog(null, "Błąd: "+e.getMessage()); }
        }
        return false;
    }

    // PANELE UI
    private JPanel createContactsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new FlowLayout());
        nameField=new JTextField(8); emailField=new JTextField(8); addressField=new JTextField(8); ageField=new JTextField(3);
        form.add(new JLabel("Imię:")); form.add(nameField); form.add(new JLabel("Mail:")); form.add(emailField);
        form.add(new JLabel("Adres:")); form.add(addressField); form.add(new JLabel("Wiek:")); form.add(ageField);
        JButton addBtn = new JButton("Dodaj"); addBtn.addActionListener(e->addContact()); form.add(addBtn);
        JButton refBtn = new JButton("Odśwież"); refBtn.addActionListener(e->refreshContacts()); form.add(refBtn);
        p.add(form, BorderLayout.NORTH);
        contactsModel = new DefaultTableModel(new String[]{"ID","Imię","Email","Wiek","Adres"}, 0);
        JTable t = new JTable(contactsModel);
        t.getSelectionModel().addListSelectionListener(e->{
            if(!e.getValueIsAdjusting() && t.getSelectedRow()!=-1) emailRecipientCombo.setSelectedItem(contactsModel.getValueAt(t.getSelectedRow(), 2));
        });
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    private JPanel createMailPanel() {
        JTabbedPane tabs = new JTabbedPane();
        JPanel send = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints(); c.insets=new Insets(5,5,5,5); c.fill=GridBagConstraints.HORIZONTAL;
        emailRecipientCombo = new JComboBox<>(); emailRecipientCombo.setEditable(true); emailRecipientCombo.setPreferredSize(new Dimension(250,25));
        setupAutoComplete(emailRecipientCombo);
        mailSubjectField=new JTextField(20); mailBodyArea=new JTextArea(5,20);
        c.gridx=0;c.gridy=0;send.add(new JLabel("Do:"),c); c.gridx=1;send.add(emailRecipientCombo,c);
        c.gridx=0;c.gridy=1;send.add(new JLabel("Temat:"),c); c.gridx=1;send.add(mailSubjectField,c);
        c.gridx=0;c.gridy=2;send.add(new JLabel("Treść:"),c); c.gridx=1;send.add(new JScrollPane(mailBodyArea),c);
        JButton sBtn = new JButton("Wyślij"); sBtn.addActionListener(e->sendCustomEmail());
        c.gridy=3; send.add(sBtn,c);
        tabs.addTab("Nowa", send);

        JPanel inbox = new JPanel(new BorderLayout());
        inboxArea=new JTextArea(); JButton checkBtn=new JButton("Pobierz (POP3)"); checkBtn.addActionListener(e->fetchEmailsViaPop3());
        inbox.add(new JScrollPane(inboxArea), BorderLayout.CENTER); inbox.add(checkBtn, BorderLayout.NORTH);
        tabs.addTab("Odebrane", inbox);
        return new JPanel(new BorderLayout()){{add(tabs);}};
    }

    private JPanel createFtpPanel() {
        JPanel p = new JPanel();
        JButton up = new JButton("Wyślij plik");
        up.addActionListener(e->{
            JFileChooser fc = new JFileChooser();
            if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) uploadFileToFtp(fc.getSelectedFile());
        });
        p.add(up);
        return p;
    }

    private void setupAutoComplete(JComboBox<String> cb) {
        final JTextField tf = (JTextField) cb.getEditor().getEditorComponent();
        tf.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                SwingUtilities.invokeLater(() -> {
                    String txt = tf.getText();
                    if(txt.isEmpty()) return;
                    List<String> f = cachedEmails.stream().filter(s->s.toLowerCase().contains(txt.toLowerCase())).collect(Collectors.toList());
                    if(!f.isEmpty()) { cb.setModel(new DefaultComboBoxModel<>(f.toArray(new String[0]))); cb.setSelectedItem(txt); cb.showPopup(); }
                });
            }
        });
    }

    // LOGIKA
    private void addContact() {
        try {
            String json = String.format("{\"name\":\"%s\",\"email\":\"%s\",\"address\":\"%s\",\"age\":%s}",
                    nameField.getText(), emailField.getText(), addressField.getText(), ageField.getText().isEmpty()?"0":ageField.getText());
            org.springframework.http.HttpHeaders h = new org.springframework.http.HttpHeaders(); h.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            restTemplate.postForObject(API_URL, new org.springframework.http.HttpEntity<>(json, h), String.class);
            log("DODANO KONTAKT"); refreshContacts();
        } catch(Exception e) { log("Błąd: "+e.getMessage()); }
    }
    private void refreshContacts() {
        try {
            cachedEmails.clear();

            if("ADMIN".equals(currentUserRole)) {
                String json = restTemplate.getForObject(API_URL, String.class);
                List<Map<String,Object>> list = mapper.readValue(json, new TypeReference<List<Map<String,Object>>>(){});
                contactsModel.setRowCount(0);
                for(Map<String,Object> c : list) {
                    contactsModel.addRow(new Object[]{c.get("id"), c.get("name"), c.get("email"), c.get("age"), c.get("address")});
                    cachedEmails.add((String)c.get("email"));
                }
            }

            if("ADMIN".equals(currentUserRole)) {
                String usersJson = restTemplate.getForObject(API_URL + "/auth/users", String.class);
                List<Map<String, Object>> users = mapper.readValue(usersJson, new TypeReference<List<Map<String, Object>>>(){});
                for(Map<String, Object> u : users) {
                    if(u.get("email") != null) {
                        cachedEmails.add((String)u.get("email"));
                    }
                }
            }
            else {
                cachedEmails.add("admin@localhost");
            }

        } catch(Exception e) {}
    }
    private void sendCustomEmail() {
        try {
            Map<String,String> p = new HashMap<>(); p.put("to", (String)emailRecipientCombo.getSelectedItem());
            p.put("subject", mailSubjectField.getText()); p.put("body", mailBodyArea.getText());
            restTemplate.postForObject(API_URL+"/send-email", p, String.class);
            JOptionPane.showMessageDialog(this, "Wysłano!");
        } catch(Exception e) { log("Błąd: "+e.getMessage()); }
    }

    private void fetchEmailsViaPop3() {
        new Thread(() -> {
            POP3Client pop3 = new POP3Client();
            try {
                SwingUtilities.invokeLater(() -> inboxArea.setText("Łączenie z pocztą jako " + currentUserName + "...\n"));

                // Łączymy się z kontenerem GreenMail
                pop3.connect("localhost", 3110);

                // ZMIANA: Logowanie dynamiczne danymi z sesji
                // Używamy loginu i hasła, które wpisałeś w oknie logowania
                if (pop3.login(currentUserName, currentSessionPassword)) {

                    POP3MessageInfo[] messages = pop3.listMessages();

                    if (messages != null && messages.length > 0) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Znaleziono ").append(messages.length).append(" wiadomości:\n\n");

                        // Pętla odwrócona - od najnowszych do najstarszych
                        // Pobieramy max 5 ostatnich wiadomości
                        int limit = 5;
                        for (int i = messages.length - 1; i >= 0 && limit > 0; i--, limit--) {
                            sb.append("=== WIADOMOŚĆ #").append(messages[i].number).append(" ===\n");

                            // Protokół POP3 pobiera "Reader" z treścią wiadomości
                            Reader r = pop3.retrieveMessage(messages[i].number);
                            if (r != null) {
                                BufferedReader reader = new BufferedReader(r);
                                String line;
                                boolean isBody = false;

                                // Czytamy maila linijka po linijce
                                while ((line = reader.readLine()) != null) {
                                    // Filtrujemy nagłówki, żeby pokazać tylko te ważne
                                    if (line.startsWith("Subject:") || line.startsWith("From:") || line.startsWith("Date:")) {
                                        sb.append(line).append("\n");
                                    }
                                    // Pusta linia w protokole mailowym oddziela nagłówki od treści
                                    if (line.isEmpty()) isBody = true;

                                    // Jeśli zaczęła się treść, wyświetlamy wszystko
                                    if (isBody) {
                                        sb.append(line).append("\n");
                                    }
                                }
                                reader.close();
                            }
                            sb.append("\n-------------------------------\n");
                        }

                        String finalContent = sb.toString();
                        SwingUtilities.invokeLater(() -> inboxArea.setText(finalContent));
                    } else {
                        SwingUtilities.invokeLater(() -> inboxArea.setText("Skrzynka jest pusta."));
                    }
                    pop3.logout();
                } else {
                    SwingUtilities.invokeLater(() -> inboxArea.setText("Błąd logowania POP3!\nSprawdź czy hasło usera w bazie zgadza się z tym w docker-compose."));
                }
                pop3.disconnect();
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> log("Błąd POP3: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void uploadFileToFtp(File f) {
        new Thread(()->{
            FTPClient ftp = new FTPClient();
            try(FileInputStream s = new FileInputStream(f)) {
                ftp.connect("localhost", 21); ftp.login("admin","admin"); ftp.enterLocalPassiveMode(); ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.storeFile(f.getName(), s); ftp.logout();
                SwingUtilities.invokeLater(()->log("Wgrano plik"));
            } catch(Exception e) { SwingUtilities.invokeLater(()->log("Błąd FTP")); }
        }).start();
    }
    private void startTcpStatusChecker() {
        new Thread(()->{ while(true) { try(Socket s=new Socket("localhost",8888)){
            SwingUtilities.invokeLater(()->{statusLabel.setText(" TCP: OK "); statusLabel.setBackground(Color.GREEN);});
        } catch(Exception e){
            SwingUtilities.invokeLater(()->{statusLabel.setText(" TCP: ERR "); statusLabel.setBackground(Color.RED);});
        } try{Thread.sleep(3000);}catch(Exception e){} } }).start();
    }
    private void log(String s) { logArea.append(s+"\n"); }

    // Pętla restartująca aplikację po wylogowaniu
    // Pętla restartująca aplikację po wylogowaniu
    public static void main(String[] args) {
        // Uruchamiamy w nowym wątku, żeby nie blokować głównego wątku startowego
        new Thread(() -> {
            while (true) {
                // 1. Tworzenie i pokazanie okna (najlepiej w wątku Swingowym)
                final ClientApp[] appHolder = new ClientApp[1];
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        appHolder[0] = new ClientApp();
                        appHolder[0].setVisible(true);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }

                ClientApp app = appHolder[0];

                // 2. Oczekiwanie na zamknięcie okna
                // POPRAWKA: Usunięto synchronized(app.getTreeLock()), które blokowało rysowanie!
                while (app.isDisplayable()) {
                    try {
                        Thread.sleep(500); // Sprawdzamy co pół sekundy czy okno żyje
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                // 3. Decyzja co dalej (Zamknąć czy Restartować)
                if (!app.loggedOut) {
                    // Jeśli użytkownik zamknął "Iks-em" (X), zamykamy program
                    System.exit(0);
                }
                // Jeśli app.loggedOut == true, pętla while(true) leci od nowa -> pokazuje logowanie
            }
        }).start();
    }
}