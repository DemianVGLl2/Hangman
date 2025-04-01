import javax.swing.*;
import java.awt.*;

import java.io.*;
import java.net.*;

import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static int imageToShow = 0;
    private static JLabel gameImageLabel;
    private static JLabel wordLabel =  new JLabel("_ _ _ _ _");
    private static JLabel statusLabel =  new JLabel("Waiting for opponent...");
    private static JLabel playerWinLabel = new JLabel(new ImageIcon(Main.class.getResource("/pearto.jpg")));
    private static JLabel playerLoseLabel = new JLabel(new ImageIcon(Main.class.getResource("/butterDog.jpg")));
    private static ImageIcon[] images;
    private static int playerRole = -1;
    private static Socket socket;
    private static OutputStream out;
    private static InputStream in;
    private static AtomicBoolean running = new AtomicBoolean(true);
    private static JFrame gameFrame;
    private static JTextField guessField;

    public static void main(String[] args) {

        images = new ImageIcon[]{
                new ImageIcon(Main.class.getResource("/pearto.jpg")),
                new ImageIcon(Main.class.getResource("/butterDog.jpg")),
                new ImageIcon(Main.class.getResource("/jinx.jpg")),
                new ImageIcon(Main.class.getResource("/pearto.jpg")),
        };

        //Connection panel
        JTextField hostField = new JTextField("127.0.0.1", 10);
        JTextField portField = new JTextField("5000", 5);

        JPanel connectionPanel = new JPanel();
        connectionPanel.add(new JLabel("Host:"));
        connectionPanel.add(hostField);
        connectionPanel.add(Box.createHorizontalStrut(15));
        connectionPanel.add(new JLabel("Port:"));
        connectionPanel.add(portField);

        int result = JOptionPane.showConfirmDialog(null, connectionPanel, "Please Enter Host and Port", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.CANCEL_OPTION) {
            return; //Canceló
        }

        String host =  hostField.getText();
        int port = Integer.parseInt(portField.getText());

        try {
            //Conectar al server
            socket = new Socket(host, port);
            out = socket.getOutputStream();
            in = socket.getInputStream();

            //Inicia listener del thread para no perder ningún mensaje
            Thread listenerThread = new Thread(() -> listenForServerMessages());
            listenerThread.setDaemon(true);
            listenerThread.start();

            showLoginDialog();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void showLoginDialog() {
        JTextField usernameField = new JTextField(10);
        JPasswordField passwordField = new JPasswordField(10);

        JPanel userAuthPanel = new JPanel(new GridLayout(0, 2));
        userAuthPanel.add(new JLabel("Username:"));
        userAuthPanel.add(usernameField);
        userAuthPanel.add(new JLabel("Password:"));
        userAuthPanel.add(passwordField);

        JPanel loginPanel = new JPanel(new BorderLayout());
        loginPanel.add(userAuthPanel, BorderLayout.CENTER);

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        loginPanel.add(buttonPanel, BorderLayout.SOUTH);

        JDialog loginDialog = new JDialog((Frame)null, "Login", true);
        loginDialog.setContentPane(loginPanel);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog, "Please enter both username and password");
                return;
            }

            try {
                sendMessage("1." + username + "." + password);
                // Response will be handled by the listener thread
                loginDialog.dispose();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(loginDialog, "Error sending login request: " + ex.getMessage());
            }
        });

        registerButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(loginDialog, "Please enter both username and password");
                return;
            }

            try {
                sendMessage("2." + username + "." + password);
                // Response will be handled by the listener thread
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(loginDialog, "Error sending register request: " + ex.getMessage());
            }
        });

        loginDialog.pack();
        loginDialog.setLocationRelativeTo(null);
        loginDialog.setVisible(true);
    }

    private static void listenForServerMessages() {
        byte[] buffer = new byte[1024];

        try {
            while (running.get()) {
                // Use a larger buffer for receiving multiple messages
                int bytesRead = in.read(buffer);
                if (bytesRead == -1) {
                    // Connection closed by server
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, "Server closed the connection",
                                "Connection Closed", JOptionPane.WARNING_MESSAGE);
                        closeConnection();
                    });
                    break;
                }

                final String response = new String(buffer, 0, bytesRead);
                System.out.println("Server: " + response);

                // Process server response on the EDT
                SwingUtilities.invokeLater(() -> processServerResponse(response));
            }
        } catch (IOException e) {
            if (running.get()) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "Error reading from server: " + e.getMessage(),
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                    closeConnection();
                });
            }
        }
    }

    private static void processServerResponse(String response) {
        String[] parts = response.split("\\.");

        if (response.startsWith("login.ok.role")) {
            try {
                // Extraer correctamente el número de rol
                // El formato es "login.ok.role.X" donde X es 1 o 2
                playerRole = Integer.parseInt(parts[3]);
                System.out.println("You are player " + playerRole);
                createGameUI();

                if (playerRole == 1) {
                    statusLabel.setText("Waiting for player 2 to join...");
                } else {
                    statusLabel.setText("Waiting for player 1 to set a word...");
                }
            } catch (Exception e) {
                System.err.println("Error parsing role from: " + response);
                e.printStackTrace();
                playerRole = -1; // Set to invalid role
                JOptionPane.showMessageDialog(null, "Error understanding server response",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (response.startsWith("login.fail")) {
            JOptionPane.showMessageDialog(null, "Login failed: " + response.substring(11),
                    "Login Error", JOptionPane.ERROR_MESSAGE);
            showLoginDialog();
        } else if (response.startsWith("register.ok")) {
            JOptionPane.showMessageDialog(null, "Registration successful. You can now log in.");
            showLoginDialog();
        } else if (response.startsWith("register.fail")) {
            JOptionPane.showMessageDialog(null, "Registration failed: " + response.substring(14),
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
        } else if (response.startsWith("game.start")) {
            if (playerRole == 1) {
                String word = JOptionPane.showInputDialog(gameFrame, "Enter a word for player 2 to guess:",
                        "Set Word", JOptionPane.QUESTION_MESSAGE);
                if (word != null && !word.isEmpty()) {
                    try {
                        sendMessage("3." + word);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (response.startsWith("word.set")) {
            String maskedWord = parts[2];
            wordLabel.setText(formatWord(maskedWord));
            statusLabel.setText("Guess a letter");
            // Reset the hangman image
            imageToShow = 0;
            changeImage(false);
        } else if (response.startsWith("guess")) {
            if (parts.length >= 4) {
                String result = parts[2]; // 0 for incorrect, 1 for correct
                String maskedWord = parts[3];

                wordLabel.setText(formatWord(maskedWord));

                if (result.equals("0")) {
                    changeImage(true); // Incorrect guess, update hangman
                    statusLabel.setText("Incorrect guess: " + parts[1]);
                } else {
                    statusLabel.setText("Correct guess: " + parts[1]);
                }
            } else if (response.startsWith("guess.win")) {
                wordLabel.setText(formatWord(parts[2]));
                statusLabel.setText("You won! The word was: " + parts[2]);
                JOptionPane.showMessageDialog(gameFrame, playerWinLabel, "You won!", JOptionPane.INFORMATION_MESSAGE);
            } else if (response.startsWith("guess.lose")) {
                wordLabel.setText(formatWord(parts[2]));
                statusLabel.setText("You lost! The word was: " + parts[2]);
                JOptionPane.showMessageDialog(gameFrame, playerLoseLabel, "You lost!", JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (response.startsWith("progress")) {
            // Update for player 1 when player 2 makes a guess
            String maskedWord = parts[1];
            char letter = parts[2].charAt(0);

            wordLabel.setText(formatWord(maskedWord));
            statusLabel.setText("Player 2 guessed: " + letter);
        } else if (response.startsWith("game.over")) {
            if (response.contains("win")) {
                statusLabel.setText("Player 2 guessed the word!");
                JOptionPane.showMessageDialog(gameFrame, "Player 2 guessed your word!",
                        "Game Over", JOptionPane.INFORMATION_MESSAGE);
            } else {
                statusLabel.setText("Player 2 ran out of attempts!");
                JOptionPane.showMessageDialog(gameFrame, "Player 2 failed to guess your word!",
                        "Game Over", JOptionPane.INFORMATION_MESSAGE);
            }

            // Ask if player 1 wants to play again
            if (playerRole == 1) {
                int restart = JOptionPane.showConfirmDialog(gameFrame, "Start a new game?",
                        "Play Again", JOptionPane.YES_NO_OPTION);
                if (restart == JOptionPane.YES_OPTION) {
                    try {
                        sendMessage("5");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (response.startsWith("game.restart")) {
            statusLabel.setText("Game restarted");
            wordLabel.setText("_ _ _ _ _");

            if (playerRole == 1) {
                String word = JOptionPane.showInputDialog(gameFrame, "Enter a new word for player 2 to guess:",
                        "Set Word", JOptionPane.QUESTION_MESSAGE);
                if (word != null && !word.isEmpty()) {
                    try {
                        sendMessage("3." + word);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                statusLabel.setText("Waiting for player 1 to set a word...");
            }
        } else if (response.startsWith("opponent.disconnected")) {
            JOptionPane.showMessageDialog(gameFrame, "Your opponent has disconnected.",
                    "Opponent Left", JOptionPane.WARNING_MESSAGE);
            statusLabel.setText("Waiting for opponent to reconnect...");
        }
    }

    private static void createGameUI() {
        gameFrame = new JFrame("Hangman Game");
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Game info panel (top)
        JPanel infoPanel = new JPanel(new BorderLayout());
        wordLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        wordLabel.setHorizontalAlignment(JLabel.CENTER);
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        infoPanel.add(wordLabel, BorderLayout.NORTH);
        infoPanel.add(statusLabel, BorderLayout.SOUTH);

        // Hangman image (center)
        gameImageLabel = new JLabel(images[0]);
        gameImageLabel.setHorizontalAlignment(JLabel.CENTER);
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.add(gameImageLabel, BorderLayout.CENTER);

        // Controls panel (bottom)
        JPanel controlPanel = new JPanel();
        guessField = new JTextField(5);
        JButton guessButton = new JButton("Guess");
        JButton forfeitButton = new JButton("Forfeit");

        guessButton.addActionListener(e -> {
            String guess = guessField.getText().trim();
            if (!guess.isEmpty() && playerRole == 2) {
                try {
                    // Only send the first character as the guess
                    sendMessage("4." + guess.charAt(0));
                    guessField.setText("");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        forfeitButton.addActionListener(e -> {
            try {
                sendMessage("close");
                closeConnection();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        controlPanel.add(new JLabel("Guess: "));
        controlPanel.add(guessField);
        controlPanel.add(guessButton);
        controlPanel.add(forfeitButton);

        // Add panels to main panel
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(imagePanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        // Set up frame
        gameFrame.setContentPane(mainPanel);
        gameFrame.setSize(400, 500);
        gameFrame.setLocationRelativeTo(null);
        gameFrame.setVisible(true);

        // Add window listener to handle closing
        gameFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    sendMessage("close");
                } catch (IOException e) {
                    // Ignore, we're closing anyway
                }
                closeConnection();
            }
        });
    }

    private static String formatWord(String word) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            formatted.append(word.charAt(i));
            if (i < word.length() - 1) {
                formatted.append(" ");
            }
        }
        return formatted.toString();
    }

    private static void sendMessage(String message) throws IOException {
        System.out.println("Sending: " + message);
        out.write(message.getBytes());
        out.flush();
    }

    private static void changeImage(boolean incorrectGuess) {
        if (incorrectGuess) {
            imageToShow++;
        }

        int imageIndex = imageToShow % images.length;
        gameImageLabel.setIcon(images[imageIndex]);

        gameImageLabel.revalidate();
        gameImageLabel.repaint();
    }

    private static void closeConnection() {
        running.set(false);
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}