package ru.eskendarov.ea.chatwell.client;

import lombok.Getter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Getter
class ResentMessages {
    
    private final String userDirectory = "chat.history";
    private String userNickname;
    private String userLogFile;
    private String pathToLogFile;
    private List<String> messages;
    
    private void init(final String userNickname) {
        this.userNickname = userNickname;
        this.userLogFile = userNickname + ".log";
        this.pathToLogFile = userDirectory + "/" + userLogFile;
    }
    private void createUserFiles(final String userNickname) {
        init(userNickname);
        try {
            try {
                try {
                    Files.createFile(Paths.get(pathToLogFile));
                } catch (NoSuchFileException e) {
                    Files.createDirectory(Paths.get(userDirectory));
                    Files.createFile(Paths.get(pathToLogFile));
                }
            } catch (FileAlreadyExistsException e) {
                System.out.println(e.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    synchronized List<String> getResentMessage(final String userNickname) {
        init(userNickname);
        try {
            try {
                messages = Files.lines(Paths.get(pathToLogFile), StandardCharsets.UTF_8).collect(Collectors.toList());
            } catch (NoSuchFileException e) {
                createUserFiles(userNickname);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return messages;
    }
    void saveChatStory(final String userNickname, final String messageIn) {
        init(userNickname);
        try {
            try {
                BufferedWriter br = Files.newBufferedWriter(Paths.get(pathToLogFile), StandardCharsets.UTF_8,
                                                            StandardOpenOption.APPEND);
                br.write(messageIn);
                br.newLine();
                br.flush();
                br.close();
            } catch (NoSuchFileException e) {
                createUserFiles(userNickname);
                saveChatStory(userNickname, messageIn);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}