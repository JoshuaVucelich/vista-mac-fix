package com.vucelich.vistamacfix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.minecraft.client.Screenshot;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev-only client sequence. {@code -Dvista_mac_fix.ship_test=true} creates a
 * creative world, assembles a Sable sub-level around a powered Vista television,
 * and writes a screenshot once that screen has been drawn.
 */
public final class ShipTvTest {
    public static final String PROPERTY = "vista_mac_fix.ship_test";
    private static final Logger LOGGER = LoggerFactory.getLogger("vista_mac_fix");

    private static int ticks;
    private static boolean worldRequested;
    private static boolean built;
    private static boolean assembled;
    private static boolean finished;
    private static int anchorX;
    private static int anchorY;
    private static int anchorZ;

    private ShipTvTest() {
    }

    public static boolean enabled() {
        return Boolean.parseBoolean(System.getProperty(PROPERTY, "false"));
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!enabled() || finished) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        ticks++;
        try {
            if (!worldRequested) {
                if (ticks < 80 || minecraft.screen == null) {
                    return;
                }
                String screen = minecraft.screen.getClass().getName();
                if (screen.contains("Loading") || screen.contains("Overlay")) {
                    return;
                }
                LOGGER.info("Vista Mac Fix: ship test screen {}", screen);
                worldRequested = true;
                String name = "vista-tv-ship-" + System.currentTimeMillis();
                LOGGER.info("Vista Mac Fix: ship test creating world {}", name);
                minecraft.createWorldOpenFlows().createFreshLevel(
                        name,
                        new LevelSettings(
                                name,
                                GameType.CREATIVE,
                                false,
                                Difficulty.PEACEFUL,
                                true,
                                new GameRules(),
                                WorldDataConfiguration.DEFAULT
                        ),
                        WorldOptions.defaultWithRandomSeed().withStructures(false),
                        WorldPresets::createNormalWorldDimensions,
                        minecraft.screen
                );
                return;
            }
            if (minecraft.player == null || minecraft.level == null) {
                return;
            }
            if (!built) {
                if (minecraft.player.tickCount < 80) {
                    return;
                }
                anchorX = minecraft.player.blockPosition().getX() + 6;
                anchorY = 120;
                anchorZ = minecraft.player.blockPosition().getZ();
                command(minecraft, "time set day");
                // Eye height lines up with the screen, a few blocks west of the west-facing television.
                command(minecraft, "tp @s " + (anchorX - 2) + " " + anchorY + " " + anchorZ);
                command(minecraft, "effect give @s minecraft:night_vision 1000 0 true");
                command(minecraft, "setblock " + anchorX + " " + anchorY + " " + anchorZ + " minecraft:stone");
                command(minecraft, "setblock " + anchorX + " " + (anchorY + 1) + " " + anchorZ + " minecraft:redstone_block");
                command(
                        minecraft,
                        "setblock " + (anchorX + 1) + " " + (anchorY + 1) + " " + anchorZ
                                + " vista:television[facing=west,powered=direct,connection=single]"
                );
                built = true;
                LOGGER.info("Vista Mac Fix: ship test placed a television at {} {} {}", anchorX + 1, anchorY + 1, anchorZ);
                return;
            }
            if (!assembled) {
                if (minecraft.player.tickCount < 120) {
                    return;
                }
                // Argument names in the tree are "from" and "to". The command itself is
                // two block positions, and assemble hangs off "sable", not "sable sub_level".
                command(
                        minecraft,
                        "sable assemble area " + anchorX + " " + anchorY + " " + anchorZ
                                + " " + (anchorX + 1) + " " + (anchorY + 1) + " " + anchorZ
                );
                assembled = true;
                LOGGER.info("Vista Mac Fix: ship test assembled a Sable sub-level");
                return;
            }
            minecraft.player.setYRot(-90.0F);
            minecraft.player.setXRot(0.0F);
            if (ContraptionTvScreen.draws() == 0 && minecraft.player.tickCount < 400) {
                return;
            }
            if (ContraptionTvScreen.draws() > 0 && minecraft.player.tickCount < 220) {
                return;
            }
            finished = true;
            int draws = ContraptionTvScreen.draws();
            boolean vistafix = vistafixPresent();
            LOGGER.info("Vista Mac Fix: SHIP_TV_TEST draws={} vistafix={}", draws, vistafix);
            String shot = vistafix ? "vista-tv-ship-with-vistafix.png" : "vista-tv-ship-without-vistafix.png";
            Screenshot.grab(minecraft.gameDirectory, shot, minecraft.getMainRenderTarget(), message -> {
                LOGGER.info("Vista Mac Fix: SHIP_TV_SCREENSHOT {}", message.getString());
                minecraft.execute(minecraft::stop);
            });
        } catch (Throwable error) {
            finished = true;
            LOGGER.warn("Vista Mac Fix: ship test failed", error);
            minecraft.stop();
        }
    }

    private static boolean vistafixPresent() {
        try {
            return ModList.get().isLoaded("vistafix");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void command(Minecraft minecraft, String command) {
        LOGGER.info("Vista Mac Fix: ship test /{}", command);
        minecraft.player.connection.sendCommand(command);
    }
}
