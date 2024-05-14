package com.nanfito.inveso;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class MainMenuController {

    @FXML
    private TextField searchField;

    @FXML
    private ListView<String> suggestionsListView;

    private final ObservableList<String> suggestions = FXCollections.observableArrayList();
    private final Map<String, String> companyData = new HashMap<>();

    @FXML
    public void initialize() {
        loadCompanyData();
        suggestionsListView.setItems(suggestions);
        suggestionsListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-background-color: #2c2c2c; -fx-text-fill: #ffffff;");
                }
            }
        });

        searchField.textProperty().addListener((obs, oldText, newText) -> filterSuggestions(newText));
        searchField.focusedProperty().addListener((obs, oldVal, newVal) -> suggestionsListView.setVisible(!newVal));

        suggestionsListView.setOnMouseClicked(event -> {
            String selectedItem = suggestionsListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                openStockPage(selectedItem);
            }
        });
        searchField.setOnAction(event -> {
            String selectedItem = searchField.getText();
            if (selectedItem != null && !selectedItem.isEmpty()) {
                openStockPage(selectedItem);
            }
        });
    }

    private void loadCompanyData() {
        // Use a relative path starting from the project's root directory
        String relativePath = "src/main/java/com/nanfito/inveso/companies.txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(relativePath))) {
            Spliterator<String> spliterator = reader.lines().spliterator();
            Stream<String> stream = StreamSupport.stream(spliterator, false);

            stream.forEach(line -> {
                String[] parts = line.split("\t");
                companyData.put(parts[0], parts[1]);  // Company Name as key, Ticker as value
                suggestions.add(parts[0]);  // Company Name
                suggestions.add(parts[1]);  // Ticker
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void filterSuggestions(String query) {
        if (query.isEmpty()) {
            suggestionsListView.setVisible(false);
        } else {
            ObservableList<String> filteredSuggestions = suggestions.filtered(s -> s.toUpperCase().contains(query.toUpperCase()));
            suggestionsListView.setItems(filteredSuggestions);
            suggestionsListView.setVisible(true);
        }
    }

    private void openStockPage(String selectedItem) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nanfito/inveso/Stock.fxml"));
            loader.setControllerFactory(clazz -> Application.getSpringContext().getBean(clazz));
            Parent root = loader.load();

            StockController stockController = loader.getController();


            Stage stage = new Stage();
           // stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            stage.show();

            stockController.initialize(determineTicker(selectedItem));

           // searchField.getScene().getWindow().hide(); // Close the current window
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String determineTicker(String selectedItem) throws IllegalArgumentException {
        if (companyData.containsValue(selectedItem)) {
            return selectedItem;  // Return as is if it's a ticker
        }
        String ticker = companyData.get(selectedItem);
        if (ticker != null) {
            return ticker;
        } else {
            throw new IllegalArgumentException("Ticker not found for: " + selectedItem);
        }
    }
}
