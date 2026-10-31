# Zeroed

Zeroed is an open-source web application for tracking shared expenses, splitting bills, and settling debts between friends, roommates, and groups.

The goal is to simplify shared expense management by automating calculations, tracking balances, and providing reminders for outstanding payments.

## Features

- **Accounts & Authentication:** User registration, login, and Google OAuth.
- **Group Management:** Create groups, invite members, and manage shared expenses.
- **Expense Splitting:** Split bills equally, by percentage, ratio, or exact amount.
- **Balance & Settlement:** Track debts, calculate balances, and record settlements.
- **Multi-Currency Support:** Convert expenses using real-time exchange rates.
- **Automatic Categorization:** Classify expenses using natural language processing.
- **Notifications:** Reminders for outstanding balances and expense updates.

*Some features are planned and may not yet be implemented.*

## Tech Stack

| Component | Technology               |
| --------- | ------------------------ |
| Frontend  | React, TypeScript, Vite  |
| Backend   | Java, Spring Boot, Maven |

## Project Structure

The repository is organized into two main directories:

- **frontend/** — React, TypeScript, and Vite application.
- **backend/** — Spring Boot REST API and Maven configuration.

The repository root contains the Dockerfile, shared configuration and VS Code workspace file.

## Development Setup

### Prerequisites

Install the following:

- Java Development Kit (JDK) 21
- Node.js and npm
- Git
- VS Code (recommended) or another IDE

### 1. Clone the Repository

Clone the Zeroed repository from GitHub and navigate to the project directory.

### 2. VS Code Workspace Setup

**VS Code users should open zeroed.code-workspace rather than opening the repository folder directly.**

1. Open VS Code.
2. Select **File → Open Workspace from File**.
3. Open **zeroed.code-workspace**.

The workspace separates the frontend and backend into their own project roots, improving Java classpath detection and development tooling.

### 3. Install Dependencies

**Frontend**

Navigate to the frontend directory and run `npm ci` to install dependencies.

**Backend**

Navigate to the backend directory and run the Maven wrapper with clean compile to download dependencies and compile the application.

On Windows, use `mvnw.cmd` instead of `./mvnw`

Setup a Postgres DB either on Render or locally and set the environment variables.

There is a `.env` file in `./backend` to set the environment variables. Spring Boot is setup to read from there on startup.

### 4. Start the Development Servers

Run the frontend and backend in separate terminals.

**Backend — Spring Boot**

Navigate to the backend directory and run `./mvnw spring-boot:run`

The backend runs at `http://localhost:8080`

**Frontend — Vite**

Navigate to the frontend directory and run `npm run dev`

The frontend runs at `http://localhost:5173`

Vite automatically proxies requests beginning with /api to the Spring Boot backend on port 8080.

Use relative /api URLs in frontend requests rather than hardcoding the backend address.

## Production Build

Zeroed is deployed to Render using a multi-stage Docker build.

1. Vite compiles the React frontend.
2. The compiled assets are copied into Spring Boot's static resources.
3. Maven packages the frontend and backend into a single executable JAR.
4. The final Docker container runs Spring Boot, serving both the SPA and REST API.

To test the production build locally, build the Docker image using `docker build -t zeroed .` and run it using `docker run --rm -p 8080:8080 zeroed`

The application will be accessible at http://localhost:8080.

## Contributing

1. Create a feature branch
2. Implement and test your changes.
3. Commit your changes with a descriptive message.
4. Open a pull request for review before merging.
