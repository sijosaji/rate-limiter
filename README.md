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
- Maven 3.6+ or Gradle 7+
- MongoDB (locally or remotely)
- An IDE (e.g., IntelliJ IDEA, Eclipse, VS Code)

## Installation

1. **Clone the Repository**:
    ```bash
    git clone https://github.com/yourusername/your-repository-name.git
    cd your-repository-name
    ```

2. **Build the Project**:
      ```bash
      mvn clean install
      ```

## Configuration

 **MongoDB Setup**:
   - Ensure MongoDB is running.
   - Update the `application.properties` or `application.yml` file located in the `src/main/resources` directory with your MongoDB connection details:

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
    java -jar target/your-application-name.jar
    ```

The application will start on `http://localhost:8080`.

## API Endpoints

### `GET /api/rate-limit`

Check the rate limit for a specific user ID.

- **Request**:
  - Query Parameters: `userId` (String) - The user ID to check the rate limit for.
  
- **Response**:
  - `200 OK`: If the request is allowed.
  - `429 TOO MANY REQUESTS`: If the request exceeds the rate limit, with a `retry-after` header indicating when the user can retry.

Example Request:
```bash
curl -X GET "http://localhost:8080/api/rate-limit?userId=someUserId"
