package ru.eskendarov.ea.chatwell.server;

import lombok.Getter;
import ru.eskendarov.ea.chatwell.mail.Mail;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.*;

import static ru.eskendarov.ea.chatwell.server.SQLHandler.*;

@Getter
class ClientHandler {
    
    private final Scanner scanner = new Scanner(System.in);
    private final DataInputStream in;
    private final DataOutputStream out;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(1);
    private final String uniqueKey = UUID.randomUUID().toString();
    private Server server;
    private Socket socket;
    private String id, nickname;
    private String[] temporaryTokens;
    private boolean isAuthorization;
    
    ClientHandler(final Server server, final Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        executorService = Executors.newFixedThreadPool(3);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        // поток чтения и отправки сообщений от имени сервера
        executorService.submit(() -> {
            while (this.socket.isConnected()) {
                final String messageFromAdmin = scanner.nextLine();
                // комманда /exit прекращает работу сервера
                if (messageFromAdmin.startsWith("/exit")) {
                    closeConnection();
                } else {
                    server.broadcastMsg("Сообщение от администрации: " + messageFromAdmin);
                }
            }
        });
        // поток чтения и отправки сообщений от пользователей
        executorService.submit(() -> {
            try {
                while (!isAuthorization) {
                    final String messageFromUsers = in.readUTF();
                    if (messageFromUsers.startsWith("/")) {
                        checkUserAuth(messageFromUsers);
                        checkGuestAuth(messageFromUsers);
                        checkSignUp(messageFromUsers);
                        confirmMail(messageFromUsers);
                    }
                    if (nickname != null) {
                        System.out.println(
                                "Клиент подключился: [никнейм: " + nickname + "] [ip_address http:/" + socket.getInetAddress() + "]");
                    }
                }
                while (isAuthorization) {
                    final String messageFromUsers = in.readUTF();
                    if (messageFromUsers.startsWith("/")) {
                        checkChangeNickname(messageFromUsers);
                        checkPrivateMessage(messageFromUsers);
                    } else {
                        saveHistory(messageFromUsers);
                        server.broadcastMsg(nickname + ": " + messageFromUsers);
                    }
                    System.out.println("Сообщение от клиента: " + nickname + ": " + messageFromUsers);
                }
            } catch (EOFException e) {
                System.err.println(
                        "Клиент отключился: [никнейм: " + nickname + "] [ip_address http:/" + socket.getInetAddress() + "]");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeConnection();
            }
        });
    }
    private void saveHistory(final String messageIn) {
        addToHistory(socket.getInetAddress().toString(), nickname, "broadcast", messageIn);
    }
    private void checkPrivateMessage(final String messageIn) {
        if (messageIn.startsWith("/w ")) {
            final String[] prvMsg = messageIn.split("\\s", 3);
            //от текущего пользователя_this клиенту имя_prvMsg[1] сообщение_prvMsg[2]
            server.privateMsg(this, prvMsg[1], prvMsg[2]);
        }
    }
    private void checkChangeNickname(final String messageIn) {
        if (messageIn.startsWith("/changeNick ") && messageIn.split("\\s").length == 2) {
            // '/change newNickname'
            String newNickname = messageIn.split("\\s")[1];
            if (changeNick(id, newNickname)) {
                server.broadcastMsg(nickname + " сменил никнейм на " + newNickname);
                sendMsg("/changeNickOk " + newNickname);
                nickname = newNickname;
                server.checkOnlineClients();
            } else {
                sendMsg("Никнейм " + newNickname + " уже существует!");
            }
        }
    }
    private void checkUserAuth(final String messageIn) {
        if (messageIn.split("\\s").length == 3) {
            // '/auth login password'
            final String nickFromDB = server.getAuthService().getNicknameByLoginAndPassword(messageIn.split("\\s")[1],
                                                                                            messageIn.split("\\s")[2]);
            if (nickFromDB != null) {
                nickname = nickFromDB;
                if (!server.isNickBusy(nickname)) {
                    id = server.getAuthService().getIdByNickname(nickname);
                    sendMsg("/authOk " + nickname);
                    server.subscribe(this);
                    isAuthorization = true;
                } else {
                    sendMsg("Учетная запись в данный момент уже используется");
                }
            } else {
                sendMsg("Некорректный логин или пароль.");
            }
        }
    }
    private void checkGuestAuth(final String messageIn) {
        if (messageIn.startsWith("/authGuest ")) {
            nickname = messageIn.split("\\s")[1];
            sendMsg("/authGuestOk " + nickname);
            sendMsg("У вас 2 минуты на авторизацию, иначе соединение закроется.");
            server.subscribe(this);
            isAuthorization = true;
            scheduledExecutor.schedule(() -> sendMsg("/authGuestNo"), 120, TimeUnit.SECONDS);
        }
    }
    private void checkSignUp(final String messageIn) {
        // '/signup login password nickname'
        if (messageIn.startsWith("/signUpData") && messageIn.split("\\s").length == 4) {
            if (!loginIsBusy(messageIn.split("\\s")[1])) {
                if (!nickIsBusy(messageIn.split("\\s")[3])) {
                    // отправляем уникальный идентификационный номер на адрес(логин) почты, для потверждения
                    new Mail().sendToClient(uniqueKey, messageIn.split("\\s")[1]);
                    sendMsg("/signUpDataOk Вам на почту отправлен уникальный идентификационный номер, пожалуйста " + "введите" + " " + "его в соответствующее поле ввода.");
                    temporaryTokens = messageIn.split("\\s");
                } else {
                    sendMsg("Пользователь с никнеймом: [" + messageIn.split("\\s")[3] + "] уже существует");
                }
            } else {
                sendMsg("Пользователь с логином: [" + messageIn.split("\\s")[1] + "] ранее был зарегистрирован.");
            }
        }
    }
    private void confirmMail(final String messageIn) {
        // '/signUpCodeMail codeFromMail'
        if (messageIn.startsWith("/signUpCodeMail") && messageIn.split("\\s").length == 2) {
            if (messageIn.split("\\s")[1].equals(uniqueKey)) {
                signUpInChat(temporaryTokens[1], temporaryTokens[2], temporaryTokens[3]);
                sendMsg("/signUpCodeMailOk Регистрация пользователя прошла успешно!");
            } else {
                sendMsg("Неправильный код регистрации!");
            }
        }
    }
    private void closeConnection() {
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.unsubscribe(this);
    }
    void sendMsg(final String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            System.out.println("Ошибка отправки сообщения" + e);
        }
    }
}