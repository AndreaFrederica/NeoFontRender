import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
}

group = "org.tiqian.nfr"
version = "0.1.0-d25567f"

base {
    archivesName.set("nfr-tiqian-jvm")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(25)
    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_25)
    }

    sourceSets {
        commonMain {
            kotlin.srcDirs(
                "../vendor/tiqian/core/src/commonMain/kotlin",
                "../vendor/tiqian/font/src/commonMain/kotlin",
                "../vendor/tiqian/shaping/api/src/commonMain/kotlin",
                "../vendor/tiqian/linebreak/src/commonMain/kotlin",
                "../vendor/tiqian/clreq/src/commonMain/kotlin",
                "../vendor/tiqian/layout/src/commonMain/kotlin",
            )
            resources.srcDirs(
                "../vendor/tiqian/linebreak/src/commonMain/resources",
            )
        }
        jvmMain {
            kotlin.srcDirs(
                "../vendor/tiqian/linebreak/src/jvmMain/kotlin",
                "../vendor/tiqian/layout/src/jvmMain/kotlin",
            )
        }
    }
}

tasks.register("verifyPinnedTiqianSource") {
    val marker = layout.projectDirectory.file(
        "../vendor/tiqian/layout/src/commonMain/kotlin/org/tiqian/layout/ParagraphLayoutEngine.kt"
    )
    inputs.file(marker)
    doLast {
        check(marker.asFile.isFile) {
            "Tiqian sources are missing. Run: git submodule update --init addons/ui-enhancements/vendor/tiqian"
        }
    }
}

tasks.matching { it.name.startsWith("compileKotlin") }.configureEach {
    dependsOn("verifyPinnedTiqianSource")
}
