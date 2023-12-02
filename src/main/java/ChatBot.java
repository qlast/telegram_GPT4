import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.util.EntityUtils;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.util.List;

public class ChatBot extends TelegramLongPollingBot {
    private final Properties properties = new Properties();
    private final String TELEGRAM_TOKEN = properties.getTelegramToken();
    private final String OPENAI_TOKEN = properties.getOpenAiToken();
    private final String NAME = properties.getNameBot();
    private final long ALLOWED_USERS = properties.getAllowedUsers();


    @Override
    public String getBotToken() {
        return TELEGRAM_TOKEN;
    }

    @Override
    public String getBotUsername() {
        return NAME;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String userMessage = update.getMessage().getText();
            Message receivedMessage = update.getMessage();
            SendMessage message = new SendMessage();
            String answer = "";
            //  System.out.println(userMessagxe);

            String chatType = receivedMessage.getChat().getType();
            System.out.println(chatType);
            long userId = update.getMessage().getFrom().getId();

            if (chatType.contains("group")) {
                if (userMessage.equalsIgnoreCase("да") || userMessage.equalsIgnoreCase("да!") || userMessage.equalsIgnoreCase("да?")) {
                    answer = "Пизда!\uD83D\uDE0E";
                    message.setReplyToMessageId(update.getMessage().getMessageId());
                } else if (userMessage.equalsIgnoreCase("пизда") || userMessage.equalsIgnoreCase("пизда!")) {
                    answer = "Да!\uD83D\uDE0E";
                    message.setReplyToMessageId(update.getMessage().getMessageId());
                } else if (userMessage.contains("@" + NAME)) {
                    String gptResponseJson = getGPT4Response(userMessage);
                    answer = extractContentFromJson(gptResponseJson);
                    message.setReplyToMessageId(update.getMessage().getMessageId());
                } else if (receivedMessage.isReply()) {
                    Message repliedMessage = receivedMessage.getReplyToMessage();
                    if (repliedMessage.getFrom().getIsBot() && repliedMessage.getFrom().getUserName().equals(NAME)) {
                        String gptResponseJson = getGPT4Response(userMessage);
                        answer = extractContentFromJson(gptResponseJson);
                        message.setReplyToMessageId(update.getMessage().getMessageId());
                    }
                }
            } else if (ALLOWED_USERS == userId) {
                String gptResponseJson = getGPT4Response(userMessage);
                answer = extractContentFromJson(gptResponseJson);
            } else
                answer = "У тебя нет доступа \uD83E\uDD72";


            if (!answer.isEmpty()) {
//                System.out.println(answer);
                message.setChatId(update.getMessage().getChatId());
                message.setText(answer);

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public String getGPT4Response(String prompt) {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost("https://api.openai.com/v1/chat/completions"); // URL может изменяться в зависимости от документации OpenAI

        // Установите заголовки
        httpPost.setHeader("Authorization", OPENAI_TOKEN); // Замените на ваш API ключ
        httpPost.setHeader("Content-Type", "application/json");

        // Установите тело запроса
        prompt = prompt.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
        String jsonBody = "{\"model\":\"gpt-4\",\"messages\":[{\"role\":\"system\",\"content\":\"\"},{\"role\":\"user\",\"content\":\"" + prompt + "!\"}]}";
        StringEntity entity = new StringEntity(jsonBody, "UTF-8");
        httpPost.setEntity(entity);

        CloseableHttpResponse response;
        try {
            response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();

            // Преобразуйте ответ в строку
            String responseBody = EntityUtils.toString(responseEntity);

            // Закройте соединения
            response.close();
            httpClient.close();

            return responseBody;
        } catch (IOException e) {
            e.printStackTrace();
            return "Error occurred while fetching response from GPT-4";
        }
    }

//    public String extractContentFromJson(String jsonResponse) {
//        try {
//            JsonParser parser = new JsonParser();
//            JsonObject json = parser.parse(jsonResponse).getAsJsonObject();
//
//            // Вывод содержимого JSON для отладки
//            System.out.println("JSON Response: " + json);
//
//            String content = json.getAsJsonArray("choices")
//                    .get(0)
//                    .getAsJsonObject()
//                    .getAsJsonObject("message")
//                    .get("content")
//                    .getAsString();
//
//            return content;
//        } catch (JsonSyntaxException e) {
//            // Логирование ошибки разбора JSON
//            System.err.println("Error parsing JSON response: " + jsonResponse);
//            e.printStackTrace();
//            return "Error parsing JSON response";
//        }
//    }
public String extractContentFromJson(String jsonResponse) {
    try {
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(jsonResponse).getAsJsonObject();

        // Проверка существования массива "choices"
        if (json.has("choices") && json.get("choices").isJsonArray()) {
            JsonArray choicesArray = json.getAsJsonArray("choices");

            // Проверка наличия элементов в массиве "choices"
            if (choicesArray.size() > 0) {
                JsonObject firstChoice = choicesArray.get(0).getAsJsonObject();

                // Проверка существования объекта "message"
                if (firstChoice.has("message") && firstChoice.get("message").isJsonObject()) {
                    JsonObject messageObject = firstChoice.getAsJsonObject("message");

                    // Проверка существования свойства "content"
                    if (messageObject.has("content")) {
                        String content = messageObject.get("content").getAsString();
                        return content;
                    }
                }
            }
        }

        return "Invalid JSON structure"; // Или другое сообщение об ошибке
    } catch (JsonSyntaxException e) {
        System.err.println("Error parsing JSON response: " + jsonResponse);
        e.printStackTrace();
        return "Error parsing JSON response";
    }
}
}