# Zeotap Assignment: Rule Engine with AST and Weather Monitoring System

## Overview
This project consists of two main applications:
1. **A Rule Engine** using **Abstract Syntax Trees (AST)** for user eligibility determination.
2. **A Real-Time Weather Monitoring System** with data processing and visualization.

## Codebase
The codebase is available on GitHub: https://github.com/harjas-romana/Zeotap_assignment_repo

## Build Instructions

### Prerequisites
- **Java Development Kit (JDK)** 8 or higher
- **Maven** (for dependency management)
- **SQLite** (for database operations)

### Steps to Build and Run

1. **Clone the repository:**
   ```bash
   git clone https://github.com/harjas-romana/Zeotap_assignment_repo
   cd Zeotap_assignment_repo
   ```

2. **Build the project using Maven:**
   ```bash
   mvn clean install
   ```

3. **Run the application**

## Design Decisions

### Rule Engine with AST
Implemented using a proprietary `ASTNode` class to represent nodes in the Abstract Syntax Tree.
Supports individual rules, rule composition, and rule evaluation with user data
Parse rules as strings into the AST using a stack-based algorithm

### Weather Monitoring System
Use OpenWeatherMap for real-time weather data and weather forecasting.
- Uses a **timer-based system** to fetch data at predetermined times, each 5 minutes.
- Stores daily summaries of weather into **SQLite** to ensure permanent storage.
- Implements **data visualization** using **JFreeChart** to plot the plotted weather data.

## Dependencies
- **org.json:json**: Parses JSON
- **org.xerial:sqlite-jdbc**: For db operations
- **org.jfree:jfreechart**: For data visualization

## Running the Applications

### Rule Engine
- The main body of the Rule Engine lives in class **`ASTNodeAnswer`**.
- This solution represents the **rule creation**, **rule combination**, and **evaluation** on sample user data.

### Weather Monitoring System
- The class **`WeatherMonitoringSystem`** contains the main method for the Weather Monitoring System.
- It continually reads the weather for a specified number of cities, analyzes the data, and stores the summaries.
- There is **graphical visualization** of the weather data.

## Debug
- Ensure you have the proper **API key** for OpenWeatherMap in the class **`WeatherMonitoringSystem`**.
- If the application is not fetching the weather data, then you may need to check your **internet connection**.
- Ensure you have appropriate **permissions** to write and create the SQLite database file.

## Conclusion
This project demonstrates a **Rule Engine** implementation using **AST** and a **Real-Time Weather Monitoring System** as well.

For any issues or questions, please raise an issue on the GitHub repository or drop a mail to **harjasr42@gmail.com**.
