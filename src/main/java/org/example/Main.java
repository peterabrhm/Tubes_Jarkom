package org.example;

import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.awt.*;
import java.sql.*;

public class Main {
    private static Map<String, String> users = new HashMap<>();
    private static java.util.List<ChatRoom> chatRooms = new ArrayList<>();
    private static Map<String, Set<String>> userRooms = new HashMap<>();
    private static final int MIN_LENGTH = 5;
    private static final int MAX_PARTICIPANTS_DEFAULT = 5;
    private static String currentUser;
    private static Connection conn;

    public static void main(String[] args) {

        try {
            conn = DatabaseUtil.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        // Create the main frame with Sign Up and Login options
        JFrame mainFrame = new JFrame("Chat Application");
        mainFrame.setSize(400, 300);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLocationRelativeTo(null);

        // Create main panel
        JPanel mainPanel = new JPanel();
        mainFrame.add(mainPanel);
        placeMainComponents(mainPanel, mainFrame);

        mainFrame.setVisible(true);
    }

    private static void registerUser(String username, String password) throws SQLException {
        String query = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createChatRoom(String name, String status, String password,
            int maxParticipants) throws SQLException {
        String query =
                "INSERT INTO chat_rooms (name, status, password, max_participants) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, status);
            stmt.setString(3, password);
            stmt.setInt(4, maxParticipants);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static boolean loginUser(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void placeMainComponents(JPanel panel, JFrame mainFrame) {
        panel.setLayout(null);

        JButton signUpButton = new JButton("Sign Up");
        signUpButton.setBounds(100, 80, 200, 25);
        panel.add(signUpButton);

        JButton loginButton = new JButton("Login");
        loginButton.setBounds(100, 120, 200, 25);
        panel.add(loginButton);

        signUpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showRegistrationFrame(mainFrame);
            }
        });

        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showLoginFrame(mainFrame);
            }
        });
    }

    private static void showRegistrationFrame(JFrame mainFrame) {
        // Create the registration frame
        JFrame registerFrame = new JFrame("User Registration");
        registerFrame.setSize(400, 300);
        registerFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        registerFrame.setLocationRelativeTo(mainFrame);

        // Create registration panel
        JPanel registerPanel = new JPanel();
        registerFrame.add(registerPanel);
        placeRegisterComponents(registerPanel, registerFrame);

        registerFrame.setVisible(true);
    }

    private static void placeRegisterComponents(JPanel panel, JFrame registerFrame) {
        panel.setLayout(null);

        JLabel userLabel = new JLabel("Username");
        userLabel.setBounds(10, 20, 80, 25);
        panel.add(userLabel);

        JTextField userText = new JTextField(20);
        userText.setBounds(100, 20, 165, 25);
        panel.add(userText);

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setBounds(10, 50, 80, 25);
        panel.add(passwordLabel);

        JPasswordField passwordText = new JPasswordField(20);
        passwordText.setBounds(100, 50, 165, 25);
        panel.add(passwordText);

        JButton registerButton = new JButton("Register");
        registerButton.setBounds(10, 80, 150, 25);
        panel.add(registerButton);

        registerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String username = userText.getText();
                String password = new String(passwordText.getPassword());

                if (username.length() < MIN_LENGTH || password.length() < MIN_LENGTH) {
                    JOptionPane.showMessageDialog(panel, "Username and password must be at least "
                            + MIN_LENGTH + " characters long.");
                } else {
                    try {
                        registerUser(username, password);
                        userRooms.put(username, new HashSet<>());
                        JOptionPane.showMessageDialog(panel, "User registered successfully.");
                        registerFrame.dispose();
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(panel,
                                "Registration failed: " + ex.getMessage());
                    }
                }
            }
        });
    }

    private static void showLoginFrame(JFrame mainFrame) {
        // Create the login frame
        JFrame loginFrame = new JFrame("User Login");
        loginFrame.setSize(400, 300);
        loginFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        loginFrame.setLocationRelativeTo(mainFrame);

        // Create login panel
        JPanel loginPanel = new JPanel();
        loginFrame.add(loginPanel);
        placeLoginComponents(loginPanel, loginFrame);

        loginFrame.setVisible(true);
    }

    private static void placeLoginComponents(JPanel panel, JFrame loginFrame) {
        panel.setLayout(null);

        JLabel userLabel = new JLabel("Username");
        userLabel.setBounds(10, 20, 80, 25);
        panel.add(userLabel);

        JTextField userText = new JTextField(20);
        userText.setBounds(100, 20, 165, 25);
        panel.add(userText);

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setBounds(10, 50, 80, 25);
        panel.add(passwordLabel);

        JPasswordField passwordText = new JPasswordField(20);
        passwordText.setBounds(100, 50, 165, 25);
        panel.add(passwordText);

        JButton loginButton = new JButton("Login");
        loginButton.setBounds(10, 80, 150, 25);
        panel.add(loginButton);

        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String username = userText.getText();
                String password = new String(passwordText.getPassword());

                if (username.length() < MIN_LENGTH || password.length() < MIN_LENGTH) {
                    JOptionPane.showMessageDialog(panel, "Username and password must be at least "
                            + MIN_LENGTH + " characters long.");
                } else {
                    if (loginUser(username, password)) {
                        JOptionPane.showMessageDialog(panel, "Login successful!");
                        currentUser = username;
                        loginFrame.dispose();
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                showMainMenu(); // Call to display the main menu
                            }
                        });
                    } else {
                        JOptionPane.showMessageDialog(panel, "Incorrect username or password.");
                    }
                }
            }
        });

    }

    private static String getUserName(String username) throws SQLException {
        String query = "SELECT username FROM users WHERE username = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static void showMainMenu() {
        // Main chat application window (after login)
        JFrame mainMenuFrame = new JFrame("Main Menu");
        mainMenuFrame.setSize(400, 300);
        mainMenuFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainMenuFrame.setLocationRelativeTo(null);

        JPanel mainMenuPanel = new JPanel();
        mainMenuPanel.setLayout(new BoxLayout(mainMenuPanel, BoxLayout.Y_AXIS));
        mainMenuFrame.add(mainMenuPanel);

        JButton createRoomButton = new JButton("Create New Room");
        createRoomButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        mainMenuPanel.add(createRoomButton);

        createRoomButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showCreateRoomFrame(mainMenuFrame);
            }
        });

        refreshMainMenu(mainMenuPanel, mainMenuFrame);

        mainMenuFrame.setVisible(true);
    }



    private static void showCreateRoomFrame(JFrame mainMenuFrame) {
        // Create the create room frame
        JFrame createRoomFrame = new JFrame("Create New Room");
        createRoomFrame.setSize(400, 300);
        createRoomFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        createRoomFrame.setLocationRelativeTo(mainMenuFrame);

        // Create create room panel
        JPanel createRoomPanel = new JPanel();
        createRoomPanel.setLayout(null);
        createRoomFrame.add(createRoomPanel);

        JLabel roomNameLabel = new JLabel("Room Name");
        roomNameLabel.setBounds(10, 20, 80, 25);
        createRoomPanel.add(roomNameLabel);

        JTextField roomNameText = new JTextField(20);
        roomNameText.setBounds(100, 20, 165, 25);
        createRoomPanel.add(roomNameText);

        JLabel statusLabel = new JLabel("Status");
        statusLabel.setBounds(10, 50, 80, 25);
        createRoomPanel.add(statusLabel);

        JComboBox<String> statusComboBox =
                new JComboBox<>(new String[] {"open", "restricted", "closed"});
        statusComboBox.setBounds(100, 50, 165, 25);
        createRoomPanel.add(statusComboBox);

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setBounds(10, 80, 80, 25);
        createRoomPanel.add(passwordLabel);

        JPasswordField passwordText = new JPasswordField(20);
        passwordText.setBounds(100, 80, 165, 25);
        createRoomPanel.add(passwordText);
        passwordText.setEnabled(false);

        statusComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String status = (String) statusComboBox.getSelectedItem();
                passwordText.setEnabled("restricted".equals(status));
            }
        });

        JLabel maxParticipantsLabel = new JLabel("Max Participants");
        maxParticipantsLabel.setBounds(10, 110, 120, 25);
        createRoomPanel.add(maxParticipantsLabel);

        JTextField maxParticipantsText = new JTextField(20);
        maxParticipantsText.setBounds(140, 110, 165, 25);
        createRoomPanel.add(maxParticipantsText);

        JButton createRoomButton = new JButton("Create Room");
        createRoomButton.setBounds(10, 140, 150, 25);
        createRoomPanel.add(createRoomButton);

        createRoomButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String roomName = roomNameText.getText();
                String status = (String) statusComboBox.getSelectedItem();
                String password = new String(passwordText.getPassword());
                int maxParticipants = MAX_PARTICIPANTS_DEFAULT;

                try {
                    maxParticipants = Integer.parseInt(maxParticipantsText.getText());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(createRoomPanel,
                            "Invalid number for max participants. Using default value.");
                }

                if (roomName.isEmpty()) {
                    JOptionPane.showMessageDialog(createRoomPanel, "Room name cannot be empty.");
                } else {
                    try {
                        createChatRoom(roomName, status, password, maxParticipants);
                        userRooms.get(currentUser).add(roomName);
                        JOptionPane.showMessageDialog(createRoomPanel,
                                "Room created successfully.");
                        createRoomFrame.dispose();
                        refreshMainMenu((JPanel) mainMenuFrame.getContentPane(), mainMenuFrame);
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(createRoomPanel,
                                "Room creation failed: " + ex.getMessage());
                    }
                }
            }
        });

        createRoomFrame.setVisible(true);
    }


    private static void refreshMainMenu(JPanel mainMenuPanel, JFrame mainMenuFrame) {
        mainMenuPanel.removeAll();

        // Add Profile button to top right corner
        JButton profileButton = new JButton("View Profile");
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(profileButton);
        mainMenuPanel.add(buttonPanel, BorderLayout.NORTH);

        profileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String name = null;
                try {
                    name = getUserName(currentUser);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                JOptionPane.showMessageDialog(mainMenuPanel, "Welcome, " + name + "!");
            }
        });

        JButton createRoomButton = new JButton("Create New Room");
        createRoomButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        mainMenuPanel.add(createRoomButton);

        createRoomButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showCreateRoomFrame(mainMenuFrame);
            }
        });

        mainMenuPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel joinedRoomsLabel = new JLabel("Joined Rooms");
        joinedRoomsLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        mainMenuPanel.add(joinedRoomsLabel);

        Set<String> joinedRooms = userRooms.get(currentUser);

        if (joinedRooms == null || joinedRooms.isEmpty()) {
            JLabel noRoomsLabel = new JLabel("No rooms joined yet.");
            noRoomsLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            mainMenuPanel.add(noRoomsLabel);
        } else {
            for (String room : joinedRooms) {
                JButton roomButton = new JButton(room);
                roomButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
                mainMenuPanel.add(roomButton);
                roomButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        showChatRoom(room);
                    }
                });
            }
        }

        mainMenuFrame.revalidate();
        mainMenuFrame.repaint();
    }


    private static void showChatRoom(String roomName) {
        // Implement chat room window
        JFrame chatRoomFrame = new JFrame("Chat Room - " + roomName);
        chatRoomFrame.setSize(400, 300);
        chatRoomFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        chatRoomFrame.setLocationRelativeTo(null);

        // Add components to the chat room frame
        JPanel chatRoomPanel = new JPanel();
        chatRoomFrame.add(chatRoomPanel);
        chatRoomPanel.setLayout(new BoxLayout(chatRoomPanel, BoxLayout.Y_AXIS));

        JLabel roomLabel = new JLabel("Welcome to " + roomName);
        roomLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        chatRoomPanel.add(roomLabel);

        chatRoomFrame.setVisible(true);
    }

    static class ChatRoom {
        private String name;
        private String status;
        private String password;
        private int maxParticipants;

        public ChatRoom(String name, String status, String password, int maxParticipants) {
            this.name = name;
            this.status = status;
            this.password = password;
            this.maxParticipants = maxParticipants;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }

        public String getPassword() {
            return password;
        }

        public int getMaxParticipants() {
            return maxParticipants;
        }
    }
}
