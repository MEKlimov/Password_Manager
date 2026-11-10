package utils.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;

public class PwnedChecker {

    public static int checkPassword(String plainPassword) throws Exception {
        // 1. Хэшируем пароль алгоритмом SHA-1
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = md.digest(plainPassword.getBytes("UTF-8"));
        
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02X", b));
        }
        String fullHash = sb.toString();

        // 2. Разделяем хэш: префикс (первые 5 символов) отправляем, суффикс (остаток) ищем сами
        String prefix = fullHash.substring(0, 5);
        String suffix = fullHash.substring(5);

        // 3. Отправляем GET-запрос к API HIBP
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.pwnedpasswords.com/range/" + prefix))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 4. Парсим ответ
        if (response.statusCode() == 200) {
            String[] lines = response.body().split("\n");
            for (String line : lines) {
                String[] parts = line.split(":");
                // Если наш суффикс есть в ответе сервера
                if (parts[0].trim().equals(suffix)) {
                    return Integer.parseInt(parts[1].trim()); // Возвращаем количество утечек
                }
            }
        }
        return 0; // Пароль безопасен, утечек не найдено
    }
}