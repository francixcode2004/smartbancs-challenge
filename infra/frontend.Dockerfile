FROM node:22-alpine AS build

WORKDIR /workspace
COPY SmartBanc-Frontend/package*.json ./
RUN npm ci
COPY SmartBanc-Frontend/ ./
RUN npm run build

FROM nginx:1.29-alpine

COPY infra/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/SmartBanc-Frontend/browser /usr/share/nginx/html
EXPOSE 80