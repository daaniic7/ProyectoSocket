package Servidor;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ServerThread extends Thread {
    private Resource r;
    private int port;
    private InetAddress ipCliente;
    private int puertoCliente;

    private static final int TAM_MAX_BUFFER = 512;
    private static final String COD_TEXTO = "UTF-8";
    private static final File BASE_DIR = new File("Usuarios");
    private static final File ANON_DIR = new File("Usuarios/anonymous");
    private static final File USERS_FILE = new File("Usuarios/usuarios.txt");

    public ServerThread(Resource _r, int portListen, InetAddress ipCliente, int puertoCliente) {
        this.port = portListen;
        this.r = _r;
        this.ipCliente = ipCliente;
        this.puertoCliente = puertoCliente;
    }

    @Override
    public void run() {
        DatagramSocket socket = null;
        File rootDir = null;
        boolean isAnonymous = false;

        try {
            socket = new DatagramSocket(port);
            System.out.println("Hilo escuchando en puerto " + port + " para cliente " + ipCliente + ":" + puertoCliente);

            byte[] bufferReceive = new byte[TAM_MAX_BUFFER];

            DatagramPacket loginPacket = new DatagramPacket(bufferReceive, bufferReceive.length);
            socket.receive(loginPacket);
            String loginMsg = new String(loginPacket.getData(), 0, loginPacket.getLength(), COD_TEXTO).trim();

            System.out.println("Intento de login: " + loginMsg);

            if (!loginMsg.startsWith("login ")) {
                sendMsg(socket, "ERROR: Debes Loguearte", loginPacket);
                System.out.println("Login fallido: formato incorrecto");
                socket.close();
                return;
            }

            String[] parts = loginMsg.split(" ");
            if (parts.length != 3) {
                sendMsg(socket, "ERROR: Formato de login incorrecto.", loginPacket);
                System.out.println("Login fallido: formato incorrecto");
                socket.close();
                return;
            }

            String usuario = parts[1];
            String clave = parts[2];

            Map<String, String> usuarios = cargarUsuarios();

            if (usuario.equals("anonymous") && clave.equals("anonymous")) {
                rootDir = ANON_DIR;
                isAnonymous = true;
                System.out.println("Login anónimo aceptado");
            } else if (usuarios.containsKey(usuario) && usuarios.get(usuario).equals(clave)) {
                rootDir = new File(BASE_DIR, usuario);
                System.out.println("Login correcto para usuario: " + usuario);
            } else {
                sendMsg(socket, "ERROR: Login incorrecto la contraseña o el usuario no existen.", loginPacket);
                System.out.println("Login fallido para usuario: " + usuario);
                socket.close();
                return;
            }

            if (!rootDir.exists())
                rootDir.mkdirs();
            sendMsg(socket, "Login exitoso. Bienvenido " + usuario, loginPacket);

            boolean fin = false;
            while (!fin) {
                DatagramPacket packetReceiver = new DatagramPacket(bufferReceive, bufferReceive.length);
                socket.receive(packetReceiver);
                String mensaje = new String(packetReceiver.getData(), 0, packetReceiver.getLength(), COD_TEXTO).trim();

                System.out.println("Comando recibido de " + usuario + ": " + mensaje);

                if (mensaje.equalsIgnoreCase("disconnect")) {
                    System.out.println("El cliente en el puerto " + port + " se ha desconectado.");
                    sendMsg(socket, "Desconexión exitosa. ¡Hasta pronto!", packetReceiver);
                    fin = true;
                    continue;
                }

                if (mensaje.equalsIgnoreCase("FIN_FTP")) {
                    fin = true;
                    continue;
                }

                if (mensaje.equalsIgnoreCase("list")) {
                    StringBuilder listado = new StringBuilder();
                    File[] archivos = rootDir.listFiles();
                    if (archivos != null && archivos.length > 0) {
                        for (File f : archivos)
                            listado.append(f.getName()).append("\n");
                    } else
                        listado.append("Directorio vacío");
                    sendMsg(socket, listado.toString(), packetReceiver);
                    System.out.println("Listado enviado a " + usuario);
                } else if (mensaje.startsWith("get ")) {
                    String nombreArchivo = mensaje.substring(4).trim();
                    if (!nombreArchivo.contains(".") || nombreArchivo.endsWith(".")) {
                        sendMsg(socket, "ERROR: Debes incluir la extensión del archivo.", packetReceiver);
                        System.out.println("Error en get: extensión inválida");
                        continue;
                    }

                    File archivo = new File(rootDir, nombreArchivo);
                    if (!archivo.exists() || !archivo.isFile()) {
                        sendMsg(socket, "ERROR: Archivo no encontrado.", packetReceiver);
                        System.out.println("Error en get: archivo no encontrado");
                        continue;
                    }

                    sendMsg(socket, "Iniciando transferencia de " + nombreArchivo, packetReceiver);
                    try (FileInputStream fis = new FileInputStream(archivo)) {
                        int leidos;
                        byte[] bufferArchivo = new byte[TAM_MAX_BUFFER];
                        while ((leidos = fis.read(bufferArchivo)) != -1) {
                            DatagramPacket dataPacket = new DatagramPacket(bufferArchivo, leidos,
                                    packetReceiver.getAddress(), packetReceiver.getPort());
                            socket.send(dataPacket);
                            Thread.sleep(10);
                        }
                        sendMsg(socket, "FIN_FTP", packetReceiver);
                        System.out.println("Archivo " + nombreArchivo + " enviado a " + usuario);
                    }

                } else if (!isAnonymous && mensaje.startsWith("remove ")) {
                    String nombreArchivo = mensaje.substring(7).trim();
                    if (!nombreArchivo.contains(".") || nombreArchivo.endsWith(".")) {
                        sendMsg(socket, "ERROR: Debes incluir la extensión del archivo.", packetReceiver);
                        System.out.println("Error en remove: extensión inválida");
                        continue;
                    }

                    File archivo = new File(rootDir, nombreArchivo);
                    String respuesta;
                    if (!archivo.exists()) {
                        respuesta = "ERROR: El archivo no existe.";
                        System.out.println("Error en remove: archivo no existe");
                    } else if (!archivo.isFile()) {
                        respuesta = "ERROR: No es un archivo válido.";
                        System.out.println("Error en remove: no es archivo válido");
                    } else if (archivo.delete()) {
                        respuesta = "Archivo eliminado correctamente.";
                        System.out.println("Archivo " + nombreArchivo + " eliminado por " + usuario);
                    } else {
                        respuesta = "ERROR: No se pudo eliminar el archivo.";
                        System.out.println("Error en remove: fallo al eliminar");
                    }
                    sendMsg(socket, respuesta, packetReceiver);

                } else if (isAnonymous && mensaje.startsWith("remove ")) {
                    sendMsg(socket, "No tienes permiso para usar este comando", packetReceiver);
                    System.out.println("Intento de remove por usuario anónimo");

                } else if (!isAnonymous && mensaje.startsWith("put ")) {
                    String nombreArchivo = mensaje.substring(4).trim();
                    if (!nombreArchivo.contains(".") || nombreArchivo.endsWith(".")) {
                        sendMsg(socket, "ERROR: El nombre del archivo debe incluir extensión.", packetReceiver);
                        System.out.println("Error en put: extensión inválida");
                        continue;
                    }

                    File archivoDestino = new File(rootDir, nombreArchivo);

                    sendMsg(socket, "Preparado para recibir " + nombreArchivo, packetReceiver);
                    try (FileOutputStream fos = new FileOutputStream(archivoDestino)) {
                        while (true) {
                            DatagramPacket dataPacket = new DatagramPacket(bufferReceive, bufferReceive.length);
                            socket.receive(dataPacket);
                            String contenido = new String(dataPacket.getData(), 0, dataPacket.getLength(), COD_TEXTO);
                            if (contenido.equals("FIN_FTP"))
                                break;
                            fos.write(dataPacket.getData(), 0, dataPacket.getLength());
                        }
                        sendMsg(socket, "Archivo recibido correctamente.", packetReceiver);
                        System.out.println("Archivo " + nombreArchivo + " recibido de " + usuario);
                    } catch (IOException e) {
                        sendMsg(socket, "ERROR: No se pudo guardar el archivo.", packetReceiver);
                        System.out.println("Error en put: fallo al guardar archivo");
                    }

                } else if (isAnonymous && mensaje.startsWith("put ")) {
                    sendMsg(socket, "No tienes permiso para usar este comando", packetReceiver);
                    System.out.println("Intento de put por usuario anónimo");

                } else {
                    sendMsg(socket, "Comando no reconocido o no permitido.", packetReceiver);
                    System.out.println("Comando no reconocido: " + mensaje);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed())
                socket.close();
        }
    }

    private void sendMsg(DatagramSocket socket, String msg, DatagramPacket ref) throws IOException {
        byte[] datos = msg.getBytes(COD_TEXTO);
        DatagramPacket respuestaPacket = new DatagramPacket(datos, datos.length,
                ref.getAddress(), ref.getPort());
        socket.send(respuestaPacket);
    }

    private Map<String, String> cargarUsuarios() {
        Map<String, String> usuarios = new HashMap<>();
        try (Scanner scanner = new Scanner(USERS_FILE)) {
            while (scanner.hasNextLine()) {
                String[] line = scanner.nextLine().split(":");
                if (line.length == 2) {
                    usuarios.put(line[0].trim(), line[1].trim());
                }
            }
        } catch (Exception e) {
            System.err.println("No se pudo cargar usuarios.");
        }
        return usuarios;
    }
}