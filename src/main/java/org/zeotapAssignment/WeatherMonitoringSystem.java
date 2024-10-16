package org.zeotapAssignment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONObject;

public class WeatherMonitoringSystem {
    private static final String API_KEY = "98b090e1fd289909b38d875896f09f3b";
    private static final String[] CITIES = {"Delhi", "Mumbai", "Chennai", "Bangalore", "Kolkata", "Hyderabad"};
    private static final String DB_URL = "jdbc:sqlite:weather_data.db";

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void setupDatabase() {
        try (Connection conn = connect()) {
            String sql = "CREATE TABLE IF NOT EXISTS daily_summary (date TEXT, city TEXT, avg_temp REAL, max_temp REAL, min_temp REAL, dominant_condition TEXT)";
            conn.createStatement().execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static String fetchWeather(String city) throws Exception {
        String urlString = String.format("http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s", city, API_KEY);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }


    private static double kelvinToCelsius(double kelvin) {
        return kelvin - 273.15;
    }

    private static void processWeatherData(String jsonResponse, String city) {
        JSONObject weatherData = new JSONObject(jsonResponse);
        JSONObject main = weatherData.getJSONObject("main");
        String condition = weatherData.getJSONArray("weather").getJSONObject(0).getString("main");

        double temp = kelvinToCelsius(main.getDouble("temp"));
        double feelsLike = kelvinToCelsius(main.getDouble("feels_like"));

        // Here you would implement logic to calculate daily aggregates and store them
        System.out.printf("Weather data for %s: Temp: %.2f°C, Feels Like: %.2f°C, Condition: %s%n", city, temp, feelsLike, condition);
    }

    private static void storeDailySummary(String date, String city, double avgTemp, double maxTemp, double minTemp, String dominantCondition) {
        try (Connection conn = connect()) {
            String sql = "INSERT INTO daily_summary (date, city, avg_temp, max_temp, min_temp, dominant_condition) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, date);
            pstmt.setString( 2, city);
            pstmt.setDouble(3, avgTemp);
            pstmt.setDouble(4, maxTemp);
            pstmt.setDouble(5, minTemp);
            pstmt.setString(6, dominantCondition);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
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
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }, 0, 300000);  // Run every 5 minutes
    }
}
