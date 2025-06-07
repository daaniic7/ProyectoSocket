import java.io.*;
import java.net.*;

public class ServerThread extends Thread {
    private Resource r;
    private int port;
    private InetAddress ipCliente;
    private int puertoCliente;

    private static final int TAM_MAX_BUFFER = 512;
    private static final String COD_TEXTO = "UTF-8";
    // private static final int TIME_MAX_LISTEN = 4000;
    private static final File ROOT_DIR = new File("/tmp/tftp/anonymous");


    public ServerThread(Resource _r, int portListen, InetAddress ipCliente, int puertoCliente) {
        this.port = portListen;
        this.r = _r;
        this.ipCliente = ipCliente;
        this.puertoCliente = puertoCliente;
    }

    @Override
    public void run() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(port);
            // socket.setSoTimeout(TIME_MAX_LISTEN);

            if (!ROOT_DIR.exists()) ROOT_DIR.mkdirs();

            byte[] bufferReceive = new byte[TAM_MAX_BUFFER];
            boolean fin = false;

            System.out.println("Hilo escuchando en puerto " + port + " para cliente " + ipCliente + ":" + puertoCliente);

            while (!fin) {
                DatagramPacket packetReceiver = new DatagramPacket(bufferReceive, bufferReceive.length);

                socket.receive(packetReceiver);

                String mensaje = new String(packetReceiver.getData(), 0, packetReceiver.getLength(), COD_TEXTO);

                if (mensaje.equalsIgnoreCase("disconnect") || mensaje.equalsIgnoreCase("FIN_FTP")) {
                    fin = true;
                    continue;
                } else if (mensaje.equalsIgnoreCase("list")) {
                    StringBuilder listado = new StringBuilder();
                    File[] archivos = ROOT_DIR.listFiles();
                    if (archivos != null && archivos.length > 0) {
                        for (File f : archivos) listado.append(f.getName()).append("\n");
                    } else listado.append("Directorio vacío");
                    byte[] respuesta = listado.toString().getBytes(COD_TEXTO);
                    DatagramPacket respuestaPacket = new DatagramPacket(respuesta, respuesta.length,
                            packetReceiver.getAddress(), packetReceiver.getPort());
                    socket.send(respuestaPacket);

                } else if (mensaje.startsWith("get ")) {
                    String nombreArchivo = mensaje.substring(4).trim();
                    File archivo = new File(ROOT_DIR, nombreArchivo);
                    if (!archivo.exists() || !archivo.isFile()) {
                        byte[] error = "ERROR: Archivo no encontrado.".getBytes(COD_TEXTO);
                        socket.send(new DatagramPacket(error, error.length, packetReceiver.getAddress(), packetReceiver.getPort()));
                        continue;
                    }
                    try (FileInputStream fis = new FileInputStream(archivo)) {
                        int leidos;
                        byte[] bufferArchivo = new byte[TAM_MAX_BUFFER];
                        while ((leidos = fis.read(bufferArchivo)) != -1) {
                            DatagramPacket dataPacket = new DatagramPacket(bufferArchivo, leidos,
                                    packetReceiver.getAddress(), packetReceiver.getPort());
                            socket.send(dataPacket);
                            Thread.sleep(10);
                        }
                        byte[] finMsg = "FIN_FTP".getBytes(COD_TEXTO);
                        socket.send(new DatagramPacket(finMsg, finMsg.length,
                                packetReceiver.getAddress(), packetReceiver.getPort()));
                    }
                } else if (mensaje.startsWith("remove ")) {
                    String nombreArchivo = mensaje.substring(7).trim();
                    File archivo = new File(ROOT_DIR, nombreArchivo);
                    String respuesta;
                    if (!archivo.exists()) respuesta = "ERROR: El archivo no existe.";
                    else if (!archivo.isFile()) respuesta = "ERROR: No es un archivo válido.";
                    else if (archivo.delete()) respuesta = "Archivo eliminado correctamente.";
                    else respuesta = "ERROR: No se pudo eliminar el archivo.";
                    byte[] datos = respuesta.getBytes(COD_TEXTO);
                    socket.send(new DatagramPacket(datos, datos.length,
                            packetReceiver.getAddress(), packetReceiver.getPort()));
                } else {
                    byte[] eco = ("Comando no reconocido: " + mensaje).getBytes(COD_TEXTO);
                    socket.send(new DatagramPacket(eco, eco.length,
                            packetReceiver.getAddress(), packetReceiver.getPort()));
                }
            }

            System.out.println("Conexión cerrada con cliente en puerto " + port);
        } catch (Exception e) {
            System.out.println("Error inesperado");
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) socket.close();
        }
    }
}