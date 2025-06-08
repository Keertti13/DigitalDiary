package com.example.myapp.demo1;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MoodTracker {
    private final ObservableList<DiaryEntryWithImage> diaryEntries;

    public MoodTracker(ObservableList<DiaryEntryWithImage> diaryEntries) {
        this.diaryEntries = diaryEntries;
    }

    public void showMoodTrackerDialog() {
        Stage stage = new Stage();
        stage.setTitle("Mood Tracker - Mood Trends");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        Label titleLabel = new Label("Mood Tracker");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();

        HBox dateRangeBox = new HBox(10);
        dateRangeBox.getChildren().addAll(
                new Label("Start Date:"), startDatePicker,
                new Label("End Date:"), endDatePicker
        );

        LineChart<String, Number> moodLineChart = createMoodLineChart();

        Button trackButton = new Button("Track Mood");
        trackButton.setOnAction(e -> updateMoodLineChart(moodLineChart, startDatePicker.getValue(), endDatePicker.getValue()));

        layout.getChildren().addAll(titleLabel, dateRangeBox, trackButton, moodLineChart);

        Scene scene = new Scene(layout, 1200, 800); // Full-screen chart
        stage.setScene(scene);
        stage.setMaximized(true); // Automatically maximize the window
        stage.show();
    }

    private LineChart<String, Number> createMoodLineChart() {
        CategoryAxis xAxis = new CategoryAxis(); // X-axis for dates
        xAxis.setLabel("Date");

        NumberAxis yAxis = new NumberAxis(0, 10, 1); // Y-axis for mood counts, whole number increments
        yAxis.setLabel("Mood Count");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Mood Trends Over Time");
        lineChart.setCreateSymbols(true); // Enable points on the line
        lineChart.setLegendVisible(true); // Show legend for better clarity
        return lineChart;
    }

    private void updateMoodLineChart(LineChart<String, Number> lineChart, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            showAlert(Alert.AlertType.ERROR, "Date Error", "Please select both start and end dates.");
            return;
        }

        // Filter diary entries within the selected date range
        List<DiaryEntryWithImage> filteredEntries = diaryEntries.stream()
                .filter(entry -> {
                    LocalDate entryDate = entry.getEntryTime().toLocalDate();
                    return !entryDate.isBefore(startDate) && !entryDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        if (filteredEntries.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Data", "No diary entries found for the selected date range.");
            lineChart.getData().clear(); // Clear the chart if no data
            return;
        }

        lineChart.getData().clear(); // Clear the chart before adding new data

        // Ensure all moods are displayed, even if they have no entries
        List<String> allMoods = List.of("Happy 😊", "Sad 😢", "Neutral 😐", "Content", "Stressed", "Excited", "Tired", "Angry");

        Map<String, Map<String, Long>> moodData = allMoods.stream()
                .collect(Collectors.toMap(
                        mood -> mood,
                        mood -> filteredEntries.stream()
                                .filter(entry -> mood.equals(entry.getMood()))
                                .collect(Collectors.groupingBy(
                                        entry -> entry.getEntryTime().toLocalDate().toString(),
                                        Collectors.counting()
                                ))
                ));

        // Fill in missing dates with zero counts for all moods
        List<String> dateRange = Stream.iterate(startDate, date -> date.plusDays(1))
                .limit(startDate.until(endDate).getDays() + 1)
                .map(LocalDate::toString)
                .collect(Collectors.toList());

        moodData.forEach((mood, dataByDate) -> {
            dateRange.forEach(date -> dataByDate.putIfAbsent(date, 0L));
        });

        // Add series for each mood to the chart
        moodData.forEach((mood, dataByDate) -> {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(mood);

            dataByDate.forEach((date, count) -> {
                XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(date, count);
                series.getData().add(dataPoint);

                // Add a tooltip for each data point
                Tooltip.install(dataPoint.getNode(), new Tooltip(mood + ": " + count + " entries on " + date));
            });

            lineChart.getData().add(series); // Add series to chart
        });

        applyMoodColors(lineChart); // Apply custom colors for each mood
    }

 private void applyMoodColors(LineChart<String, Number> lineChart) {
    Map<String, String> moodColors = Map.of(
        "Happy 😊", "#FFD700",
        "Sad 😢", "#1E90FF",
        "Neutral 😐", "#A9A9A9",
        "Content", "#32CD32",
        "Stressed", "#FF6347",
        "Excited", "#FF4500",
        "Tired", "#8A2BE2",
        "Angry", "#DC143C"
    );

    for (XYChart.Series<String, Number> series : lineChart.getData()) {
        String mood = series.getName();
        String color = moodColors.getOrDefault(mood, "#000000");

        // Use a listener to wait for the series node to be ready
        series.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle("-fx-stroke: " + color + "; -fx-stroke-width: 2px;");
            }
        });
    }
}

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}




















