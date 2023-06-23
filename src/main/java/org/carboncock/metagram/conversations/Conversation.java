package org.carboncock.metagram.conversations;

import org.carboncock.metagram.conversations.types.AsyncConversation;
import org.carboncock.metagram.conversations.types.SyncConversation;

import java.util.Map;
import java.util.function.Function;

public abstract class Conversation<T extends ConversationTemplate> {

    protected long id;

    protected T template;

    // TODO add the filter

    protected Function<T, String> close;

    protected Function<T, String> everySubmit;

    protected Function<T, String> specificSubmit;

    protected int specificSubmitIndex;

    protected Map<Integer, String[]> placeHolders;

    protected String closeCallback;

    protected String closeCallbackText;

    protected String closeCommand;

    protected String closeCommandText;

    public static AsyncConversation<? extends ConversationTemplate> asyncConversation(){
        return new AsyncConversation<>();
    }

    public static SyncConversation<? extends ConversationTemplate> syncConversation(){
        return new SyncConversation<>();
    }

    protected abstract Conversation<T> id(long id);
    protected abstract Conversation<T> template(T template);
    protected abstract Conversation<T> onClose(Function<T, String> func);
    protected abstract Conversation<T> onEverySubmit(Function<T, String> func);
    protected abstract Conversation<T> onSpecificSubmit(Function<T, String> func, int index);
    protected abstract Conversation<T> placeHolder(int index, String... placeholders);
    protected abstract Conversation<T> closeOnCallback(String callback, String text);
    protected abstract Conversation<T> closeOnCommand(String command, String text);
    protected abstract Conversation<T> build();
}
