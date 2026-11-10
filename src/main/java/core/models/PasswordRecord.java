package core.models;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class PasswordRecord {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty category;
    private final SimpleStringProperty title;
    private final SimpleStringProperty username;
    private final SimpleStringProperty encryptedPassword;

    public PasswordRecord(int id, String category, String title, String username, String encryptedPassword) {
        this.id = new SimpleIntegerProperty(id);
        this.category = new SimpleStringProperty(category);
        this.title = new SimpleStringProperty(title);
        this.username = new SimpleStringProperty(username);
        this.encryptedPassword = new SimpleStringProperty(encryptedPassword);
    }

    // Геттеры для свойств (нужны для JavaFX TableView)
    public SimpleIntegerProperty idProperty() { return id; }
    public SimpleStringProperty categoryProperty() { return category; }
    public SimpleStringProperty titleProperty() { return title; }
    public SimpleStringProperty usernameProperty() { return username; }
    public SimpleStringProperty encryptedPasswordProperty() { return encryptedPassword; }

    // Обычные геттеры
    public int getId() { return id.get(); }
    public String getCategory() { return category.get(); }
    public String getTitle() { return title.get(); }
    public String getUsername() { return username.get(); }
    public String getEncryptedPassword() { return encryptedPassword.get(); }
}