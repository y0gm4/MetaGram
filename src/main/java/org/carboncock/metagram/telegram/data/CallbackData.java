package org.carboncock.metagram.telegram.data;

import lombok.Getter;
import lombok.ToString;
import org.carboncock.metagram.annotation.Callback;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;

@ToString
@Getter
public class CallbackData extends Data<Callback> {

    private String cValue;

    private final Map<String, Object> parameters;

    public CallbackData(TelegramLongPollingBot botInstance, Update update, Map<String, Object> parameters){
        this.botInstance = botInstance;
        this.update = update;
        this.parameters = parameters;
    }

    @Override
    protected void onProcessAnnotation(Callback callback) {
        cValue = callback.value();
    }
}
