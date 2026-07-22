
pluginManagement {
    repositories {
        maven {
            // RetroFuturaGradle
            name = "GTNH Maven"
            url = uri("https://nexus.gtnewhorizons.com/repository/public/")
            mavenContent {
                includeGroup("com.gtnewhorizons")
                includeGroupByRegex("com\\.gtnewhorizons\\..+")
            }
        }
        gradlePluginPortal()
        maven {
            name = "WagYourMaven"
            url = uri("https://maven.wagyourtail.xyz/releases")
        }
        maven {
            name = "Cleanroom Maven"
            url = uri("https://maven.cleanroommc.com/")
        }
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    id("com.gtnewhorizons.gtnhsettingsconvention") version("2.0.20")
}

include(":ui-enhancements")
project(":ui-enhancements").projectDir = file("addons/ui-enhancements")
