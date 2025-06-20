# Google Drive Project

This project is a Java-based application designed to manage files and directories, similar to Google Drive. It uses a PostgreSQL database for data storage and supports various file operations.

## Features
- File and directory management
- Database integration (PostgreSQL)
- RESTful API (if applicable)
- Docker support (if applicable)

## Prerequisites
- Java 11 or higher
- Maven
- PostgreSQL
- Docker (optional, for containerized deployment)

## Environment Setup

Create a `.env` file in the project root with the following content:

```
DB_NAME=jhil_database
DB_USER=jhkumari
DB_PASSWORD=abcd1234
DB_PORT=5432
```

Alternatively, update your `application-dev.properties` and `application-prod.properties` files to use these values if not using `.env` directly.

## Database Setup

1. Ensure PostgreSQL is running.
2. Create the database and user if they do not exist:
   ```sql
   CREATE DATABASE jhil_database;
   CREATE USER jhkumari WITH PASSWORD 'abcd1234';
   GRANT ALL PRIVILEGES ON DATABASE jhil_database TO jhkumari;
   ```
3. (Optional) Use the provided `init.sql` to initialize the schema:
   ```sh
   psql -U jhkumari -d jhil_database -f init.sql
   ```

## Build and Run

To build the project:
```sh
mvn clean install
```

To run the project:
```sh
mvn spring-boot:run
```

## Docker (Optional)
If you want to run the project using Docker, use the provided `docker-compose.yml`:
```sh
docker-compose up --build
```

## Testing
To run tests:
```sh
mvn test
```

## Project Structure
- `src/main/java/org/griddynamics/` - Java source code
- `src/main/resources/` - Configuration and resource files
- `init.sql` - Database initialization script
- `docker-compose.yml` - Docker Compose configuration

## License
This project is for educational purposes.

