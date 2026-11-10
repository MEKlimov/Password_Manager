package ui.windows;

import core.database.DatabaseManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.crypto.SecretKey;
import java.awt.*;

public class App extends Application {

    // Глобальная ссылка на текущее активное окно (входа или главное)
    public static Stage activeStage; 

    @Override
    public void start(Stage primaryStage) {
        DatabaseManager.initialize();
        Platform.setImplicitExit(false);
        createTrayIcon(); // Инициализируем трей один раз
        showLoginScreen(primaryStage);
    }

    public static void showLoginScreen(Stage stage) {
        activeStage = stage; // Делаем окно входа текущим активным окном
        boolean isFirstRun = !DatabaseManager.isMasterPasswordSet();

        stage.setTitle(isFirstRun ? "Создание базы" : "Авторизация");
        stage.setAlwaysOnTop(true);
        stage.setAlwaysOnTop(false);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));

        Label label = new Label(isFirstRun ? "Придумайте Мастер-пароль (от 8 символов):" : "Введите ваш Мастер-пароль:");
        PasswordField passwordField = new PasswordField();
        Button loginButton = new Button(isFirstRun ? "Создать базу" : "Войти");

        loginButton.setOnAction(e -> {
            String masterPassword = passwordField.getText();
            
            if (masterPassword.length() < 8) {
                if (!isFirstRun) DatabaseManager.logLoginAttempt(false);
                passwordField.clear(); // Очищаем поле при ошибке
                new Alert(Alert.AlertType.ERROR, "Пароль слишком короткий!").show();
                return;
            }

            try {
                if (isFirstRun) {
                    DatabaseManager.setMasterPassword(masterPassword);
                    new Alert(Alert.AlertType.INFORMATION, "Мастер-пароль успешно создан! Теперь войдите.").showAndWait();
                    passwordField.clear(); // Очищаем поле
                    showLoginScreen(stage);
                } else {
                    SecretKey key = DatabaseManager.verifyMasterPassword(masterPassword);
                    if (key != null) {
                        DatabaseManager.logLoginAttempt(true);
                        
                        // КРИТИЧЕСКИЙ ФИКС БЕЗОПАСНОСТИ: Удаляем пароль из UI
                        passwordField.clear(); 
                        
                        MainWindow mainWindow = new MainWindow(key);
                        mainWindow.show();
                        stage.hide(); // Прячем пустое окно авторизации
                    } else {
                        DatabaseManager.logLoginAttempt(false);
                        passwordField.clear(); // Удаляем неверный пароль из UI
                        new Alert(Alert.AlertType.ERROR, "Неверный Мастер-пароль!").show();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace(); 
                new Alert(Alert.AlertType.ERROR, "Произошла системная ошибка! Проверьте терминал.").show();
            }
        });

        layout.getChildren().addAll(label, passwordField, loginButton);
        Scene scene = new Scene(layout, 300, 150);
        stage.setScene(scene);
        stage.show();
    }

    private void createTrayIcon() {
        if (!SystemTray.isSupported()) return;

        try {
            SystemTray tray = SystemTray.getSystemTray();
            java.awt.Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));
            PopupMenu popup = new PopupMenu();

            MenuItem showItem = new MenuItem("Развернуть");
            showItem.addActionListener(e -> Platform.runLater(() -> {
                // Разворачиваем именно то окно, которое сейчас активно (login или main)
                if (activeStage != null) {
                    activeStage.show();
                    activeStage.toFront();
                }
            }));

            MenuItem exitItem = new MenuItem("Выход");
            exitItem.addActionListener(e -> {
                Platform.exit();
                System.exit(0);
            });

            popup.add(showItem);
            popup.addSeparator();
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "Password Manager", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                if (activeStage != null) {
                    activeStage.show();
                    activeStage.toFront();
                }
            }));

            tray.add(trayIcon);
        } catch (Exception e) {
            System.err.println("Не удалось инициализировать трей: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}