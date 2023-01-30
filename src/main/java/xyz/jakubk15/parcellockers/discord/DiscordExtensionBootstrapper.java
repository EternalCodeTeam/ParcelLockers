package xyz.jakubk15.parcellockers.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordExtensionBootstrapper {

	private static JDA jda;

	public static void init(String[] args) {
		jda = JDABuilder.createDefault(args[0])
			.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
			.setActivity(Activity.watching("your parcels"))
			.setEnabledIntents(GatewayIntent.DIRECT_MESSAGE_REACTIONS,
				GatewayIntent.DIRECT_MESSAGES,
				GatewayIntent.GUILD_MEMBERS,
				GatewayIntent.GUILD_MESSAGE_REACTIONS,
				GatewayIntent.GUILD_MESSAGES,
				GatewayIntent.MESSAGE_CONTENT)
			.build();
	}

	public static void shutdown() {
		jda.shutdown();
	}

}
