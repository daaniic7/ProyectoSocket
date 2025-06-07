import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class ServerTftp {
    private static final int PORT_CONNECT_TFTP = 44444;
    private static final int TAM_MAX_BUFFER = 512;
    private static final String COD_TEXTO = "UTF-8";
    private static final int TIME_MAX_LISTEN = 10000;

    private static DatagramSocket socket;

    public static void main(String[] args) {
        Resource r = new Resource();  
        DatagramPacket sendPacket;

        try {
            socket = new DatagramSocket(PORT_CONNECT_TFTP);
            socket.setSoTimeout(TIME_MAX_LISTEN);

            while (!r.endClient()) {
                try {
                    byte[] bufferReceiver = new byte[TAM_MAX_BUFFER];
                    DatagramPacket packetReceiver = new DatagramPacket(bufferReceiver, bufferReceiver.length);
                    socket.receive(packetReceiver);

                    InetAddress ipClient = packetReceiver.getAddress();
                    String msg = new String(packetReceiver.getData(), 0, packetReceiver.getLength(), COD_TEXTO);
                    System.out.println("Cliente con ip " + ipClient);

                    int port = r.devPort();
                    if (port != -1) {
                        byte[] bufferSend = String.valueOf(port).getBytes(COD_TEXTO);
                        sendPacket = new DatagramPacket(bufferSend, bufferSend.length, ipClient, packetReceiver.getPort());
                        socket.send(sendPacket);

                        new ServerThread(r, port, ipClient, packetReceiver.getPort()).start();

                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("Timeout esperando cliente...");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } 
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                System.out.println("Cerrando socket...");
                socket.close();
            }
        }
    }
}
