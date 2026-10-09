plugins {
    java
    id("com.gradleup.shadow") version "9.0.0"
}

group = "com.huidu.expandeddelight"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/")
    mavenLocal()
}

val ceVersion = "26.9.1"

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.1.0")

    // CraftEngine API
    compileOnly("net.momirealms:craft-engine-bukkit:$ceVersion")
    compileOnly("net.momirealms:craft-engine-core:$ceVersion")
    compileOnly("net.momirealms:craft-engine-bukkit-proxy:$ceVersion")

    // FarmersDelight API (從主插件構建產物引用)
    compileOnly(fileTree("../Farmersdelight-Plugin-main/build/libs") {
        include("*-api.jar", "farmersdelight-plugin-*-api.jar", "farmersdelight-*-api.jar")
    })
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand("version" to version)
    }
}

tasks.shadowJar {
    archiveBaseName.set("expandeddelight")
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
