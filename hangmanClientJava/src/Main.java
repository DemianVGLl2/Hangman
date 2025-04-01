import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Main {
    private static int imageToShow = 0;
    private static JLabel gameImageLabel;
    private static JLabel playerWinLabel = new JLabel(new ImageIcon(Main.class.getResource("/pearto.jpg")));
    private static JLabel playerLoseLabel = new JLabel(new ImageIcon(Main.class.getResource("/butterDog.jpg")));
    private static ImageIcon[] images;
    private static boolean isPlayerTurn = false;
    private static boolean gameOver = false;
    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;
    private static JPanel userAuthPanel;
    private static JPanel winPanel;
    private static JPanel losePanel;
    private static JTextField usernameField;
    private static JTextField passwordField;

    public static void main(String[] args) {
        // Initialize UI components (same as before)
        images = new ImageIcon[]{
                new ImageIcon(Main.class.getResource("/pearto.jpg")),
                new ImageIcon(Main.class.getResource("/butterDog.jpg")),
                new ImageIcon(Main.class.getResource("/jinx.jpg")),
                new ImageIcon(Main.class.getResource("/pearto.jpg"))
        };

        String host, port;
        Object[] options = {"Forfeit", "Send letter"};
        JTextField hostField = new JTextField(10);
        JTextField portField = new JTextField(5);


        JPanel connectionPanel = new JPanel();
        connectionPanel.add(new JLabel("Host:"));
        connectionPanel.add(hostField);
        connectionPanel.add(Box.createHorizontalStrut(15));
        connectionPanel.add(new JLabel("Port:"));
        connectionPanel.add(portField);


        usernameField = new JTextField(10);
        passwordField = new JTextField(10);


        userAuthPanel = new JPanel();
        userAuthPanel.add(new JLabel("Username:"));
        userAuthPanel.add(usernameField);
        userAuthPanel.add(new JLabel("Password:"));
        userAuthPanel.add(passwordField);


        JTextField guessField = new JTextField(5);

        gameImageLabel = new JLabel(images[0]);
        gameImageLabel.setPreferredSize(new Dimension(500, 800));

        JPanel guessPanel = new JPanel();
        guessPanel.setLayout(new BoxLayout(guessPanel, BoxLayout.Y_AXIS));
        guessPanel.add(gameImageLabel);
        guessPanel.add(new JLabel("Guess a letter:"));
        guessPanel.add(guessField);

        winPanel = new JPanel();
        winPanel.setLayout(new BoxLayout(winPanel, BoxLayout.Y_AXIS));
        winPanel.add(playerWinLabel);

        losePanel = new JPanel();
        losePanel.setLayout(new BoxLayout(losePanel, BoxLayout.Y_AXIS));
        losePanel.add(playerLoseLabel);

        do {
            int result = JOptionPane.showConfirmDialog(null, connectionPanel,
                    "Please Enter Host and Port Values", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION && hostField.getText() != null && portField.getText() != null) {
                try {
                    // Establish connection
                    socket = new Socket(hostField.getText(), Integer.parseInt(portField.getText()));
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    // Start game loop
                    gameLoop();

                } catch (IOException error) {
                    JOptionPane.showMessageDialog(null, "Connection error: " + error.getMessage());
                } finally {
                    closeConnection();
                }
            }
        } while (hostField.getText() != null && portField.getText() != null);
    }

    private static void gameLoop() {
        try {
            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                String[] parts = serverResponse.split("\\.");

                // Handle different server commands
                switch (parts[0]) {
                    case "1": // Authentication successful
                        isPlayerTurn = true;
                        handleGameState(parts);
                        break;

                    case "2": // Game state update
                        handleGameState(parts);
                        break;

                    case "3": // Game over
                        gameOver = true;
                        showGameOver(parts[2].equals("0"));
                        return;

                    case "4": // Turn update
                        isPlayerTurn = parts[1].equals("1");
                        break;

                    case "5": // Authentication required
                        handleAuthentication();
                        break;

                    case "6": // Word input required
                        handleWordInput();
                        break;
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Connection lost: " + e.getMessage());
        }
    }

    private static void handleGameState(String[] parts) {
        if (parts.length > 2 && parts[2].equals("1")) {
            changeImage();
        }

        if (isPlayerTurn && imageToShow < 4) {
            String guess = JOptionPane.showInputDialog(null,
                    "Enter your guess (Attempt " + (imageToShow + 1) + " of 4)");
            if (guess != null && !guess.isEmpty()) {
                out.println(guess); // Send guess to server
            }
        }
    }

    private static void handleAuthentication() {
        int auth = JOptionPane.showConfirmDialog(null, userAuthPanel,
                "Please write your user and password", JOptionPane.OK_CANCEL_OPTION);
        if (auth == JOptionPane.OK_OPTION) {
            out.println(usernameField.getText() + ":" + passwordField.getText());
        }
    }

    private static void handleWordInput() {
        String word = JOptionPane.showInputDialog(null,
                "It's your turn to add a word", "Word Input", JOptionPane.PLAIN_MESSAGE);
        if (word != null && !word.isEmpty()) {
            out.println(word);
        }
    }

    private static void showGameOver(boolean won) {
        if (won) {
            JOptionPane.showMessageDialog(null, winPanel, "You won! yay!", JOptionPane.PLAIN_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, losePanel, "You lose!", JOptionPane.PLAIN_MESSAGE);
        }
    }

    private static void changeImage() {
        imageToShow++;
        int imageIndex = imageToShow % images.length;
        gameImageLabel.setIcon(images[imageIndex]);
        gameImageLabel.revalidate();
        gameImageLabel.repaint();
    }

    private static void closeConnection() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}