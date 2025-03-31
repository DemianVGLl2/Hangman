import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ImageIcon;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import java.awt.*;

import java.io.*;
import java.net.*;

public class Main {
    private static int imageToShow = 0;
    private static JLabel gameImageLabel;
    private static JLabel playerWinLabel = new JLabel(new ImageIcon(Main.class.getResource("/pearto.jpg")));
    private static JLabel playerLoseLabel = new JLabel(new ImageIcon(Main.class.getResource("/butterDog.jpg")));
    private static ImageIcon[] images;

    public static void main(String[] args) {

        images = new ImageIcon[]{
                new ImageIcon(Main.class.getResource("/pearto.jpg")),
                new ImageIcon(Main.class.getResource("/butterDog.jpg")),
                new ImageIcon(Main.class.getResource("/jinx.jpg")),
                new ImageIcon(Main.class.getResource("/pearto.jpg")),
        };

        String host, port;
        Object[] options = {"Forfeit"};
        //Text field para pedir host y puerto
        JTextField hostField = new JTextField(10);
        JTextField portField = new JTextField(5);

        JPanel connectionPanel = new JPanel();
        connectionPanel.add(new JLabel("Host:"));
        connectionPanel.add(hostField);
        connectionPanel.add(Box.createHorizontalStrut(15)); // a spacer
        connectionPanel.add(new JLabel("Port:"));
        connectionPanel.add(portField);

        //Text field y panel para usuario y contrasena
        JTextField usernameField = new JTextField(10);
        JTextField passwordField = new JTextField(10);

        JPanel userAuthPanel = new JPanel();
        userAuthPanel.add(new JLabel("Username:"));
        userAuthPanel.add(usernameField);
        userAuthPanel.add(new JLabel("Password:"));
        userAuthPanel.add(passwordField);


        //Text field para los guess
        JTextField guessField = new JTextField(5);
        JTextField fullWordField = new JTextField(5);

        //Imagen en el panel de juego
        gameImageLabel = new JLabel(images[0]);
        gameImageLabel.setPreferredSize(new Dimension(500, 800));
        JButton nextImageButton = new JButton("Next Image");
        nextImageButton.addActionListener(e -> changeImage(true));

        //Panel para el juego
        JPanel guessPanel = new JPanel();
        guessPanel.setLayout(new BoxLayout(guessPanel, BoxLayout.Y_AXIS));
        guessPanel.add(gameImageLabel);
        guessPanel.add(new JLabel("Guess a letter:"));
        guessPanel.add(guessField);
        guessPanel.add(Box.createHorizontalStrut(15));
        guessPanel.add(new JLabel("Or guess the word:"));
        guessPanel.add(fullWordField);
        guessPanel.add(nextImageButton);

        JPanel winPanel = new JPanel();
        winPanel.setLayout(new BoxLayout(winPanel, BoxLayout.Y_AXIS));
        winPanel.add(playerWinLabel);

        JPanel losePanel = new JPanel();
        losePanel.setLayout(new BoxLayout(losePanel, BoxLayout.Y_AXIS));
        losePanel.add(playerLoseLabel);

        int result = JOptionPane.showConfirmDialog(null, connectionPanel,
                "Please Enter Host and Port Values", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            host = hostField.getText();
            port = portField.getText();

            try (Socket socket = new Socket(host, Integer.parseInt(port)); //Instancia un socket
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) { //Para tomar lo que escribe el usuario

            System.out.println("Conectado al servidor en " + host + ":" + port);
            byte[] buffer = new byte[10000];
            String message;

            while (true) {
                System.out.print("Mensaje al servidor: ");
                message = userInput.readLine();
                out.write(message.getBytes());
                out.flush();
                if ("close".equalsIgnoreCase(message)) {
                    System.out.println("Cerrando conexiÃ³n...");
                    break;
                }

                int bytesRead = in.read(buffer);
                if(bytesRead == -1) break;

                String response = new String(buffer, 0, bytesRead); 
                System.out.println("Servidor: " + response);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


            System.out.println(host + ":" + port);
            if (host.equals("localhost") && port.equals("5000")) { //Cambiar a si el usuario esta auntenticado
                int auth = JOptionPane.showConfirmDialog(null, userAuthPanel, "Please write your user and password",
                        JOptionPane.OK_CANCEL_OPTION);
                        
                        
                        
                        // if(autenticado)
                int guess = JOptionPane.showOptionDialog(null, guessPanel,
                        "What will it be...", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                changeImage(true); //Cambiar a si el guess es correcto o incorrecto en vez de true
            }
            if(true){ //Si es turno del usuario para escribir la palabra
                String word = JOptionPane.showInputDialog(null, "Es tu turno de escribir una palabra");
                //Mandar la palabra si es el turno del usuario
            }
            if(true){ // CAmbiar a si el juego termino
                if(true){ //Cambiar a si el usuario gano
                    JOptionPane.showMessageDialog(null, winPanel, "You won! yay!", JOptionPane.YES_NO_OPTION);
                }
                if(true){ //Cambiar a si el usuario perdio
                    JOptionPane.showMessageDialog(null, losePanel, "You lose! you suck!", JOptionPane.YES_NO_OPTION);
                }
            }

        }
    }

    private static void changeImage(boolean value){
        if(value){
            imageToShow ++;
        }

        int imageIndex = imageToShow % images.length;
        gameImageLabel.setIcon(images[imageIndex]);

        gameImageLabel.revalidate();
        gameImageLabel.repaint();
    }
}