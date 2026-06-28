package com.formicfrontier.qa;

import com.formicfrontier.registry.ModBlocks;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Deterministic compiler for LLM-authored LAYERED structure schematics.
 *
 * Motivation (2024-2026 literature): LLMs are weak at "spatially correct"
 * imperative geometry code (VoxelCodeBench 2026) but strong at editing a
 * declarative, visible representation (T2BM / BuilderGPT). So a building is
 * authored as a stack of 2D character grids (one per Y layer) + a palette that
 * maps each character to a SEMANTIC material name (a closed vocabulary, not raw
 * Minecraft block ids - avoids the "illegal material" failure T2BM reports).
 * This class is the fixed, deterministic geometry step: schematic -> blocks.
 *
 * JSON shape:
 * {
 *   "name": "food_chamber_v1",
 *   "palette": { "M": "mound", "#": "dark", "C": "food_core", ".": "air" },
 *   "layers": [ { "y": 0, "rows": ["..MM..", ".MMMM."] }, ... ]
 * }
 *
 * The origin is the BOTTOM-CENTRE of the footprint. 'air' (and any unmapped
 * character) is skipped, so the building sits on existing ground and its
 * interior voids stay open.
 */
public final class FormicSchematic {
	private FormicSchematic() {
	}

	/** Resolve a semantic material name to a block. Closed vocabulary the LLM may
	 * use in a palette; unknown names fall back to mound earth so a typo never
	 * crashes the build (it just reads as plain earth). */
	public static Block material(String name) {
		return switch (name == null ? "" : name.toLowerCase()) {
			case "mound" -> ModBlocks.NEST_MOUND;
			case "dark", "core_wall" -> ModBlocks.NEST_CORE;
			case "food_core" -> ModBlocks.FOOD_CHAMBER;
			case "food_node" -> ModBlocks.FOOD_NODE;
			case "dirt" -> Blocks.COARSE_DIRT;
			case "rooted" -> Blocks.ROOTED_DIRT;
			case "mud" -> Blocks.PACKED_MUD;
			case "roots" -> Blocks.MANGROVE_ROOTS;
			case "path" -> Blocks.DIRT_PATH;
			case "moss" -> Blocks.MOSS_BLOCK;
			case "glow" -> Blocks.OCHRE_FROGLIGHT;
			case "air", "" -> Blocks.AIR;
			default -> ModBlocks.NEST_MOUND;
		};
	}

	/** Build the schematic at the given bottom-centre origin from a classpath
	 * resource (e.g. "formic_structures/food_chamber_v1.json"). */
	public static void place(ServerLevel level, BlockPos origin, String resourcePath) {
		JsonObject root = load(resourcePath);
		if (root == null) {
			return;
		}
		Map<Character, Block> palette = new HashMap<>();
		JsonObject paletteJson = root.getAsJsonObject("palette");
		for (Map.Entry<String, com.google.gson.JsonElement> e : paletteJson.entrySet()) {
			if (e.getKey().isEmpty()) {
				continue;
			}
			palette.put(e.getKey().charAt(0), material(e.getValue().getAsString()));
		}
		JsonArray layers = root.getAsJsonArray("layers");
		int width = 0;
		int depth = 0;
		for (com.google.gson.JsonElement le : layers) {
			JsonArray rows = le.getAsJsonObject().getAsJsonArray("rows");
			depth = Math.max(depth, rows.size());
			for (com.google.gson.JsonElement r : rows) {
				width = Math.max(width, r.getAsString().length());
			}
		}
		int halfW = width / 2;
		int halfD = depth / 2;
		for (com.google.gson.JsonElement le : layers) {
			JsonObject layer = le.getAsJsonObject();
			int y = layer.get("y").getAsInt();
			JsonArray rows = layer.getAsJsonArray("rows");
			for (int r = 0; r < rows.size(); r++) {
				String row = rows.get(r).getAsString();
				for (int c = 0; c < row.length(); c++) {
					Block block = palette.get(row.charAt(c));
					if (block == null || block == Blocks.AIR) {
						continue;          // 'air' / unmapped -> leave the cell untouched
					}
					BlockPos pos = new BlockPos(origin.getX() + c - halfW, origin.getY() + y, origin.getZ() + r - halfD);
					level.setBlockAndUpdate(pos, block.defaultBlockState());
				}
			}
		}
	}

	private static JsonObject load(String resourcePath) {
		try (InputStream in = FormicSchematic.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (in == null) {
				return null;
			}
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception exception) {
			return null;
		}
	}
}
