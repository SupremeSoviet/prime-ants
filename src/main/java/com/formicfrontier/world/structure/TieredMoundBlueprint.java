package com.formicfrontier.world.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Small, validated intermediate representation for an LLM-authored mound.
 *
 * <p>The blueprint describes a few overlapping tapered tiers and carved mouths,
 * not individual blocks. That keeps the editable data compact while a
 * deterministic compiler owns connectivity, material legality and placement.</p>
 */
public record TieredMoundBlueprint(
		int schemaVersion,
		String name,
		int seed,
		String palette,
		List<Tier> tiers,
		List<Terrace> terraces,
		List<Chamber> chambers,
		List<Pit> pits,
		List<Connection> connections,
		List<Mouth> mouths
) {
	public static final int SUPPORTED_SCHEMA_VERSION = 1;
	private static final int MAX_RADIUS = 24;
	private static final int MAX_HEIGHT = 48;
	private static final Set<String> SUPPORTED_PALETTES = Set.of(
			"earth", "food_store", "nursery", "mine", "chitin_farm", "barracks", "market",
			"pheromone_archive", "armory", "diplomacy_shrine", "resin_depot", "fungus_garden", "venom_press",
			"watch_post", "great_mound", "trade_hub"
	);
	private static final Set<String> SUPPORTED_CHAMBER_PURPOSES = Set.of(
			"queen_hall", "storage", "lookout", "food_store", "nursery", "mine", "chitin_farm", "barracks", "market",
			"archive_hall", "archive_loft", "armory_forge", "armory_vault", "diplomacy_shrine",
			"resin_workshop", "resin_vault", "fungus_garden", "venom_press_hall", "venom_vault",
			"watch_guard", "watch_lookout", "great_larder", "great_workshop", "great_crown",
			"trade_hub_court", "trade_hub_warehouse", "trade_hub_brokerage"
	);

	public TieredMoundBlueprint {
		tiers = List.copyOf(tiers);
		terraces = List.copyOf(terraces);
		chambers = List.copyOf(chambers);
		pits = List.copyOf(pits);
		connections = List.copyOf(connections);
		mouths = List.copyOf(mouths);
		validate(schemaVersion, name, palette, tiers, terraces, chambers, pits, connections, mouths);
	}

	public static TieredMoundBlueprint load(String resourcePath) {
		try (InputStream stream = TieredMoundBlueprint.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (stream == null) {
				throw new IllegalArgumentException("Missing tiered mound blueprint: " + resourcePath);
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			List<Tier> tiers = new ArrayList<>();
			for (JsonElement element : requiredArray(root, "tiers")) {
				JsonObject tier = element.getAsJsonObject();
				tiers.add(new Tier(
						requiredInt(tier, "baseY"),
						requiredInt(tier, "height"),
						requiredDouble(tier, "baseRadiusX"),
						requiredDouble(tier, "baseRadiusZ"),
						requiredDouble(tier, "topRadiusX"),
						requiredDouble(tier, "topRadiusZ"),
						requiredDouble(tier, "offsetX"),
						requiredDouble(tier, "offsetZ")
				));
			}
			List<Terrace> terraces = new ArrayList<>();
			for (JsonElement element : requiredArray(root, "terraces")) {
				JsonObject terrace = element.getAsJsonObject();
				terraces.add(new Terrace(
						requiredInt(terrace, "x"),
						requiredInt(terrace, "y"),
						requiredInt(terrace, "z"),
						requiredDouble(terrace, "radiusX"),
						requiredDouble(terrace, "radiusZ"),
						requiredInt(terrace, "thickness")
				));
			}
			List<Chamber> chambers = new ArrayList<>();
			for (JsonElement element : requiredArray(root, "chambers")) {
				JsonObject chamber = element.getAsJsonObject();
				chambers.add(new Chamber(
						requiredString(chamber, "id"),
						requiredString(chamber, "purpose"),
						requiredInt(chamber, "x"),
						requiredInt(chamber, "floorY"),
						requiredInt(chamber, "z"),
						requiredDouble(chamber, "radiusX"),
						requiredDouble(chamber, "radiusZ"),
						requiredInt(chamber, "height"),
						optionalBoolean(chamber, "openToSky")
				));
			}
			List<Connection> connections = new ArrayList<>();
			List<Pit> pits = new ArrayList<>();
			for (JsonElement element : optionalArray(root, "pits")) {
				JsonObject pit = element.getAsJsonObject();
				pits.add(new Pit(
						requiredString(pit, "id"),
						requiredString(pit, "chamber"),
						requiredInt(pit, "x"),
						requiredInt(pit, "z"),
						requiredDouble(pit, "radiusX"),
						requiredDouble(pit, "radiusZ"),
						requiredInt(pit, "depth")
				));
			}
			for (JsonElement element : requiredArray(root, "connections")) {
				JsonObject connection = element.getAsJsonObject();
				connections.add(new Connection(
						requiredString(connection, "id"),
						requiredString(connection, "from"),
						requiredString(connection, "to"),
						requiredInt(connection, "startX"),
						requiredInt(connection, "startZ"),
						requiredString(connection, "direction"),
						requiredInt(connection, "width")
				));
			}
			List<Mouth> mouths = new ArrayList<>();
			for (JsonElement element : requiredArray(root, "mouths")) {
				JsonObject mouth = element.getAsJsonObject();
				mouths.add(new Mouth(
						requiredInt(mouth, "x"),
						requiredInt(mouth, "y"),
						requiredInt(mouth, "frontZ"),
						requiredInt(mouth, "width"),
						requiredInt(mouth, "height"),
						requiredInt(mouth, "depth")
				));
			}
			return new TieredMoundBlueprint(
					requiredInt(root, "schemaVersion"),
					requiredString(root, "name"),
					requiredInt(root, "seed"),
					requiredString(root, "palette"),
					tiers,
					terraces,
					chambers,
					pits,
					connections,
					mouths
			);
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException("Invalid tiered mound blueprint " + resourcePath + ": " + exception.getMessage(), exception);
		} catch (Exception exception) {
			throw new IllegalStateException("Could not load tiered mound blueprint " + resourcePath, exception);
		}
	}

	public int minX() {
		int tierMin = tiers.stream().mapToInt(tier -> (int) Math.floor(tier.offsetX() - tier.baseRadiusX()) - 1).min().orElse(0);
		int terraceMin = terraces.stream().mapToInt(terrace -> (int) Math.floor(terrace.x() - terrace.radiusX()) - 1).min().orElse(tierMin);
		return Math.min(tierMin, terraceMin);
	}

	public int maxX() {
		int tierMax = tiers.stream().mapToInt(tier -> (int) Math.ceil(tier.offsetX() + tier.baseRadiusX()) + 1).max().orElse(0);
		int terraceMax = terraces.stream().mapToInt(terrace -> (int) Math.ceil(terrace.x() + terrace.radiusX()) + 1).max().orElse(tierMax);
		return Math.max(tierMax, terraceMax);
	}

	public int minZ() {
		int tierMin = tiers.stream().mapToInt(tier -> (int) Math.floor(tier.offsetZ() - tier.baseRadiusZ()) - 1).min().orElse(0);
		int terraceMin = terraces.stream().mapToInt(terrace -> (int) Math.floor(terrace.z() - terrace.radiusZ()) - 1).min().orElse(tierMin);
		return Math.min(tierMin, terraceMin);
	}

	public int maxZ() {
		int tierMax = tiers.stream().mapToInt(tier -> (int) Math.ceil(tier.offsetZ() + tier.baseRadiusZ()) + 1).max().orElse(0);
		int terraceMax = terraces.stream().mapToInt(terrace -> (int) Math.ceil(terrace.z() + terrace.radiusZ()) + 1).max().orElse(tierMax);
		return Math.max(tierMax, terraceMax);
	}

	public int maxY() {
		return tiers.stream().mapToInt(Tier::topY).max().orElse(0);
	}

	public int minY() {
		return pits.stream().mapToInt(pit -> {
			Chamber chamber = chamberById(chambers, pit.chamber());
			return chamber.floorY() - pit.depth();
		}).min().orElse(0);
	}

	public boolean contains(int x, int y, int z) {
		if (terraces.stream().anyMatch(terrace -> terrace.contains(x, y, z))) {
			return true;
		}
		for (Tier tier : tiers) {
			if (!tier.containsY(y)) {
				continue;
			}
			double progress = tier.progressAt(y);
			double radiusX = lerp(tier.baseRadiusX(), tier.topRadiusX(), progress);
			double radiusZ = lerp(tier.baseRadiusZ(), tier.topRadiusZ(), progress);
			double nx = (x - tier.offsetX()) / radiusX;
			double nz = (z - tier.offsetZ()) / radiusZ;
			double distance = nx * nx + nz * nz;
			if (distance <= 0.82) {
				return true;
			}
			double boundaryWobble = signedNoise(x, y, z, seed) * 0.07;
			if (distance <= 1.0 + boundaryWobble) {
				return true;
			}
		}
		return false;
	}

	public boolean isSolid(int x, int y, int z) {
		return contains(x, y, z)
				&& mouths.stream().noneMatch(mouth -> mouth.carves(x, y, z))
				&& chambers.stream().noneMatch(chamber -> chamber.carves(x, y, z))
				&& pits.stream().noneMatch(pit -> pit.carves(x, y, z, chamberById(chambers, pit.chamber())))
				&& connections.stream().noneMatch(connection -> connection.carves(x, y, z, chambers));
	}

	private static void validate(int schemaVersion, String name, String palette, List<Tier> tiers, List<Terrace> terraces,
			List<Chamber> chambers, List<Pit> pits, List<Connection> connections, List<Mouth> mouths) {
		if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
			throw new IllegalArgumentException("Unsupported schemaVersion " + schemaVersion);
		}
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Blueprint name must not be blank");
		}
		if (!SUPPORTED_PALETTES.contains(palette)) {
			throw new IllegalArgumentException("Unsupported material palette " + palette);
		}
		if (tiers.isEmpty()) {
			throw new IllegalArgumentException("At least one tier is required");
		}
		int previousBase = -1;
		int connectedTop = -1;
		for (int index = 0; index < tiers.size(); index++) {
			Tier tier = tiers.get(index);
			if (tier.baseY() < 0 || tier.height() < 2 || tier.topY() >= MAX_HEIGHT) {
				throw new IllegalArgumentException("Tier " + index + " has invalid vertical range");
			}
			if (tier.baseY() < previousBase) {
				throw new IllegalArgumentException("Tiers must be ordered from bottom to top");
			}
			if (index == 0 && tier.baseY() != 0) {
				throw new IllegalArgumentException("The first tier must begin at y=0");
			}
			if (index > 0 && tier.baseY() > connectedTop) {
				throw new IllegalArgumentException("Tier " + index + " does not overlap the mound below it");
			}
			validateRadius(tier.baseRadiusX(), "baseRadiusX", index);
			validateRadius(tier.baseRadiusZ(), "baseRadiusZ", index);
			validateRadius(tier.topRadiusX(), "topRadiusX", index);
			validateRadius(tier.topRadiusZ(), "topRadiusZ", index);
			if (tier.topRadiusX() > tier.baseRadiusX() || tier.topRadiusZ() > tier.baseRadiusZ()) {
				throw new IllegalArgumentException("Tier " + index + " must taper toward its top");
			}
			if (Math.abs(tier.offsetX()) > MAX_RADIUS || Math.abs(tier.offsetZ()) > MAX_RADIUS) {
				throw new IllegalArgumentException("Tier " + index + " offset exceeds the safety envelope");
			}
			previousBase = tier.baseY();
			connectedTop = Math.max(connectedTop, tier.topY());
		}
		for (int index = 0; index < terraces.size(); index++) {
			Terrace terrace = terraces.get(index);
			if (terrace.y() < 1 || terrace.y() > connectedTop || terrace.thickness() < 1 || terrace.thickness() > 3) {
				throw new IllegalArgumentException("Terrace " + index + " has an invalid vertical range");
			}
			validateRadius(terrace.radiusX(), "terrace radiusX", index);
			validateRadius(terrace.radiusZ(), "terrace radiusZ", index);
			if (Math.abs(terrace.x()) > MAX_RADIUS || Math.abs(terrace.z()) > MAX_RADIUS) {
				throw new IllegalArgumentException("Terrace " + index + " offset exceeds the safety envelope");
			}
			boolean anchored = tiers.stream().anyMatch(tier -> tier.containsY(terrace.y())
					&& tierDistance(tier, terrace.x(), terrace.y(), terrace.z()) <= 0.82);
			if (!anchored) {
				throw new IllegalArgumentException("Terrace " + index + " must overlap the stable core of a tier");
			}
		}
		if (chambers.stream().map(Chamber::id).distinct().count() != chambers.size()) {
			throw new IllegalArgumentException("Chamber ids must be unique");
		}
		for (int index = 0; index < chambers.size(); index++) {
			Chamber chamber = chambers.get(index);
			if (chamber.id() == null || chamber.id().isBlank()) {
				throw new IllegalArgumentException("Chamber " + index + " id must not be blank");
			}
			if (!SUPPORTED_CHAMBER_PURPOSES.contains(chamber.purpose())) {
				throw new IllegalArgumentException("Chamber " + index + " has unsupported purpose " + chamber.purpose());
			}
			if (chamber.floorY() < 0 || chamber.height() < 3 || chamber.height() > 7 || chamber.topY() > connectedTop) {
				throw new IllegalArgumentException("Chamber " + index + " has an invalid vertical range");
			}
			if (chamber.openToSky() && chamber.topY() != connectedTop) {
				throw new IllegalArgumentException("Open chamber " + index + " must reach the mound top");
			}
			validateRadius(chamber.radiusX(), "chamber radiusX", index);
			validateRadius(chamber.radiusZ(), "chamber radiusZ", index);
			if (chamber.radiusX() > 8.0 || chamber.radiusZ() > 8.0
					|| Math.abs(chamber.x()) > MAX_RADIUS || Math.abs(chamber.z()) > MAX_RADIUS) {
				throw new IllegalArgumentException("Chamber " + index + " exceeds the interior safety envelope");
			}
			boolean enclosed = tiers.stream().anyMatch(tier -> tier.containsY(chamber.floorY() + 1)
					&& tierDistance(tier, chamber.x(), chamber.floorY() + 1, chamber.z()) <= 0.82);
			if (!enclosed) {
				throw new IllegalArgumentException("Chamber " + index + " must begin inside a stable tier core");
			}
		}
		if (pits.stream().map(Pit::id).distinct().count() != pits.size()) {
			throw new IllegalArgumentException("Pit ids must be unique");
		}
		for (int index = 0; index < pits.size(); index++) {
			Pit pit = pits.get(index);
			if (pit.id() == null || pit.id().isBlank() || pit.depth() < 1 || pit.depth() > 4) {
				throw new IllegalArgumentException("Pit " + index + " has invalid identity or depth");
			}
			if (!Double.isFinite(pit.radiusX()) || !Double.isFinite(pit.radiusZ())
					|| pit.radiusX() < 1.0 || pit.radiusZ() < 1.0
					|| pit.radiusX() > 4.0 || pit.radiusZ() > 4.0) {
				throw new IllegalArgumentException("Pit " + index + " exceeds the supported footprint");
			}
			Chamber chamber = chamberById(chambers, pit.chamber());
			if (!chamber.containsFloor(pit.x(), pit.z())
					|| Math.abs(pit.x() - chamber.x()) + pit.radiusX() > chamber.radiusX() - 0.25
					|| Math.abs(pit.z() - chamber.z()) + pit.radiusZ() > chamber.radiusZ() - 0.25) {
				throw new IllegalArgumentException("Pit " + index + " must fit inside chamber " + chamber.id());
			}
		}
		if (connections.stream().map(Connection::id).distinct().count() != connections.size()) {
			throw new IllegalArgumentException("Connection ids must be unique");
		}
		for (int index = 0; index < connections.size(); index++) {
			Connection connection = connections.get(index);
			if (connection.id() == null || connection.id().isBlank() || connection.width() < 1 || connection.width() > 2) {
				throw new IllegalArgumentException("Connection " + index + " has invalid identity or width");
			}
			Chamber from = chamberById(chambers, connection.from());
			Chamber to = chamberById(chambers, connection.to());
			int rise = to.floorY() - from.floorY();
			if (rise < 2 || rise > 12 || (connection.dx() == 0 && connection.dz() == 0)) {
				throw new IllegalArgumentException("Connection " + index + " must rise 2-12 blocks in a cardinal direction");
			}
			if (!from.containsFloor(connection.startX(), connection.startZ())) {
				throw new IllegalArgumentException("Connection " + index + " must start inside chamber " + from.id());
			}
			int endX = connection.startX() + connection.dx() * rise;
			int endZ = connection.startZ() + connection.dz() * rise;
			if (!to.containsFloor(endX, endZ)) {
				throw new IllegalArgumentException("Connection " + index + " must land inside chamber " + to.id());
			}
			for (int step = 0; step < rise; step++) {
				int x = connection.startX() + connection.dx() * step;
				int y = from.floorY() + step + 1;
				int z = connection.startZ() + connection.dz() * step;
				boolean inside = tiers.stream().anyMatch(tier -> tier.containsY(y)
						&& tierDistance(tier, x, y, z) <= 1.0);
				if (!inside) {
					throw new IllegalArgumentException("Connection " + index + " leaves the mound near step " + step);
				}
			}
		}
		for (int index = 0; index < mouths.size(); index++) {
			Mouth mouth = mouths.get(index);
			if (mouth.width() < 1 || mouth.width() > 5 || mouth.width() % 2 == 0) {
				throw new IllegalArgumentException("Mouth " + index + " width must be an odd value from 1 to 5");
			}
			if (mouth.height() < 2 || mouth.height() > 6 || mouth.depth() < 2 || mouth.depth() > 8) {
				throw new IllegalArgumentException("Mouth " + index + " exceeds the supported size");
			}
			if (mouth.y() < 1 || mouth.topY() > connectedTop || mouth.frontZ() >= 0) {
				throw new IllegalArgumentException("Mouth " + index + " must be a south-facing opening inside the mound height");
			}
		}
	}

	private static void validateRadius(double value, String field, int tierIndex) {
		if (!Double.isFinite(value) || value < 1.0 || value > MAX_RADIUS) {
			throw new IllegalArgumentException("Tier " + tierIndex + " has invalid " + field);
		}
	}

	private static double lerp(double start, double end, double progress) {
		return start + (end - start) * progress;
	}

	private static double tierDistance(Tier tier, int x, int y, int z) {
		double progress = tier.progressAt(y);
		double radiusX = lerp(tier.baseRadiusX(), tier.topRadiusX(), progress);
		double radiusZ = lerp(tier.baseRadiusZ(), tier.topRadiusZ(), progress);
		double nx = (x - tier.offsetX()) / radiusX;
		double nz = (z - tier.offsetZ()) / radiusZ;
		return nx * nx + nz * nz;
	}

	private static Chamber chamberById(List<Chamber> chambers, String id) {
		return chambers.stream().filter(chamber -> chamber.id().equals(id)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown chamber id " + id));
	}

	private static double signedNoise(int x, int y, int z, int seed) {
		long value = x * 73428767L ^ y * 912931L ^ z * 43828933L ^ seed * 199999L;
		value ^= value >>> 33;
		value *= 0xff51afd7ed558ccdL;
		value ^= value >>> 33;
		return ((value & 0xffffL) / 32767.5) - 1.0;
	}

	private static JsonArray requiredArray(JsonObject object, String key) {
		if (!object.has(key) || !object.get(key).isJsonArray()) {
			throw new IllegalArgumentException("Missing array '" + key + "'");
		}
		return object.getAsJsonArray(key);
	}

	private static JsonArray optionalArray(JsonObject object, String key) {
		if (!object.has(key)) {
			return new JsonArray();
		}
		if (!object.get(key).isJsonArray()) {
			throw new IllegalArgumentException("Expected array '" + key + "'");
		}
		return object.getAsJsonArray(key);
	}

	private static boolean optionalBoolean(JsonObject object, String key) {
		if (!object.has(key)) {
			return false;
		}
		if (!object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isBoolean()) {
			throw new IllegalArgumentException("Expected boolean '" + key + "'");
		}
		return object.get(key).getAsBoolean();
	}

	private static int requiredInt(JsonObject object, String key) {
		if (!object.has(key)) {
			throw new IllegalArgumentException("Missing integer '" + key + "'");
		}
		return object.get(key).getAsInt();
	}

	private static double requiredDouble(JsonObject object, String key) {
		if (!object.has(key)) {
			throw new IllegalArgumentException("Missing number '" + key + "'");
		}
		return object.get(key).getAsDouble();
	}

	private static String requiredString(JsonObject object, String key) {
		if (!object.has(key)) {
			throw new IllegalArgumentException("Missing string '" + key + "'");
		}
		return object.get(key).getAsString();
	}

	public record Tier(
			int baseY,
			int height,
			double baseRadiusX,
			double baseRadiusZ,
			double topRadiusX,
			double topRadiusZ,
			double offsetX,
			double offsetZ
	) {
		public int topY() {
			return baseY + height - 1;
		}

		public boolean containsY(int y) {
			return y >= baseY && y <= topY();
		}

		public double progressAt(int y) {
			return (y - baseY) / (double) (height - 1);
		}
	}

	/** A shallow, mound-attached earth shelf that makes an inhabited floor legible. */
	public record Terrace(int x, int y, int z, double radiusX, double radiusZ, int thickness) {
		public boolean contains(int px, int py, int pz) {
			if (py > y || py < y - thickness + 1) {
				return false;
			}
			double nx = (px - x) / radiusX;
			double nz = (pz - z) / radiusZ;
			return nx * nx + nz * nz <= 1.0;
		}
	}

	/** A vaulted room carved behind one facade mouth and furnished by purpose. */
	public record Chamber(String id, String purpose, int x, int floorY, int z,
			double radiusX, double radiusZ, int height, boolean openToSky) {
		public int topY() {
			return floorY + height;
		}

		public boolean carves(int px, int py, int pz) {
			if (py <= floorY || py > topY()) {
				return false;
			}
			double ceilingScale = openToSky ? 1.0 : py == topY() ? 0.72 : py == topY() - 1 ? 0.9 : 1.0;
			double nx = (px - x) / (radiusX * ceilingScale);
			double nz = (pz - z) / (radiusZ * ceilingScale);
			return nx * nx + nz * nz <= 1.0;
		}

		public boolean containsFloor(int px, int pz) {
			double nx = (px - x) / radiusX;
			double nz = (pz - z) / radiusZ;
			return nx * nx + nz * nz <= 1.0;
		}
	}

	/** A shallow stepped excavation cut through one chamber floor. */
	public record Pit(String id, String chamber, int x, int z,
			double radiusX, double radiusZ, int depth) {
		public int depthAt(int px, int pz) {
			double nx = (px - x) / radiusX;
			double nz = (pz - z) / radiusZ;
			double distance = nx * nx + nz * nz;
			if (distance > 1.0) {
				return 0;
			}
			return distance <= 0.36 ? depth : Math.max(1, depth - 1);
		}

		public boolean carves(int px, int py, int pz, Chamber owner) {
			int localDepth = depthAt(px, pz);
			return localDepth > 0 && py <= owner.floorY() && py > owner.floorY() - localDepth;
		}

		public int bottomY(int px, int pz, Chamber owner) {
			return owner.floorY() - depthAt(px, pz);
		}
	}

	/** A compact stair passage that rises one block per horizontal step. */
	public record Connection(String id, String from, String to, int startX, int startZ,
			String direction, int width) {
		public int dx() {
			return switch (direction) {
				case "east" -> 1;
				case "west" -> -1;
				case "north", "south" -> 0;
				default -> 0;
			};
		}

		public int dz() {
			return switch (direction) {
				case "south" -> 1;
				case "north" -> -1;
				case "east", "west" -> 0;
				default -> 0;
			};
		}

		public boolean carves(int px, int py, int pz, List<Chamber> chambers) {
			Chamber fromChamber = chamberById(chambers, from);
			Chamber toChamber = chamberById(chambers, to);
			int rise = toChamber.floorY() - fromChamber.floorY();
			int sideX = -dz();
			int sideZ = dx();
			for (int step = 0; step < rise; step++) {
				for (int lane = 0; lane < width; lane++) {
					int x = startX + dx() * step + sideX * lane;
					int y = fromChamber.floorY() + step;
					int z = startZ + dz() * step + sideZ * lane;
					if (px == x && pz == z && py >= y + 1 && py <= y + 2) {
						return true;
					}
				}
			}
			int landingX = startX + dx() * rise;
			int landingZ = startZ + dz() * rise;
			return px == landingX && pz == landingZ
					&& py >= toChamber.floorY() + 1 && py <= toChamber.floorY() + 2;
		}
	}

	public record Mouth(int x, int y, int frontZ, int width, int height, int depth) {
		public int topY() {
			return y + height - 1;
		}

		public int rearZ() {
			return frontZ + depth;
		}

		public boolean carves(int px, int py, int pz) {
			int halfWidth = width / 2;
			if (px < x - halfWidth || px > x + halfWidth || py < y || py > topY()
					|| pz < frontZ || pz >= rearZ()) {
				return false;
			}
			// Leave opposite top corners in place so the opening is irregular rather
			// than a perfectly rectangular punched hole.
			return py != topY() || Math.abs(px - x) != halfWidth || Math.floorMod(px + py + pz, 2) == 0;
		}
	}
}
