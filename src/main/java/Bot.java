/*
 * Copyright (c) Grigory "vcusnx" Markovich, 2023.
 */


import com.cjcrafter.openai.OpenAI;
import com.cjcrafter.openai.chat.ChatMessage;
import com.cjcrafter.openai.chat.ChatRequest;
import com.cjcrafter.openai.chat.ChatResponse;
import com.cjcrafter.openai.exception.OpenAIError;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class Bot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return Helpers.TELEGRAM_BOT_NAME;
    }

    @Override
    public String getBotToken() {
        return Tokens.TELEGRAM;
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.printf(
                "User: %s,%nMessage: %s%n%n",
                update.getMessage().getChat().getFirstName(),
                update.getMessage().getText());

        try {
            answer(update);
        } catch (TelegramApiException | OpenAIError e) {
            throw new RuntimeException(e);
        }
    }

    public void answer(Update update) throws TelegramApiException, OpenAIError {

        // Telegram
        SendMessage message = new SendMessage();

        if (update.hasMessage() && update.getMessage().hasText()) {

            // Proceed messages in Telegram
            if (update.getMessage().getText().equals("/start")) {
                message.setChatId(update.getMessage().getChatId().toString());
                message.setReplyToMessageId(update.getMessage().getMessageId());
                message.setParseMode(ParseMode.MARKDOWN);
                message.setText(Texts.GREETINGS
                        .formatted(update.getMessage().getChat().getFirstName()));

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if (update.getMessage().getText().equals("/help")) {
                message.setChatId(update.getMessage().getChatId().toString());
                message.setReplyToMessageId(update.getMessage().getMessageId());
                message.setParseMode(ParseMode.MARKDOWN);
                message.setText(Texts.HELP);

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else {
                String text;

                // ChatGPT
                OpenAI openai = new OpenAI(Tokens.CHAT_GPT);
                ChatMessage chatMessage;
                List<ChatMessage> messages;
                ChatRequest request;
                ChatResponse response;

                chatMessage = ChatMessage.toSystemMessage(update.getMessage().getText());

                messages = new ArrayList<>(List.of(chatMessage));
                request = ChatRequest.builder()
                        .model("gpt-3.5-turbo")
                        .messages(messages).build();

                messages.add(ChatMessage.toUserMessage(String.valueOf(chatMessage)));
                response = openai.createChatCompletion(request);
                text = response.get(0).getMessage().getContent();

                System.out.println("Bot: " + text);

                // Telegram
                message.setChatId(update.getMessage().getChatId().toString());
                message.setReplyToMessageId(update.getMessage().getMessageId());
                message.setText(text);

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
