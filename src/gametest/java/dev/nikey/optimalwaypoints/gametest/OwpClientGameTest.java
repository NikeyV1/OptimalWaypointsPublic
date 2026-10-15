package dev.nikey.optimalwaypoints.gametest;

import dev.nikey.optimalwaypoints.OwpClient;
import dev.nikey.optimalwaypoints.input.OwpKeybinds;
import dev.nikey.optimalwaypoints.protocol.Feature;
import dev.nikey.optimalwaypoints.screen.ImportScreen;
import dev.nikey.optimalwaypoints.screen.SettingsScreen;
import dev.nikey.optimalwaypoints.screen.WaypointEditScreen;
import dev.nikey.optimalwaypoints.screen.WaypointListScreen;
import dev.nikey.optimalwaypoints.waypoint.Waypoint;
import dev.nikey.optimalwaypoints.waypoint.WaypointKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;

public final class OwpClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		singleplayer(ctx);
		String paperDir = System.getenv("OWP_PAPER_DIR");
		if (paperDir != null) {
			paper(ctx, Path.of(paperDir));
		}
	}

	private void singleplayer(ClientGameTestContext ctx) {
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			sp.getServer().runCommand("time set noon");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("fill -2 -60 8 2 -58 8 minecraft:stone");
			sp.getServer().runCommand("setblock 4 -60 5 minecraft:chest");
			sp.getServer().runCommand("tp @a 0.5 -60 0.5 0 1");
			ctx.waitTicks(10);

			ctx.runOnClient(mc -> {
				OwpClient client = OwpClient.get();
				require(client.waypoints().isLoaded(), "waypoints loaded in singleplayer");
				client.waypoints().add(new Waypoint("Base", 8, -58, 40, "minecraft:overworld", 0x5555FF, WaypointKind.STANDARD));
				client.waypoints().add(new Waypoint("Farm", -15, -60, 60, "minecraft:overworld", 0x55FF55, WaypointKind.STANDARD));
				client.waypoints().add(new Waypoint("Death", -4, -60, 20, "minecraft:overworld", 0x000000, WaypointKind.DEATH));
				Waypoint mine = new Waypoint("Mine", 30, -60, 300, "minecraft:overworld", 0xFFAA00, WaypointKind.STANDARD);
				mine.setGlobal(true);
				client.waypoints().add(mine);
				Waypoint chest = new Waypoint("Chest", 4, -60, 5, "minecraft:overworld", 0xFFAA00, WaypointKind.BLOCK);
				chest.addBlock(new BlockPos(4, -60, 5));
				client.waypoints().add(chest);
			});

			ctx.getInput().holdKey(OwpKeybinds.markBlock);
			ctx.waitTicks(3);
			ctx.runOnClient(mc -> mc.player.setXRot(8.5F));
			ctx.waitTicks(3);
			ctx.runOnClient(mc -> mc.player.setXRot(-6.7F));
			ctx.waitTicks(3);
			ctx.getInput().releaseKey(OwpKeybinds.markBlock);
			ctx.waitTicks(2);
			ctx.runOnClient(mc -> {
				Waypoint marker = OwpClient.get().waypoints().blockMarkerAt(new BlockPos(0, -59, 8), "minecraft:overworld");
				require(marker != null, "keybind created a block marker");
				require(marker.blocks().size() == 3, "painting added 3 blocks, got " + marker.blocks().size());
				mc.player.setXRot(1.0F);
			});
			ctx.waitTicks(5);
			ctx.takeScreenshot("owp_01_in_world");

			ctx.runOnClient(mc -> mc.player.setYRot(10.0F));
			ctx.waitTicks(3);
			ctx.takeScreenshot("owp_02_look_base");

			ctx.setScreen(() -> new WaypointListScreen(null));
			ctx.waitTicks(3);
			ctx.takeScreenshot("owp_03_list");
			ctx.runOnClient(mc -> {
				Screen screen = mc.gui.screen();
				double x = screen.width / 2.0 - 190;
				screen.mouseClicked(new MouseButtonEvent(x, 70, new MouseButtonInfo(1, 0)), false);
				require(screen.getFocused() instanceof EditBox, "filter box receives clicks, focused: " + screen.getFocused());
				screen.mouseClicked(new MouseButtonEvent(x, 94, new MouseButtonInfo(1, 0)), false);
				require(screen.getFocused() instanceof CycleButton<?>, "sort button receives clicks, focused: " + screen.getFocused());
			});

			ctx.setScreen(() -> new WaypointEditScreen(null, OwpClient.get().waypoints().current().get(1), false));
			ctx.waitTicks(3);
			ctx.takeScreenshot("owp_04_edit");

			ctx.setScreen(() -> new SettingsScreen(null));
			ctx.waitTicks(3);
			ctx.takeScreenshot("owp_05_settings");

			ctx.runOnClient(mc -> mc.keyboardHandler.setClipboard("owp-waypoint:1:Village:V:120:70:-340:55FF55:overworld"));
			ctx.setScreen(() -> new ImportScreen(null));
			ctx.waitTicks(3);
			ctx.takeScreenshot("owp_05b_import");
			ctx.setScreen(() -> null);

			sp.getServer().runCommand("tellraw @a \"<Steve> owp-waypoint:1:Iron Farm:I:120:64:-300:55FF55:overworld\"");
			String self = ctx.computeOnClient(mc -> mc.player.getGameProfile().name());
			sp.getServer().runCommand("tellraw @a \"<" + self + "> owp-waypoint:1:Spawn:S:0:64:0:FFFF55:overworld\"");
			ctx.runOnClient(mc -> {
				mc.player.connection.sendCommand("owp here Home");
				mc.player.connection.sendCommand("owp status");
			});
			ctx.waitTicks(5);
			ctx.runOnClient(mc -> require(OwpClient.get().waypoints().current().stream().anyMatch(w -> w.name().equals("Home")), "client command added a waypoint"));
			ctx.setScreen(() -> new ChatScreen("", false));
			ctx.waitTicks(3);
			ctx.takeScreenshot("owp_06_chat");
			ctx.setScreen(() -> null);
		}
	}

	private void paper(ClientGameTestContext ctx, Path dir) {
		Path console = dir.resolve("console.fifo");
		Path log = dir.resolve("server.log");
		ctx.runOnClient(mc -> ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString("localhost:25566"),
			new ServerData("Paper", "localhost:25566", ServerData.Type.OTHER), false, null));
		ctx.waitFor(mc -> mc.level != null && mc.player != null, 1200);
		ctx.waitFor(mc -> OwpClient.get().policy().serverManaged(), 400);
		String name = ctx.computeOnClient(mc -> mc.player.getGameProfile().name());
		ctx.waitTicks(20);
		require(read(log).contains(name + " uses Optimal Waypoints"), "Paper plugin detected the mod user");

		ctx.runOnClient(mc -> mc.player.connection.sendChat("owp-waypoint:1:Own Base:B:1:70:3:5555FF:overworld"));
		command(console, "say owp-waypoint:1:Console Farm:F:50:70:50:55FF55:overworld");
		ctx.waitTicks(20);
		ctx.setScreen(() -> new ChatScreen("", false));
		ctx.waitTicks(3);
		ctx.takeScreenshot("owp_06b_paper_share");
		ctx.setScreen(() -> null);
		ctx.runOnClient(mc -> mc.player.connection.sendCommand("owp addshared owp-waypoint:1:Console Farm:F:50:70:50:55FF55:overworld"));
		ctx.waitTicks(5);
		ctx.runOnClient(mc -> require(OwpClient.get().waypoints().current().stream().anyMatch(w -> w.name().equals("Console Farm")),
			"shared waypoint with spaces was added"));

		command(console, "optimalwaypoints policy through_walls false");
		command(console, "optimalwaypoints maxdistance 1000");
		ctx.waitFor(mc -> !OwpClient.get().policy().allows(Feature.THROUGH_WALLS) && OwpClient.get().policy().maxRenderDistance() == 1000, 200);
		require(OwpClient.get().policy().allows(Feature.BLOCK_HIGHLIGHTS), "other features stay allowed");

		command(console, "optimalwaypoints policy mod false");
		ctx.waitFor(mc -> !OwpClient.get().policy().modAllowed(), 200);
		ctx.waitTicks(30);
		ctx.setScreen(() -> new ChatScreen("", false));
		ctx.waitTicks(3);
		ctx.takeScreenshot("owp_07_paper_chat");
		ctx.setScreen(() -> null);

		command(console, "optimalwaypoints policy mod true");
		command(console, "optimalwaypoints policy through_walls true");
		command(console, "optimalwaypoints maxdistance 0");
		ctx.waitFor(mc -> OwpClient.get().policy().current().isUnrestricted(), 200);
		command(console, "optimalwaypoints users");
		ctx.waitTicks(10);
		ctx.runOnClient(mc -> mc.disconnectWithProgressScreen());
		ctx.waitFor(mc -> mc.level == null, 400);
		ctx.setScreen(TitleScreen::new);
		ctx.waitTicks(2);
	}

	private static void command(Path console, String command) {
		try {
			Files.writeString(console, command + "\n", StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new AssertionError("Could not send console command", e);
		}
	}

	private static String read(Path file) {
		try {
			return Files.readString(file);
		} catch (IOException e) {
			return "";
		}
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError("Optimal Waypoints gametest failed: " + message);
		}
	}
}
