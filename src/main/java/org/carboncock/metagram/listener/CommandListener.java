package org.carboncock.metagram.listener;

import org.carboncock.metagram.telegram.data.CommandData;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface CommandListener extends Listener {
    void onCommand(CommandData cmd);

    default void onHelpCommand(TelegramLongPollingBot bot, Update update) { }
}
