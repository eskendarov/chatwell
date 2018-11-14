package ru.eskendarov.ea.chatwell.mail;

/*
 *
 * Класс Mail реализовывает метод, который принимает на вход идентификационный ключ
 * и адрес электронной почты (являющийся также и уникальным логином пользователя).
 *
 * */

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Mail {
    
    private String headline = "ChatWell Authorization";
    private String bodyMessage = "Ваш ключ для авторизации: ";
    private boolean debugStatus = false;
    private String keyUUID, sendToMailAddress;
    
    public void sendToClient(final String keyUUID, final String sendToMailAddress) {
        final MailUtility utility = new MailUtility();
        try {
            Email email = new SimpleEmail();
            email.setSmtpPort(587);
            email.setAuthenticator(new DefaultAuthenticator(utility.getSourceAddress(), utility.getSourcePassword()));
            email.setDebug(debugStatus);
            email.setHostName("smtp.gmail.com");
            email.setFrom(utility.getSourceAddress());
            email.setSubject(headline);
            email.setMsg(bodyMessage + keyUUID);
            email.addTo(sendToMailAddress);
            email.setTLS(true);
            email.send();
            System.out.println("Код на почту отправлен! \n[" + keyUUID + "]");
        } catch (Exception e) {
            System.out.println("Ошибка отправки кода на почту :: " + e);
        }
    }
}