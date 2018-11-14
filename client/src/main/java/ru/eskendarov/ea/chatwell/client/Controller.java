package ru.eskendarov.ea.chatwell.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class Controller implements Initializable {
    
    static final SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    List<String> stringList;
    private String currentNickname;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean authorized;
    @FXML
    private TextArea chatTextArea;
    @FXML
    private PasswordField passwordField;
    @FXML
    private VBox authorizationPanel, userListPanel, signUpPanel, confirmMailPanel;
    @FXML
    private TextField messageField, loginField, loginReg, passwordReg, nicknameReg, codeMailField;
    @FXML
    private ListView<String> userList;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mainAction();
        setAuthorizedStatus(false);
        ObservableList<String> observableList = FXCollections.observableArrayList();
        userList.setItems(observableList);
        userList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                messageField.clear();
                messageField.appendText("/w ");
                messageField.appendText(userList.getSelectionModel().getSelectedItem());
                messageField.appendText(" ");
                // '/w nick1 '
                messageField.requestFocus();
                messageField.end();
            }
        });
    }
    private void setAuthorizedStatus(final boolean authorized) {
        this.authorized = authorized;
        chatTextArea.setWrapText(true);
        if (authorized) {
            chatPanelOn();
            // Выводим последние 100 сообщений на экран
            Platform.runLater(() -> {
                if (!"Гость".equals(currentNickname) && currentNickname != null) {
                    stringList = new ResentMessages().getResentMessage(currentNickname);
                    try {
                        if (stringList.size() < 100) {
                            stringList.forEach(msg -> chatTextArea.appendText(msg + "\n"));
                        } else {
                            for (int i = stringList.size() - 100; i < stringList.size(); i++) {
                                chatTextArea.appendText(stringList.get(i) + "\n");
                            }
                        }
                    } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
                        System.out.println(e.toString());
                    }
                }
            });
        } else {
            authPanelOn();
            currentNickname = null;
        }
    }
    private void mainAction() {
        connect();
        executor.submit(() -> {
            try {
                while (!authorized) {
                    final String messageFromServer = in.readUTF();
                    if (messageFromServer.startsWith("/")) {
                        checkSignUp(messageFromServer);
                        authorize(messageFromServer);
                    } else {
                        // Иначе печатаем алерт с сервера
                        chatTextArea.appendText(messageFromServer + "\n");
                    }
                }
                while (socket.isConnected()) {
                    final String currentTime = "[" + time.format(new Date()) + "] ";
                    final String messageFromServer = in.readUTF();
                    if (messageFromServer.startsWith("/")) {
                        updateUserList(messageFromServer);
                        changeNick(messageFromServer);
                        checkTimeOutGuest(messageFromServer);
                    } else {
                        // Иначе печатаем алерт с сервера
                        chatTextArea.appendText(currentTime + messageFromServer + "\n");
                        if (!"Гость".equals(currentNickname) && currentNickname != null) {
                            new ResentMessages().saveChatStory(currentNickname, currentTime + messageFromServer);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(e.toString());
            }
        });
    }
    private void connect() {
        try {
            socket = new Socket("188.243.234.207", 33333);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (ConnectException e) {
            chatTextArea.appendText("Нет связи с сервером...\n");
        } catch (IOException e) {
            chatTextArea.appendText("Ошибка передачи данных: \n" + e + "\n");
        }
    }
    private void authorize(final String messageFromServer) {
        if (messageFromServer.startsWith("/authOk")) {
            // '/authOk нашНикнейм' Если пришло сообщение такого вида, авторизация прошла успешно
            setAuthorizedStatus(true);
            currentNickname = messageFromServer.split("\\s")[1];
        }
        if (messageFromServer.startsWith("/authGuestOk")) {
            // '/guestOk нашНикнейм' Если пришло сообщение такого вида, авторизация гостя прошла успешно
            setAuthorizedStatus(true);
            currentNickname = messageFromServer.split("\\s")[1];
        }
    }
    private void checkSignUp(final String messageFromServer) {
        if (messageFromServer.startsWith("/signUpDataOk")) {
            chatTextArea.appendText(messageFromServer.split("\\s", 2)[1] + "\n");
        }
        if (messageFromServer.startsWith("/signUpCodeMailOk")) {
            chatTextArea.appendText(messageFromServer.split("\\s", 2)[1] + "\n");
        }
    }
    private void changeNick(final String messageFromServer) {
        if (messageFromServer.startsWith("/changeNickOk")) {
            currentNickname = messageFromServer.split("\\s")[1];
        }
    }
    private void updateUserList(final String messageFromServer) {
        if (messageFromServer.startsWith("/userList")) {
            // Делим строку с никнеймами на список никнеймов через пробел.
            String[] userName = messageFromServer.split("\\s");
            Platform.runLater(() -> {
                userList.getItems().clear();
                for (int i = 1; i < userName.length; i++) {
                    userList.getItems().add(userName[i]);
                }
                messageField.requestFocus();
            });
        }
    }
    private void checkTimeOutGuest(final String messageFromServer) {
        
        if (messageFromServer.startsWith("/authGuestNo")) {
            Platform.runLater(this::closeConnection);
        }
    }
    public void sendSignUp() {
        if (socket == null || socket.isClosed()) {
            mainAction();
        }
        try {
            out.writeUTF(
                    "/signUpData " + loginReg.getText() + " " + passwordReg.getText() + " " + nicknameReg.getText());
            out.flush();
            loginReg.clear();
            passwordReg.clear();
            nicknameReg.clear();
            confirmMailPanelOn();
        } catch (IOException e) {
            chatTextArea.appendText("Ошибка регистрации: " + e + "\n");
        }
    }
    public void sendCodeFromMail() {
        if (socket == null || socket.isClosed()) {
            mainAction();
        }
        try {
            out.writeUTF("/signUpCodeMail " + codeMailField.getText());
            out.flush();
            codeMailField.clear();
            authPanelOn();
        } catch (IOException e) {
            chatTextArea.appendText("Ошибка отправки кода потверджения: " + e + "\n");
        }
    }
    public void sendMessageToChat() {
        if (authorized) {
            try {
                out.writeUTF(messageField.getText());
                out.flush();
                messageField.clear();
                messageField.requestFocus();
            } catch (IOException e) {
                chatTextArea.appendText("Ошибка отправки сообщения: " + e.toString() + "\n");
            }
        } else {
            chatTextArea.appendText("Вы не можете отправить сообщение в чат," + " пожалуйста авторизуйтесь!\n");
        }
    }
    public void sendAuthorization() {
        if (socket == null || socket.isClosed()) {
            mainAction();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passwordField.getText());
            out.flush();
            loginField.clear();
            passwordField.clear();
            if (authorized) {
                messageField.requestFocus();
            } else {
                loginField.requestFocus();
            }
        } catch (IOException e) {
            chatTextArea.appendText("Ошибка авторизации: " + e + "\n");
        }
    }
    public void sendAuthAsGuest() {
        if (socket == null || socket.isClosed()) {
            mainAction();
        }
        try {
            out.writeUTF("/authGuest Гость");
            out.flush();
            if (authorized) {
                messageField.requestFocus();
            } else {
                loginField.requestFocus();
            }
        } catch (IOException e) {
            chatTextArea.appendText("Ошибка авторизации гостя: " + e + "\n");
        }
    }
    public void closeConnection() {
        try {
            socket.close();
            out.close();
            in.close();
        } catch (IOException e) {
            chatTextArea.appendText("Ошибка завершения соединения: " + e + "\n");
        } finally {
            setAuthorizedStatus(false);
            chatTextArea.appendText("Вы покинули чат!\n");
        }
    }
    public void authPanelOn() {
        chatTextArea.clear();
        confirmMailPanel.setVisible(false);
        confirmMailPanel.setManaged(false);
        userListPanel.setVisible(false);
        userListPanel.setManaged(false);
        signUpPanel.setVisible(false);
        signUpPanel.setManaged(false);
        authorizationPanel.setVisible(true);
        authorizationPanel.setManaged(true);
        loginField.requestFocus();
    }
    public void signUpPanelOn() {
        chatTextArea.clear();
        authorizationPanel.setVisible(false);
        authorizationPanel.setManaged(false);
        confirmMailPanel.setVisible(false);
        confirmMailPanel.setManaged(false);
        userListPanel.setVisible(false);
        userListPanel.setManaged(false);
        signUpPanel.setVisible(true);
        signUpPanel.setManaged(true);
        loginReg.requestFocus();
    }
    private void confirmMailPanelOn() {
        chatTextArea.clear();
        authorizationPanel.setVisible(false);
        authorizationPanel.setManaged(false);
        userListPanel.setVisible(false);
        userListPanel.setManaged(false);
        signUpPanel.setVisible(false);
        signUpPanel.setManaged(false);
        confirmMailPanel.setVisible(true);
        confirmMailPanel.setManaged(true);
        codeMailField.requestFocus();
    }
    private void chatPanelOn() {
        chatTextArea.clear();
        authorizationPanel.setVisible(false);
        authorizationPanel.setManaged(false);
        confirmMailPanel.setVisible(false);
        confirmMailPanel.setManaged(false);
        signUpPanel.setVisible(false);
        signUpPanel.setManaged(false);
        userListPanel.setVisible(true);
        userListPanel.setManaged(true);
    }
}