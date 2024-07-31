package src.codefonte;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class PingApp extends JFrame {

    private JTextField ipField;
    private JTextArea resultArea;
    private JButton pingButton;
    private JButton discoverButton;
    private JComboBox<String> targetsComboBox;

    public PingApp() {
        createUI();
    }

    private void createUI() {
        setTitle("Aplicativo de Ping");
        setSize(800, 600); // Aumentar o tamanho da janela
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        ipField = new JTextField(15);
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true); // Habilitar quebra de linha
        resultArea.setWrapStyleWord(true); // Quebrar linhas em palavras

        pingButton = new JButton("Ping");
        discoverButton = new JButton("Descobrir Rede");
        targetsComboBox = new JComboBox<>(new String[]{"Selecione um destino", "Google DNS (8.8.8.8)", "Roteador (192.168.1.1)"});

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JPanel topPanel = new JPanel();
        topPanel.add(discoverButton);
        topPanel.add(new JLabel("Endereço IP:"));
        topPanel.add(ipField);
        topPanel.add(pingButton);
        topPanel.add(targetsComboBox);
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        add(panel);

        discoverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                discoverNetwork();
            }
        });

        pingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ipAddress = ipField.getText();
                ping(ipAddress);
            }
        });

        targetsComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedTarget = (String) targetsComboBox.getSelectedItem();
                if (selectedTarget != null && !selectedTarget.equals("Selecione um destino")) {
                    if (selectedTarget.equals("Google DNS (8.8.8.8)")) {
                        ipField.setText("8.8.8.8");
                    } else if (selectedTarget.equals("Roteador (192.168.1.1)")) {
                        ipField.setText("192.168.1.1");
                    }
                }
            }
        });
    }

    private void discoverNetwork() {
        resultArea.setText("Descobrindo dispositivos na mesma faixa de rede...\n");
        List<String> activeHosts = new ArrayList<>();
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);

            for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                if (address.getAddress() instanceof InetAddress) {
                    String ipAddress = address.getAddress().getHostAddress();
                    int prefixLength = address.getNetworkPrefixLength();
                    String baseIp = ipAddress.substring(0, ipAddress.lastIndexOf(".") + 1);
                    int range = (int) Math.pow(2, 32 - prefixLength);

                    for (int i = 1; i < range - 1; i++) {
                        String ip = baseIp + i;
                        try {
                            ProcessBuilder processBuilder = new ProcessBuilder("ping", "-n", "1", "-w", "100", ip);
                            Process process = processBuilder.start();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP850"));
                            String line;
                            boolean isReachable = false;
                            while ((line = reader.readLine()) != null) {
                                if (line.contains("TTL=")) {
                                    isReachable = true;
                                    activeHosts.add(ip);
                                    break;
                                }
                            }
                        } catch (Exception ex) {
                            resultArea.append("Erro ao pingar " + ip + ": " + ex.getMessage() + "\n");
                        }
                    }
                }
            }
        } catch (Exception ex) {
            resultArea.append("Erro ao obter informações da rede: " + ex.getMessage() + "\n");
        }

        if (!activeHosts.isEmpty()) {
            resultArea.append("Dispositivos ativos encontrados:\n");
            for (String host : activeHosts) {
                resultArea.append(host + "\n");
            }
        } else {
            resultArea.append("Nenhum dispositivo ativo encontrado na faixa de rede.\n");
        }
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
            resultArea.append(result.toString());
            if (isReachable) {
                resultArea.append("Ping bem-sucedido.\n");
            } else {
                analyzePingResult(result.toString());
            }
        } catch (Exception ex) {
            resultArea.append("Erro ao tentar pingar: " + ex.getMessage() + "\n");
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
