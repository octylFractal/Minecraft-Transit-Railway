package mtr.data;

import mtr.path.PathFinderBase;
import mtr.path.RoutePathFinder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RailwayData extends PersistentState {

	private static final String NAME = "mtr_train_data";
	private static final String KEY_STATIONS = "stations";
	private static final String KEY_STATION = "station_";
	private static final String KEY_PLATFORMS = "platforms";
	private static final String KEY_PLATFORM = "platform_";
	private static final String KEY_ROUTES = "routes";
	private static final String KEY_ROUTE = "route_";
	private static final String KEY_TRAINS = "trains";
	private static final String KEY_TRAIN = "train_";

	private final Set<Station> stations;
	private final Set<Platform> platforms;
	private final Set<Route> routes;
	private final Set<Train> trains;

	private final int VIEW_DISTANCE = 32;

	public RailwayData() {
		super(NAME);
		stations = new HashSet<>();
		platforms = new HashSet<>();
		routes = new HashSet<>();
		trains = new HashSet<>();
	}

	@Override
	public void fromTag(CompoundTag tag) {
		final CompoundTag tagStations = tag.getCompound(KEY_STATIONS);
		for (String key : tagStations.getKeys()) {
			stations.add(new Station(tagStations.getCompound(key)));
		}

		final CompoundTag tagNewPlatforms = tag.getCompound(KEY_PLATFORMS);
		for (String key : tagNewPlatforms.getKeys()) {
			platforms.add(new Platform(tagNewPlatforms.getCompound(key)));
		}

		final CompoundTag tagNewRoutes = tag.getCompound(KEY_ROUTES);
		for (String key : tagNewRoutes.getKeys()) {
			routes.add(new Route(tagNewRoutes.getCompound(key)));
		}

		final CompoundTag tagNewTrains = tag.getCompound(KEY_TRAINS);
		for (String key : tagNewTrains.getKeys()) {
			trains.add(new Train(tagNewTrains.getCompound(key)));
		}
	}

	@Override
	public CompoundTag toTag(CompoundTag tag) {
		final CompoundTag tagStations = new CompoundTag();
		int i = 0;
		for (Station station : stations) {
			tagStations.put(KEY_STATION + i, station.toCompoundTag());
			i++;
		}
		tag.put(KEY_STATIONS, tagStations);

		final CompoundTag tagNewPlatforms = new CompoundTag();
		int j = 0;
		for (Platform platform : platforms) {
			tagNewPlatforms.put(KEY_PLATFORM + j, platform.toCompoundTag());
			j++;
		}
		tag.put(KEY_PLATFORMS, tagNewPlatforms);

		final CompoundTag tagNewRoutes = new CompoundTag();
		int k = 0;
		for (Route route : routes) {
			tagNewRoutes.put(KEY_ROUTE + k, route.toCompoundTag());
			k++;
		}
		tag.put(KEY_ROUTES, tagNewRoutes);

		final CompoundTag tagNewTrains = new CompoundTag();
		int l = 0;
		for (Train train : trains) {
			tagNewTrains.put(KEY_TRAIN + l, train.toCompoundTag());
			l++;
		}
		tag.put(KEY_TRAINS, tagNewTrains);

		return tag;
	}

	public void addStation(Station station) {
		stations.add(station);
		markDirty();
	}

	public Set<Station> getStations() {
		return stations;
	}

	public void addPlatform(Platform newPlatform) {
		platforms.removeIf(newPlatform::overlaps);
		platforms.add(newPlatform);
		markDirty();
	}

	public Set<Platform> getPlatforms(WorldAccess world) {
		validateData(world);
		return platforms;
	}

	public Set<Route> getRoutes() {
		return routes;
	}

	public void addTrain(Train train) {
		trains.add(train);
		markDirty();
	}

	public Set<Train> getTrains() {
		return trains;
	}

	public void removeTrains() {
		trains.clear();
	}

	public void simulateTrains(WorldAccess world) {
		trains.forEach(train -> {
			final int trainLength = train.posX.length;
			final int distanceRemaining = train.path.size() - Math.max(train.pathIndex[0], train.pathIndex[trainLength - 1]);

			if (distanceRemaining <= 0) {
				train.speed = 0;

				if (train.stationIds.isEmpty()) {
					// TODO train is dead
				} else {
					final Station station = getStationById(train.stationIds.get(0));
					final BlockPos start1 = new BlockPos(train.posX[0], train.posY[0], train.posZ[0]);
					final BlockPos start2 = new BlockPos(train.posX[trainLength - 1], train.posY[trainLength - 1], train.posZ[trainLength - 1]);
					final BlockPos destinationPos = station.getCenter();
					final boolean reverse = PathFinderBase.distanceBetween(start1, destinationPos) > PathFinderBase.distanceBetween(start2, destinationPos);
					final RoutePathFinder routePathFinder = new RoutePathFinder(world, reverse ? start1 : start2, station);

					train.resetPathIndex(reverse);
					train.path.clear();
					train.path.addAll(routePathFinder.findPath());
					train.stationIds.remove(0);
				}
			} else {
				if (MathHelper.square(train.speed) >= 2 * train.trainType.getAcceleration() * (distanceRemaining - 1)) {
					if (train.speed >= train.trainType.getAcceleration() * 2) {
						train.speed -= train.trainType.getAcceleration();
					}
				} else if (train.speed < train.trainType.getMaxSpeed()) {
					train.speed += train.trainType.getAcceleration();
				}

				for (int i = 0; i < trainLength; i++) {
					if (train.pathIndex[i] < train.path.size()) {
						final Pos3f newPos = train.path.get(train.pathIndex[i]);
						final Pos3f movement = new Pos3f(newPos.getX() - train.posX[i], newPos.getY() - train.posY[i], newPos.getZ() - train.posZ[i]);

						if (movement.lengthSquared() < MathHelper.square(2 * train.speed)) {
							train.pathIndex[i]++;
						}

						movement.normalize();
						movement.scale(train.speed);
						train.posX[i] += movement.getX();
						train.posY[i] += movement.getY();
						train.posZ[i] += movement.getZ();
					}
				}
			}

			final List<? extends PlayerEntity> players = world.getPlayers();
			for (int i = 0; i < trainLength - 1; i++) {
				final float xAverage = (train.posX[i] + train.posX[i + 1]) / 2;
				final float yAverage = (train.posY[i] + train.posY[i + 1]) / 2;
				final float zAverage = (train.posZ[i] + train.posZ[i + 1]) / 2;
				final boolean playerNearby = players.stream().anyMatch(player -> PathFinderBase.distanceBetween(new BlockPos(xAverage, yAverage, zAverage), player.getBlockPos()) < VIEW_DISTANCE);

				if (playerNearby && train.entities[i] == null) {
					train.entities[i] = train.trainType.create((World) world, xAverage, yAverage, zAverage);
					world.spawnEntity(train.entities[i]);
				}
				if (train.entities[i] != null) {
					if (playerNearby) {
						final float yaw = (float) Math.toDegrees(MathHelper.atan2(train.posX[i + 1] - train.posX[i], train.posZ[i + 1] - train.posZ[i]));
						final float pitch = (float) Math.toDegrees(Math.asin((train.posY[i + 1] - train.posY[i]) / train.trainType.getSpacing()));
						train.entities[i].updatePositionAndAngles(xAverage, yAverage, zAverage, yaw, pitch);
					} else {
						train.entities[i].kill();
						train.entities[i] = null;
					}
				}
			}
		});
		markDirty();
	}

	public void setData(WorldAccess world, Set<Station> stations, Set<Platform> platforms, Set<Route> routes, Set<Train> trains) {
		this.stations.clear();
		this.stations.addAll(stations);
		this.platforms.clear();
		this.platforms.addAll(platforms);
		this.routes.clear();
		this.routes.addAll(routes);
		this.trains.clear();
		this.trains.addAll(trains);
		validateData(world);
	}

	private Station getStationById(long id) {
		return stations.stream().filter(station -> station.id == id).findFirst().orElse(null);
	}

	private void validateData(WorldAccess world) {
		platforms.removeIf(platform -> !platform.hasRail(world));
		routes.forEach(route -> route.stationIds.removeIf(stationId -> getStationById(stationId) == null));
		trains.removeIf(train -> train.stationIds.isEmpty());
		markDirty();
	}

	public static boolean isBetween(int value, int value1, int value2) {
		return value >= Math.min(value1, value2) && value <= Math.max(value1, value2);
	}

	public static RailwayData getInstance(World world) {
		if (world instanceof ServerWorld) {
			return ((ServerWorld) world).getPersistentStateManager().getOrCreate(RailwayData::new, NAME);
		} else {
			return null;
		}
	}
}
