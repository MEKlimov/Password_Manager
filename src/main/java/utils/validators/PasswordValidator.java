package utils.validators;

public class PasswordValidator {

    // Вычисляет силу пароля от 0.0 (очень слабый) до 1.0 (идеальный)
    public static double calculateStrength(String password) {
        if (password == null || password.isEmpty()) return 0.0;
        
        double score = 0.0;
        
        if (password.length() >= 8) score += 0.2; // Базовая длина
        if (password.length() >= 12) score += 0.2; // Хорошая длина
        if (password.matches(".*[a-z].*") && password.matches(".*[A-Z].*")) score += 0.2; // Разный регистр
        if (password.matches(".*\\d.*")) score += 0.2; // Есть цифры
        if (password.matches(".*[!@#$%^&*()\\-_=+\\[\\]{}|;:,.<>?/].*")) score += 0.2; // Есть спецсимволы
        
        return Math.min(1.0, score);
    }

    // Возвращает текст для метки
    public static String getStrengthLabel(double score) {
        if (score == 0.0) return "";
        if (score <= 0.4) return "Слабый пароль";
        if (score <= 0.8) return "Средний пароль";
        return "Надежный пароль!";
    }

    // Возвращает CSS-стиль для цвета полоски
    public static String getStrengthColor(double score) {
        if (score <= 0.4) return "-fx-accent: red;";
        if (score <= 0.8) return "-fx-accent: orange;";
        return "-fx-accent: green;";
    }
}