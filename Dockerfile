# syntax=docker/dockerfile:1
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

# Construcción dividida en invocaciones de Gradle independientes.
# Cada RUN levanta su propio proceso Java/Node y lo libera por completo al
# terminar, evitando que las compilaciones (Kotlin/JS, webpack y Kotlin/JVM)
# acumulen memoria dentro del mismo proceso — esto fue lo que causó el
# OOMKilled (exit 137) al combinarlas.
# El cache mount persiste ~/.gradle (dependencias Gradle/Node/Yarn + build
# cache) ENTRE builds de Hugging Face aunque el COPY de código fuente invalide
# las capas de Docker, acelerando despliegues subsecuentes.

# Paso 1a: compilar Kotlin/JS a JS puro (proceso JVM, sin Node todavía).
RUN --mount=type=cache,target=/root/.gradle,id=rhnaf-gradle-cache \
    SKIP_ANDROID=true ./gradlew :web:compileDevelopmentExecutableKotlinJs \
    --no-daemon \
    --build-cache \
    --max-workers=1 \
    -Dorg.gradle.jvmargs="-Xmx1024m -XX:+UseParallelGC -XX:MaxMetaspaceSize=320m"

# Paso 1b: empaquetar con webpack (proceso Node) — la JVM del paso anterior
# ya terminó y liberó su memoria por completo antes de que arranque Node.
RUN --mount=type=cache,target=/root/.gradle,id=rhnaf-gradle-cache \
    SKIP_ANDROID=true ./gradlew :web:jsBrowserDevelopmentWebpack \
    --no-daemon \
    --build-cache \
    --max-workers=1 \
    -Dorg.gradle.jvmargs="-Xmx768m -XX:+UseParallelGC -XX:MaxMetaspaceSize=256m" \
    -Dnode.options="--max-old-space-size=768"

# Paso 2: Servidor (Kotlin/JVM) — proceso limpio, sin Node de por medio.
RUN --mount=type=cache,target=/root/.gradle,id=rhnaf-gradle-cache \
    SKIP_ANDROID=true ./gradlew :server:installDist \
    --no-daemon \
    --build-cache \
    --max-workers=1 \
    -Dorg.gradle.jvmargs="-Xmx1536m -XX:+UseParallelGC -XX:MaxMetaspaceSize=384m"

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
