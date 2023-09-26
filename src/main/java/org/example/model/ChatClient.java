package org.example.model;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;

public class ChatClient extends Application {
    private static final String SERVER_ADDRESS = "172.16.16.161"; // Cambia a la dirección IP del servidor si es necesario
    private static final int SERVER_PORT = 8080;
    private PrintWriter out;
    private BufferedReader in;
    private String nickname;
    private String room;

    private TextArea chatArea;
    private TextField messageField;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Chat Client");

        BorderPane root = new BorderPane();
        chatArea = new TextArea();
        chatArea.setEditable(false);
        root.setCenter(chatArea);

        VBox inputBox = new VBox();
        inputBox.setSpacing(5);
        messageField = new TextField();
        messageField.setPromptText("Escribe tu mensaje");
        Button sendButton = new Button("Enviar");
        inputBox.getChildren().addAll(messageField, sendButton);
        root.setBottom(inputBox);

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();

        connectToServer();

        sendButton.setOnAction(e -> sendMessage());

        primaryStage.setOnCloseRequest(e -> disconnectFromServer());
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Solicitar al cliente un apodo (nickname)
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Nickname");
            dialog.setHeaderText(null);
            dialog.setContentText("Ingrese su nickname:");
            dialog.showAndWait().ifPresent(name -> {
                nickname = name;
                out.println(nickname);
            });

            // Inicialmente, unirse a una sala
            joinRoom();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void joinRoom() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Unirse a una sala");
        dialog.setHeaderText(null);
        dialog.setContentText("Ingrese el nombre de la sala:");
        dialog.showAndWait().ifPresent(roomName -> {
            room = roomName;
            out.println("/join " + roomName);
        });
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            out.println(message);
            messageField.clear();
        }
    }

    private void disconnectFromServer() {
        if (out != null) {
            out.println("/quit");
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (out != null) {
            out.close();
        }
    }

    private void appendMessage(String message) {
        Platform.runLater(() -> chatArea.appendText(message + "\n"));
    }
}
