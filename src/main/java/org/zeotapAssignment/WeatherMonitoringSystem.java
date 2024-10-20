package org.zeotapAssignment;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.json.JSONObject;

class DailySummary {
    double sumTemp;
    double sumHumidity;
    double sumWindSpeed;
    int count;
    double maxTemp;
    double minTemp;
    double maxWindSpeed;
    String dominantCondition;
    Map<String, Integer> conditionCount;

    public DailySummary() {
        sumTemp = 0.0;
        sumHumidity = 0.0;
        sumWindSpeed = 0.0;
        count = 0;
        maxTemp = Double.NEGATIVE_INFINITY;
        minTemp = Double.POSITIVE_INFINITY;
        maxWindSpeed = Double.NEGATIVE_INFINITY;
        conditionCount = new HashMap<>();
    }

    public void addWeatherData(double temp, double humidity, double windSpeed, String condition) {
        sumTemp += temp;
        sumHumidity += humidity;
        sumWindSpeed += windSpeed;
        count++;
        maxTemp = Math.max(maxTemp, temp);
        minTemp = Math.min(minTemp, temp);
        maxWindSpeed = Math.max(maxWindSpeed, windSpeed);

        // Count occurrences of each weather condition
        conditionCount.put(condition, conditionCount.getOrDefault(condition, 0) + 1);

        // Update the dominant condition
        dominantCondition = getDominantCondition();
    }

    public double getAverageTemp() {
        return count > 0 ? sumTemp / count : 0;
    }

    public double getAverageHumidity() {
        return count > 0 ? sumHumidity / count : 0;
    }

    public double getAverageWindSpeed() {
        return count > 0 ? sumWindSpeed / count : 0;
    }

    private String getDominantCondition() {
        return conditionCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
    }
}

public class WeatherMonitoringSystem {
    private static final String API_KEY = "98b090e1fd289909b38d875896f09f3b";
    private static final String[] CITIES = {"Delhi", "Mumbai", "Chennai", "Bangalore", "Kolkata", "Hyderabad"};
    private static final String DB_URL = "jdbc:sqlite:weather_data.db";
    private static final double HIGH_TEMPERATURE_THRESHOLD = 35.0;

    private static Connection conn;
    private static Map<String, DailySummary> dailySummaries = new HashMap<>();

    private static void setupDatabase() {
        try {
            conn = DriverManager.getConnection(DB_URL);
            // Drop the old table if it exists (optional, only if data is not needed)
            String dropTable = "DROP TABLE IF EXISTS daily_summary";
            conn.createStatement().execute(dropTable);

            // Create a new table with the updated schema
            String createTable = "CREATE TABLE IF NOT EXISTS daily_summary ("
                    + "date TEXT, "
                    + "city TEXT, "
                    + "avg_temp REAL, "
                    + "avg_humidity REAL, "
                    + "avg_wind_speed REAL, "
                    + "max_temp REAL, "
                    + "min_temp REAL, "
                    + "max_wind_speed REAL, "
                    + "dominant_condition TEXT)";
            conn.createStatement().execute(createTable);
        } catch (SQLException e) {
            System.err.println("Database setup error: " + e.getMessage());
        }
    }

    private static String fetchWeather(String city) throws Exception {
        String urlString = String.format("http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric", city, API_KEY);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    private static void processWeatherData(String jsonResponse, String city) {
        JSONObject weatherData = new JSONObject(jsonResponse);
        JSONObject main = weatherData.getJSONObject("main");
        JSONObject wind = weatherData.getJSONObject("wind");
        String condition = weatherData.getJSONArray("weather").getJSONObject(0).getString("main");

        double temp = main.getDouble("temp");
        double humidity = main.getDouble("humidity");
        double windSpeed = wind.getDouble("speed");

        System.out.printf("Weather data for %s: Temp: %.2f°C, Humidity: %.2f%%, Wind Speed: %.2f m/s, Condition: %s%n", city, temp, humidity, windSpeed, condition);

        // Updating daily summary
        dailySummaries.computeIfAbsent(city, k -> new DailySummary()).addWeatherData(temp, humidity, windSpeed, condition);

        // Check if temperature exceeds the threshold
        if (temp > HIGH_TEMPERATURE_THRESHOLD) {
            System.out.println("Alert: Temperature exceeds " + HIGH_TEMPERATURE_THRESHOLD + "°C in " + city + "!");
        }
    }

    private static String fetchWeatherForecast(String city) throws Exception {
        String urlString = String.format("http://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=metric", city, API_KEY);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    private static void processForecastData(String jsonResponse, String city) {
        JSONObject forecastData = new JSONObject(jsonResponse);
        Map<String, DailySummary> forecastSummaries = new HashMap<>();

        // Parse the list of forecasts
        for (Object forecast : forecastData.getJSONArray("list")) {
            JSONObject forecastObject = (JSONObject) forecast;
            JSONObject main = forecastObject.getJSONObject("main");
            JSONObject wind = forecastObject.getJSONObject("wind");
            String condition = forecastObject.getJSONArray("weather").getJSONObject(0).getString("main");

            double temp = main.getDouble("temp");
            double humidity = main.getDouble("humidity");
            double windSpeed = wind.getDouble("speed");
            String date = forecastObject.getString("dt_txt").split(" ")[0];

            // Update forecast summaries for each day
            forecastSummaries.computeIfAbsent(date, k -> new DailySummary())
                    .addWeatherData(temp, humidity, windSpeed, condition);
        }

        // Display the forecast summary for the city
        System.out.printf("Forecast summary for %s:\n", city);
        for (Map.Entry<String, DailySummary> entry : forecastSummaries.entrySet()) {
            String date = entry.getKey();
            DailySummary summary = entry.getValue();
            System.out.printf("Date: %s, Avg Temp: %.2f°C, Dominant Condition: %s\n",
                    date, summary.getAverageTemp(), summary.dominantCondition);
        }
    }

    private static void storeDailySummaries() {
        for (Map.Entry<String, DailySummary> entry : dailySummaries.entrySet()) {
            String city = entry.getKey();
            DailySummary summary = entry.getValue();
            String date = java.time.LocalDate.now().toString();

            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO daily_summary (date, city, avg_temp, avg_humidity, avg_wind_speed, max_temp, min_temp, max_wind_speed, dominant_condition) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                pstmt.setString(1, date);
                pstmt.setString(2, city);
                pstmt.setDouble(3, summary.getAverageTemp());
                pstmt.setDouble(4, summary.getAverageHumidity());
                pstmt.setDouble(5, summary.getAverageWindSpeed());
                pstmt.setDouble(6, summary.maxTemp);
                pstmt.setDouble(7, summary.minTemp);
                pstmt.setDouble(8, summary.maxWindSpeed);
                pstmt.setString(9, summary.dominantCondition);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Error storing daily summary: " + e.getMessage());
            }
        }
    }

    private static void visualizeDailySummaries() {
        try {
            String sql = "SELECT city, avg_temp, avg_humidity, avg_wind_speed, max_temp, min_temp FROM daily_summary";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet resultSet = pstmt.executeQuery();

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            while (resultSet.next()) {
                String city = resultSet.getString("city");
                double avgTemp = resultSet.getDouble("avg_temp");
                double avgHumidity = resultSet.getDouble("avg_humidity");
                double avgWindSpeed = resultSet.getDouble("avg_wind_speed");

                dataset.addValue(avgTemp, "Average Temperature", city);
                dataset.addValue(avgHumidity, "Average Humidity", city);
                dataset.addValue(avgWindSpeed, "Average Wind Speed", city);
            }

            JFreeChart chart = ChartFactory.createBarChart(
                    "Daily Weather Summaries",
                    "City",
                    "Values",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true, true, false
            );

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(800, 600));

            JFrame frame = new JFrame("Weather Monitoring System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(chartPanel);
            frame.pack();
            frame.setVisible(true);
        } catch (SQLException e) {
            System.err.println("Error visualizing daily summaries: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        setupDatabase();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (String city : CITIES) {
                    try {
                        String weatherData = fetchWeather(city);
                        processWeatherData(weatherData, city);

                        String forecastData = fetchWeatherForecast(city);
                        processForecastData(forecastData, city);
                    } catch (Exception e) {
                        System.err.println("Error fetching data: " + e.getMessage());
                    }
                }

                // Store daily summaries
                storeDailySummaries();

                // Visualize daily summaries
                visualizeDailySummaries();
            }
            // Running every 5 minutes.
        }, 0, 300000);
    }
}
