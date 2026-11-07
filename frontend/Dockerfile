FROM node:22-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine
USER root
RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY docker-entrypoint.d/16-taskflow-defaults.envsh /docker-entrypoint.d/16-taskflow-defaults.envsh
COPY docker-entrypoint.d/18-taskflow-config.sh /docker-entrypoint.d/18-taskflow-config.sh
COPY --from=build /src/dist/frontend/browser /usr/share/nginx/html
RUN chmod 755 /docker-entrypoint.d/16-taskflow-defaults.envsh /docker-entrypoint.d/18-taskflow-config.sh \
    && chown -R nginx:nginx /usr/share/nginx/html /etc/nginx/templates /docker-entrypoint.d/16-taskflow-defaults.envsh /docker-entrypoint.d/18-taskflow-config.sh
USER nginx
EXPOSE 8080
