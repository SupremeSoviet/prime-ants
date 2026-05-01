package com.formicfrontier.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.tutorial.TutorialSteps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class VisualQaClient {
	private static final String[] SCENES = {
			"colony_overview",
			"colony_ground",
			"ant_lineup",
			"tablet_en",
			"tablet_ru",
			"progression_scene"
	};
	private static final int WAIT_FOR_WORLD_TICKS = Integer.getInteger("formic.visualQa.worldWaitTicks", 600);
	private static final int COMMAND_TO_SCREENSHOT_TICKS = Integer.getInteger("formic.visualQa.captureDelayTicks", 70);
	private static final int SCENE_TOTAL_TICKS = Integer.getInteger("formic.visualQa.sceneTicks", 110);
	private static final boolean EXIT_ON_COMPLETE = Boolean.parseBoolean(System.getProperty("formic.visualQa.exit", "true"));
	private static final String WORLD_ID = System.getProperty("formic.visualQa.world", "FormicVisualQA");
	private static final Path OUTPUT_DIR = Path.of(System.getProperty("formic.visualQa.dir", "build/visual-qa"));
	private static final Path SCREENSHOT_DIR = OUTPUT_DIR.resolve("screenshots");
	private static final List<String> CAPTURES = new ArrayList<>();
	private static int waitTicks;
	private static int sceneIndex = -1;
	private static int sceneTicks;
	private static boolean openWorldRequested;
	private static boolean tutorialDisabled;
	private static boolean wroteWaitingReport;
	private static boolean wroteFinalReport;

	private VisualQaClient() {
	}

	public static void initialize() {
		if (!Boolean.getBoolean("formic.visualQa")) {
			return;
		}
		ClientTickEvents.END_CLIENT_TICK.register(VisualQaClient::tick);
	}

	private static void tick(Minecraft client) {
		if (!tutorialDisabled) {
			tutorialDisabled = true;
			client.getTutorial().setStep(TutorialSteps.NONE);
		}
		if (client.player == null || client.level == null || client.getConnection() == null) {
			waitForWorld(client);
			return;
		}
		if (sceneIndex < 0) {
			sceneIndex = 0;
			sceneTicks = 0;
			prepareOutput();
		}
		if (sceneIndex >= SCENES.length) {
			writeFinalReport(client, "complete");
			if (EXIT_ON_COMPLETE) {
				client.stop();
			}
			return;
		}

		String scene = SCENES[sceneIndex];
		sceneTicks++;
		if (sceneTicks == 1) {
			if (client.screen != null) {
				client.setScreen(null);
			}
			client.getConnection().sendCommand("formic qa scene " + scene);
		}
		if (sceneTicks == COMMAND_TO_SCREENSHOT_TICKS) {
			capture(client, scene);
		}
		if (sceneTicks >= SCENE_TOTAL_TICKS) {
			sceneIndex++;
			sceneTicks = 0;
		}
	}

	private static void waitForWorld(Minecraft client) {
		waitTicks++;
		if (!openWorldRequested && waitTicks > 80 && client.screen != null) {
			openWorldRequested = true;
			client.createWorldOpenFlows().openWorld(WORLD_ID, () -> {
			});
			return;
		}
		if (waitTicks < WAIT_FOR_WORLD_TICKS || wroteWaitingReport) {
			return;
		}
		wroteWaitingReport = true;
		prepareOutput();
		writeText(OUTPUT_DIR.resolve("visual-qa-summary.md"), """
				# Visual QA

				Status: waiting_for_world

				The client started with `-Dformic.visualQa=true`, but no playable world was loaded before the timeout.
				Expected world folder: `%s`.
				""".formatted(WORLD_ID));
		writeText(OUTPUT_DIR.resolve("visual-qa-summary.json"), "{\"status\":\"waiting_for_world\",\"screenshots\":[]}\n");
		if (EXIT_ON_COMPLETE) {
			client.stop();
		}
	}

	private static void capture(Minecraft client, String scene) {
		prepareOutput();
		String filename = scene + ".png";
		Screenshot.grab(OUTPUT_DIR.toFile(), filename, client.getMainRenderTarget(), 1, message -> {
		});
		CAPTURES.add("screenshots/" + filename);
	}

	private static void writeFinalReport(Minecraft client, String status) {
		if (wroteFinalReport) {
			return;
		}
		wroteFinalReport = true;
		StringBuilder json = new StringBuilder();
		json.append("{\n");
		json.append("  \"status\": \"").append(status).append("\",\n");
		json.append("  \"capturedAt\": \"").append(Instant.now()).append("\",\n");
		json.append("  \"worldLoaded\": ").append(client.level != null).append(",\n");
		json.append("  \"screenshots\": [");
		for (int i = 0; i < CAPTURES.size(); i++) {
			if (i > 0) {
				json.append(", ");
			}
			json.append("\"").append(CAPTURES.get(i)).append("\"");
		}
		json.append("]\n");
		json.append("}\n");
		writeText(OUTPUT_DIR.resolve("visual-qa-summary.json"), json.toString());

		StringBuilder markdown = new StringBuilder();
		markdown.append("# Visual QA\n\n");
		markdown.append("Status: ").append(status).append("\n\n");
		for (String capture : CAPTURES) {
			markdown.append("- ").append(capture).append("\n");
		}
		writeText(OUTPUT_DIR.resolve("visual-qa-summary.md"), markdown.toString());
	}

	private static void prepareOutput() {
		try {
			Files.createDirectories(SCREENSHOT_DIR);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to create visual QA output directory " + SCREENSHOT_DIR, exception);
		}
	}

	private static void writeText(Path path, String content) {
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, content);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to write visual QA report " + path, exception);
		}
	}
}
