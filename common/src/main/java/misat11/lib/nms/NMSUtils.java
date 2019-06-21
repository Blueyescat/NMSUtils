package misat11.lib.nms;

import java.lang.reflect.Constructor;

import org.bukkit.Bukkit;
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
			Class<?> EnumClientCommand = getNMSClass("EnumClientCommand");
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
			
		}
	}
	
	public static void respawn(Plugin instance, Player player, long delay) {
		new BukkitRunnable() {
			
			@Override
			public void run() {
				/*if (IS_SPIGOT_SERVER) {
					try {
						player.spigot().respawn();
					} catch (Throwable t) {
						internalRespawn(player);
					}
				} else {*/
					internalRespawn(player);
				//}
			}
		}.runTaskLater(instance, 20L);
	}
}