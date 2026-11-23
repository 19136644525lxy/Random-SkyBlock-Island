package yifei.rsl.network;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import yifei.rsl.client.ClientGameplayActionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge server-authoritative action channel for the gameplay menu. The client
 * sends an action id, the server runs the matching command while capturing the
 * feedback lines, and sends them back so the menu can render the result inline.
 */
public final class GameplayActionNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("rsl", "gameplay_action"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_RESET = "reset";
    public static final String ACTION_WAND = "wand";
    public static final String ACTION_AREAS = "areas";
    public static final String ACTION_STATUS = "status";
    public static final String ACTION_DELAY = "delay";

    private GameplayActionNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, ActionRequestC2S.class, ActionRequestC2S::encode, ActionRequestC2S::decode,
                (message, supplier) -> {
                    NetworkEvent.Context context = supplier.get();
                    context.enqueueWork(() -> execute(context.getSender(), message.action, message.delay));
                    context.setPacketHandled(true);
                });
        CHANNEL.registerMessage(1, ActionResultS2C.class, ActionResultS2C::encode, ActionResultS2C::decode,
                (message, supplier) -> {
                    NetworkEvent.Context context = supplier.get();
                    context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientGameplayActionHandler.result(message.lines)));
                    context.setPacketHandled(true);
                });
    }

    public static void sendRequest(String action, int delay) {
        CHANNEL.sendToServer(new ActionRequestC2S(action, delay));
    }

    private static void sendResult(ServerPlayer player, List<String> lines) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ActionResultS2C(lines));
    }

    private static void execute(ServerPlayer player, String action, int delay) {
        if (player == null) {
            return;
        }
        String command = commandFor(action, delay);
        if (command == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        CommandSource output = new CommandSource() {
            @Override
            public void sendSystemMessage(Component component) {
                lines.add(component);
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        CommandSourceStack source = player.createCommandSourceStack().withSource(output);
        server.getCommands().performPrefixedCommand(source, command);
        if (lines.isEmpty()) {
            lines.add(Component.translatable("rsl.menu.action_done"));
        }
        List<String> json = new ArrayList<>(lines.size());
        for (Component line : lines) {
            json.add(Component.Serializer.toJson(line));
        }
        sendResult(player, json);
    }

    private static String commandFor(String action, int delay) {
        return switch (action) {
            case ACTION_START -> "randomskyblock start";
            case ACTION_STOP -> "randomskyblock stop";
            case ACTION_RESET -> "randomskyblock reset";
            case ACTION_WAND -> "randomskyblock wand";
            case ACTION_AREAS -> "randomskyblock areas";
            case ACTION_STATUS -> "randomskyblock status";
            case ACTION_DELAY -> delay >= 1 && delay <= 3600 ? "randomskyblock delay " + delay : null;
            default -> null;
        };
    }

    public static final class ActionRequestC2S {
        private final String action;
        private final int delay;

        public ActionRequestC2S(String action, int delay) {
            this.action = action;
            this.delay = delay;
        }

        public static void encode(ActionRequestC2S message, FriendlyByteBuf buf) {
            buf.writeUtf(message.action, 32);
            buf.writeVarInt(message.delay);
        }

        public static ActionRequestC2S decode(FriendlyByteBuf buf) {
            return new ActionRequestC2S(buf.readUtf(32), buf.readVarInt());
        }
    }

    public static final class ActionResultS2C {
        private final List<String> lines;

        public ActionResultS2C(List<String> lines) {
            this.lines = lines;
        }

        public static void encode(ActionResultS2C message, FriendlyByteBuf buf) {
            buf.writeVarInt(message.lines.size());
            for (String line : message.lines) {
                buf.writeUtf(line, 262144);
            }
        }

        public static ActionResultS2C decode(FriendlyByteBuf buf) {
            int count = buf.readVarInt();
            List<String> lines = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                lines.add(buf.readUtf(262144));
            }
            return new ActionResultS2C(lines);
        }
    }
}
