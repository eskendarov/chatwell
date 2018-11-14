package ru.eskendarov.ea.chatwell.server;

import lombok.Getter;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Getter
class SQLHandler {
    
    static final SimpleDateFormat date = new SimpleDateFormat("dd.MM.yy");
    static final SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
    private static Connection connection;
    private static Statement stmt;
    static Statement getStmt() {
        return stmt;
    }
    static void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:users.db");
        stmt = connection.createStatement();
    }
    static void disconnect() {
        try {
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    static void addToHistory(final String sourceIp, final String sourceNick, final String distanceNick, final String message) {
        try {
            
            stmt.executeUpdate(String.format(
                    "INSERT INTO history (source_ip, date, time, source_nick, distance_nick, message) VALUES ( '%s', '%s', " + "'%s', '%s', '%s', " + "'%s');",
                    sourceIp, date.format(new Date()), time.format(new Date()), sourceNick, distanceNick, message));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    static boolean changeNick(final String clientId, final String newNick) {
        try {
            stmt.executeUpdate(String.format("UPDATE users SET nickname = '%s' WHERE id = '%s';", newNick, clientId));
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    static void signUpInChat(final String login, final String password, final String nickname) {
        try {
            System.out.println(stmt.getUpdateCount());
            stmt.executeUpdate(
                    String.format("INSERT INTO users (id, login, password, nickname) VALUES ('%s', '%s', '%s', '%s');",
                                  UUID.randomUUID().toString(), login, password, nickname));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    static boolean nickIsBusy(String nickname) {
        try {
            ResultSet rs = stmt.executeQuery(
                    String.format("SELECT nickname FROM users WHERE nickname = '%s';", nickname));
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
    static boolean loginIsBusy(String login) {
        try {
            ResultSet rs = stmt.executeQuery(String.format("SELECT login FROM users WHERE login = '%s';", login));
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
}