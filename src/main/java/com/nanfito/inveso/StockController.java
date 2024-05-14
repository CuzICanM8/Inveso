package com.nanfito.inveso;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class StockController {

    String ticker;
    @Autowired
    private APIController apiController;

    @FXML
    private LineChart<Number, Number> stockChart;

    @FXML
    private NumberAxis yAxis;

    @FXML
    private TableView<DailyStockData> tableView;

    @FXML
    private TableColumn<DailyStockData, String> dateColumn;

    @FXML
    private TableColumn<DailyStockData, Double> priceColumn;

    @FXML
    private TableColumn<DailyStockData, Double> deltaColumn;

    @FXML
    private ListView<ArticleInfo> newsListView;

    @FXML
    private TextFlow summaryTextFlow;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressLabel;

    @FXML
    private Label timeRemainingLabel;

    private KeyPhraseExtractor keyPhraseExtractor;
    private NewsFetcher newsFetcher;
    private SentimentAnalyzer sentimentAnalyzer;
    private SummaryGenerator summaryGenerator;

    private List<String> keyPhrases;

    /**
     * Initialize the StockController.
     */
    public void initialize(String ticker) {
        this.ticker = ticker;
        configureTableView();
        loadStockData(ticker);
        loadStockGraph(ticker);

        keyPhraseExtractor = new KeyPhraseExtractor();
        newsFetcher = new NewsFetcher();
        sentimentAnalyzer = new SentimentAnalyzer();
        summaryGenerator = new SummaryGenerator();

        List<Article> rawArticles = newsFetcher.fetchNewsArticles(ticker);
        List<ArticleInfo> cleanArticles = processArticles(rawArticles);

        // Initialize progress bar
        Platform.runLater(() -> {
            progressBar.setProgress(0);
            progressLabel.setText("0/0 steps completed");
            timeRemainingLabel.setText("Estimated time remaining: calculating...");
        });

        // Serialize CoreNLP requests
        List<String> descriptions = rawArticles.stream()
                .map(article -> cleanText(article.getDescription()))
                .collect(Collectors.toList());

        // Start the process and update the progress bar
        processSentimentsAndKeyPhrases(descriptions, rawArticles);

        Platform.runLater(() -> {
            newsListView.getItems().clear();
            newsListView.getItems().addAll(cleanArticles);
            newsListView.setCellFactory(listView -> new ListCell<ArticleInfo>() {
                @Override
                protected void updateItem(ArticleInfo item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        Hyperlink link = new Hyperlink(item.getHeadline());
                        link.setOnAction(e -> openURL(item.getUrl()));
                        setGraphic(link);
                    }
                }
            });
        });
    }

    private void processSentimentsAndKeyPhrases(List<String> descriptions, List<Article> rawArticles) {
        long startTime = System.currentTimeMillis();
        int totalSteps = descriptions.size() * 3; // 1 step for queuing, 1 step for processing each description, and 1 step for each key phrase extraction
        List<CompletableFuture<Double>> sentimentFutures = new ArrayList<>();

        for (int i = 0; i < descriptions.size(); i++) {
            int index = i;
            CompletableFuture<Double> future = CompletableFuture.supplyAsync(() -> {
                updateProgress(index, totalSteps, startTime, "Queueing sentiment analysis...");
                return sentimentAnalyzer.analyzeSentiments(Collections.singletonList(descriptions.get(index))).join();
            }).thenApply(score -> {
                updateProgress(index + descriptions.size(), totalSteps, startTime, "Processing sentiment analysis...");
                return score;
            });
            sentimentFutures.add(future);
        }

        CompletableFuture<Void> allSentimentFutures = CompletableFuture.allOf(sentimentFutures.toArray(new CompletableFuture[0]));

        CompletableFuture<List<Double>> sentimentFuture = allSentimentFutures.thenApply(v ->
                sentimentFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
        );

        sentimentFuture.thenAccept(sentimentScores -> {
            long sentimentDuration = System.currentTimeMillis() - startTime;
            updateProgress(descriptions.size() * 2, totalSteps, sentimentDuration, "Completed sentiment analysis...");

            CompletableFuture<Void> keyPhrasesFuture = extractKeyPhrases(descriptions, totalSteps, sentimentDuration);

            keyPhrasesFuture.thenAccept(v2 -> {
                Platform.runLater(() -> {
                    if (sentimentScores.isEmpty()) {
                        summaryTextFlow.getChildren().setAll(new Text("No significant sentiment scores to display."));
                    } else {
                        String summary = summaryGenerator.generateSummary(rawArticles, sentimentScores, keyPhrases);
                        summaryTextFlow.getChildren().setAll(new Text(summary));
                    }
                });
                updateProgress(totalSteps, totalSteps, sentimentDuration + (System.currentTimeMillis() - startTime - sentimentDuration), "Extraction complete.");
            }).exceptionally(ex -> {
                Platform.runLater(() -> summaryTextFlow.getChildren().setAll(new Text("Error extracting key phrases: " + ex.getMessage())));
                return null;
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> summaryTextFlow.getChildren().setAll(new Text("Error analyzing sentiments: " + ex.getMessage())));
            return null;
        });
    }

    private void updateProgress(int completed, int total, long elapsedTime, String message) {
        Platform.runLater(() -> {
            progressBar.setProgress((double) completed / total);
            progressLabel.setText(message + " " + completed + "/" + total + " steps completed");
            long remainingTime = (elapsedTime / Math.max(1, completed)) * (total - completed);
            timeRemainingLabel.setText("Estimated time remaining: " + (remainingTime / 1000) + " seconds");
        });
    }

    private CompletableFuture<Void> extractKeyPhrases(List<String> descriptions, int totalSteps, long elapsedTime) {
        List<String> chunks = descriptions.stream()
                .map(description -> keyPhraseExtractor.chunkText(description, KeyPhraseExtractor.CHUNK_SIZE))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        int totalChunks = chunks.size();
        keyPhrases = Collections.synchronizedList(new ArrayList<>());

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                keyPhrases.addAll(keyPhraseExtractor.extractKeyPhrases(chunks.get(index)));
                updateProgress(descriptions.size() * 2 + keyPhrases.size(), totalSteps + totalChunks, elapsedTime, "Processing key phrases...");
            });
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Process the raw articles into cleaned ArticleInfo objects.
     */
    private List<ArticleInfo> processArticles(List<Article> articles) {
        return articles.stream()
                .map(article -> new ArticleInfo(article.getTitle(), article.getUrl()))
                .collect(Collectors.toList());
    }

    /**
     * Convert sentiment scores to human-readable labels.
     */
    private String convertScoreToLabel(double score) {
        if (score >= 1.5) return "Very Positive";
        else if (score >= 0.5) return "Positive";
        else if (score >= -0.5) return "Neutral";
        else if (score >= -1.5) return "Negative";
        else return "Very Negative";
    }

    /**
     * Configure the TableView's columns to display the stock data.
     */
    private void configureTableView() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("closePrice"));
        deltaColumn.setCellValueFactory(new PropertyValueFactory<>("percentChange"));
    }

    /**
     * Load and display stock data into the TableView.
     */
    private void loadStockData(String ticker) {
        PolygonApiResponse tableData = apiController.getStockTable(ticker);
        Platform.runLater(() -> {
            tableView.getItems().clear();
            tableData.getResults().forEach(result -> {
                LocalDateTime dateTime = LocalDateTime.ofEpochSecond(result.getT() / 1000, 0, ZoneOffset.UTC);
                String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                double percentChange = ((result.getC() - result.getO()) / result.getO()) * 100;
                tableView.getItems().add(new DailyStockData(formattedDate, result.getC(), ((double) Math.round(percentChange * 1000) / 1000) + "%"));
            });
        });
    }

    /**
     * Load and display stock price graph data.
     */
    private void loadStockGraph(String ticker) {
        PolygonApiResponse graphData = apiController.getStockGraph(ticker);
        Platform.runLater(() -> {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(ticker);

            if (!graphData.getResults().isEmpty()) {
                long minTimestamp = graphData.getResults().stream()
                        .min(Comparator.comparingLong(PolygonApiResponse.Result::getT))
                        .get()
                        .getT();

                graphData.getResults().forEach(result -> {
                    long daysSinceStart = TimeUnit.MILLISECONDS.toDays(result.getT() - minTimestamp);
                    series.getData().add(new XYChart.Data<>(daysSinceStart + 1, result.getC()));
                });

                double yMin = graphData.getResults().stream().mapToDouble(PolygonApiResponse.Result::getC).min().orElse(0);
                double yMax = graphData.getResults().stream().mapToDouble(PolygonApiResponse.Result::getC).max().orElse(0);
                double offset = 2.5;

                yAxis.setAutoRanging(false);
                yAxis.setLowerBound(yMin - offset);
                yAxis.setUpperBound(yMax + offset);
            }

            stockChart.getData().clear();
            stockChart.getData().add(series);
            stockChart.setLegendVisible(false);
            applyChartStyles();
        });
    }

    /**
     * Apply styles to the stock chart for better visuals.
     */
    private void applyChartStyles() {
        stockChart.setStyle("-fx-stroke: green; -fx-stroke-width: 2px;");
        stockChart.getData().forEach(series -> {
            Node line = series.getNode().lookup(".chart-series-line");
            if (line != null) {
                line.setStyle("-fx-stroke: green; -fx-stroke-width: 2px;");
            }
            for (XYChart.Data<Number, Number> data : series.getData()) {
                Node symbol = data.getNode();
                if (symbol != null) {
                    symbol.setStyle("-fx-background-color: transparent, transparent;");
                }
            }
        });
    }

    /**
     * Generate a URL for a news article based on the stock ticker and headline.
     */
    private String generateNewsArticleURL(String ticker, String headline) {
        return "https://news.example.com/search?q=" + ticker + "+" + headline.replaceAll("[^A-Za-z0-9]", "+");
    }

    /**
     * Open a URL in the default web browser.
     */
    private void openURL(String url) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                System.out.println("Error opening URL: " + e.getMessage());
            }
        } else {
            try {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
            } catch (Exception e) {
                System.out.println("Error opening URL via command line: " + e.getMessage());
            }
        }
    }

    /**
     * Clean a string to remove unwanted characters.
     */
    private String cleanText(String input) {
        return input.replaceAll("[^\\w\\s,.!?;:@'\"()-/$]+", "").trim(); // Filters out non-standard characters
    }

    /**
     * Data class representing a processed news article.
     */
    static class ArticleInfo {
        private final String headline;
        private final String url;

        public ArticleInfo(String headline, String url) {
            this.headline = headline;
            this.url = url;
        }

        public String getHeadline() {
            return headline;
        }

        public String getUrl() {
            return url;
        }
    }
}
