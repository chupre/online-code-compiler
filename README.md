# Online Code Compiler

An online code compiler that allows users to write, compile, and run code in multiple programming languages directly in their browser.

## Features

- Write code in multiple languages (currently supporting C++ and Python)
- Real-time code execution
- Syntax highlighting
- Responsive UI with dark mode support

## Architecture

The application consists of:

1. **Frontend**: React/TypeScript application with CodeMirror for code editing
2. **Backend**: Spring Boot application with REST APIs
3. **Database**: PostgreSQL for storing user data and code snippets
4. **Sandbox**: Isolated Docker containers for secure code execution

## Prerequisites

- Docker and Docker Compose
- Java 17+
- Node.js 18+
- Maven

## Getting Started

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd online-code-compiler
   ```

2. Set up environment variables:
   ```bash
   cp .env.example .env
   # Edit .env file with your configuration
   ```

3. Start all services with Docker Compose:
   ```bash
   docker-compose up
   ```

   This will start:
   - Frontend (React app) on port 3000
   - Backend (Spring Boot) on port 8080
   - PostgreSQL database on port 5433
   - Sandboxes for C++ and Python code execution

4. Access the application:
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080

## Development Setup

### Backend

1. Navigate to the project root directory
2. Install dependencies and build the project:
   ```bash
   ./mvnw clean install
   ```
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

### Frontend

1. Navigate to the frontend directory:
   ```bash
   cd frontend/online-code-compiler
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm run dev
   ```

## Docker Configuration

The project includes Docker configuration for all services:

- `Dockerfile` for the backend application
- `frontend/online-code-compiler/Dockerfile` for the frontend application
- `docker-compose.yml` for orchestrating all services

### Frontend Docker Image

The frontend Docker image uses a multi-stage build:
1. First stage builds the React application using Node.js
2. Second stage serves the built files using Nginx

To build and run the frontend Docker image separately:
```bash
cd frontend/online-code-compiler
docker build -t occ-frontend .
docker run -p 3000:80 occ-frontend
```

## Project Structure

```
.
├── src/                   # Backend source code
│   ├── main/
│   │   ├── java/          # Java source files
│   │   └── resources/     # Configuration files
│   └── test/              # Test files
├── frontend/              # Frontend application
│   └── online-code-compiler/
│       ├── src/           # React source files
│       ├── public/        # Static assets
│       └── Dockerfile     # Frontend Docker configuration
├── docker-compose.yml     # Docker Compose configuration
├── Dockerfile             # Backend Docker configuration
└── pom.xml               # Maven configuration
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

[ wip ]