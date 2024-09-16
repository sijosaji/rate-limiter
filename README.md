# Rate Limiter Spring Boot Application

This is a Spring Boot application that implements a simple rate limiting mechanism using MongoDB. The application exposes an API endpoint to check the rate limit for a specific user ID.

## Table of Contents
- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Endpoints](#api-endpoints)

## Requirements

To build and run this application, you need the following installed on your system:

- Java 21
- Maven 3.6+
- MongoDB (locally or remotely)
- An IDE (e.g., IntelliJ IDEA, Eclipse, VS Code)

## Installation

1. **Clone the Repository**:
    ```bash
    git clone https://github.com/sijosaji/ratelimiter.git
    cd ratelimiter
    ```

2. **Build the Project**:
      ```bash
      mvn clean install
      ```

## Configuration

 **MongoDB Setup**:
   - Ensure MongoDB is running.
   - Update the `application.properties` file located in the `src/main/resources` directory with your MongoDB connection details:

     ```properties
     spring.data.mongodb.uri=mongodb://localhost:27017/mongo_migration
     ```


## Running the Application

You can run the application in two ways:

1. **Using Maven**:
    ```bash
    mvn spring-boot:run
    ```

2. **Using the Executable JAR**:
    ```bash
    java -jar target/ratelimiter-0.0.1-SNAPSHOT.jar
    ```

The application will start on `http://localhost:9001`.

## API Endpoints

### `PUT /api/rate-limit/{userId}`

Check the rate limit for a specific user ID.

- **Request**:
  - Path Parameter: `userId` (String) - The user ID to check the rate limit for.
  
- **Response**:
  - `200 OK`: If the request is allowed.
  - `429 TOO MANY REQUESTS`: If the request exceeds the rate limit, with a `retry-after` header indicating time in **seconds** when the user can retry.

Example Request:
```bash
curl -X PUT "http://localhost:9001/api/rate-limit/12345"
