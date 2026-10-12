package fr.luuuccasss.discrimination.mixin;

import fr.luuuccasss.discrimination.Opinion;
import fr.luuuccasss.discrimination.Prejudice;
import fr.luuuccasss.discrimination.Prejudiced;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin implements Prejudiced {
    /** Un échange « réservé » coûte le maximum : une pile entière. */
    @Unique private static final int LOCKED_PRICE = 64;

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

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void discrimination$write(NbtCompound nbt, CallbackInfo ci) {
        if (discrimination$prejudice != null) nbt.putString("DiscriminationPrejudice", discrimination$prejudice.name());
        NbtCompound familiarity = new NbtCompound();
        discrimination$familiarity.forEach((uuid, value) -> familiarity.putInt(uuid.toString(), value));
        nbt.put("DiscriminationFamiliarity", familiarity);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void discrimination$read(NbtCompound nbt, CallbackInfo ci) {
        try {
            if (nbt.contains("DiscriminationPrejudice")) discrimination$prejudice = Prejudice.valueOf(nbt.getString("DiscriminationPrejudice"));
        } catch (IllegalArgumentException ignored) {
            discrimination$prejudice = null;
        }
        NbtCompound familiarity = nbt.getCompound("DiscriminationFamiliarity");
        for (String key : familiarity.getKeys()) {
            try {
                discrimination$familiarity.put(UUID.fromString(key), familiarity.getInt(key));
            } catch (IllegalArgumentException ignored) {
                // clé invalide, on l'oublie
            }
        }
    }

    /**
     * Accroupi avec une fleur : un cadeau, qui compte double pour l'habitude.
     * Sinon, un villageois qui refuse te claque la porte au nez, et les autres te disent ce qu'ils pensent.
     */
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void discrimination$interact(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || player.isSpectator()) return;
        VillagerEntity self = (VillagerEntity) (Object) this;
        if (self.isBaby() || !self.isAlive()) return;
        ServerWorld world = (ServerWorld) self.getWorld();
        ItemStack held = player.getStackInHand(hand);
        if (player.isSneaking() && held.isIn(ItemTags.FLOWERS)) {
            int before = discrimination$getFamiliarity(player.getUuid());
            discrimination$setFamiliarity(player.getUuid(), before + 2);
            if (!player.isCreative()) held.decrement(1);
            world.spawnParticles(ParticleTypes.HEART, self.getX(), self.getEyeY() + 0.5, self.getZ(), 2, 0.3, 0.2, 0.3, 0);
            world.playSound(null, self.getBlockPos(), SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.NEUTRAL, 1f, 1.1f);
            acceptedMessage(serverPlayer, before);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }
        Prejudice prejudice = Opinion.prejudice(self);
        Prejudice.Verdict verdict = Opinion.of(self, serverPlayer);
        if (verdict.refuse()) {
            self.setHeadRollingTimeLeft(40);
            world.playSound(null, self.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 1f, 0.8f);
            world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, self.getX(), self.getEyeY() + 0.4, self.getZ(), 1, 0, 0, 0, 0);
            player.sendMessage(remark(prejudice, verdict.remark()), true);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }
        if (verdict.remark() != null) player.sendMessage(remark(prejudice, verdict.remark()), true);
    }

    @Unique
    private static Text remark(Prejudice prejudice, String remark) {
        return Text.literal("[" + prejudice.label + "] ").formatted(prejudice.color)
            .append(Text.literal(remark).formatted(Formatting.WHITE));
    }

    @Unique
    private void acceptedMessage(ServerPlayerEntity player, int before) {
        if (before < Opinion.ACCEPTED_AT && discrimination$getFamiliarity(player.getUuid()) >= Opinion.ACCEPTED_AT
            && discrimination$prejudice != Prejudice.TOLERANT) {
            player.sendMessage(Text.literal("« Finalement, t'es pas si mal. »").formatted(Formatting.GREEN), true);
        }
    }

    /** Prix selon la tête du client, et les meilleurs échanges « réservés ». */
    @Inject(method = "prepareOffersFor", at = @At("TAIL"))
    private void discrimination$prices(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        VillagerEntity self = (VillagerEntity) (Object) this;
        Prejudice.Verdict verdict = Opinion.of(self, serverPlayer);
        TradeOfferList offers = self.getOffers();
        for (int i = 0; i < offers.size(); i++) {
            TradeOffer offer = offers.get(i);
            int base = offer.getOriginalFirstBuyItem().getCount();
            if (verdict.lockBest() && i >= offers.size() / 2) {
                offer.increaseSpecialPrice(LOCKED_PRICE);
            } else if (verdict.malus() > 0) {
                offer.increaseSpecialPrice(Math.max(1, (int) Math.ceil(base * verdict.malus())));
            } else if (verdict.malus() < 0) {
                offer.increaseSpecialPrice(Math.min(-1, (int) Math.floor(base * verdict.malus())));
            }
        }
    }

    /** Chaque échange fait passer un peu le préjugé. */
    @Inject(method = "afterUsing", at = @At("HEAD"))
    private void discrimination$habit(TradeOffer offer, CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        if (!(self.getCustomer() instanceof ServerPlayerEntity player)) return;
        int before = discrimination$getFamiliarity(player.getUuid());
        discrimination$setFamiliarity(player.getUuid(), before + 1);
        acceptedMessage(player, before);
    }
}
