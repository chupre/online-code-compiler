# Stage 1: Build the React application
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy the rest of the application
COPY . .

# build-time args
ARG VITE_URL
ARG VITE_API_URL

# Vite only uses variables prefixed with VITE_ at build time
ENV VITE_API_URL=${VITE_API_URL}

RUN VITE_API_URL=$VITE_API_URL npm run build

# Stage 2: Serve the application using Nginx
FROM nginx:alpine

# Copy the built application from the builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]