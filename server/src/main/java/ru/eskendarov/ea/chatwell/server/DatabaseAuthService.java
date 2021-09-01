package ru.eskendarov.ea.chatwell.server;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseAuthService implements AuthService {
    
    @Override
    public boolean start() {
        try {
            SQLHandler.connect();
        } catch (ClassNotFoundException | SQLException e) {
            return false;
        }
        return true;
    }
    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try {
            ResultSet rs = SQLHandler.getStmt().executeQuery(
                    String.format("SELECT nickname FROM users WHERE login = '%s' AND password = '%s';", login,
                                  password));
            if (!rs.next()) {
                return null;
            }
            return rs.getString("nickname");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    public String getIdByNickname(String nickname) {
        try {
            ResultSet rs = SQLHandler.getStmt().executeQuery(
                    String.format("SELECT id FROM users WHERE nickname = '%s';", nickname));
            rs.next();
            return rs.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    public void stop() {
        SQLHandler.disconnect();
    }
}