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

- Docker
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

### 3. Setting up dev environment

We use Docker to setup containerized production deployments and dev environments to maintain consistency and ease of setup between developers.

In the root project directory run:

```
docker compose up --build
```

It will download all required images to build the dev container.


To start the container run:

```
docker compose up
```

The frontend dev server is hosted at `http://localhost:5173` and all API requests are proxied to the Spring Boot backend. The Docker containers mount the local filesystem rather than copying so any changes made locally will automatically update.

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
