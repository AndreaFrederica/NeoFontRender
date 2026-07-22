import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

extensions.extraProperties["reobfuscatedJarType"] = ReobfuscatedJar::class.java
