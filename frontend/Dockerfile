# ----- Build stage -----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build -- --configuration production

# ----- Runtime stage -----
FROM nginx:alpine
COPY --from=build /app/dist/bvmt-ui/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
