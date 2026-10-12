package fr.luuuccasss.discrimination;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerType;

/**
 * /profil                               ton origine, ta classe sociale et l'avis du villageois le plus proche
 * /profil <joueur>                      celui d'un autre joueur (op)
 * /profil origine <joueur> <biome>      changer l'origine d'un joueur (op) : plains, desert, snow, taiga, savanna, jungle, swamp
 */
public final class ProfileCommand {
    private ProfileCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("profil")
            .executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrException()))
            .then(argument("joueur", EntityArgument.player())
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayer(ctx, "joueur"))))
            .then(literal("origine")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(argument("joueur", EntityArgument.player())
                    .then(argument("biome", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (ResourceKey<VillagerType> type : Origin.all()) builder.suggest(Origin.id(type).replace("minecraft:", ""));
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "biome");
                            ResourceKey<VillagerType> type = Origin.byId(id.contains(":") ? id : "minecraft:" + id);
                            if (type == null) {
                                ctx.getSource().sendFailure(Component.literal("Biome inconnu : " + id));
                                return 0;
                            }
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "joueur");
                            ((Profiled) target).discrimination$setOrigin(type);
                            return show(ctx.getSource(), target);
                        })))));
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        ResourceKey<VillagerType> origin = ((Profiled) player).discrimination$getOrigin();
        SocialClass socialClass = SocialClass.of(player);
        MutableComponent text = Component.literal("Profil de ").withStyle(ChatFormatting.GRAY)
            .append(player.getDisplayName())
            .append(Component.literal("\n  Origine : ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(origin == null ? "pas encore recensé" : Origin.label(origin)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n  Classe sociale : ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(socialClass.label + " (" + SocialClass.wealth(player) + " pts)").withStyle(socialClass.color));
        Villager villager = Gossip.closestVillager(player.level(), player, 16);
        if (villager != null) {
            Prejudice prejudice = Opinion.prejudice(villager);
            Prejudice.Verdict verdict = Opinion.of(villager, player);
            int familiarity = ((Prejudiced) villager).discrimination$getFamiliarity(player.getUUID());
            text.append(Component.literal("\n  Villageois le plus proche : ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(prejudice.label).withStyle(prejudice.color))
                .append(Component.literal(", " + Origin.from(villager.getVillagerData().type().unwrapKey().orElse(null)))
                    .withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n    → ").withStyle(ChatFormatting.GRAY))
                .append(describe(verdict))
                .append(Component.literal("  (habitude " + familiarity + "/" + Opinion.ACCEPTED_AT + ")").withStyle(ChatFormatting.DARK_GRAY));
        }
        source.sendSuccess(() -> text, false);
        return socialClass.ordinal();
    }

    private static Component describe(Prejudice.Verdict verdict) {
        if (verdict.refuse()) return Component.literal("refuse de commercer avec toi").withStyle(ChatFormatting.RED);
        StringBuilder sb = new StringBuilder();
        if (verdict.malus() > 0) sb.append("prix +").append(Math.round(verdict.malus() * 100)).append(" %");
        else if (verdict.malus() < 0) sb.append("remise de ").append(Math.round(-verdict.malus() * 100)).append(" %");
        if (verdict.lockBest()) sb.append(sb.isEmpty() ? "" : ", ").append("meilleurs échanges réservés");
        if (sb.isEmpty()) return Component.literal("te traite normalement").withStyle(ChatFormatting.WHITE);
        return Component.literal(sb.toString()).withStyle(verdict.hostile() ? ChatFormatting.RED : ChatFormatting.GREEN);
    }
}
