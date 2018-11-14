package ru.eskendarov.ea.chatwell.mail;

import org.junit.Test;

import java.util.UUID;

public class MailTest {
    
    private final String key = UUID.randomUUID().toString();
    
    @Test
    public void sendMail() {
        final String toAddress = "envereskendarov@gmail.com";
        System.out.println("Test key: " + key);
        final Mail mail = new Mail();
        mail.setHeadline("Сloud greets you! ;)");
        mail.setBodyMessage("Take it easy, it's just a key: ");
        mail.setDebugStatus(true);
        System.out.println(mail.getBodyMessage() + "\n" + mail.getHeadline());
        mail.sendToClient(key, toAddress);
    }
}