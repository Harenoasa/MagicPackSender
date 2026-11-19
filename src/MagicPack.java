import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Properties;

public class MagicPack {
    private static void send(String mac, String ipv4) throws IOException {
        String normalized = mac.replace("-", "").replace(":", "");
        if (!normalized.matches("[0-9a-fA-F]{12}")) {
            throw new IllegalArgumentException("Invalid MAC address: " + mac);
        }
        byte[] bytes = HexFormat.of().parseHex("FFFFFFFFFFFF" + normalized.repeat(16));
        InetAddress address = InetAddress.getByName(ipv4);
        if (!(address instanceof Inet4Address)) {
            throw new IllegalArgumentException("An IPv4 address is required");
        }
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.send(new DatagramPacket(bytes, bytes.length, address, 9));
        }
    }

    public static void main(String[] args) throws IOException {
        Properties config = new Properties();
        Path path = Path.of("resource", "wake-on-lan.properties");
        // Prefer the local configuration; packaged applications can use the classpath.
        try (InputStream input = Files.isRegularFile(path)
                ? Files.newInputStream(path)
                : MagicPack.class.getResourceAsStream("/wake-on-lan.properties")) {
            if (input == null) {
                throw new IOException("Missing configuration: " + path.toAbsolutePath());
            }
            config.load(input);
        }
        String mac = required(config, "mac");
        String ip = required(config, "ip");
        send(mac, ip);
        System.out.println("Magic packet sent to " + ip + ":9");
    }

    private static String required(Properties config, String key) {
        String value = config.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing configuration property: " + key);
        }
        return value.trim();
    }
}
