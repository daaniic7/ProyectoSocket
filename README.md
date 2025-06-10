# Proyecto TFTP UDP - Versión Final

Este proyecto es una implementación mejorada del protocolo TFTP (Trivial File Transfer Protocol) usando **UDP** en Java, con soporte para autenticación por sesión, comandos ampliados y organización modular en cliente y servidor.

---

## 📁 Estructura del proyecto

```
ProyectoSocket/
├── Cliente/
│   └── ClientTftp.java
├── Servidor/
│   ├── ServerTftp.java
│   ├── ServerThread.java
│   └── Resource.java
├── Usuarios/
│   ├── usuarios.txt
│   ├── anonymous/
│   ├── dani/
│   ├── santi/
│   ├── marcos/
│   └── alex/
```

---

## 🚀 ¿Qué hace el programa?

El cliente puede enviar los siguientes comandos al servidor TFTP:

- `login <usuario> <contraseña>` → Inicia sesión con las credenciales.
- `list` → Lista los archivos disponibles del usuario.
- `get <archivo>` → Descarga un archivo desde su carpeta.
- `remove <archivo>` → Elimina un archivo (solo usuarios autenticados).
- `put <archivo>` → Sube un archivo a su carpeta (solo usuarios autenticados).
- `disconnect` → Finaliza la sesión.

---

## 🛠️ Preparación del entorno

### 1. Crear estructura de usuarios

```bash
mkdir -p Usuarios/anonymous Usuarios/dani Usuarios/santi Usuarios/marcos Usuarios/alex
echo -e "dani:dani\nsanti:santi\nmarcos:marcos\nalex:alex" > Usuarios/usuarios.txt
```

### 2. Añadir archivos de prueba

```bash
echo "archivo público" > Usuarios/anonymous/publico.txt
echo "privado dani" > Usuarios/dani/archivo1.txt
```

---

## 🧪 Compilación

Desde la raíz del proyecto:

```bash
javac Cliente/*.java Servidor/*.java
```

---

## ▶️ Ejecución

### Servidor:
```bash
java Servidor.ServerTftp
```

### Cliente:
```bash
java Cliente.ClientTftp
```

---

## 🧾 Ejemplo de uso

1. **Login**:
`login dani dani`
2. **Ver archivos disponibles**:
`list`
3. **Subir archivo**:
`put archivo_local.txt`
4. **Descargar archivo**:
`get archivo1.txt`
5. **Eliminar archivo**:
`remove archivo1.txt`
6. **Salir**:
`disconnect`

---

## 🔐 Permisos según usuario

| Comando   | anonymous | usuarios registrados |
|-----------|-----------|----------------------|
| list      | ✅        | ✅                   |
| get       | ✅        | ✅                   |
| put       | ❌        | ✅                   |
| remove    | ❌        | ✅                   |

---

## 📘 Diagrama UML Simplificado

```plaintext
+-----------------------+        +----------------------+
|     ClientTftp        |        |     ServerTftp       |
+-----------------------+        +----------------------+
| - socket              |        | - socket             |
| - connectTftp()       |<-----> | - main()             |
| - runCommand()        |        |  └── crea hilo        |
+-----------------------+        +----------------------+
          |                                 |
          v                                 v
   +------------------+          +------------------------+
   |  Usuario Interfaz|          |     ServerThread       |
   +------------------+          +------------------------+
   | login, list...   |          | recibe y gestiona      |
   | comandos texto   |          | comandos del cliente   |
   +------------------+          +------------------------+
```

---

## 📌 Detalles Técnicos

- El puerto de conexión inicial es **44444**.
- Cada cliente se asigna a un puerto dinámico distinto.
- Se usa `DatagramSocket` y `DatagramPacket` para toda la comunicación.
- El servidor lanza un hilo (`ServerThread`) por cliente.
- El acceso a archivos es local y por carpeta individual.
- `anonymous` solo puede usar `list` y `get`. Otros comandos muestran un mensaje de denegación.
- Los comandos `put`, `get`, `remove` requieren que el nombre del archivo tenga extensión.
- Toda la comunicación se realiza por UDP, con sincronización mínima y mensajes `FIN_FTP` de cierre.

---

## 👤 Créditos

Proyecto desarrollado por:

- 👨‍💻 Daniel Cornejo García
- 🧪 Curso DAM — IES Virgen del Carmen
- 💻 Asignatura: Programación de servicios y procesos
