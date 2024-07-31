import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PingApp extends JFrame {

    private JTextField ipField;
    private JTextArea resultArea;
    private JButton pingButton;

    public PingApp() {
        createUI();
    }

    private void createUI() {
        setTitle("Aplicativo de Ping");
        setSize(600, 400); // Aumentar o tamanho da janela
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        ipField = new JTextField(20);
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true); // Habilitar quebra de linha
        resultArea.setWrapStyleWord(true); // Quebrar linhas em palavras

        pingButton = new JButton("Ping");

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Endereço IP:"));
        topPanel.add(ipField);
        topPanel.add(pingButton);
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        add(panel);

        pingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ipAddress = ipField.getText();
                ping(ipAddress);
            }
        });
    }

    private void ping(String ipAddress) {
        try {
            resultArea.setText("Pingando " + ipAddress + "...\n");
            ProcessBuilder processBuilder = new ProcessBuilder("ping", "-n", "4", ipAddress);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP850")); // Usar CP850 para suportar caracteres acentuados corretamente
            StringBuilder result = new StringBuilder();
            String line;
            boolean isReachable = false;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
                if (line.contains("TTL=")) {
                    isReachable = true;
                }
            }
            if (isReachable) {
                resultArea.append("Ping bem-sucedido.\n");
            } else {
                analyzePingResult(result.toString());
            }
            resultArea.append(result.toString());
        } catch (Exception ex) {
            resultArea.setText("Erro: " + ex.getMessage());
        }
    }

    private void analyzePingResult(String pingOutput) {
        boolean errorFound = false;

        if (pingOutput.contains("Esgotado o tempo limite do pedido")) {
            resultArea.append("Erro: Tempo de resposta esgotado. O host de destino pode estar desligado ou inacessível.\n");
            errorFound = true;
        } 
        if (pingOutput.contains("Host de destino inacessível")) {
            resultArea.append("Erro: Host de destino inacessível. O host pode estar desconectado da rede.\n");
            errorFound = true;
        } 
        if (pingOutput.contains("Não foi possível encontrar o host")) {
            resultArea.append("Erro: Não foi possível encontrar o host. Verifique o endereço IP ou nome do host e tente novamente.\n");
            errorFound = true;
        }
        
        // Sugestões para qualquer erro encontrado
        if (errorFound) {
            resultArea.append("Possíveis causas e ações:\n");
            resultArea.append("1. Host de destino inacessível, verifique se o equipamento de destino está ligado.\n");
            resultArea.append("2. Host inacessível, verifique se os cabos do equipamento de destino estão devidamente conectados.\n");
            resultArea.append("3. Verifique se o endereço IP ou nome do host está correto e tente novamente.\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PingApp().setVisible(true);
            }
        });
    }
}
