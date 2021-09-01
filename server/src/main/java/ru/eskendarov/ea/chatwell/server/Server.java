package ru.eskendarov.ea.chatwell.server;

import lombok.Getter;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

@Getter
class Server {
    
    private static final int PORT = 33333;
    private Vector<ClientHandler> clients;
    private AuthService authService;
    Server() {
        authService = new DatabaseAuthService();
        if (!authService.start()) {
            System.out.println("Сервис авторизации не удалось запустить.");
            System.exit(0);
        }
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            clients = new Vector<>();
            System.out.println("Сервер запущен. Ожидаем подключение клиента");
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket);
            }
        } catch (BindException e) {
            System.out.println("Соединение на порту: " + PORT + " уже запущено! [Address already in use: JVM_Bind]");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            authService.stop();
        }
    }
    void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        //                broadcastMsg(clientHandler.getNickname() + " зашел(а) в чат.");
        checkOnlineClients();
    }
    void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        //                broadcastMsg(clientHandler.getNickname() + " покинул(а) чат.");
        checkOnlineClients();
    }
    void broadcastMsg(String msg) {
        for (ClientHandler o : clients) {
            o.sendMsg(msg);
        }
    }
    void privateMsg(ClientHandler currentClient, String distClient, String message) {
        for (ClientHandler o : clients) {
            // Сравниваем никнеймы игнорируя регистр
            if (distClient.equalsIgnoreCase(o.getNickname())) {
                o.sendMsg("[" + distClient + "] <- [" + currentClient.getNickname() + "] : " + message);
                currentClient.sendMsg("[" + currentClient.getNickname() + "] -> [" + distClient + "] : " + message);
                SQLHandler.addToHistory(currentClient.getSocket().getInetAddress().toString(),
                                        currentClient.getNickname(), distClient, message);
                return;
            }
        }
        currentClient.sendMsg("Пользователь " + distClient + " не в сети, либо не существует ");
    }
    void checkOnlineClients() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("/userList ");
        for (ClientHandler o : clients) {
            // Составляем список клиентов через пробел
            stringBuilder.append(o.getNickname()).append(" ");
        }
        String out = stringBuilder.toString();
        for (ClientHandler o : clients) {
            o.sendMsg(out);
        }
    }
    boolean isNickBusy(String nickname) {
        for (ClientHandler o : clients) {
            // никнейм у нас уникальный несмотря на регистр
            if (o.getNickname().equalsIgnoreCase(nickname)) {
                return true;
            }
        }
        return false;
    }
}