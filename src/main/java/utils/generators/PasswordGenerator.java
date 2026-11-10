package utils.generators;

import java.security.SecureRandom;

public class PasswordGenerator {

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:,.<>?/";

    // Метод для генерации пароля
    public static String generate(int length, boolean useUpper, boolean useLower, boolean useDigits, boolean useSpecial) {
        StringBuilder charPool = new StringBuilder();

        // Собираем пул разрешенных символов на основе галочек
        if (useLower) charPool.append(LOWERCASE);
        if (useUpper) charPool.append(UPPERCASE);
        if (useDigits) charPool.append(DIGITS);
        if (useSpecial) charPool.append(SPECIAL);

        // Если пользователь убрал все галочки, по умолчанию используем строчные буквы
        if (charPool.length() == 0) {
            charPool.append(LOWERCASE);
        }

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        // Генерируем случайную строку заданной длины
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(charPool.length());
            password.append(charPool.charAt(randomIndex));
        }

        return password.toString();
    }
}