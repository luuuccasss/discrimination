package fr.luuuccasss.discrimination;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.village.VillagerType;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * /profil                               ton origine, ta classe sociale et l'avis du villageois le plus proche
 * /profil <joueur>                      celui d'un autre joueur (op)
 * /profil origine <joueur> <biome>      changer l'origine d'un joueur (op) : plains, desert, snow, taiga, savanna, jungle, swamp
 */
public final class ProfileCommand {
    private ProfileCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("profil")
            .executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))
            .then(argument("joueur", EntityArgumentType.player())
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> show(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "joueur"))))
            .then(literal("origine")
                .requires(source -> source.hasPermissionLevel(2))
                .then(argument("joueur", EntityArgumentType.player())
                    .then(argument("biome", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (RegistryKey<VillagerType> type : Origin.all()) builder.suggest(Origin.id(type).replace("minecraft:", ""));
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "biome");
                            RegistryKey<VillagerType> type = Origin.byId(id.contains(":") ? id : "minecraft:" + id);
                            if (type == null) {
                                ctx.getSource().sendError(Text.literal("Biome inconnu : " + id));
                                return 0;
                            }
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "joueur");
                            ((Profiled) target).discrimination$setOrigin(type);
                            return show(ctx.getSource(), target);
                        })))));
    }

    private static int show(ServerCommandSource source, ServerPlayerEntity player) {
        RegistryKey<VillagerType> origin = ((Profiled) player).discrimination$getOrigin();
        SocialClass socialClass = SocialClass.of(player);
        MutableText text = Text.literal("Profil de ").formatted(Formatting.GRAY)
            .append(player.getDisplayName())
            .append(Text.literal("\n  Origine : ").formatted(Formatting.GRAY))
            .append(Text.literal(origin == null ? "pas encore recensé" : Origin.label(origin)).formatted(Formatting.WHITE))
            .append(Text.literal("\n  Classe sociale : ").formatted(Formatting.GRAY))
            .append(Text.literal(socialClass.label + " (" + SocialClass.wealth(player) + " pts)").formatted(socialClass.color));
        VillagerEntity villager = player.getWorld().getClosestEntity(VillagerEntity.class, TargetPredicate.DEFAULT, player,
            player.getX(), player.getY(), player.getZ(), player.getBoundingBox().expand(16));
        if (villager != null && !villager.isBaby()) {
            Prejudice prejudice = Opinion.prejudice(villager);
            Prejudice.Verdict verdict = Opinion.of(villager, player);
            int familiarity = ((Prejudiced) villager).discrimination$getFamiliarity(player.getUuid());
            text.append(Text.literal("\n  Villageois le plus proche : ").formatted(Formatting.GRAY))
                .append(Text.literal(prejudice.label).formatted(prejudice.color))
                .append(Text.literal(", " + Origin.from(villager.getVillagerData().type().getKey().orElse(null))).formatted(Formatting.GRAY))
                .append(Text.literal("\n    → ").formatted(Formatting.GRAY))
                .append(describe(verdict))
                .append(Text.literal("  (habitude " + familiarity + "/" + Opinion.ACCEPTED_AT + ")").formatted(Formatting.DARK_GRAY));
        }
        source.sendFeedback(() -> text, false);
        return socialClass.ordinal();
    }

    private static Text describe(Prejudice.Verdict verdict) {
        if (verdict.refuse()) return Text.literal("refuse de commercer avec toi").formatted(Formatting.RED);
        StringBuilder sb = new StringBuilder();
        if (verdict.malus() > 0) sb.append("prix +").append(Math.round(verdict.malus() * 100)).append(" %");
        else if (verdict.malus() < 0) sb.append("remise de ").append(Math.round(-verdict.malus() * 100)).append(" %");
        if (verdict.lockBest()) sb.append(sb.isEmpty() ? "" : ", ").append("meilleurs échanges réservés");
        if (sb.isEmpty()) return Text.literal("te traite normalement").formatted(Formatting.WHITE);
        return Text.literal(sb.toString()).formatted(verdict.hostile() ? Formatting.RED : Formatting.GREEN);
    }
}
