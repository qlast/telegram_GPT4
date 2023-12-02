import java.io.*;//ss

public class Properties {
    private String telegramToken;
    private String openAiToken;
    private String nameBot;
    private long allowedUsers;

    public String getTelegramToken() {
        return telegramToken;
    }

    public String getOpenAiToken() {
        return openAiToken;
    }

    public String getNameBot() {
        return nameBot;
    }

    public long getAllowedUsers() {
        return allowedUsers;
    }

    public Properties() {

        FileInputStream fis;
        java.util.Properties property = new java.util.Properties();

        try {
            fis = new FileInputStream("src/main/resources/config.properties");
            property.load(fis);

            telegramToken = property.getProperty("token_telegram");
            openAiToken = property.getProperty("openai_token");
            nameBot = property.getProperty("name_bot");
            allowedUsers = Long.parseLong(property.getProperty("allowed_users"));

            System.out.println( " telegramToken: " + telegramToken);
            System.out.println(" openAiToken: " + openAiToken);
            System.out.println(" nameBot: " + nameBot);
            System.out.println(" allowedUsers: " + allowedUsers);
        } catch (IOException e) {
            System.err.println("ОШИБКА: Файл свойств отсуствует!");
        }

    }

}