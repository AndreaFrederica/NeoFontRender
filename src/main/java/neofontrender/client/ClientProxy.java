package neofontrender.client;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import com.cleanroommc.modularui.api.text.MuiTextBackends;
import neofontrender.NeoFontRender;
import neofontrender.common.CommonProxy;
import neofontrender.client.integration.NfrMuiTextBackend;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.splash.ModernSplashDetector;
import neofontrender.splash.SplashCompat;
import neofontrender.api.text.StructuredTextApi;
import neofontrender.core.font.pipeline.builtin.HexChatStructuredMiddleware;
import neofontrender.core.font.pipeline.builtin.TinkersAntiqueSyntaxProvider;
import neofontrender.text.pipeline.TextPipelinePlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.jar.JarFile;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        NeoFontRender.LOGGER.info("ClientProxy preInit");
        synchronizeStandaloneTextCore(event);
        StructuredTextApi.register(TinkersAntiqueSyntaxProvider.INSTANCE);
        StructuredTextApi.register(new TextPipelinePlugin() {
            @Override public String id() { return "neofontrender:builtins"; }

            @Override
            public java.util.Collection<? extends
                    neofontrender.text.pipeline.StructuredTextMiddleware> structuredMiddlewares() {
                return java.util.Collections.singletonList(HexChatStructuredMiddleware.INSTANCE);
            }
        });
        super.preInit(event);
    }

    /**
     * Older development deployments may leave text-core.jar in mods/1.12.2.
     * Keep that optional standalone copy synchronized with the version embedded
     * in the NFR distribution, otherwise its classes can shadow the embedded jar.
     */
    private static void synchronizeStandaloneTextCore(FMLPreInitializationEvent event) {
        File sourceFile = event.getSourceFile();
        if (sourceFile == null || !sourceFile.isFile()) return;
        File gameDir = event.getModConfigurationDirectory().getParentFile();
        if (gameDir == null) return;
        Path target = gameDir.toPath().resolve("mods").resolve("1.12.2").resolve("text-core.jar");
        try (JarFile jar = new JarFile(sourceFile)) {
            java.util.jar.JarEntry entry = jar.getJarEntry("text-core.jar");
            if (entry == null) return;
            Files.createDirectories(target.getParent());
            Path staged = Files.createTempFile(target.getParent(), "text-core", ".jar.tmp");
            try (InputStream input = jar.getInputStream(entry)) {
                Files.copy(input, staged, StandardCopyOption.REPLACE_EXISTING);
            }
            if (Files.exists(target) && Files.mismatch(target, staged) < 0) {
                Files.deleteIfExists(staged);
                return;
            }
            if (Files.exists(target)) {
                String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                Path backup = target.resolveSibling("text-core.jar.bak-nfr-" + stamp);
                Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
                NeoFontRender.LOGGER.warn("Updated stale standalone text-core.jar; old copy backed up as {}", backup.getFileName());
            }
            Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            NeoFontRender.LOGGER.info("Synchronized standalone text-core.jar from NeoFontRender");
        } catch (Exception error) {
            NeoFontRender.LOGGER.warn("Could not synchronize standalone text-core.jar; embedded text core will remain available", error);
        }
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // MUI remains independent of NFR. NFR registers its implementation on
        // the client after the required MUI dependency has been loaded.
        MuiTextBackends.register(new NfrMuiTextBackend());

        if (!NeofontrenderConfig.isLoaded()) {
            NeofontrenderConfig.load();
        }
        if (ModernSplashDetector.isInstalled()) {
            if (SplashCompat.isInstalled()) {
                NeoFontRender.LOGGER.info("ModernSplash font override is active");
            } else if (NeofontrenderConfig.splashFontOverrideEnabled()
                    && NeofontrenderConfig.compatModernSplash()) {
                NeoFontRender.LOGGER.warn("ModernSplash detected but font override was not installed. " +
                        "This usually means ModernSplash changed its internal structure; splash screen will use the default bitmap font.");
            }
        }
        NeofontrenderBranding.applyModMetadata();
        NeofontrenderKeyHandler.init();
        MinecraftForge.EVENT_BUS.register(new NeofontrenderMainMenuBranding());
        MinecraftForge.EVENT_BUS.register(new NeofontrenderOptionsButtonHandler());
        ClientCommandHandler.instance.registerCommand(new NeofontrenderCommand());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }
}
