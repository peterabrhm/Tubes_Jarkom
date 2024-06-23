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
    private String roomOwner;

    public ChatRoom(String name, String status, String password, int maxParticipants, String roomOwner) {
        this.name = name;
        this.status = status;
        this.password = password;
        this.maxParticipants = maxParticipants;
        this.roomOwner = roomOwner;
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

    public String getRoomOwner() {
        return roomOwner;
    }

    public void setRoomOwner(String roomOwner) {
        this.roomOwner = roomOwner;
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

    private static void cleanup() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            if (conn != null) conn.close();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                cleanup();
            }
        }));

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
                                       int maxParticipants, String roomOwner) throws SQLException {
        String query = "INSERT INTO chat_rooms (name, status, password, max_participants, room_owner) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, status);
            stmt.setString(3, password);
            stmt.setInt(4, maxParticipants);
            stmt.setString(5, roomOwner);
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
        placeLoginComponents(loginPanel, loginFrame, mainFrame);

        loginFrame.setVisible(true);
    }

    private static void placeLoginComponents(JPanel panel, JFrame loginFrame, JFrame mainFrame) {
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
                                loginFrame.dispatchEvent(new WindowEvent(loginFrame, WindowEvent.WINDOW_CLOSING));
                                showMainMenu(); // Call to display the main menu
                                mainFrame.setVisible(false);
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

        JButton btnViewProfile = new JButton("View Profile");
        btnViewProfile.setAlignmentX(JButton.CENTER_ALIGNMENT);
        btnViewProfile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showUserProfile();
            }
        });
        mainMenuPanel.add(btnViewProfile);


        JButton createRoomButton = new JButton("Create New Room");
        createRoomButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        mainMenuPanel.add(createRoomButton);

        createRoomButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showCreateRoomFrame(mainMenuFrame);
//                mainMenuFrame.dispatchEvent(new WindowEvent(mainMenuFrame, WindowEvent.WINDOW_CLOSING));
                mainMenuFrame.setVisible(false);
            }
        });

        mainMenuPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel joinedRoomsLabel = new JLabel("Joined Rooms");
        joinedRoomsLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        mainMenuPanel.add(joinedRoomsLabel);

        JPanel joinedRoomsPanel = new JPanel();
        joinedRoomsPanel.setLayout(new BoxLayout(joinedRoomsPanel, BoxLayout.Y_AXIS));
        mainMenuPanel.add(joinedRoomsPanel);

        mainMenuPanel.add(Box.createRigidArea(new Dimension(0, 10)));

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

            if (room.getRoomOwner().equals(currentUser)) {
                JButton deleteRoomButton = new JButton("Delete Room");
                deleteRoomButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
                deleteRoomButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        deleteChatRoom(room.getName());
                        out.println("Kick @");
                        chatRooms.remove(room);
                        mainMenuFrame.setVisible(false);
                        showMainMenu();
                    }
                });
                roomPanel.add(deleteRoomButton);

                JButton removeParticipantButton = new JButton("Remove Participant");
                removeParticipantButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
                removeParticipantButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String participant = JOptionPane.showInputDialog(mainMenuFrame, "Enter participant's username to remove:");
                        if (participant != null && !participant.isEmpty()) {
                            removeParticipantFromRoom(room.getName(), participant);
                            out.println("Kick " + participant);
                            mainMenuFrame.setVisible(false);
                            showMainMenu();
                        }
                    }
                });
                roomPanel.add(removeParticipantButton);
            }

            if (room.isUserJoined()) {
                JButton enterButton = new JButton("Enter");
                enterButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
                enterButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        showChatRoom(room.getName(), mainMenuFrame);
                    }
                });

                JButton leaveButton = new JButton("Leave");
                leaveButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
                leaveButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        leaveChatRoom(room.getName(), currentUser);
                        mainMenuFrame.setVisible(false);
                    }
                });

                roomPanel.add(enterButton);
                roomPanel.add(leaveButton);

                joinedRoomsPanel.add(roomPanel);
            } else {
                JButton joinButton = new JButton("Join");
                joinButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
                joinButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        joinChatRoom(room);
                        mainMenuFrame.setVisible(false);
                    }
                });

                roomPanel.add(joinButton);

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

    private static void showUserProfile() {
        try {
            String query = "SELECT * FROM users WHERE username = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            preparedStatement.setString(1, currentUser); // Ganti dengan username yang sesuai
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String username = resultSet.getString("username");
                // Tampilkan profil pengguna
                JOptionPane.showMessageDialog(null, "Username: " + username);
            } else {
                JOptionPane.showMessageDialog(null, "Pengguna tidak ditemukan.");
            }

            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        String query = "SELECT cr.name, cr.status, cr.max_participants, cr.room_owner, " +
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
                String roomOwner = rs.getString("room_owner");

                ChatRoom room = new ChatRoom(name, status, null, maxParticipants, roomOwner);
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
        // Send join request to the server (this should be implemented on the server-side)
        out.println(currentUser + " has join " + room.getName());

        // Add the current user to the room's participants list in the database
        String query = "INSERT INTO room_participants (room_id, username) " +
                "VALUES ((SELECT id FROM chat_rooms WHERE name = ?), ?)";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, room.getName());
            stmt.setString(2, currentUser);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Show a notification that the user has joined the room
        JOptionPane.showMessageDialog(null, "You have joined the room: " + room.getName());

        // Refresh the main menu
        showMainMenu();
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
        placeCreateRoomComponents(createRoomPanel, createRoomFrame, mainMenuFrame);

        createRoomFrame.setVisible(true);
    }

    private static void placeCreateRoomComponents(JPanel panel, JFrame createRoomFrame, JFrame mainMenuFrame) {
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
                String roomOwner = currentUser; // Assuming the current user is the room owner
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
                    createChatRoom(name, status, password, maxParticipants, roomOwner);
                    joinChatRoom(new ChatRoom(name, status, password, maxParticipants, roomOwner));
                    chatRooms.add(new ChatRoom(name, status, password, maxParticipants, roomOwner));
                    JOptionPane.showMessageDialog(panel, "Room created successfully and joined.");
                    createRoomFrame.dispose();
                    createRoomFrame.dispatchEvent(new WindowEvent(createRoomFrame, WindowEvent.WINDOW_CLOSING));
//                    showMainMenu(); // Refresh main menu after creating and joining room
//                    mainMenuFrame.dispatchEvent(new WindowEvent(mainMenuFrame, WindowEvent.WINDOW_CLOSING));
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


    private static void showChatRoom(String roomName, JFrame mainMenuFrame) {
        JFrame chatRoomFrame = new JFrame("Chat Room: " + roomName);
        chatRoomFrame.setSize(600, 400);
        chatRoomFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Dispose on close to prevent exiting the application
        chatRoomFrame.setLocationRelativeTo(null);

        JPanel chatRoomPanel = new JPanel();
        chatRoomPanel.setLayout(new BoxLayout(chatRoomPanel, BoxLayout.Y_AXIS));
        chatRoomFrame.add(new JScrollPane(chatRoomPanel));

        JLabel roomLabel = new JLabel("Welcome to " + roomName);
        roomLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        chatRoomPanel.add(roomLabel);

        JTextArea chatArea = new JTextArea(15, 50);
        chatArea.setEditable(false);
        chatRoomPanel.add(new JScrollPane(chatArea));

        JTextField messageField = new JTextField();
        JButton sendButton = new JButton("Send");
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.X_AXIS));
        messagePanel.add(messageField);
        messagePanel.add(sendButton);
        chatRoomPanel.add(messagePanel);

        JButton leaveButton = new JButton("Leave Room");
        leaveButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        leaveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                leaveChatRoom(roomName, currentUser);
                chatRoomFrame.dispose();
                chatRoomFrame.dispatchEvent(new WindowEvent(chatRoomFrame, WindowEvent.WINDOW_CLOSING));
//                showMainMenu();
                mainMenuFrame.setVisible(false);
            }
        });
        chatRoomPanel.add(leaveButton);

        JButton viewParticipantsButton = new JButton("View Participants");
        viewParticipantsButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        viewParticipantsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewParticipants(roomName);
            }
        });
        chatRoomPanel.add(viewParticipantsButton);


        JButton backButton = new JButton("Back to Main Menu");
        backButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chatRoomFrame.dispose();
                chatRoomFrame.dispatchEvent(new WindowEvent(chatRoomFrame, WindowEvent.WINDOW_CLOSING));
                showMainMenu(); // Go back to main menu
                mainMenuFrame.setVisible(false);
            }
        });
        chatRoomPanel.add(backButton);

        java.util.List<String> oldMessages = loadMessage(roomName);
        for (String message : oldMessages) {
            chatArea.append(message + "\n");
        }

        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String message = messageField.getText();
                if (!message.isEmpty()) {
                    out.println(currentUser + ": " + message);
                    chatArea.append("Me: " + message + "\n");
                    messageField.setText("");
                    createNewMessage(roomName, currentUser, message);
                }
            }
        });

        // Fetch the chat room to get the owner information
//        ChatRoom room = chatRooms.stream().filter(r -> r.getName().equals(roomName)).findFirst().orElse(null);
//
//        if (room != null && room.getRoomOwner().equals(currentUser)) {
//            JButton deleteRoomButton = new JButton("Delete Room");
//            deleteRoomButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
//            deleteRoomButton.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    deleteChatRoom(roomName);
//                    chatRoomFrame.dispose();
//                    showMainMenu();
//                }
//            });
//            chatRoomPanel.add(deleteRoomButton);
//
//            JButton removeParticipantButton = new JButton("Remove Participant");
//            removeParticipantButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
//            removeParticipantButton.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    String participant = JOptionPane.showInputDialog(chatRoomFrame, "Enter participant's username to remove:");
//                    if (participant != null && !participant.isEmpty()) {
//                        removeParticipantFromRoom(roomName, participant);
//                    }
//                }
//            });
//            chatRoomPanel.add(removeParticipantButton);
//        }

            new Thread(new Runnable() {
            public void run() {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        String[] participant = message.split(" ");
                        if(participant[0].equals("Kick")) {
                            if (participant[1].equals(currentUser) || participant[1].equals("@")) {
                                chatRoomFrame.dispatchEvent(new WindowEvent(chatRoomFrame, WindowEvent.WINDOW_CLOSING));
                                showMainMenu();
                                break;
                            }
                        }

                        if (!message.startsWith(currentUser + ":")) {
                            chatArea.append(message + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        chatRoomFrame.setVisible(true);
    }

    private static java.util.List<String> loadMessage(String roomName) {
        String queryLoadMessage = "SELECT * FROM pesan WHERE id_room = (SELECT id FROM chat_rooms WHERE name = ?)";

        java.util.List<String> messages = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(queryLoadMessage)) {
            stmt.setString(1, roomName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String message = rs.getString("chat");
                String sender = rs.getString("username");
                messages.add(sender + ": " + message);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }

    private static void createNewMessage(String roomName, String username, String chat) {
        String queryCreateNewMessage = "INSERT INTO pesan (id_room, username, chat) VALUES ((SELECT id FROM chat_rooms WHERE name = ?), ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(queryCreateNewMessage)) {
            stmt.setString(1, roomName);
            stmt.setString(2, username);
            stmt.setString(3, chat);
            boolean rs = stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void deleteChatRoom(String roomName) {
        String queryDeleteParticipants = "DELETE FROM room_participants WHERE room_id = (SELECT id FROM chat_rooms WHERE name = ?)";
        String queryDeleteRoom = "DELETE FROM chat_rooms WHERE name = ? AND room_owner = ?";

        try {
            conn.setAutoCommit(false); // Start a transaction
            try (PreparedStatement stmtDeleteParticipants = conn.prepareStatement(queryDeleteParticipants)) {
                stmtDeleteParticipants.setString(1, roomName);
                stmtDeleteParticipants.executeUpdate();
            }

            try (PreparedStatement stmtDeleteRoom = conn.prepareStatement(queryDeleteRoom)) {
                stmtDeleteRoom.setString(1, roomName);
                stmtDeleteRoom.setString(2, currentUser);
                int rowsAffected = stmtDeleteRoom.executeUpdate();

                if (rowsAffected > 0) {
                    // Notify all clients about the room deletion
//                    ChatServer.broadcastMessage("ROOM_DELETED:" + roomName);
                    JOptionPane.showMessageDialog(null, "Room " + roomName + " deleted successfully.");
                    conn.commit(); // Commit the transaction
                } else {
                    JOptionPane.showMessageDialog(null, "Failed to delete room: room not found or you are not the owner.");
                    conn.rollback(); // Rollback the transaction
                }
            }
        } catch (SQLException e) {
            try {
                conn.rollback(); // Rollback the transaction in case of any exception
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to delete room: " + e.getMessage());
        } finally {
            try {
                conn.setAutoCommit(true); // Reset auto-commit mode
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void removeParticipantFromRoom(String roomName, String participant) {
        String query = "DELETE FROM room_participants WHERE room_id = (SELECT id FROM chat_rooms WHERE name = ?) AND username = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, roomName);
            stmt.setString(2, participant);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // Notify the specific participant about their removal
//                ChatServer.broadcastMessage("PARTICIPANT_REMOVED:" + roomName + ":" + participant);
                JOptionPane.showMessageDialog(null, "Participant " + participant + " removed from room " + roomName + " successfully.");
            } else {
                JOptionPane.showMessageDialog(null, "Failed to remove participant: participant not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to remove participant: " + e.getMessage());
        }
    }

    private static void viewParticipants(String roomName) {
        String query = "SELECT username FROM room_participants " +
                "WHERE room_id = (SELECT id FROM chat_rooms WHERE name = ?)";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, roomName);
            ResultSet rs = stmt.executeQuery();

            StringBuilder participants = new StringBuilder("Participants in " + roomName + ":\n");
            while (rs.next()) {
                participants.append(rs.getString("username")).append("\n");
            }

            // Display participants in a message dialog
            JOptionPane.showMessageDialog(null, participants.toString(), "Participants", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static void leaveChatRoom(String roomName, String username) {
        // Send leave request to the server (this should be implemented on the server-side)
        out.println(username + " has left " + roomName);

        // Remove the current user from the room's participants list in the database
        String query = "DELETE FROM room_participants WHERE room_id = " +
                "(SELECT id FROM chat_rooms WHERE name = ?) AND username = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, roomName);
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Refresh the main menu
        showMainMenu();
    }


}
