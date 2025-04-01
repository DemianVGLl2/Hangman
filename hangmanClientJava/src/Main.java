import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static boolean gameActive = false;

    // Referencias a las ventanas activas para poder cerrarlas
    private static JFrame mainFrame = null;
    private static JDialog activeGameDialog = null;
    private static JOptionPane activeOptionPane = null;
    
    // Configuración de imágenes con escalado
    static {
        String[] imagePaths = {"/image0.jpg", "/image1.jpg", "/image2.jpg", "/image3.jpg", "/image4.jpg", "/image5.jpg", "/image6.jpg"};
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
                // Crear el marco principal de la aplicación
                mainFrame = new JFrame("Hangman Game");
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                mainFrame.setSize(400, 500);
                mainFrame.setLocationRelativeTo(null);
                
                // Panel inicial con mensaje de carga
                JPanel initialPanel = new JPanel(new BorderLayout());
                initialPanel.add(new JLabel("Conectando al servidor...", SwingConstants.CENTER), BorderLayout.CENTER);
                mainFrame.setContentPane(initialPanel);
                mainFrame.setVisible(true);
                
                setupNetworkConnection();
                if(handleLogin()){
                    handleCreateUser();
                }
                handleAuthentication();
                // Ahora solo esperamos a que el servidor asigne el rol
            } catch (Exception e) {
                JOptionPane.showMessageDialog(mainFrame, "Error: " + e.getMessage());
                System.exit(1);
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

        int result = JOptionPane.showConfirmDialog(mainFrame, connectionPanel,
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

        int result = JOptionPane.showConfirmDialog(mainFrame, authPanel,
                "Autenticación", JOptionPane.OK_CANCEL_OPTION);
        
        if (result != JOptionPane.OK_OPTION) System.exit(0);
        
        String credentials = "1." + userField.getText() + "." + new String(passField.getPassword());
        sendMessage(credentials);
        
        // Mostrar panel de espera
        JPanel waitPanel = new JPanel(new BorderLayout());
        waitPanel.add(new JLabel("Esperando validación del servidor...", SwingConstants.CENTER), BorderLayout.CENTER);
        mainFrame.setContentPane(waitPanel);
        mainFrame.revalidate();
        mainFrame.repaint();
    }

    private static void handleCreateUser() {
        JPanel authPanel = new JPanel();
        JTextField userField = new JTextField(10);
        JPasswordField passField = new JPasswordField(10);
        
        authPanel.add(new JLabel("Nuevo usuario:"));
        authPanel.add(userField);
        authPanel.add(new JLabel("Nueva contraseña:"));
        authPanel.add(passField);

        int result = JOptionPane.showConfirmDialog(mainFrame, authPanel,
                "Crear Usuario", JOptionPane.OK_CANCEL_OPTION);
        
        if (result != JOptionPane.OK_OPTION) System.exit(0);
        
        String credentials = "2." + userField.getText() + "." + new String(passField.getPassword());
        sendMessage(credentials);
    }

    private static boolean handleLogin() {
        JDialog dialog = new JDialog(mainFrame, "Iniciar sesión", true);
        JPanel authPanel = new JPanel();
        JButton btnLogIn = new JButton("Log In");
        JButton btnCreateUser = new JButton("Crear Usuario");

        AtomicBoolean createUser = new AtomicBoolean(false);

        btnCreateUser.addActionListener(e -> {
            createUser.set(true);
            dialog.dispose();
        });

        btnLogIn.addActionListener(e -> {
            createUser.set(false);
            dialog.dispose();
        });

        authPanel.add(btnLogIn);
        authPanel.add(btnCreateUser);

        dialog.setContentPane(authPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);

        return createUser.get();
    }

    private static void handleGameFlow() {
        gameActive = true;
        
        if (playerRole == 1) {
            String word = JOptionPane.showInputDialog(mainFrame, "Escribe la palabra secreta:");
            if (word != null && !word.trim().isEmpty()) {
                sendMessage("3." + word);
                showGameMonitor();
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Debes ingresar una palabra válida.");
                handleGameFlow(); // Volver a solicitar la palabra
            }
        } else if (playerRole == 2) {
            showGuessInterface();
        } else {
            JOptionPane.showMessageDialog(mainFrame, "Error: Rol no válido asignado por el servidor.");
        }
    }

    private static void showGuessInterface() {
        JPanel guessPanel = new JPanel();
        guessPanel.setLayout(new BoxLayout(guessPanel, BoxLayout.Y_AXIS));
        
        // Inicializar la etiqueta de imagen si aún no existe
        if (gameImageLabel == null) {
            gameImageLabel = new JLabel(images[0]);
            gameImageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        
        // Panel para la imagen y la palabra
        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.Y_AXIS));
        displayPanel.add(Box.createVerticalStrut(10));
        displayPanel.add(gameImageLabel);
        displayPanel.add(Box.createVerticalStrut(10));
        
        // Asegurar que el label de la palabra tiene un tamaño adecuado
        wordDisplayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        wordDisplayLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        displayPanel.add(wordDisplayLabel);
        displayPanel.add(Box.createVerticalStrut(20));
        
        // Campo para adivinar letras
        JPanel letterPanel = new JPanel();
        JTextField letterField = new JTextField(3);
        JButton letterButton = new JButton("Adivinar Letra");
        
        letterButton.addActionListener(e -> {
            String letter = letterField.getText().trim();
            if (!letter.isEmpty()) {
                sendMessage("4." + letter.charAt(0));
                letterField.setText("");
            }
        });
        
        letterPanel.add(new JLabel("Letra: "));
        letterPanel.add(letterField);
        letterPanel.add(letterButton);
        
        // Campo para adivinar palabra completa
        JPanel wordPanel = new JPanel();
        JTextField fullWordField = new JTextField(15);
        JButton fullWordButton = new JButton("Adivinar Palabra");
        
        fullWordButton.addActionListener(e -> {
            String fullWord = fullWordField.getText().trim();
            if (!fullWord.isEmpty()) {
                sendMessage("5." + fullWord);
                fullWordField.setText("");
            }
        });
        
        wordPanel.add(new JLabel("Palabra: "));
        wordPanel.add(fullWordField);
        wordPanel.add(fullWordButton);
        
        // Botón para salir del juego
        JPanel forfeitPanel = new JPanel();
        JButton forfeitButton = new JButton("Abandonar Juego");
        
        forfeitButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(mainFrame, 
                "¿Estás seguro de que quieres abandonar el juego?", 
                "Confirmar", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                sendMessage("close");
                JOptionPane.showMessageDialog(mainFrame, "Has abandonado el juego.");
                System.exit(0);
            }
        });
        
        forfeitPanel.add(forfeitButton);
        
        // Agregar todos los paneles al panel principal
        guessPanel.add(displayPanel);
        guessPanel.add(Box.createVerticalStrut(20));
        guessPanel.add(letterPanel);
        guessPanel.add(Box.createVerticalStrut(10));
        guessPanel.add(wordPanel);
        guessPanel.add(Box.createVerticalStrut(20));
        guessPanel.add(forfeitPanel);
        
        // Actualizar el contenido del marco principal
        mainFrame.setContentPane(guessPanel);
        mainFrame.setTitle("Hangman - Jugador Adivinando");
        mainFrame.revalidate();
        mainFrame.repaint();
    }
    
    private static void showGameMonitor() {
        JPanel monitorPanel = new JPanel();
        monitorPanel.setLayout(new BoxLayout(monitorPanel, BoxLayout.Y_AXIS));
        
        JLabel statusLabel = new JLabel("Estado del Juego:", SwingConstants.CENTER);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
        // Asegurar que el label de la palabra tiene un tamaño adecuado
        wordDisplayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        wordDisplayLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        
        // Botón para salir del juego
        JPanel forfeitPanel = new JPanel();
        JButton forfeitButton = new JButton("Abandonar Juego");
        
        forfeitButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(mainFrame, 
                "¿Estás seguro de que quieres abandonar el juego?", 
                "Confirmar", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                sendMessage("close");
                JOptionPane.showMessageDialog(mainFrame, "Has abandonado el juego.");
                System.exit(0);
            }
        });
        
        forfeitPanel.add(forfeitButton);
        
        monitorPanel.add(Box.createVerticalStrut(20));
        monitorPanel.add(statusLabel);
        monitorPanel.add(Box.createVerticalStrut(20));
        monitorPanel.add(wordDisplayLabel);
        monitorPanel.add(Box.createVerticalStrut(40));
        monitorPanel.add(forfeitPanel);
        
        // Actualizar el contenido del marco principal
        mainFrame.setContentPane(monitorPanel);
        mainFrame.setTitle("Hangman - Monitor de Juego");
        mainFrame.revalidate();
        mainFrame.repaint();
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
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(mainFrame, "Conexión perdida con el servidor");
                System.exit(1);
            });
        }
    }

    private static void processServerMessage(String msg) {
        System.out.println("Server: " + msg);
        
        if (msg.startsWith("login.ok.role")) {
            try {
                String[] parts = msg.split("\\.");
                playerRole = Integer.parseInt(parts[3]);
                // Actualizar la interfaz cuando recibamos word.set o word.ok
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(mainFrame, "Error procesando el mensaje de login: " + e.getMessage())
                );
            }
        } 
        else if (msg.startsWith("game.start")) {
            SwingUtilities.invokeLater(() -> handleGameFlow());
        }
        else if (msg.startsWith("word.ok")) {
            // Para el jugador 1, después de que su palabra fue aceptada
            // No es necesario hacer nada más aquí, ya que ya se mostró la interfaz de monitor
        }
        else if (msg.startsWith("word.set")) {
            String[] parts = msg.split("\\.");
            SwingUtilities.invokeLater(() -> {
                wordDisplayLabel.setText(parts[2]);
                // Si aún no se ha mostrado la interfaz, mostrarla ahora
                if (!gameActive && playerRole == 2) {
                    handleGameFlow();
                }
            });
        }
        else if (msg.startsWith("progress")) {
            String[] parts = msg.split("\\.");
            SwingUtilities.invokeLater(() -> {
                wordDisplayLabel.setText(parts[1]);
            });
        }
        else if (msg.startsWith("guess.")) {
            String[] parts = msg.split("\\.");
            SwingUtilities.invokeLater(() -> {
                if (parts[1].equals("win") || parts[1].equals("lose")) {
                    wordDisplayLabel.setText(parts[2]); // Mostrar palabra completa
                    showGameResult(parts[1].equals("win"));
                } else {
                    wordDisplayLabel.setText(parts[3]); // Actualizar progreso
                    if (parts[2].equals("0")) { // Intento incorrecto
                        updateGameImage(false);
                    }
                }
            });
        }
        else if (msg.startsWith("game.over")) {
            attempts = 0;
            updateGameImage(true);
            boolean won = msg.contains("win");
            showGameResult(won);
        }
        else if (msg.startsWith("restart.ok") || msg.startsWith("game.restart")) {
            SwingUtilities.invokeLater(() -> {
                attempts = 0;
                updateGameImage(true);
                wordDisplayLabel.setText(""); // Limpiar la palabra mostrada
                gameActive = false;
                
                JOptionPane.showMessageDialog(mainFrame, "El juego se reinició. Iniciando nueva ronda.");
                
                // Reiniciar el flujo del juego directamente
                handleGameFlow();
            });
        }
        else if (msg.startsWith("restart.fail")) {
            SwingUtilities.invokeLater(() -> 
                JOptionPane.showMessageDialog(mainFrame, "No se pudo reiniciar el juego.")
            );
        }
        else if (msg.startsWith("error.format_incorrect")) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(mainFrame, "Error: Formato incorrecto. Por favor, revisa la entrada.");
                handleAuthentication();
            });
        }
        else if (msg.startsWith("login.fail.invalid_credentials")) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(mainFrame, "Error: Credenciales inválidas. Inténtalo de nuevo.");
                handleAuthentication();
            });
        }
        else if (msg.startsWith("register.fail.user_exists")) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(mainFrame, "Error: El usuario ya existe. Por favor, intenta iniciar sesión.");
                handleAuthentication();
            });
        }
        else if (msg.startsWith("login.fail.server_full")) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(mainFrame, "Error: El servidor está lleno. Inténtalo más tarde.");
                System.exit(0);
            });
        }
    }

    private static void showGameResult(boolean won) {
        SwingUtilities.invokeLater(() -> {
            JPanel resultPanel = new JPanel();
            resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
            
            JLabel resultLabel = new JLabel(won ? "¡Ganaste!" : "¡Perdiste!");
            resultLabel.setFont(new Font("Arial", Font.BOLD, 18));
            resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            JButton restartButton = new JButton("Reiniciar");
            restartButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            restartButton.addActionListener(e -> sendMessage("6"));
            
            resultPanel.add(Box.createVerticalStrut(20));
            resultPanel.add(resultLabel);
            resultPanel.add(Box.createVerticalStrut(20));
            resultPanel.add(restartButton);
            resultPanel.add(Box.createVerticalStrut(20));
            
            // Usar el mainFrame en lugar de un diálogo modal
            mainFrame.setContentPane(resultPanel);
            mainFrame.setTitle("Fin del Juego");
            mainFrame.revalidate();
            mainFrame.repaint();
        });
    }

    private static synchronized void sendMessage(String msg) {
        try {
            out.write(msg.getBytes());
            out.flush();
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> 
                JOptionPane.showMessageDialog(mainFrame, "Error enviando mensaje: " + e.getMessage())
            );
        }
    }

    private static void updateGameImage(boolean reset) {
        SwingUtilities.invokeLater(() -> {
            if (gameImageLabel == null) {
                gameImageLabel = new JLabel(images[0]);
            }
            if (reset) {
                attempts = 0;
            } else {
                attempts++;
            }
            System.out.println("Actual attempts: " + attempts);
            if (attempts >= images.length - 1) {
                gameImageLabel.setIcon(images[images.length - 1]);
            } else {
                gameImageLabel.setIcon(images[attempts]);
            }
        });
    }
}