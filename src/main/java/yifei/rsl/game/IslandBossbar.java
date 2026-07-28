package yifei.rsl.game;

import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.entity.boss.BossBarManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class IslandBossbar {
	private static final Identifier BOSSBAR_ID = new Identifier("rsl", "island_timer");
	
	private CommandBossBar bossbar;
	private final Set<ServerPlayerEntity> players = new HashSet<>();
	
	public void createBossbar() {
		if (bossbar != null) {
			removeBossbar();
		}
		
		bossbar = new CommandBossBar(
			BOSSBAR_ID,
			Text.translatable("rsl.bossbar.title", 0)
		);
		bossbar.setColor(CommandBossBar.Color.BLUE);
		bossbar.setStyle(CommandBossBar.Style.PROGRESS);
	}
	
	public void removeBossbar() {
		if (bossbar != null) {
			for (ServerPlayerEntity player : players) {
				bossbar.removePlayer(player);
			}
			players.clear();
			bossbar = null;
		}
	}
	
	public void update(int secondsRemaining, int totalSeconds) {
		if (bossbar == null) {
			return;
		}
		
		bossbar.setName(Text.translatable("rsl.bossbar.title", secondsRemaining));
		float progress = (float) secondsRemaining / totalSeconds;
		bossbar.setPercent(progress);
	}
	
	public void addPlayer(ServerPlayerEntity player) {
		if (bossbar != null && !players.contains(player)) {
			bossbar.addPlayer(player);
			players.add(player);
		}
	}
	
	public void removePlayer(ServerPlayerEntity player) {
		if (bossbar != null && players.contains(player)) {
			bossbar.removePlayer(player);
			players.remove(player);
		}
	}
	
	public void addAllPlayers(java.util.Collection<ServerPlayerEntity> playerList) {
		for (ServerPlayerEntity player : playerList) {
			addPlayer(player);
		}
	}
}
