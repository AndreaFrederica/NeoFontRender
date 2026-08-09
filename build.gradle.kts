import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

extensions.extraProperties["reobfuscatedJarType"] = ReobfuscatedJar::class.java

evaluationDependsOn(":addons:ui-enhancements")
val uiEnhancementsJar = project(":addons:ui-enhancements").tasks.named<Jar>("jar")
evaluationDependsOn(":addons:electric-elytra")
val electricElytraJar = project(":addons:electric-elytra").tasks.named<Jar>("jar")

tasks.named<JavaExec>("runClient25") {
    dependsOn(uiEnhancementsJar)
    dependsOn(electricElytraJar)
    classpath(uiEnhancementsJar.flatMap { it.archiveFile })
    classpath(electricElytraJar.flatMap { it.archiveFile })
}
