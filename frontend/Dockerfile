# Stage 1: build React app
FROM node:20-alpine AS build

WORKDIR /app
COPY package.json ./
RUN npm install --no-audit --no-fund

COPY . .

ARG REACT_APP_API_URL=/api
ARG REACT_APP_FRAUD_API_URL=/ml
ENV REACT_APP_API_URL=${REACT_APP_API_URL}
ENV REACT_APP_FRAUD_API_URL=${REACT_APP_FRAUD_API_URL}

RUN npm run build

# Stage 2: serve with nginx
FROM nginx:1.27-alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

