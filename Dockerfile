# Etapa 1: Construcción
FROM eclipse-temurin:17-jdk-jammy AS build
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
# SKIP_ANDROID=true evita que Gradle intente configurar el módulo Android
# Limitamos los workers a 1 para evitar errores de memoria (OOM) en Hugging Face
RUN SKIP_ANDROID=true ./gradlew :web:jsBrowserDistribution :server:installDist --no-daemon --max-workers=1

# Etapa 2: Ejecución
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copiar el servidor instalado desde la etapa de build
COPY --from=build /app/server/build/install/server /app/server

# Crear carpeta static y copiar los archivos de la Web App (JS/HTML/CSS)
RUN mkdir -p /app/static
COPY --from=build /app/web/build/dist/js/productionExecutable /app/static/

# Configurar puerto para Hugging Face Spaces (7860 es el estándar)
ENV PORT=7860
EXPOSE 7860

# El servidor buscará la carpeta "static" en el directorio de trabajo
CMD ["./server/bin/server"]
