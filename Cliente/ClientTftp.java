package Cliente;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientTftp {
    private static final int PORT_CONNECT_TFTP = 44444;
    private static final int TAM_MAX_BUFFER = 512;
    private static final String COD_TEXTO = "UTF-8";

    private static String IP_SERVER;
    private static DatagramSocket socket;
    private static InetAddress ipServer;

    private static Integer connectTftp() {
        try {
            socket = new DatagramSocket();
            ipServer = InetAddress.getByName(IP_SERVER);

            byte[] bufferSend = "Enviando petición de conexión".getBytes(COD_TEXTO);
            DatagramPacket sendPacket = new DatagramPacket(bufferSend, bufferSend.length, ipServer, PORT_CONNECT_TFTP);
            socket.send(sendPacket);

            byte[] bufferReceive = new byte[TAM_MAX_BUFFER];
            DatagramPacket receivePacket = new DatagramPacket(bufferReceive, bufferReceive.length);
            socket.receive(receivePacket);

            Integer newPort = Integer.parseInt(new String(receivePacket.getData(), 0, receivePacket.getLength()));
            System.out.println("Conectado al puerto dinámico: " + newPort);

            socket.close();
            return newPort;
        } catch (Exception e) {
            System.out.println("Error en la conexión inicial: " + e.getMessage());
            return null;
        }
    }

    public static void runCommand(int port) {
        try (Scanner sc = new Scanner(System.in)) {
            socket = new DatagramSocket();
            ipServer = InetAddress.getByName(IP_SERVER);
            socket.setSoTimeout(4000);

            while (true) {
                System.out.print("Comando TFTP: ");
                String comando = sc.nextLine().trim();

                if (comando.toLowerCase().startsWith("put ")) {
                    String nombreArchivo = comando.substring(4).trim();
                    if (!nombreArchivo.contains(".")) {
                        System.out.println("ERROR: El nombre del archivo debe incluir una extensión.");
                        continue;
                    }

                    File archivoLocal = new File(nombreArchivo);
                    if (!archivoLocal.exists()) {
                        System.out.println("ERROR: El archivo no existe localmente.");
                        continue;
                    }

                    byte[] bufferSend = comando.getBytes(COD_TEXTO);
                    DatagramPacket sendPacket = new DatagramPacket(bufferSend, bufferSend.length, ipServer, port);
                    socket.send(sendPacket);

                    try (FileInputStream fis = new FileInputStream(archivoLocal)) {
                        byte[] bufferArchivo = new byte[TAM_MAX_BUFFER];
                        int leidos;
                        while ((leidos = fis.read(bufferArchivo)) != -1) {
                            DatagramPacket dataPacket = new DatagramPacket(bufferArchivo, leidos, ipServer, port);
                            socket.send(dataPacket);
                            Thread.sleep(10);
                        }
                    }

                    byte[] fin = "FIN_FTP".getBytes(COD_TEXTO);
                    socket.send(new DatagramPacket(fin, fin.length, ipServer, port));

                    byte[] bufferRecv = new byte[TAM_MAX_BUFFER];
                    DatagramPacket recvPacket = new DatagramPacket(bufferRecv, bufferRecv.length);
                    socket.receive(recvPacket);
                    String respuesta = new String(recvPacket.getData(), 0, recvPacket.getLength(), COD_TEXTO);
                    System.out.println("Servidor: " + respuesta);
                    continue;
                }

                if (comando.toLowerCase().startsWith("get ") || comando.toLowerCase().startsWith("remove ")) {
                    String nombreArchivo = comando.substring(comando.indexOf(" ") + 1).trim();
                    if (!nombreArchivo.contains(".")) {
                        System.out.println("ERROR: El nombre del archivo debe incluir una extensión.");
                        continue;
                    }
                }

                byte[] bufferSend = comando.getBytes(COD_TEXTO);
                DatagramPacket sendPacket = new DatagramPacket(bufferSend, bufferSend.length, ipServer, port);
                socket.send(sendPacket);

                if (comando.equalsIgnoreCase("disconnect"))
                    break;

                if (comando.toLowerCase().startsWith("get ")) {
                    String nombreArchivo = comando.substring(4).trim();

                    byte[] bufferRecv = new byte[TAM_MAX_BUFFER];
                    DatagramPacket recvPacket = new DatagramPacket(bufferRecv, bufferRecv.length);
                    socket.receive(recvPacket);

                    String contenido = new String(recvPacket.getData(), 0, recvPacket.getLength(), COD_TEXTO);

                    if (contenido.startsWith("ERROR")) {
                        System.out.println(contenido);
                        continue;
                    }

                    File archivoDestino = new File(nombreArchivo);
                    try (FileOutputStream fos = new FileOutputStream(archivoDestino)) {
                        fos.write(contenido.getBytes(COD_TEXTO)); 

                        while (true) {
                            byte[] siguienteBuffer = new byte[TAM_MAX_BUFFER];
                            DatagramPacket siguientePacket = new DatagramPacket(siguienteBuffer, siguienteBuffer.length);
                            socket.receive(siguientePacket);
                            String bloque = new String(siguientePacket.getData(), 0, siguientePacket.getLength(), COD_TEXTO);
                            if (bloque.equals("FIN_FTP"))
                                break;
                            fos.write(siguientePacket.getData(), 0, siguientePacket.getLength());
                        }
                        System.out.println("Archivo recibido correctamente.");
                    }
                } else {
                    byte[] bufferRecv = new byte[TAM_MAX_BUFFER];
                    DatagramPacket recvPacket = new DatagramPacket(bufferRecv, bufferRecv.length);
                    socket.receive(recvPacket);
                    String respuesta = new String(recvPacket.getData(), 0, recvPacket.getLength(), COD_TEXTO);
                    System.out.println("Respuesta del servidor:" + respuesta);
                }
            }

            System.out.println("Cliente desconectado.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed())
                socket.close();
        }
    }

    public static void main(String[] args) throws IOException {
        IP_SERVER = "127.0.0.1";
        Integer port = connectTftp();
        if (port != null)
            runCommand(port);
    }
}