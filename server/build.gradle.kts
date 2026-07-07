plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation:3.0.3")
    implementation("io.ktor:ktor-server-cors:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    // Ktor Client (for Hugging Face)
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-content-negotiation")
    
    // Database: Exposed + H2
    val exposedVersion = "0.56.0"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
    implementation("com.h2database:h2:2.2.224")
    implementation("org.postgresql:postgresql:42.7.4") // Persistencia real (Neon/Supabase/Render)
    implementation("com.zaxxer:HikariCP:5.1.0")
}

application {
    mainClass.set("com.example.rhnaf.ApplicationKt")
}
