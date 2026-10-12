package fr.luuuccasss.discrimination.mixin;

import com.mojang.serialization.Codec;
import fr.luuuccasss.discrimination.Opinion;
import fr.luuuccasss.discrimination.Prejudice;
import fr.luuuccasss.discrimination.Prejudiced;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class VillagerMixin implements Prejudiced {
    /** Un échange « réservé » coûte le maximum : une pile entière. */
    @Unique private static final int LOCKED_PRICE = 64;
    /** Habitude par joueur, clé = UUID en texte. */
    @Unique private static final Codec<Map<String, Integer>> FAMILIARITY_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    @Unique private Prejudice discrimination$prejudice;
    @Unique private final Map<UUID, Integer> discrimination$familiarity = new HashMap<>();

    @Override
    public Prejudice discrimination$getPrejudice() {
        return discrimination$prejudice;
    }

    @Override
    public void discrimination$setPrejudice(Prejudice prejudice) {
        discrimination$prejudice = prejudice;
    }

    @Override
    public int discrimination$getFamiliarity(UUID player) {
        return discrimination$familiarity.getOrDefault(player, 0);
    }

    @Override
    public void discrimination$setFamiliarity(UUID player, int value) {
        discrimination$familiarity.put(player, Math.max(0, Math.min(value, Opinion.ACCEPTED_AT)));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void discrimination$write(ValueOutput output, CallbackInfo ci) {
        if (discrimination$prejudice != null) output.putString("DiscriminationPrejudice", discrimination$prejudice.name());
        Map<String, Integer> familiarity = new HashMap<>();
        discrimination$familiarity.forEach((uuid, value) -> familiarity.put(uuid.toString(), value));
        output.store("DiscriminationFamiliarity", FAMILIARITY_CODEC, familiarity);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void discrimination$read(ValueInput input, CallbackInfo ci) {
        try {
            input.getString("DiscriminationPrejudice").ifPresent(name -> discrimination$prejudice = Prejudice.valueOf(name));
        } catch (IllegalArgumentException ignored) {
            discrimination$prejudice = null;
        }
        input.read("DiscriminationFamiliarity", FAMILIARITY_CODEC).ifPresent(familiarity -> familiarity.forEach((key, value) -> {
            try {
                discrimination$familiarity.put(UUID.fromString(key), value);
            } catch (IllegalArgumentException ignored) {
                // clé invalide, on l'oublie
            }
        }));
    }

    @Unique
    private static boolean discrimination$isFlower(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().is(BlockTags.SMALL_FLOWERS);
    }

    /**
     * Accroupi avec une fleur : un cadeau, qui compte double pour l'habitude.
     * Sinon, un villageois qui refuse te claque la porte au nez, et les autres te disent ce qu'ils pensent.
     */
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void discrimination$interact(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.isSpectator()) return;
        Villager self = (Villager) (Object) this;
        if (self.isBaby() || !self.isAlive()) return;
        ServerLevel level = serverPlayer.level();
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && discrimination$isFlower(held)) {
            int before = discrimination$getFamiliarity(player.getUUID());
            discrimination$setFamiliarity(player.getUUID(), before + 2);
            if (!player.isCreative()) held.shrink(1);
            level.sendParticles(ParticleTypes.HEART, self.getX(), self.getEyeY() + 0.5, self.getZ(), 2, 0.3, 0.2, 0.3, 0);
            level.playSound(null, self.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1f, 1.1f);
            discrimination$acceptedMessage(serverPlayer, before);
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        Prejudice prejudice = Opinion.prejudice(self);
        Prejudice.Verdict verdict = Opinion.of(self, serverPlayer);
        if (verdict.refuse()) {
            self.setUnhappyCounter(40);
            level.playSound(null, self.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1f, 0.8f);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, self.getX(), self.getEyeY() + 0.4, self.getZ(), 1, 0, 0, 0, 0);
            serverPlayer.sendSystemMessage(discrimination$remark(prejudice, verdict.remark()), true);
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        if (verdict.remark() != null) serverPlayer.sendSystemMessage(discrimination$remark(prejudice, verdict.remark()), true);
    }

    @Unique
    private static Component discrimination$remark(Prejudice prejudice, String remark) {
        return Component.literal("[" + prejudice.label + "] ").withStyle(prejudice.color)
            .append(Component.literal(remark).withStyle(ChatFormatting.WHITE));
    }

    @Unique
    private void discrimination$acceptedMessage(ServerPlayer player, int before) {
        if (before < Opinion.ACCEPTED_AT && discrimination$getFamiliarity(player.getUUID()) >= Opinion.ACCEPTED_AT
            && discrimination$prejudice != Prejudice.TOLERANT) {
            player.sendSystemMessage(Component.literal("« Finalement, t'es pas si mal. »").withStyle(ChatFormatting.GREEN), true);
        }
    }

    /** Prix selon la tête du client, et les meilleurs échanges « réservés ». */
    @Inject(method = "updateSpecialPrices", at = @At("TAIL"))
    private void discrimination$prices(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        Villager self = (Villager) (Object) this;
        Prejudice.Verdict verdict = Opinion.of(self, serverPlayer);
        MerchantOffers offers = self.getOffers();
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            int base = offer.getBaseCostA().getCount();
            if (verdict.lockBest() && i >= offers.size() / 2) {
                offer.addToSpecialPriceDiff(LOCKED_PRICE);
            } else if (verdict.malus() > 0) {
                offer.addToSpecialPriceDiff(Math.max(1, (int) Math.ceil(base * verdict.malus())));
            } else if (verdict.malus() < 0) {
                offer.addToSpecialPriceDiff(Math.min(-1, (int) Math.floor(base * verdict.malus())));
            }
        }
    }

    /** Chaque échange fait passer un peu le préjugé. */
    @Inject(method = "rewardTradeXp", at = @At("HEAD"))
    private void discrimination$habit(MerchantOffer offer, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        if (!(self.getTradingPlayer() instanceof ServerPlayer player)) return;
        int before = discrimination$getFamiliarity(player.getUUID());
        discrimination$setFamiliarity(player.getUUID(), before + 1);
        discrimination$acceptedMessage(player, before);
    }
}
