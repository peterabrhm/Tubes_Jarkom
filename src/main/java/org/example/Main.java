package org.example;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.awt.*;
import java.sql.*;
import java.io.*;
import java.net.*;

class ChatRoom {
    private String name;
    private String status;
    private String password;
    private int maxParticipants;
    private int participantsCount;
    private boolean userJoined; // New field

    public ChatRoom(String name, String status, String password, int maxParticipants) {
        this.name = name;
        this.status = status;
        this.password = password;
        this.maxParticipants = maxParticipants;
    }

    // getters and setters
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

    public int getParticipantsCount() {
        return participantsCount;
    }

    public void setParticipantsCount(int participantsCount) {
        this.participantsCount = participantsCount;
    }

    public boolean isUserJoined() {
        return userJoined;
    }

    public void setUserJoined(boolean userJoined) {
        this.userJoined = userJoined;
    }
}


public class Main {
    private static Map<String, String> users = new HashMap<>();
    private static java.util.List<ChatRoom> chatRooms = new ArrayList<>();
    private static Map<String, Set<String>> userRooms = new HashMap<>();
    private static final int MIN_LENGTH = 5;
    private static final int MAX_PARTICIPANTS_DEFAULT = 5;
    private static String currentUser;
    private static Connection conn;

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    private static void connectToServer() {
        String serverAddress = "127.0.0.1"; // or the appropriate server address
        int port = 12345; // or the appropriate port number

        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Connected to the server!");

            // Start a thread to read messages from the server
//            new Thread(new Runnable() {
//                public void run() {
//                    try {
//                        String message;
//                        while ((message = in.readLine()) != null) {
//                            System.out.println("Server: " + message);
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to connect to the server.");
        }
    }

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
            boolean loginSuccess = rs.next();

            if (loginSuccess) {
                currentUser = username;
                connectToServer();
                return true;
            } else {
                return false;
            }
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

    private static void showMainMenu() {
        JFrame mainMenuFrame = new JFrame("Main Menu");
        mainMenuFrame.setSize(600, 400);
        mainMenuFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainMenuFrame.setLocationRelativeTo(null);

        JPanel mainMenuPanel = new JPanel();
        mainMenuPanel.setLayout(new BoxLayout(mainMenuPanel, BoxLayout.Y_AXIS));
        mainMenuFrame.add(new JScrollPane(mainMenuPanel));

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

        JPanel joinedRoomsPanel = new JPanel();
        joinedRoomsPanel.setLayout(new BoxLayout(joinedRoomsPanel, BoxLayout.Y_AXIS));
        mainMenuPanel.add(joinedRoomsPanel);

        JLabel availableRoomsLabel = new JLabel("Available Rooms");
        availableRoomsLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        mainMenuPanel.add(availableRoomsLabel);

        JPanel availableRoomsPanel = new JPanel();
        availableRoomsPanel.setLayout(new BoxLayout(availableRoomsPanel, BoxLayout.Y_AXIS));
        mainMenuPanel.add(availableRoomsPanel);

        java.util.List<ChatRoom> chatRooms = fetchChatRoomsWithDetails();

        for (ChatRoom room : chatRooms) {
            JPanel roomPanel = new JPanel();
            roomPanel.setLayout(new BoxLayout(roomPanel, BoxLayout.Y_AXIS));
            roomPanel.setBorder(BorderFactory.createTitledBorder(room.getName()));

            JLabel roomDetails = new JLabel("Type: " + room.getStatus() +
                    ", Participants: " + room.getParticipantsCount() +
                    "/" + room.getMaxParticipants());
            roomDetails.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            roomPanel.add(roomDetails);

            JButton actionButton = new JButton(room.isUserJoined() ? "Enter" : "Join");
            actionButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
            actionButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    joinChatRoom(room);
                }
            });

            roomPanel.add(actionButton);

            if (room.isUserJoined()) {
                joinedRoomsPanel.add(roomPanel);
            } else {
                availableRoomsPanel.add(roomPanel);
            }
        }

        if (joinedRoomsPanel.getComponentCount() == 0) {
            JLabel noJoinedRoomsLabel = new JLabel("No joined rooms.");
            noJoinedRoomsLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            joinedRoomsPanel.add(noJoinedRoomsLabel);
        }

        if (availableRoomsPanel.getComponentCount() == 0) {
            JLabel noAvailableRoomsLabel = new JLabel("No rooms available.");
            noAvailableRoomsLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            availableRoomsPanel.add(noAvailableRoomsLabel);
        }

        mainMenuFrame.setVisible(true);
    }


    private static java.util.List<String> fetchChatRooms() {
        java.util.List<String> chatRooms = new ArrayList<>();
        String query = "SELECT name FROM chat_rooms";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                chatRooms.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chatRooms;
    }

    private static java.util.List<ChatRoom> fetchChatRoomsWithDetails() {
        java.util.List<ChatRoom> chatRooms = new ArrayList<>();
        String query = "SELECT cr.name, cr.status, cr.max_participants, " +
                "(SELECT COUNT(*) FROM room_participants rp WHERE rp.room_id = cr.id) AS participants_count, " +
                "EXISTS (SELECT 1 FROM room_participants rp WHERE rp.room_id = cr.id AND rp.username = ?) AS user_joined " +
                "FROM chat_rooms cr";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, currentUser);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                String status = rs.getString("status");
                int maxParticipants = rs.getInt("max_participants");
                int participantsCount = rs.getInt("participants_count");
                boolean userJoined = rs.getBoolean("user_joined");

                ChatRoom room = new ChatRoom(name, status, null, maxParticipants);
                room.setParticipantsCount(participantsCount);
                room.setUserJoined(userJoined);
                chatRooms.add(room);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chatRooms;
    }

    private static void joinChatRoom(ChatRoom room) {
        if ("Private".equals(room.getStatus())) {
            String password = JOptionPane.showInputDialog("Enter password for " + room.getName());
            if (password == null || !room.getPassword().equals(password.trim())) {
                JOptionPane.showMessageDialog(null, "Incorrect password or canceled.");
                return;
            }
        }

        // Check if the user is already a participant of the room
        String checkQuery = "SELECT COUNT(*) FROM room_participants WHERE room_id = (SELECT id FROM chat_rooms WHERE name = ? LIMIT 1) AND username = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
            checkStmt.setString(1, room.getName());
            checkStmt.setString(2, currentUser);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                // User is already a participant of the room
                JOptionPane.showMessageDialog(null, "You are already a participant of this room.");
                showChatRoom(room.getName());
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Add the current user to the room participants in the database
        String query = "INSERT INTO room_participants (room_id, username) VALUES ((SELECT id FROM chat_rooms WHERE name = ? LIMIT 1), ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, room.getName());
            stmt.setString(2, currentUser);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Update local userRooms map
        userRooms.computeIfAbsent(currentUser, k -> new HashSet<>()).add(room.getName());

        // Show the chat room
        showChatRoom(room.getName());
    }


    private static void showCreateRoomFrame(JFrame mainMenuFrame) {
        // Create the new room frame
        JFrame createRoomFrame = new JFrame("Create New Room");
        createRoomFrame.setSize(400, 300);
        createRoomFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        createRoomFrame.setLocationRelativeTo(mainMenuFrame);

        // Create new room panel
        JPanel createRoomPanel = new JPanel();
        createRoomFrame.add(createRoomPanel);
        placeCreateRoomComponents(createRoomPanel, createRoomFrame);

        createRoomFrame.setVisible(true);
    }

    private static void placeCreateRoomComponents(JPanel panel, JFrame createRoomFrame) {
        panel.setLayout(null);

        JLabel nameLabel = new JLabel("Room Name");
        nameLabel.setBounds(10, 20, 80, 25);
        panel.add(nameLabel);

        JTextField nameText = new JTextField(20);
        nameText.setBounds(100, 20, 165, 25);
        panel.add(nameText);

        JLabel statusLabel = new JLabel("Status");
        statusLabel.setBounds(10, 50, 80, 25);
        panel.add(statusLabel);

        JComboBox<String> statusComboBox = new JComboBox<>(new String[]{"Public", "Private"});
        statusComboBox.setBounds(100, 50, 165, 25);
        panel.add(statusComboBox);

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setBounds(10, 80, 80, 25);
        panel.add(passwordLabel);

        JPasswordField passwordText = new JPasswordField(20);
        passwordText.setBounds(100, 80, 165, 25);
        panel.add(passwordText);

        JLabel maxParticipantsLabel = new JLabel("Max Participants");
        maxParticipantsLabel.setBounds(10, 110, 120, 25);
        panel.add(maxParticipantsLabel);

        JTextField maxParticipantsText = new JTextField(String.valueOf(MAX_PARTICIPANTS_DEFAULT));
        maxParticipantsText.setBounds(130, 110, 135, 25);
        panel.add(maxParticipantsText);

        JButton createButton = new JButton("Create Room");
        createButton.setBounds(10, 140, 150, 25);
        panel.add(createButton);

        // Disable password text field initially since the default is "Public"
        passwordText.setEnabled(false);

        // Add ActionListener to handle room creation
        createButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String name = nameText.getText();
                String status = (String) statusComboBox.getSelectedItem();
                String password = new String(passwordText.getPassword());
                int maxParticipants;
                try {
                    maxParticipants = Integer.parseInt(maxParticipantsText.getText());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(panel, "Invalid number for max participants.");
                    return;
                }

                try {
                    if ("Public".equals(status)) {
                        password = null; // Set password to null for public rooms
                    }
                    createChatRoom(name, status, password, maxParticipants);
                    chatRooms.add(new ChatRoom(name, status, password, maxParticipants));
                    JOptionPane.showMessageDialog(panel, "Room created successfully.");
                    createRoomFrame.dispose();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            showMainMenu(); // Refresh main menu after creating room
                        }
                    });
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(panel, "Failed to create room: " + ex.getMessage());
                }
            }
        });

        // Disable/Enable password text field based on the selected status
        statusComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedStatus = (String) statusComboBox.getSelectedItem();
                if ("Public".equals(selectedStatus)) {
                    passwordText.setEnabled(false); // Disable password field
                    passwordText.setText(""); // Clear the password field
                } else {
                    passwordText.setEnabled(true); // Enable password field
                }
            }
        });
    }



    private static void showChatRoom(String roomName) {
        JFrame chatRoomFrame = new JFrame("Chat Room: " + roomName);
        chatRoomFrame.setSize(500, 400);
        chatRoomFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        chatRoomFrame.setLocationRelativeTo(null);

        JPanel chatRoomPanel = new JPanel();
        chatRoomPanel.setLayout(new BorderLayout());
        chatRoomFrame.add(chatRoomPanel);

        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        chatRoomPanel.add(scrollPane, BorderLayout.CENTER);

        JTextField messageField = new JTextField();
        chatRoomPanel.add(messageField, BorderLayout.SOUTH);

        messageField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String message = messageField.getText();
//                chatArea.append(currentUser + ": " + message + "\n");
                messageField.setText("");
                // Send message to the server here
                out.println(currentUser + ": " + message);
            }
        });

        chatRoomFrame.setVisible(true);

        // Thread to read messages from the server and update the chat area
        new Thread(new Runnable() {
            public void run() {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        final String finalMessage = message;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                chatArea.append(finalMessage + "\n");
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


}
