# Etapa 1: Construcción
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copiar archivos de configuración de Gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Copiar código fuente
COPY shared shared
COPY server server
COPY web web
# COPY app app (No necesario para el despliegue en HF)

# Permisos para el ejecutable de Gradle
RUN chmod +x gradlew

# Construir la Web App y el Servidor
# Usamos jsBrowserDevelopmentDistribution para evitar el timeout de optimización en HF
RUN SKIP_ANDROID=true ./gradlew :web:jsBrowserDevelopmentDistribution :server:installDist --no-daemon --max-workers=1

# Etapa 2: Ejecución
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copiar el servidor instalado desde la etapa de build
COPY --from=build /app/server/build/install/server /app/server

# Crear carpeta static y copiar los archivos de la Web App (versión de desarrollo para rapidez)
RUN mkdir -p /app/static
COPY --from=build /app/web/build/dist/js/developmentExecutable /app/static/

# Configurar puerto para Hugging Face Spaces (7860 es el estándar)
ENV PORT=7860
EXPOSE 7860

# El servidor buscará la carpeta "static" en el directorio de trabajo
CMD ["./server/bin/server"]
