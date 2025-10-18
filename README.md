# Marketsim
A tool for viewing market data and experimenting with investment strategies

## FinnhubClient Integration

The project includes a robust FinnhubClient implementation for fetching real-time stock market data from the Finnhub API. The client provides functionality for polling candle data and processing trade messages.

### Features

- Real-time stock market data polling (60-second intervals)
- Support for candle data with OHLCV (Open, High, Low, Close, Volume)
- Automatic data persistence to database
- Error handling and retry mechanisms
- Thread-safe implementation

### Prerequisites

- Java Development Kit (JDK)
- Finnhub API Key
- Database setup (supports both in-memory and file-based SQLite)

### Configuration

#### API Key Setup

The client requires a Finnhub API key to function. Set your API key as an environment variable:

```bash
export FINNHUB_API_KEY=your_api_key_here
```

### Usage

#### Database Setup
---

The project uses SQLite for data storage. The database file will be created in the `data/` directory.

#### Basic Usage

```java
// Initialize database manager
DatabaseManager db = new DatabaseManager("data/market.db");

// Start client for a specific stock symbol
FinnhubClient client = FinnhubClient.start(db, "AAPL");

// When done, stop polling
client.stopPolling();
db.close();
```

#### Manual Configuration

```java
// Create client instance
FinnhubClient client = new FinnhubClient("AAPL");

// Start polling with custom handlers
client.startPolling(
    // Data handler
    candleData -> {
        // Process the candle data
    },
    // Error handler
    error -> {
        System.err.println("Error: " + error.getMessage());
    }
);
```

### Data Structure

The client processes two types of data:

1. Candle Data:
   - Timestamp
   - Open price
   - High price
   - Low price
   - Close price
   - Volume

2. Trade Messages:
   - Symbol
   - Price
   - Timestamp
   - Volume

### Error Handling

The client includes comprehensive error handling:
- API key validation
- Network error handling
- Data parsing error handling
- Database insertion error handling
