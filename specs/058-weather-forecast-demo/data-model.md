# Data Model: Weather Forecast Demo

**Feature**: 058-weather-forecast-demo
**Date**: 2026-03-23

---

## IP Types (Custom Information Packet Types)

### Coordinates
- `latitude: Double` — Geographic latitude (-90 to 90)
- `longitude: Double` — Geographic longitude (-180 to 180)

### HttpResponse
- `statusCode: Int` — HTTP status code (200 = success)
- `body: String` — Raw response body (JSON string)

### ForecastData
- `dates: List<String>` — Array of date strings (YYYY-MM-DD format)
- `maxTemperatures: List<Double>` — Array of daily high temperatures
- `minTemperatures: List<Double>` — Array of daily low temperatures
- `temperatureUnit: String` — Unit label (e.g., "°C")

### ForecastDisplayList
- `entries: List<ForecastEntry>` — Formatted display entries

### ForecastChartData
- `labels: List<String>` — X-axis labels (date or day abbreviations)
- `values: List<Double>` — Y-axis values (high temperatures)
- `unit: String` — Temperature unit label

---

## Internal Data Classes

### ForecastEntry
A single day's forecast formatted for list display.
- `date: String` — Display date (e.g., "Mon 03/23")
- `high: Double` — Daily high temperature
- `low: Double` — Daily low temperature
- `unit: String` — Temperature unit

### ChartData
Chart-ready data structure.
- `labels: List<String>` — Day labels for X-axis
- `values: List<Double>` — Temperature values for Y-axis
- `unit: String` — Temperature unit
- `minValue: Double` — Minimum Y value (for axis scaling)
- `maxValue: Double` — Maximum Y value (for axis scaling)

---

## State Model (WeatherForecastState)

Singleton object with observable state flows:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| forecastEntries | List\<ForecastEntry\> | emptyList() | Parsed forecast for list display |
| chartData | ChartData? | null | Chart-ready data |
| errorMessage | String? | null | Error text to display |
| isLoading | Boolean | false | Loading indicator |
| latitude | Double | 40.16 | Configurable latitude (Longmont, CO) |
| longitude | Double | -105.10 | Configurable longitude |

---

## Node Pipeline

```
TriggerSource (SOURCE)
  → outputs Coordinates
  → HttpFetcher (TRANSFORMER)
    → outputs HttpResponse
    → JsonParser (TRANSFORMER)
      → outputs ForecastData
      → DataMapper (TRANSFORMER, 1-in 2-out)
        → output1: ForecastDisplayList
        → output2: ForecastChartData
        → ForecastDisplay (SINK, 2-in anyInput)
          → updates WeatherForecastState
```

---

## Open-Meteo Response Schema (External)

```json
{
  "latitude": 40.16,
  "longitude": -105.1,
  "daily": {
    "time": ["2026-03-23", "2026-03-24", "2026-03-25", "2026-03-26", "2026-03-27", "2026-03-28", "2026-03-29"],
    "temperature_2m_max": [15.2, 18.1, 12.4, 20.3, 22.1, 19.5, 16.8],
    "temperature_2m_min": [3.4, 5.6, 1.2, 7.8, 9.1, 6.3, 4.5]
  },
  "daily_units": {
    "temperature_2m_max": "°C",
    "temperature_2m_min": "°C"
  }
}
```
