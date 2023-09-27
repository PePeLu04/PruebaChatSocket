package org.example.model;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ChatServer {
    private static final int PORT = 8080;
    private static final int MAX_CLIENTS = 100;

    private static Map<String, PrintWriter> clients = new HashMap<>();
    private static Map<String, String> clientRooms = new HashMap<>();
    private static Map<String, Set<String>> rooms = new HashMap<>();
    private static ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTS);

    public static void main(String[] args) {
        System.out.println("Chat Server is running...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                pool.execute(new ClientHandler(serverSocket.accept()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String nickname;
        private String room;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {

            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("Welcome to the Chat Server. Please enter your nickname:");
                nickname = in.readLine();

                // Ensure nickname is unique
                synchronized (clients) {
                    while (clients.containsKey(nickname)) {
                        out.println("Nickname already in use. Please choose another:");
                        nickname = in.readLine();
                    }
                    clients.put(nickname, out);
                }

                out.println("Welcome, " + nickname + "!");
                broadcast("User " + nickname + " has joined the chat.");


                out.println("Please enter the name of the room you want to join or create:");
                while (true) {
                    room = in.readLine();
                    if (room.isEmpty()) {
                        out.println("Room name cannot be empty. Please try again:");
                    } else {
                        break;
                    }
                }

                joinRoom(room);

                out.println("You are now in room: " + room);
                broadcastToRoom(room, "User " + nickname + " has joined the room.");

                String message;
                while ((message = in.readLine()) != null) {
                    if ("/quit".equals(message)) {
                        break;
                    } else if (message.startsWith("/join ")) {
                        String newRoom = message.substring(6);
                        joinRoom(newRoom);
                    } else {
                        broadcastToRoom(room, nickname + ": " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (nickname != null) {
                    synchronized (clients) {
                        clients.remove(nickname);
                    }
                    if (room != null) {
                        leaveRoom(room);
                    }
                    broadcast(nickname + " has left the chat.");
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void joinRoom(String room) {
            synchronized (rooms) {
                clientRooms.put(nickname, room);
                rooms.computeIfAbsent(room, k -> new HashSet<>()).add(nickname);
            }
        }

        private void leaveRoom(String room) {
            synchronized (rooms) {
                Set<String> roomMembers = rooms.get(room);
                if (roomMembers != null) {
                    roomMembers.remove(nickname);
                    if (roomMembers.isEmpty()) {
                        rooms.remove(room);
                    }
                }
                clientRooms.remove(nickname);
            }
        }
    }

    private static void broadcast(String message) {
        synchronized (clients) {
            for (PrintWriter client : clients.values()) {
                client.println(message);
            }
        }
    }

    private static void broadcastToRoom(String room, String message) {
        synchronized (rooms) {
            Set<String> roomMembers = rooms.get(room);
            if (roomMembers != null) {
                for (String member : roomMembers) {
                    PrintWriter client = clients.get(member);
                    if (client != null) {
                        client.println(message);
                    }
                }
            }
        }
        System.out.println("Mensaje recibido: " + message);
    }
}
