package fr.luuuccasss.discrimination.client;

import fr.luuuccasss.discrimination.Discrimination;
import fr.luuuccasss.discrimination.Origin;
import fr.luuuccasss.discrimination.Prejudice;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Identifier;

/** Les préjugés se voient : haut-de-forme et monocle pour les snobs, béret aux couleurs de leur région pour les chauvins. */
public class AccessoriesFeatureRenderer extends FeatureRenderer<VillagerEntity, VillagerResemblingModel<VillagerEntity>> {
    private static final Identifier TEXTURE = new Identifier(Discrimination.MOD_ID, "textures/entity/accessoires.png");

    private final ModelPart monocle;
    private final ModelPart topHat;
    private final ModelPart beret;

    public AccessoriesFeatureRenderer(FeatureRendererContext<VillagerEntity, VillagerResemblingModel<VillagerEntity>> context) {
        super(context);
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartData monocleData = root.addChild("monocle", ModelPartBuilder.create().uv(0, 0).cuboid(-3.5f, -7.5f, -4.3f, 4, 4, 0),
            ModelTransform.NONE);
        monocleData.addChild("chain", ModelPartBuilder.create().uv(48, 0).cuboid(-3.6f, -3.5f, -4.25f, 1, 5, 0), ModelTransform.NONE);
        ModelPartData hatData = root.addChild("top_hat", ModelPartBuilder.create(), ModelTransform.NONE);
        hatData.addChild("brim", ModelPartBuilder.create().uv(0, 20).cuboid(-5.5f, -11, -5.5f, 11, 1, 11), ModelTransform.NONE);
        hatData.addChild("crown", ModelPartBuilder.create().uv(0, 32).cuboid(-3.5f, -17, -3.5f, 7, 6, 7), ModelTransform.NONE);
        ModelPartData beretData = root.addChild("beret", ModelPartBuilder.create().uv(0, 8).cuboid(-4.5f, -11.5f, -4.5f, 9, 2, 9),
            ModelTransform.of(0, 0, 0, 0, 0, 0.12f));
        beretData.addChild("pom", ModelPartBuilder.create().uv(40, 8).cuboid(-0.5f, -12.5f, -0.5f, 1, 1, 1), ModelTransform.NONE);
        ModelPart model = TexturedModelData.of(data, 64, 64).createModel();
        monocle = model.getChild("monocle");
        topHat = model.getChild("top_hat");
        beret = model.getChild("beret");
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, VillagerEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (entity.isInvisible() || entity.isBaby()) return;
        Prejudice prejudice = PrejudiceCache.get(entity.getId());
        if (prejudice != Prejudice.SNOB && prejudice != Prejudice.CHAUVIN) return;
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        int overlay = OverlayTexture.DEFAULT_UV;

        matrices.push();
        getContextModel().getHead().rotate(matrices);
        if (prejudice == Prejudice.SNOB) {
            monocle.render(matrices, consumer, light, overlay);
            topHat.render(matrices, consumer, light, overlay);
        } else {
            int color = Origin.color(entity.getVillagerData().getType());
            beret.render(matrices, consumer, light, overlay,
                ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, 1f);
        }
        matrices.pop();
    }
}
