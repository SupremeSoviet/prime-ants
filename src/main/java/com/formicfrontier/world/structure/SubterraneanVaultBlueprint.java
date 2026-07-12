package com.formicfrontier.world.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validated, compact IR for protected rooms and descending stair tunnels below
 * an existing mound. The compiler owns the shell thickness and tunnel envelope;
 * the JSON only describes room roles and traversable topology.
 */
public record SubterraneanVaultBlueprint(
		int schemaVersion,
		String name,
		int seed,
		String palette,
		SurfaceAccess surfaceAccess,
		List<Chamber> chambers,
		List<Descent> connections
) {
	public static final int SUPPORTED_SCHEMA_VERSION = 1;
	private static final Set<String> SUPPORTED_PALETTES = Set.of("queen_vault");
	private static final Set<String> SUPPORTED_PURPOSES = Set.of("vault_guard", "vault_treasury", "vault_sanctum");

	public SubterraneanVaultBlueprint {
		chambers = List.copyOf(chambers);
		connections = List.copyOf(connections);
		validate(schemaVersion, name, palette, surfaceAccess, chambers, connections);
	}

	public static SubterraneanVaultBlueprint load(String resourcePath) {
		try (InputStream stream = SubterraneanVaultBlueprint.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (stream == null) {
				throw new IllegalArgumentException("Missing subterranean vault blueprint: " + resourcePath);
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject access = requiredObject(root, "surfaceAccess");
			SurfaceAccess surfaceAccess = new SurfaceAccess(
					requiredString(access, "id"),
					requiredString(access, "to"),
					requiredInt(access, "surfaceFloorY"),
					requiredInt(access, "startX"),
					requiredInt(access, "startZ"),
					requiredString(access, "direction"),
					requiredInt(access, "width")
			);
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
						requiredInt(chamber, "height")
				));
			}
			List<Descent> connections = new ArrayList<>();
			for (JsonElement element : requiredArray(root, "connections")) {
				JsonObject connection = element.getAsJsonObject();
				connections.add(new Descent(
						requiredString(connection, "id"),
						requiredString(connection, "from"),
						requiredString(connection, "to"),
						requiredInt(connection, "startX"),
						requiredInt(connection, "startZ"),
						requiredString(connection, "direction"),
						requiredInt(connection, "width")
				));
			}
			return new SubterraneanVaultBlueprint(
					requiredInt(root, "schemaVersion"),
					requiredString(root, "name"),
					requiredInt(root, "seed"),
					requiredString(root, "palette"),
					surfaceAccess,
					chambers,
					connections
			);
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException("Invalid subterranean vault blueprint " + resourcePath + ": "
					+ exception.getMessage(), exception);
		} catch (Exception exception) {
			throw new IllegalStateException("Could not load subterranean vault blueprint " + resourcePath, exception);
		}
	}

	public Chamber chamber(String id) {
		return chamberById(chambers, id);
	}

	public int minX() {
		int value = chambers.stream().mapToInt(chamber ->
				(int) Math.floor(chamber.x() - chamber.radiusX() - Chamber.SHELL_THICKNESS) - 1).min().orElse(0);
		value = Math.min(value, Math.min(surfaceAccess.startX(), surfaceAccess.landingX(chamber(surfaceAccess.to()))) - 2);
		for (Descent connection : connections) {
			value = Math.min(value, Math.min(connection.startX(), connection.landingX(chamber(connection.from()), chamber(connection.to()))) - 2);
		}
		return value;
	}

	public int maxX() {
		int value = chambers.stream().mapToInt(chamber ->
				(int) Math.ceil(chamber.x() + chamber.radiusX() + Chamber.SHELL_THICKNESS) + 1).max().orElse(0);
		value = Math.max(value, Math.max(surfaceAccess.startX(), surfaceAccess.landingX(chamber(surfaceAccess.to()))) + 2);
		for (Descent connection : connections) {
			value = Math.max(value, Math.max(connection.startX(), connection.landingX(chamber(connection.from()), chamber(connection.to()))) + 2);
		}
		return value;
	}

	public int minZ() {
		int value = chambers.stream().mapToInt(chamber ->
				(int) Math.floor(chamber.z() - chamber.radiusZ() - Chamber.SHELL_THICKNESS) - 1).min().orElse(0);
		value = Math.min(value, Math.min(surfaceAccess.startZ(), surfaceAccess.landingZ(chamber(surfaceAccess.to()))) - 2);
		for (Descent connection : connections) {
			value = Math.min(value, Math.min(connection.startZ(), connection.landingZ(chamber(connection.from()), chamber(connection.to()))) - 2);
		}
		return value;
	}

	public int maxZ() {
		int value = chambers.stream().mapToInt(chamber ->
				(int) Math.ceil(chamber.z() + chamber.radiusZ() + Chamber.SHELL_THICKNESS) + 1).max().orElse(0);
		value = Math.max(value, Math.max(surfaceAccess.startZ(), surfaceAccess.landingZ(chamber(surfaceAccess.to()))) + 2);
		for (Descent connection : connections) {
			value = Math.max(value, Math.max(connection.startZ(), connection.landingZ(chamber(connection.from()), chamber(connection.to()))) + 2);
		}
		return value;
	}

	public int minY() {
		return chambers.stream().mapToInt(Chamber::floorY).min().orElse(-1);
	}

	public int maxY() {
		return surfaceAccess.surfaceFloorY();
	}

	public boolean isSolid(int x, int y, int z) {
		boolean inEnvelope = chambers.stream().anyMatch(chamber -> chamber.containsEnvelope(x, y, z))
				|| surfaceAccess.envelopes(x, y, z, chamber(surfaceAccess.to()))
				|| connections.stream().anyMatch(connection -> connection.envelopes(
						x, y, z, chamber(connection.from()), chamber(connection.to())));
		if (!inEnvelope) {
			return false;
		}
		if (chambers.stream().anyMatch(chamber -> chamber.carves(x, y, z))) {
			return false;
		}
		if (surfaceAccess.carvesHeadroom(x, y, z, chamber(surfaceAccess.to()))) {
			return false;
		}
		return connections.stream().noneMatch(connection -> connection.carvesHeadroom(
				x, y, z, chamber(connection.from()), chamber(connection.to())));
	}

	private static void validate(int schemaVersion, String name, String palette, SurfaceAccess surfaceAccess,
			List<Chamber> chambers, List<Descent> connections) {
		if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
			throw new IllegalArgumentException("Unsupported schemaVersion " + schemaVersion);
		}
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Blueprint name must not be blank");
		}
		if (!SUPPORTED_PALETTES.contains(palette)) {
			throw new IllegalArgumentException("Unsupported vault palette " + palette);
		}
		if (chambers.size() < 2 || chambers.stream().map(Chamber::id).distinct().count() != chambers.size()) {
			throw new IllegalArgumentException("Vault needs at least two uniquely named chambers");
		}
		for (int index = 0; index < chambers.size(); index++) {
			Chamber chamber = chambers.get(index);
			if (chamber.id() == null || chamber.id().isBlank() || !SUPPORTED_PURPOSES.contains(chamber.purpose())) {
				throw new IllegalArgumentException("Chamber " + index + " has invalid identity or purpose");
			}
			if (chamber.floorY() > -3 || chamber.floorY() < -32 || chamber.height() < 3 || chamber.height() > 6
					|| chamber.topY() > -1) {
				throw new IllegalArgumentException("Chamber " + index + " must remain safely below the surface");
			}
			if (!validRadius(chamber.radiusX()) || !validRadius(chamber.radiusZ())
					|| Math.abs(chamber.x()) > 24 || Math.abs(chamber.z()) > 24) {
				throw new IllegalArgumentException("Chamber " + index + " exceeds the vault safety envelope");
			}
		}
		if (surfaceAccess == null || surfaceAccess.id() == null || surfaceAccess.id().isBlank()
				|| surfaceAccess.surfaceFloorY() != 0 || !validDirection(surfaceAccess.direction())
				|| surfaceAccess.width() < 1 || surfaceAccess.width() > 2) {
			throw new IllegalArgumentException("Surface access has an invalid identity or stair contract");
		}
		Chamber accessTarget = chamberById(chambers, surfaceAccess.to());
		int accessDrop = surfaceAccess.drop(accessTarget);
		if (accessDrop < 3 || accessDrop > 16
				|| !accessTarget.containsFloor(surfaceAccess.landingX(accessTarget), surfaceAccess.landingZ(accessTarget))) {
			throw new IllegalArgumentException("Surface access must land inside its target chamber");
		}
		if (connections.stream().map(Descent::id).distinct().count() != connections.size()) {
			throw new IllegalArgumentException("Descent ids must be unique");
		}
		for (int index = 0; index < connections.size(); index++) {
			Descent connection = connections.get(index);
			if (connection.id() == null || connection.id().isBlank() || !validDirection(connection.direction())
					|| connection.width() < 1 || connection.width() > 2) {
				throw new IllegalArgumentException("Descent " + index + " has an invalid identity or stair contract");
			}
			Chamber from = chamberById(chambers, connection.from());
			Chamber to = chamberById(chambers, connection.to());
			int drop = connection.drop(from, to);
			if (drop < 2 || drop > 12 || !from.containsFloor(connection.startX(), connection.startZ())
					|| !to.containsFloor(connection.landingX(from, to), connection.landingZ(from, to))) {
				throw new IllegalArgumentException("Descent " + index + " must travel down between authored chambers");
			}
		}
		assertReachableTopology(surfaceAccess, chambers, connections);
	}

	private static void assertReachableTopology(SurfaceAccess surfaceAccess, List<Chamber> chambers,
			List<Descent> connections) {
		Set<String> reached = new HashSet<>();
		ArrayDeque<String> queue = new ArrayDeque<>();
		reached.add(surfaceAccess.to());
		queue.add(surfaceAccess.to());
		while (!queue.isEmpty()) {
			Chamber current = chamberById(chambers, queue.removeFirst());
			for (Descent connection : connections) {
				if (connection.from().equals(current.id()) && reached.add(connection.to())) {
					queue.addLast(connection.to());
				}
			}
			for (Chamber candidate : chambers) {
				if (current.floorY() == candidate.floorY() && interiorsOverlap(current, candidate)
						&& reached.add(candidate.id())) {
					queue.addLast(candidate.id());
				}
			}
		}
		if (reached.size() != chambers.size()) {
			Set<String> unreachable = new HashSet<>();
			chambers.forEach(chamber -> unreachable.add(chamber.id()));
			unreachable.removeAll(reached);
			throw new IllegalArgumentException("Vault contains unreachable chambers " + unreachable);
		}
	}

	private static boolean interiorsOverlap(Chamber first, Chamber second) {
		int y = first.floorY() + 1;
		int minX = (int) Math.floor(Math.max(first.x() - first.radiusX(), second.x() - second.radiusX()));
		int maxX = (int) Math.ceil(Math.min(first.x() + first.radiusX(), second.x() + second.radiusX()));
		int minZ = (int) Math.floor(Math.max(first.z() - first.radiusZ(), second.z() - second.radiusZ()));
		int maxZ = (int) Math.ceil(Math.min(first.z() + first.radiusZ(), second.z() + second.radiusZ()));
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (first.carves(x, y, z) && second.carves(x, y, z)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean validRadius(double radius) {
		return Double.isFinite(radius) && radius >= 2.0 && radius <= 8.0;
	}

	private static boolean validDirection(String direction) {
		return Set.of("east", "west", "north", "south").contains(direction);
	}

	private static Chamber chamberById(List<Chamber> chambers, String id) {
		return chambers.stream().filter(chamber -> chamber.id().equals(id)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown vault chamber " + id));
	}

	private static boolean pathEnvelope(int px, int py, int pz, int startX, int startZ, int startY,
			int drop, int dx, int dz, int width, int ceilingCap) {
		int sideX = -dz;
		int sideZ = dx;
		for (int step = 0; step < drop; step++) {
			for (int lane = 0; lane < width; lane++) {
				int x = startX + dx * step + sideX * lane;
				int y = startY - step;
				int z = startZ + dz * step + sideZ * lane;
				if (py <= ceilingCap && py >= y - 1 && py <= y + 3
						&& Math.abs(px - x) <= 1 && Math.abs(pz - z) <= 1) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean pathCarvesHeadroom(int px, int py, int pz, int startX, int startZ, int startY,
			int drop, int dx, int dz, int width, int landingY) {
		int sideX = -dz;
		int sideZ = dx;
		for (int step = 0; step < drop; step++) {
			for (int lane = 0; lane < width; lane++) {
				int x = startX + dx * step + sideX * lane;
				int y = startY - step;
				int z = startZ + dz * step + sideZ * lane;
				if (px == x && pz == z && py >= y + 1 && py <= y + 2) {
					return true;
				}
			}
		}
		int landingX = startX + dx * drop;
		int landingZ = startZ + dz * drop;
		return px == landingX && pz == landingZ && py >= landingY + 1 && py <= landingY + 2;
	}

	private static JsonObject requiredObject(JsonObject object, String key) {
		if (!object.has(key) || !object.get(key).isJsonObject()) {
			throw new IllegalArgumentException("Missing object " + key);
		}
		return object.getAsJsonObject(key);
	}

	private static JsonArray requiredArray(JsonObject object, String key) {
		if (!object.has(key) || !object.get(key).isJsonArray()) {
			throw new IllegalArgumentException("Missing array " + key);
		}
		return object.getAsJsonArray(key);
	}

	private static String requiredString(JsonObject object, String key) {
		if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
			throw new IllegalArgumentException("Missing string " + key);
		}
		return object.get(key).getAsString();
	}

	private static int requiredInt(JsonObject object, String key) {
		if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
			throw new IllegalArgumentException("Missing integer " + key);
		}
		return object.get(key).getAsInt();
	}

	private static double requiredDouble(JsonObject object, String key) {
		if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
			throw new IllegalArgumentException("Missing number " + key);
		}
		return object.get(key).getAsDouble();
	}

	public record Chamber(String id, String purpose, int x, int floorY, int z,
			double radiusX, double radiusZ, int height) {
		private static final double SHELL_THICKNESS = 1.5;

		public int topY() {
			return floorY + height;
		}

		public boolean containsFloor(int px, int pz) {
			double nx = (px - x) / radiusX;
			double nz = (pz - z) / radiusZ;
			return nx * nx + nz * nz <= 1.0;
		}

		public boolean carves(int px, int py, int pz) {
			if (py <= floorY || py > topY()) {
				return false;
			}
			double ceilingScale = py == topY() ? 0.72 : py == topY() - 1 ? 0.9 : 1.0;
			double nx = (px - x) / (radiusX * ceilingScale);
			double nz = (pz - z) / (radiusZ * ceilingScale);
			return nx * nx + nz * nz <= 1.0;
		}

		public boolean containsEnvelope(int px, int py, int pz) {
			// Chamber ceilings remain fully subterranean. Only the explicitly authored
			// surface-access envelope may touch y=0, so an upgrade cannot recolour the
			// Great Mound's public floor or functional centre block.
			if (py < floorY || py > topY() + 2 || py >= 0) {
				return false;
			}
			double ceilingScale = py == topY() + 2 ? 0.65 : py == topY() + 1 ? 0.85 : 1.0;
			double nx = (px - x) / ((radiusX + SHELL_THICKNESS) * ceilingScale);
			double nz = (pz - z) / ((radiusZ + SHELL_THICKNESS) * ceilingScale);
			return nx * nx + nz * nz <= 1.0;
		}
	}

	public record SurfaceAccess(String id, String to, int surfaceFloorY, int startX, int startZ,
			String direction, int width) {
		public int dx() {
			return direction.equals("east") ? 1 : direction.equals("west") ? -1 : 0;
		}

		public int dz() {
			return direction.equals("south") ? 1 : direction.equals("north") ? -1 : 0;
		}

		public int drop(Chamber target) {
			return surfaceFloorY - target.floorY();
		}

		public int landingX(Chamber target) {
			return startX + dx() * drop(target);
		}

		public int landingZ(Chamber target) {
			return startZ + dz() * drop(target);
		}

		public boolean envelopes(int x, int y, int z, Chamber target) {
			return pathEnvelope(x, y, z, startX, startZ, surfaceFloorY, drop(target), dx(), dz(), width,
					surfaceFloorY);
		}

		public boolean carvesHeadroom(int x, int y, int z, Chamber target) {
			return pathCarvesHeadroom(x, y, z, startX, startZ, surfaceFloorY, drop(target), dx(), dz(), width,
					target.floorY());
		}
	}

	public record Descent(String id, String from, String to, int startX, int startZ,
			String direction, int width) {
		public int dx() {
			return direction.equals("east") ? 1 : direction.equals("west") ? -1 : 0;
		}

		public int dz() {
			return direction.equals("south") ? 1 : direction.equals("north") ? -1 : 0;
		}

		public int drop(Chamber fromChamber, Chamber toChamber) {
			return fromChamber.floorY() - toChamber.floorY();
		}

		public int landingX(Chamber fromChamber, Chamber toChamber) {
			return startX + dx() * drop(fromChamber, toChamber);
		}

		public int landingZ(Chamber fromChamber, Chamber toChamber) {
			return startZ + dz() * drop(fromChamber, toChamber);
		}

		public boolean envelopes(int x, int y, int z, Chamber fromChamber, Chamber toChamber) {
			return pathEnvelope(x, y, z, startX, startZ, fromChamber.floorY(), drop(fromChamber, toChamber),
					dx(), dz(), width, fromChamber.topY() + 1);
		}

		public boolean carvesHeadroom(int x, int y, int z, Chamber fromChamber, Chamber toChamber) {
			return pathCarvesHeadroom(x, y, z, startX, startZ, fromChamber.floorY(),
					drop(fromChamber, toChamber), dx(), dz(), width, toChamber.floorY());
		}
	}
}
