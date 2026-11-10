package ui.windows;

import core.crypto.CryptoService;
import core.database.DatabaseManager;
import core.models.PasswordRecord;
import utils.generators.PasswordGenerator;
import utils.validators.PasswordValidator;
import utils.api.PwnedChecker;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.crypto.SecretKey;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class MainWindow {

    private final SecretKey masterKey;
    private final ObservableList<PasswordRecord> masterData = FXCollections.observableArrayList();
    private FilteredList<PasswordRecord> filteredData;
    private String currentCategory = "Все";
    private PauseTransition clipboardTimer;

    public MainWindow(SecretKey masterKey) {
        this.masterKey = masterKey;
    }

    private void copyToClipboardSecurely(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);

        if (clipboardTimer != null) clipboardTimer.stop();

        clipboardTimer = new PauseTransition(Duration.seconds(30));
        clipboardTimer.setOnFinished(event -> {
            clipboard.clear();
            System.out.println("Буфер обмена очищен.");
        });
        clipboardTimer.play();
    }

    public void show() {
        Stage stage = new Stage();
        stage.setTitle("Умный менеджер паролей");
        App.activeStage = stage;

        BorderPane mainLayout = new BorderPane();

        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        Button btnAdd = new Button("Добавить");
        Button btnEdit = new Button("Редактировать");
        Button btnDelete = new Button("Удалить");
        Button btnCheckPwned = new Button("Проверить утечки"); // НОВАЯ КНОПКА
        Button btnExport = new Button("Экспорт");
        Button btnAudit = new Button("Журнал");
        Button btnGenerate = new Button("Генератор");
        TextField searchField = new TextField();
        searchField.setPromptText("Поиск...");
        
        toolbar.getChildren().addAll(btnAdd, btnEdit, btnDelete, new Separator(), btnCheckPwned, btnExport, btnAudit, btnGenerate, searchField);
        mainLayout.setTop(toolbar);

        VBox categoryPanel = new VBox(5);
        categoryPanel.setPadding(new Insets(10));
        categoryPanel.setPrefWidth(150);
        Button btnAll = new Button("Все");
        Button btnSocial = new Button("Соцсети");
        Button btnBanks = new Button("Банки");
        Button btnWork = new Button("Работа");
        Button btnPersonal = new Button("Личное");
        List<Button> categoryButtons = Arrays.asList(btnAll, btnSocial, btnBanks, btnWork, btnPersonal);
        categoryPanel.getChildren().add(new Label("Категории:"));
        categoryPanel.getChildren().addAll(categoryButtons);
        mainLayout.setLeft(categoryPanel);

        TableView<PasswordRecord> table = new TableView<>();
        TableColumn<PasswordRecord, String> titleCol = new TableColumn<>("Название");
        titleCol.setCellValueFactory(cellData -> cellData.getValue().titleProperty());
        TableColumn<PasswordRecord, String> categoryCol = new TableColumn<>("Категория");
        categoryCol.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
        TableColumn<PasswordRecord, String> userCol = new TableColumn<>("Логин");
        userCol.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
        TableColumn<PasswordRecord, String> passCol = new TableColumn<>("Пароль");
        passCol.setCellValueFactory(cellData -> cellData.getValue().encryptedPasswordProperty());
        
        passCol.setCellFactory(column -> new TableCell<>() {
            final Label passLabel = new Label("********");
            final Button copyIconBtn = new Button("📋");
            final HBox graphic = new HBox(10, passLabel, copyIconBtn);
            {
                graphic.setAlignment(Pos.CENTER_LEFT);
                copyIconBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                copyIconBtn.setOnAction(e -> {
                    PasswordRecord record = getTableRow().getItem();
                    if (record != null) {
                        try {
                            String decrypted = CryptoService.decrypt(record.getEncryptedPassword(), masterKey);
                            copyToClipboardSecurely(decrypted);
                            copyIconBtn.setText("✅");
                            new Thread(() -> {
                                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                                Platform.runLater(() -> copyIconBtn.setText("📋"));
                            }).start();
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : graphic);
            }
        });

        table.getColumns().addAll(titleCol, categoryCol, userCol, passCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        mainLayout.setCenter(table);

        filteredData = new FilteredList<>(masterData, p -> true);
        table.setItems(filteredData);
        Runnable updateFilter = () -> {
            String searchText = searchField.getText().toLowerCase();
            filteredData.setPredicate(record -> {
                boolean matchesCategory = currentCategory.equals("Все") || record.getCategory().equals(currentCategory);
                boolean matchesSearch = searchText.isEmpty() || 
                                    record.getTitle().toLowerCase().contains(searchText) || 
                                    record.getUsername().toLowerCase().contains(searchText);
                return matchesCategory && matchesSearch;
            });
        };
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());
        for (Button btn : categoryButtons) {
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> {
                currentCategory = btn.getText();
                categoryButtons.forEach(b -> b.setStyle(""));
                btn.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-font-weight: bold;");
                updateFilter.run();
            });
        }

        Runnable refreshTable = () -> {
            masterData.setAll(DatabaseManager.getAllRecords());
            updateFilter.run();
        };
        refreshTable.run(); 
        btnAll.fire(); 

        btnDelete.setOnAction(e -> {
            PasswordRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                DatabaseManager.deleteRecord(selected.getId());
                refreshTable.run();
            }
        });
        btnAdd.setOnAction(e -> showAddDialog(refreshTable));
        btnEdit.setOnAction(e -> {
            PasswordRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showEditDialog(selected, refreshTable);
        });
        btnGenerate.setOnAction(e -> showGeneratorDialog());
        btnAudit.setOnAction(e -> showAuditDialog());
        btnExport.setOnAction(e -> handleExport(stage));

        // --- ЛОГИКА ПРОВЕРКИ НА УТЕЧКИ ---
        btnCheckPwned.setOnAction(e -> {
            PasswordRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Сначала выберите запись в таблице!").show();
                return;
            }

            btnCheckPwned.setDisable(true);
            btnCheckPwned.setText("Загрузка...");

            // Запускаем сетевой запрос в отдельном потоке
            new Thread(() -> {
                try {
                    String decrypted = CryptoService.decrypt(selected.getEncryptedPassword(), masterKey);
                    int count = PwnedChecker.checkPassword(decrypted);

                    // Возвращаемся в главный поток для отрисовки интерфейса
                    Platform.runLater(() -> {
                        btnCheckPwned.setDisable(false);
                        btnCheckPwned.setText("Проверить утечки");

                        if (count > 0) {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("ВНИМАНИЕ! Утечка данных");
                            alert.setHeaderText("Этот пароль скомпрометирован.");
                            alert.setContentText("Мы нашли этот пароль в " + count + " известных утечках хакерских баз данных. Настоятельно рекомендуем изменить его в сервисе " + selected.getTitle() + " как можно скорее!");
                            alert.show();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Всё чисто");
                            alert.setHeaderText("Пароль безопасен");
                            alert.setContentText("Этот пароль не был найден в публичных утечках. Можно спать спокойно.");
                            alert.show();
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        btnCheckPwned.setDisable(false);
                        btnCheckPwned.setText("Проверить утечки");
                        new Alert(Alert.AlertType.ERROR, "Ошибка проверки! Проверьте подключение к интернету.").show();
                    });
                }
            }).start();
        });

        Scene scene = new Scene(mainLayout, 1050, 600);
        
        PauseTransition lockTimer = new PauseTransition(Duration.minutes(3)); // Тестовые 15 секунд
        lockTimer.setOnFinished(e -> {
            stage.close(); 
            System.out.println("Автоблокировка: окно таблицы уничтожено.");
            App.showLoginScreen(new Stage());
        });

        scene.addEventFilter(InputEvent.ANY, event -> lockTimer.playFromStart());
        lockTimer.play(); 

        stage.setOnCloseRequest(event -> {
            event.consume();
            stage.hide();
            System.out.println("Таблица свернута в трей.");
        });

        stage.setScene(scene);
        stage.show();
    }

    private void handleExport(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить резервную копию");
        fileChooser.setInitialFileName("backup.enc");
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                StringBuilder sb = new StringBuilder();
                for (PasswordRecord r : DatabaseManager.getAllRecords()) {
                    sb.append(r.getCategory()).append("|").append(r.getTitle()).append("|")
                      .append(r.getUsername()).append("|").append(r.getEncryptedPassword()).append("\n");
                }
                Files.writeString(file.toPath(), sb.toString());
                new Alert(Alert.AlertType.INFORMATION, "Экспорт завершен!").show();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    private void showGeneratorDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Генератор паролей");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        TextField resultField = new TextField();
        resultField.setEditable(false);
        resultField.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 16px;");

        HBox lengthBox = new HBox(10);
        lengthBox.setAlignment(Pos.CENTER_LEFT);
        Label lengthLabel = new Label("Длина: 16");
        lengthLabel.setMinWidth(100); 
        Slider lengthSlider = new Slider(8, 64, 16);
        lengthSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            lengthLabel.setText("Длина: " + newVal.intValue()));
        lengthBox.getChildren().addAll(lengthLabel, lengthSlider);

        CheckBox cbUpper = new CheckBox("A-Z"); cbUpper.setSelected(true);
        CheckBox cbLower = new CheckBox("a-z"); cbLower.setSelected(true);
        CheckBox cbDigits = new CheckBox("0-9"); cbDigits.setSelected(true);
        CheckBox cbSpecial = new CheckBox("!@#$");

        HBox actionBox = new HBox(10);
        Button generateBtn = new Button("Сгенерировать");
        Button copyBtn = new Button("Копировать");
        actionBox.getChildren().addAll(generateBtn, copyBtn);

        generateBtn.setOnAction(e -> resultField.setText(PasswordGenerator.generate(
                (int) lengthSlider.getValue(), cbUpper.isSelected(), cbLower.isSelected(), 
                cbDigits.isSelected(), cbSpecial.isSelected())));
        
        copyBtn.setOnAction(e -> {
            if (!resultField.getText().isEmpty()) copyToClipboardSecurely(resultField.getText());
        });

        generateBtn.fire();
        content.getChildren().addAll(resultField, lengthBox, cbUpper, cbLower, cbDigits, cbSpecial, actionBox);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void showAuditDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Аудит безопасности");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        ListView<String> listView = new ListView<>(FXCollections.observableArrayList(DatabaseManager.getAuditLogs()));
        listView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setStyle(item.contains("ОШИБКА") ? "-fx-text-fill: red; -fx-font-weight: bold;" : "-fx-text-fill: green;");
                }
            }
        });
        listView.setPrefSize(400, 300);
        dialog.getDialogPane().setContent(listView);
        dialog.showAndWait();
    }

    private void showAddDialog(Runnable onSuccess) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Новая запись");
        ButtonType saveBtnType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        ComboBox<String> categoryBox = new ComboBox<>(FXCollections.observableArrayList("Соцсети", "Банки", "Работа", "Личное"));
        categoryBox.setValue("Личное");
        TextField titleField = new TextField();
        TextField userField = new TextField();
        PasswordField passField = new PasswordField(); 
        ProgressBar strengthBar = new ProgressBar(0);
        strengthBar.setMaxWidth(Double.MAX_VALUE);
        Label strengthLabel = new Label("");
        VBox strengthBox = new VBox(2, strengthBar, strengthLabel);
        passField.textProperty().addListener((obs, oldVal, newVal) -> {
            double score = PasswordValidator.calculateStrength(newVal);
            strengthBar.setProgress(score);
            strengthBar.setStyle(PasswordValidator.getStrengthColor(score));
            strengthLabel.setText(PasswordValidator.getStrengthLabel(score));
        });
        grid.add(new Label("Категория:"), 0, 0); grid.add(categoryBox, 1, 0);
        grid.add(new Label("Название:"), 0, 1); grid.add(titleField, 1, 1);
        grid.add(new Label("Логин:"), 0, 2); grid.add(userField, 1, 2);
        grid.add(new Label("Пароль:"), 0, 3); grid.add(passField, 1, 3);
        grid.add(new Label("Сила:"), 0, 4); grid.add(strengthBox, 1, 4);
        dialog.getDialogPane().setContent(grid);
        final Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                String encryptedPass = CryptoService.encrypt(passField.getText(), masterKey);
                DatabaseManager.addRecord(categoryBox.getValue(), titleField.getText(), userField.getText(), encryptedPass);
                onSuccess.run();
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        dialog.showAndWait();
    }

    private void showEditDialog(PasswordRecord record, Runnable onSuccess) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Редактирование");
        ButtonType saveBtnType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        ComboBox<String> categoryBox = new ComboBox<>(FXCollections.observableArrayList("Соцсети", "Банки", "Работа", "Личное"));
        categoryBox.setValue(record.getCategory());
        TextField titleField = new TextField(record.getTitle());
        TextField userField = new TextField(record.getUsername());
        PasswordField passField = new PasswordField();
        ProgressBar strengthBar = new ProgressBar(0);
        strengthBar.setMaxWidth(Double.MAX_VALUE);
        Label strengthLabel = new Label("");
        VBox strengthBox = new VBox(2, strengthBar, strengthLabel);
        passField.textProperty().addListener((obs, oldVal, newVal) -> {
            double score = PasswordValidator.calculateStrength(newVal);
            strengthBar.setProgress(score);
            strengthBar.setStyle(PasswordValidator.getStrengthColor(score));
            strengthLabel.setText(PasswordValidator.getStrengthLabel(score));
        });
        try { passField.setText(CryptoService.decrypt(record.getEncryptedPassword(), masterKey)); } catch (Exception ex) { ex.printStackTrace(); }
        grid.add(new Label("Категория:"), 0, 0); grid.add(categoryBox, 1, 0);
        grid.add(new Label("Название:"), 0, 1); grid.add(titleField, 1, 1);
        grid.add(new Label("Логин:"), 0, 2); grid.add(userField, 1, 2);
        grid.add(new Label("Пароль:"), 0, 3); grid.add(passField, 1, 3);
        grid.add(new Label("Сила:"), 0, 4); grid.add(strengthBox, 1, 4);
        dialog.getDialogPane().setContent(grid);
        final Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                String encryptedPass = CryptoService.encrypt(passField.getText(), masterKey);
                DatabaseManager.updateRecord(record.getId(), categoryBox.getValue(), titleField.getText(), userField.getText(), encryptedPass);
                onSuccess.run();
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        dialog.showAndWait();
    }
}