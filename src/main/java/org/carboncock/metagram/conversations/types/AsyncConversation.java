package org.carboncock.metagram.conversations.types;

import org.carboncock.metagram.conversations.Conversation;
import org.carboncock.metagram.conversations.ConversationTemplate;

import java.util.function.Function;

public class AsyncConversation<T extends ConversationTemplate> extends Conversation<T> {
    @Override
    protected AsyncConversation<T> id(long id) {
        return null;
    }

    @Override
    protected AsyncConversation<T> template(T template) {
        return null;
    }

    @Override
    protected AsyncConversation<T> onClose(Function<T, String> func) {
        return null;
    }

    @Override
    protected AsyncConversation<T> onEverySubmit(Function<T, String> func) {
        return null;
    }

    @Override
    protected AsyncConversation<T> onSpecificSubmit(Function<T, String> func, int index) {
        return null;
    }

    @Override
    protected AsyncConversation<T> placeHolder(int index, String... placeholders) {
        return null;
    }

    @Override
    protected AsyncConversation<T> closeOnCallback(String callback, String text) {
        return null;
    }

    @Override
    protected AsyncConversation<T> closeOnCommand(String command, String text) {
        return null;
    }

    @Override
    protected AsyncConversation<T> build() {
        return null;
    }
}
