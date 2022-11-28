package org.carboncock.metagram.listener;

import org.carboncock.metagram.telegram.data.CallbackData;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface CallbackListener extends Listener {
    void onCallback(CallbackData callbackData);
}
