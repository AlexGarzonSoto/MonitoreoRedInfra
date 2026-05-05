# ── Etapa 1: Build ─────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Instalar dependencias primero para aprovechar caché de capas
COPY package*.json ./
RUN npm ci --prefer-offline

COPY . .
RUN npm run build

# ── Etapa 2: Servidor Nginx ─────────────────────────────────────────────────
FROM nginx:1.25-alpine

RUN apk upgrade --no-cache

# Eliminar configuración por defecto
RUN rm /etc/nginx/conf.d/default.conf

# Copiar build y configuración
COPY --from=builder /app/dist        /usr/share/nginx/html
COPY nginx.conf                      /etc/nginx/conf.d/default.conf

# Usuario no-root (nginx ya corre como www-data en los workers)
# El proceso master de nginx requiere root para bind :80; los workers usan www-data
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
