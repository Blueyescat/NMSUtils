package misat11.lib.nms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
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

	public static Class<?> getNMSClass(String clazz) throws ClassNotFoundException {
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

	public static void sendParticles(List<Player> viewers, String particleName, Location loc, int count, double offsetX,
			double offsetY, double offsetZ, double extra) {
		for (Player player : viewers) {
			try {
				player.spawnParticle(Particle.valueOf(particleName.toUpperCase()), loc.getX(), loc.getY(), loc.getZ(),
						count, offsetX, offsetY, offsetZ, extra);
			} catch (Throwable t) {
				sendParticlesInternal(player, particleName.toUpperCase(), (float) loc.getX(), (float) loc.getY(),
						(float) loc.getZ(), count, (float) offsetX, (float) offsetY, (float) offsetZ, (float) extra);
			}
		}
	}

	private static void sendParticlesInternal(Player player, String particleName, float x, float y, float z, int count,
			float offsetX, float offsetY, float offsetZ, float extra) {
		try {
			Class<?> EnumParticle = getNMSClass("EnumParticle");
			Class<?> Packet = getNMSClass("Packet");
			Class<?> PacketPlayOutWorldParticles = getNMSClass("PacketPlayOutWorldParticles");
			Object selectedParticle = null;
			for (Object obj : EnumParticle.getEnumConstants()) {
				if (particleName.equals(obj.getClass().getMethod("b").invoke(obj))) {
					selectedParticle = obj;
					break;
				}
			}
			Object packet = PacketPlayOutWorldParticles
					.getConstructor(EnumParticle, boolean.class, float.class, float.class, float.class, float.class,
							float.class, float.class, float.class, int.class, int[].class)
					.newInstance(selectedParticle, true, x, y, z, offsetX, offsetY, offsetZ, extra, count,
							new int[] {});
			Object handler = player.getClass().getMethod("getHandle").invoke(player);
			Object playerConnection = handler.getClass().getField("playerConnection").get(handler);
			playerConnection.getClass().getMethod("sendPacket", Packet).invoke(playerConnection, packet);
		} catch (Throwable t) {

		}
	}

	public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		try {
			player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
		} catch (Throwable t) {
			internalSendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
		}
	}

	private static void internalSendTitle(Player player, String title, String subtitle, int fadeIn, int stay,
			int fadeOut) {
		try {
			Class<?> PacketPlayOutTitle = getNMSClass("PacketPlayOutTitle");
			Class<?> IChatBaseComponent = getNMSClass("IChatBaseComponent");
			Class<?> Packet = getNMSClass("Packet");
			Class<?> ChatSerializer;
			Class<?> EnumTitleAction;
			try {
				ChatSerializer = getNMSClass("IChatBaseComponent$ChatSerializer");
			} catch (ClassNotFoundException exception) {
				ChatSerializer = getNMSClass("ChatSerializer");
			}
			try {
				EnumTitleAction = getNMSClass("PacketPlayOutTitle$EnumTitleAction");
			} catch (ClassNotFoundException exception) {
				EnumTitleAction = getNMSClass("EnumTitleAction");
			}
			Object titleComponent = ChatSerializer.getMethod("a", String.class).invoke(null,
					"{\"text\": \"" + title + "\"}");
			Object subtitleComponent = ChatSerializer.getMethod("a", String.class).invoke(null,
					"{\"text\": \"" + subtitle + "\"}");
			Object handler = player.getClass().getMethod("getHandle").invoke(player);
			Object connection = handler.getClass().getField("playerConnection").get(handler);
			Object TITLE = null;
			Object SUBTITLE = null;
			Object TIMES = null;
			for (Object obj : EnumTitleAction.getEnumConstants()) {
				if ("TITLE".equals(obj.getClass().getMethod("name").invoke(obj))) {
					TITLE = obj;
				} else if ("SUBTITLE".equals(obj.getClass().getMethod("name").invoke(obj))) {
					SUBTITLE = obj;
				} else if ("TIMES".equals(obj.getClass().getMethod("name").invoke(obj))) {
					TIMES = obj;
				}
			}
			Object titlePacket = PacketPlayOutTitle.getConstructor(EnumTitleAction, IChatBaseComponent)
					.newInstance(TITLE, titleComponent);
			Object subtitlePacket = PacketPlayOutTitle.getConstructor(EnumTitleAction, IChatBaseComponent)
					.newInstance(SUBTITLE, subtitleComponent);
			Object timesPacket = PacketPlayOutTitle
					.getConstructor(EnumTitleAction, IChatBaseComponent, int.class, int.class, int.class)
					.newInstance(TIMES, null, fadeIn, stay, fadeOut);
			Method sendPacket = connection.getClass().getClass().getMethod("sendPacket", Packet);
			sendPacket.invoke(connection, titlePacket);
			sendPacket.invoke(connection, subtitlePacket);
			sendPacket.invoke(connection, timesPacket);
		} catch (Throwable t) {

		}
	}

	public static void makeMobAttackTarget(Mob mob, double speed, double follow, double attackDamage) {
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
			Object attackO = attrMap.getClass().getMethod("b", IAttribute).invoke(attrMap, ATTACK_DAMAGE);
			attackO.getClass().getMethod("setValue", double.class).invoke(attackO, attackDamage);
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
