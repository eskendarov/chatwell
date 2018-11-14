package ru.eskendarov.ea.chatwell.mail;

import org.junit.Test;

public class MailUtilityTest {
    
    @Test
    public void goTest() {
        System.out.println(new MailUtility().getSourceAddress());
        System.out.println(new MailUtility().getSourcePassword());
    }
}