import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
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

import java.io.IOException;

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
            //System.out.println(chatType); /**логирует запросы*/
            long userId = update.getMessage().getFrom().getId();

            if (chatType.contains("group")) {
                if (userMessage.equalsIgnoreCase("да") || userMessage.equalsIgnoreCase("да!") || userMessage.equalsIgnoreCase("да?")) {
                    answer = "что да?!\uD83D\uDE0E";
                    message.setReplyToMessageId(update.getMessage().getMessageId());
                } else if (userMessage.equalsIgnoreCase("работаешь?") || userMessage.equalsIgnoreCase("работаешь!")) {
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
                //System.out.println(answer);
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

    public String extractContentFromJson(String jsonResponse) {
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(jsonResponse).getAsJsonObject();
        System.out.println(json);
        String content = json.getAsJsonArray("choices")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("message")
                .get("content")
                .getAsString();

        return content;
    }

}