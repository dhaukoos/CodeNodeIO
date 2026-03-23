# Quickstart: Weather Forecast Demo

**Feature**: 058-weather-forecast-demo
**Date**: 2026-03-23

---

## Scenario 1: Run the Weekly Forecast Flow

1. Launch graphEditor
2. Load the WeatherForecast module (it should appear in available modules)
3. Observe the flowGraph canvas shows 5 connected nodes: TriggerSource → HttpFetcher → JsonParser → DataMapper → ForecastDisplay
4. Click "Start" in the Runtime Preview panel
5. Click the "Refresh" trigger in the preview UI
6. **Expected**: Within 10 seconds, the list view shows 7 days of forecast data with dates, high temps, and low temps in °C

## Scenario 2: View the Temperature Chart

1. Complete Scenario 1 (forecast data is displayed)
2. Observe the preview UI area
3. **Expected**: A line chart renders above or alongside the list, showing 7 data points for daily high temperatures with labeled X-axis (dates) and Y-axis (temperature in °C)

## Scenario 3: Change Location and Refresh

1. Complete Scenario 1
2. Select the TriggerSource node on the canvas
3. In the Properties Panel, change latitude to 48.85 and longitude to 2.35 (Paris, France)
4. Click "Refresh" in the preview
5. **Expected**: New forecast data loads for Paris; temperatures and dates update in both list and chart

## Scenario 4: Network Error Handling

1. Disconnect from the internet (or block the Open-Meteo domain)
2. Launch graphEditor and load the WeatherForecast module
3. Start the flow and click "Refresh"
4. **Expected**: Within 5 seconds, the preview shows an error message like "Network error: Unable to fetch forecast data" instead of crashing

## Scenario 5: Observe IP Type Colors on Canvas

1. Load the WeatherForecast module
2. Observe the connections between nodes on the canvas
3. **Expected**: Each connection is color-coded by its IP Type:
   - TriggerSource → HttpFetcher: Teal (Coordinates)
   - HttpFetcher → JsonParser: Orange (HttpResponse)
   - JsonParser → DataMapper: Blue (ForecastData)
   - DataMapper → ForecastDisplay (output1): Green (ForecastDisplayList)
   - DataMapper → ForecastDisplay (output2): Purple (ForecastChartData)

## Scenario 6: Inspect IP Types in IP Palette

1. Load the WeatherForecast module
2. Open the IP Types palette in the graphEditor
3. **Expected**: Five custom IP Types are listed: Coordinates, HttpResponse, ForecastData, ForecastDisplayList, ForecastChartData — each with distinct colors and property definitions
