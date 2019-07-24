package misat11.lib.nms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class NMSUtils {

	public static final boolean NMS_BASED_SERVER, IS_SPIGOT_SERVER;
	public static final String NMS_VERSION;

	static {
		boolean isNMS, isSpigot;
		String nmsVersion = "";

		try {
			Class.forName("org.bukkit.craftbukkit.Main");
			isNMS = true;
		} catch (ClassNotFoundException e) {
			isNMS = false;
		}

		if (isNMS) {
			String packName = Bukkit.getServer().getClass().getPackage().getName();
			nmsVersion = packName.substring(packName.lastIndexOf('.') + 1);
		}

		try {
			Package spigotPackage = Package.getPackage("org.spigotmc");
			isSpigot = (spigotPackage != null);
		} catch (Exception e) {
			isSpigot = false;
		}

		NMS_BASED_SERVER = isNMS;
		IS_SPIGOT_SERVER = isSpigot;
		NMS_VERSION = nmsVersion;
	}

	private static Class<?> getNMSClass(String clazz) throws ClassNotFoundException {
		return Class.forName("net.minecraft.server." + NMS_VERSION + "." + clazz);
	}

	private static void internalRespawn(Player player) {
		try {
			Class<?> PacketPlayInClientCommand = getNMSClass("PacketPlayInClientCommand");
			Class<?> EnumClientCommand = getNMSClass("PacketPlayInClientCommand$EnumClientCommand");
			Constructor<?> constructor = PacketPlayInClientCommand.getDeclaredConstructor(EnumClientCommand);
			Object selectedObj = null;
			for (Object obj : EnumClientCommand.getEnumConstants()) {
				if ("PERFORM_RESPAWN".equals(obj.getClass().getMethod("name").invoke(obj))) {
					selectedObj = obj;
					break;
				}
			}
			Object packet = constructor.newInstance(selectedObj);
			Object handler = player.getClass().getMethod("getHandle").invoke(player);
			Object connection = handler.getClass().getField("playerConnection").get(handler);
			connection.getClass().getMethod("a", PacketPlayInClientCommand).invoke(connection, packet);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static void disableEntityAI(LivingEntity entity) {
		try {
			Class<?> NBTTagCompound = getNMSClass("NBTTagCompound");
			Object handler = entity.getClass().getMethod("getHandle").invoke(entity);
			Object tag = handler.getClass().getMethod("getNBTTag").invoke(handler);
			if (tag == null) {
				tag = NBTTagCompound.getConstructor().newInstance();
			}
			handler.getClass().getMethod("c", NBTTagCompound).invoke(handler, tag);
			NBTTagCompound.getMethod("setInt", String.class, int.class).invoke(tag, "NoAI", 1);
			handler.getClass().getMethod("f", NBTTagCompound).invoke(handler, tag);
		} catch (Throwable t) {
		}
	}

	public static void fakeExp(Player player, float percentage, int levels) {
		try {
			Class<?> PacketPlayOutExperience = getNMSClass("PacketPlayOutExperience");
			Class<?> Packet = getNMSClass("Packet");
			Object handler = player.getClass().getMethod("getHandle").invoke(player);
			Object playerConnection = handler.getClass().getField("playerConnection").get(handler);
			Object packet = PacketPlayOutExperience.getConstructor(float.class, int.class, int.class)
					.newInstance(percentage, player.getTotalExperience(), levels);
			playerConnection.getClass().getMethod("sendPacket", Packet).invoke(playerConnection, packet);
		} catch (Throwable t) {

		}
	}

	public static void changeTNTSheepAI(Mob mob, double speed, double follow) {
		try {
			Class<?> PathfinderGoalSelector = getNMSClass("PathfinderGoalSelector");
			Class<?> PathfinderGoalMeleeAttack = getNMSClass("PathfinderGoalMeleeAttack");
			Class<?> EntityCreature = getNMSClass("EntityCreature");
			Class<?> PathfinderGoal = getNMSClass("PathfinderGoal");
			Class<?> GenericAttributes = getNMSClass("GenericAttributes");
			Class<?> IAttribute = getNMSClass("IAttribute");
			Object methodProfiler = getMethodProfiler(mob.getWorld());
			Object handler = mob.getClass().getMethod("getHandle").invoke(mob);
			Object goalSelector = PathfinderGoalSelector.getConstructors()[0].newInstance(methodProfiler);
			Object targetSelector = PathfinderGoalSelector.getConstructors()[0].newInstance(methodProfiler);
			handler.getClass().getField("goalSelector").set(handler, goalSelector);
			handler.getClass().getField("targetSelector").set(handler, targetSelector);
			Object attack = PathfinderGoalMeleeAttack.getConstructor(EntityCreature, double.class, boolean.class)
					.newInstance(handler, 1.0D, false);
			goalSelector.getClass().getMethod("a", int.class, PathfinderGoal).invoke(goalSelector, 0, attack);
			Object MOVEMENT_SPEED = GenericAttributes.getField("MOVEMENT_SPEED").get(null);
			Object FOLLOW_RANGE = GenericAttributes.getField("FOLLOW_RANGE").get(null);
			Object ATTACK_DAMAGE = GenericAttributes.getField("ATTACK_DAMAGE").get(null);
			Object speedO = handler.getClass().getMethod("getAttributeInstance", IAttribute).invoke(handler,
					MOVEMENT_SPEED);
			speedO.getClass().getMethod("setValue", double.class).invoke(speedO, speed);
			Object followO = handler.getClass().getMethod("getAttributeInstance", IAttribute).invoke(handler,
					FOLLOW_RANGE);
			followO.getClass().getMethod("setValue", double.class).invoke(followO, follow);
			Object attrMap = handler.getClass().getMethod("getAttributeMap").invoke(handler);
			Object attackO = attrMap.getClass().getMethod("b", IAttribute).invoke(attrMap,
					ATTACK_DAMAGE);
			attackO.getClass().getMethod("setValue", double.class).invoke(attackO, 0);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private static Object getMethodProfiler(World world) {
		try {
			Object handler = world.getClass().getMethod("getHandle").invoke(world);
			Object methodProfiler;
			try {
				methodProfiler = handler.getClass().getMethod("getMethodProfiler").invoke(handler);
			} catch (Throwable t) {
				Field methodProfilerField = handler.getClass().getField("methodProfiler");
				methodProfilerField.setAccessible(true);
				methodProfiler = methodProfilerField.get(handler);
			}
			return methodProfiler;
		} catch (Throwable t) {

		}
		return null;
	}

	public static void respawn(Plugin instance, Player player, long delay) {
		new BukkitRunnable() {

			@Override
			public void run() {
				if (IS_SPIGOT_SERVER) {
					try {
						player.spigot().respawn();
					} catch (Throwable t) {
						internalRespawn(player);
					}
				} else {
					internalRespawn(player);
				}
			}
		}.runTaskLater(instance, delay);
	}
}
