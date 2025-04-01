import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Main {
    private static int attempts = 0;
    private static JLabel gameImageLabel;
    private static JLabel wordDisplayLabel = new JLabel("", SwingConstants.CENTER);
    private static ImageIcon[] images;
    private static Socket socket;
    private static OutputStream out;
    private static InputStream in;
    private static int playerRole = -1;
    private static final Object[] options = {"Forfeit"};
    
    // Configuración de imágenes con escalado
    static {
        String[] imagePaths = {"/pearto.jpg", "/butterDog.jpg", "/jinx.jpg", "/pearto.jpg"};
        images = new ImageIcon[imagePaths.length];
        
        for (int i = 0; i < imagePaths.length; i++) {
            ImageIcon original = new ImageIcon(Main.class.getResource(imagePaths[i]));
            Image scaled = original.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH);
            images[i] = new ImageIcon(scaled);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                setupNetworkConnection();
                handleAuthentication();
                handleGameFlow();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            }
        });
    }

    private static void setupNetworkConnection() throws IOException {
        JPanel connectionPanel = new JPanel();
        JTextField hostField = new JTextField("127.0.0.1", 10);
        JTextField portField = new JTextField("5000", 5);
        
        connectionPanel.add(new JLabel("Host:"));
        connectionPanel.add(hostField);
        connectionPanel.add(Box.createHorizontalStrut(15));
        connectionPanel.add(new JLabel("Port:"));
        connectionPanel.add(portField);

        int result = JOptionPane.showConfirmDialog(null, connectionPanel,
                "Conexión al Servidor", JOptionPane.OK_CANCEL_OPTION);
        
        if (result != JOptionPane.OK_OPTION) System.exit(0);
        
        socket = new Socket(hostField.getText(), Integer.parseInt(portField.getText()));
        out = socket.getOutputStream();
        in = socket.getInputStream();
        
        new Thread(Main::listenToServer).start();
    }

    private static void handleAuthentication() {
        JPanel authPanel = new JPanel();
        JTextField userField = new JTextField(10);
        JPasswordField passField = new JPasswordField(10);
        
        authPanel.add(new JLabel("Usuario:"));
        authPanel.add(userField);
        authPanel.add(new JLabel("Contraseña:"));
        authPanel.add(passField);

        int result = JOptionPane.showConfirmDialog(null, authPanel,
                "Autenticación", JOptionPane.OK_CANCEL_OPTION);
        
        if (result != JOptionPane.OK_OPTION) System.exit(0);
        
        String credentials = "1." + userField.getText() + "." + new String(passField.getPassword());
        sendMessage(credentials);
    }

    private static void handleGameFlow() {
        // Esperar asignación de rol del servidor
        while (playerRole == -1) Thread.onSpinWait();
        
        if (playerRole == 1) {
            String word = JOptionPane.showInputDialog("Escribe la palabra secreta:");
            sendMessage("3." + word);
            showGameMonitor();
        } else {
            showGuessInterface();
        }
    }

    private static void showGuessInterface() {
        JPanel guessPanel = new JPanel();
        guessPanel.setLayout(new BoxLayout(guessPanel, BoxLayout.Y_AXIS));
        
        gameImageLabel = new JLabel(images[0]);
        gameImageLabel.setPreferredSize(new Dimension(300, 300));
        
        // Campo para la letra
        JTextField letterField = new JTextField(1);
        JButton guessButton = new JButton("Adivinar");

        // Campo para palabra completa
        JTextField fullWordField = new JTextField(10);
        JButton fullWordButton = new JButton("Adivinar palabra");
        
        guessButton.addActionListener(e -> {
            sendMessage("4." + letterField.getText().trim());
            letterField.setText("");
        });

        fullWordButton.addActionListener(e -> {
            sendMessage("4." + fullWordField.getText().trim()); // Comando hipotético para palabra
            fullWordField.setText("");
        });

        guessPanel.add(gameImageLabel);
        guessPanel.add(wordDisplayLabel);

        JPanel letterPanel = new JPanel();
        letterPanel.add(new JLabel("Letra:"));
        letterPanel.add(letterField);
        letterPanel.add(guessButton);
        
        JPanel wordPanel = new JPanel();
        wordPanel.add(new JLabel("Palabra completa:"));
        wordPanel.add(fullWordField);
        wordPanel.add(fullWordButton);
        
        guessPanel.add(letterPanel);
        guessPanel.add(wordPanel);

        JOptionPane.showOptionDialog(null, guessPanel, "Adivina la Palabra",
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
    }

    private static void showGameMonitor() {
        // Interfaz para el jugador que estableció la palabra
        JPanel monitorPanel = new JPanel();
        monitorPanel.add(new JLabel("Estado del Juego:"));
        monitorPanel.add(wordDisplayLabel);
        
        JOptionPane.showOptionDialog(null, monitorPanel, "Monitor del Juego",
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, 
                null, options, options[0]);
    }

    private static void listenToServer() {
        byte[] buffer = new byte[1024];
        try {
            while (true) {
                int bytesRead = in.read(buffer);
                if(bytesRead == -1) break;
                
                String response = new String(buffer, 0, bytesRead);
                processServerMessage(response);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Conexión perdida con el servidor");
        }
    }

    private static void processServerMessage(String msg) {
        System.out.println("Server: " + msg);
        
        if (msg.startsWith("login.ok.role")) {
            playerRole = Integer.parseInt(msg.split("\\.")[3]);
        } 
        else if (msg.startsWith("progress") || msg.startsWith("guess.") || msg.startsWith("word.set")) {
            handleGameProgress(msg);
        }
        else if (msg.startsWith("game.over")) {
            attempts = 0;
            updateGameImage(true);
        }
        else if (msg.startsWith("error")) {
            JOptionPane.showMessageDialog(null, msg);
        }
    }

    private static void handleGameProgress(String msg) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = msg.split("\\.");
            
            if (msg.startsWith("word.set")) { // Mensaje inicial para el jugador 2
                wordDisplayLabel.setText(parts[2]);
            }
            else if (msg.startsWith("progress")) { // Para el jugador 1
                wordDisplayLabel.setText(parts[1]);
            }
            else if (msg.startsWith("guess.")) { // Para el jugador 2
                if (parts[1].equals("win") || parts[1].equals("lose")) {
                    wordDisplayLabel.setText(parts[2]); // Mostrar palabra completa
                    showGameResult(parts[1].equals("win"));
                } else {
                    wordDisplayLabel.setText(parts[3]); // Actualizar progreso
                    if (parts[2].equals("0")) { // Intento incorrecto
                        attempts++;
                        updateGameImage(false);
                    }
                }
            }
        });
    }

    private static void showGameResult(boolean won) {
        SwingUtilities.invokeLater(() -> {
            JPanel resultPanel = new JPanel();
            resultPanel.add(new JLabel(won ? "¡Ganaste!" : "¡Perdiste!"));
            
            JButton restartButton = new JButton("Reiniciar");
            restartButton.addActionListener(e -> sendMessage("5"));
            
            resultPanel.add(restartButton);
            JOptionPane.showMessageDialog(null, resultPanel);
        });
    }

    private static synchronized void sendMessage(String msg) {
        try {
            out.write(msg.getBytes());
            out.flush();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error enviando mensaje");
        }
    }

    private static void updateGameImage(boolean reset) {
        SwingUtilities.invokeLater(() -> {
            if (reset) attempts = 0;
            else attempts++;
            
            gameImageLabel.setIcon(images[attempts % images.length]);
        });
    }
}