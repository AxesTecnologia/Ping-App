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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PingApp extends JFrame {

    private JTextField ipField;
    private JTextArea resultArea;
    private JButton pingButton;
    private JButton discoverButton;
    private JButton tracerouteButton;
    private JButton bandwidthTestButton;
    private JComboBox<String> targetsComboBox;

    public PingApp() {
        createUI();
    }

    private void createUI() {
        setTitle("Aplicativo de Ping e Traceroute");
        setSize(1000, 700); // Aumentando o tamanho da janela
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        ipField = new JTextField(15);
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        pingButton = new JButton("Ping");
        discoverButton = new JButton("Descobrir Rede");
        tracerouteButton = new JButton("Traceroute");
        bandwidthTestButton = new JButton("Teste de Banda");
        targetsComboBox = new JComboBox<>(new String[]{"Selecione um destino", "Google DNS (8.8.8.8)", "Roteador (192.168.1.1)"});

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); // Ajustando o layout para caber mais botões
        topPanel.add(discoverButton);
        topPanel.add(new JLabel("Endereço IP:"));
        topPanel.add(ipField);
        topPanel.add(pingButton);
        topPanel.add(tracerouteButton);
        topPanel.add(bandwidthTestButton); // Adicionando o botão de teste de banda
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

        tracerouteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ipAddress = ipField.getText();
                traceroute(ipAddress);
            }
        });

        bandwidthTestButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ipAddress = ipField.getText();
                testBandwidth(ipAddress);
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
        Map<String, String> ipMacMap = new HashMap<>();
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

            // Get the ARP table to map IPs to MAC addresses
            ProcessBuilder arpBuilder = new ProcessBuilder("arp", "-a");
            Process arpProcess = arpBuilder.start();
            BufferedReader arpReader = new BufferedReader(new InputStreamReader(arpProcess.getInputStream()));
            String arpLine;
            while ((arpLine = arpReader.readLine()) != null) {
                String[] parts = arpLine.split("\\s+");
                if (parts.length >= 3 && parts[0].matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                    ipMacMap.put(parts[0], parts[1]);
                }
            }

        } catch (Exception ex) {
            resultArea.append("Erro ao obter informações da rede: " + ex.getMessage() + "\n");
        }

        if (!activeHosts.isEmpty()) {
            resultArea.append("Dispositivos ativos encontrados:\n");
            for (String host : activeHosts) {
                String macAddress = ipMacMap.get(host);
                String deviceName = getDeviceNameByMac(macAddress);
                resultArea.append(host + " (" + (deviceName != null ? deviceName : "Desconhecido") + ")\n");
            }
        } else {
            resultArea.append("Nenhum dispositivo ativo encontrado na faixa de rede.\n");
        }
    }

    private String getDeviceNameByMac(String macAddress) {
        if (macAddress == null) return null;

        // Here you can implement a lookup using a database or a service that provides MAC address vendor information
        // For simplicity, let's return a mock name based on the MAC address
        if (macAddress.startsWith("00:1A:2B")) {
            return "Dispositivo A";
        } else if (macAddress.startsWith("00:1C:3D")) {
            return "Dispositivo B";
        } else {
            return "Fabricante Desconhecido";
        }
    }

    private void ping(String ipAddress) {
        try {
            resultArea.setText("Pingando " + ipAddress + "...\n");
            ProcessBuilder processBuilder = new ProcessBuilder("ping", "-n", "4", ipAddress);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP850"));
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

    private void traceroute(String ipAddress) {
        try {
            resultArea.setText("Realizando traceroute para " + ipAddress + "...\n");
            ProcessBuilder processBuilder = new ProcessBuilder("tracert", ipAddress);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP850"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            resultArea.append(result.toString());
        } catch (Exception ex) {
            resultArea.append("Erro ao tentar realizar traceroute: " + ex.getMessage() + "\n");
        }
    }

    private void testBandwidth(String ipAddress) {
        try {
            resultArea.setText("Testando banda com " + ipAddress + "...\n");
            String iperfPath = "C:\\iperf3\\iperf3.exe";  // Altere este caminho para o local onde está o iperf3.exe
            ProcessBuilder processBuilder = new ProcessBuilder(iperfPath, "-c", ipAddress);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP850"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            resultArea.append(result.toString());
        } catch (Exception ex) {
            resultArea.append("Erro ao tentar realizar teste de banda: " + ex.getMessage() + "\n");
        }
    }


    private void analyzePingResult(String pingOutput) {
        boolean errorFound = false;

        if (pingOutput.contains("Esgotado o tempo limite do pedido")) {
            resultArea.append("Erro: Tempo de resposta esgotado. O host de destino pode estar desligado ou inacessível.\n");
            errorFound = true;
        }
        if (pingOutput.contains("Host de destino inacessível")) {
            resultArea.append("Erro: Host de destino inacessível. Verifique se o endereço IP está correto e se o dispositivo está na mesma rede.\n");
            errorFound = true;
        }
        if (!errorFound) {
            resultArea.append("Ping não teve sucesso por motivos desconhecidos.\n");
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