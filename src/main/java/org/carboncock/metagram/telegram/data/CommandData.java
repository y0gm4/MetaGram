package org.carboncock.metagram.telegram.data;

import lombok.Getter;
import lombok.ToString;
import org.carboncock.metagram.annotations.Command;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Optional;

@ToString
@Getter
public class CommandData extends Data<Command> {

    private String cName;

    private String[] aliases;

    private final User sender;

    private final String[] args;

    private Optional<Message> reply;

    public CommandData(String[] args, User sender, Optional<Message> reply, TelegramLongPollingBot botInstance, Update update){
        this.args = args;
        this.sender = sender;
        this.reply = reply;
        this.botInstance = botInstance;
        this.update = update;
    }

    @Override
    protected void onProcessAnnotation(Command command) {
        this.cName = command.value();
        this.aliases = command.aliases();
    }
}
