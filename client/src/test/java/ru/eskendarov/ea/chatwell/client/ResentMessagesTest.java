package ru.eskendarov.ea.chatwell.client;

import org.junit.Test;

import java.util.List;

public class ResentMessagesTest {
    
    private String login = "Eskendarov";
    
    @Test
    public void LogTest() {
        String messageIn = "Hello Java, How are you?";
        new ResentMessages().saveChatStory(login, messageIn);
        new ResentMessages().saveChatStory(login, login);
        new ResentMessages().getResentMessage(login).forEach(System.out::println);
    }
    @Test
    public void getList() {
        final List<String> stringList = new ResentMessages().getResentMessage(login);
        for (int i = stringList.size() - 100; i < stringList.size(); i++) {
            System.out.println(stringList.get(i));
        }
    }
}