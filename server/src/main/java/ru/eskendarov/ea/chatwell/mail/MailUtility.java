package ru.eskendarov.ea.chatwell.mail;

/*
 *
 * Класс MailUtility возвращает настройки, из файла хранящегося в папке ресурсов, для дальнейшей
 * отправки идентификационного ключа по E-Mail.
 *
 **/


import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Getter
class MailUtility {
    
    private String sourceAddress, sourcePassword;
    
    MailUtility() {
        try {
            BufferedReader reader = Files.newBufferedReader(Paths.get("mail.setting"));
            final String s = reader.readLine();
            if (s.startsWith("setting")) {
                sourceAddress = s.split("\\s")[1];
                sourcePassword = s.split("\\s")[2];
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}