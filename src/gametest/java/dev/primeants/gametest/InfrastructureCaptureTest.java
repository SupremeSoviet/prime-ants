package dev.primeants.gametest;

import dev.primeants.PrimeAnts;
import java.nio.file.Path;
import java.time.Instant;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

/** One development-only capture of ordinary generated terrain, with the natural spawn and camera. */
public final class InfrastructureCaptureTest implements FabricClientGameTest {
    public static final long SEED = 2026100401L;

    @Override
    public void runTest(ClientGameTestContext context) {
        PrimeAnts.LOGGER.info("T01 infrastructure capture started: time={}, seed={}", Instant.now(), SEED);
        try (TestSingleplayerContext world = context.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(settings -> settings.setSeed(Long.toString(SEED)))
                .create()) {
            int waitedTicks = world.getConnection().waitForChunksRender();
            context.waitForScreen(null);
            context.waitTicks(5);
            Path image = context.takeScreenshot(TestScreenshotOptions.of("t01-infrastructure")
                    .disableCounterPrefix()
                    .withSize(1280, 720)
                    .withDestinationDir(Path.of(System.getProperty("prime_ants.captureDir"))));
            PrimeAnts.LOGGER.info("T01 infrastructure capture saved: path={}, seed={}, chunkWaitTicks={}, time={}",
                    image.toAbsolutePath(), SEED, waitedTicks, Instant.now());
        }
    }
}
