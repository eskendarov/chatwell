package ru.eskendarov.ea.chatwell.server;

public interface AuthService {
    
    String getNicknameByLoginAndPassword(final String login, final String password);
    String getIdByNickname(final String s);
    boolean start();
    void stop();
}