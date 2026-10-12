package fr.luuuccasss.discrimination.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.luuuccasss.discrimination.Discrimination;
import fr.luuuccasss.discrimination.Origin;
import fr.luuuccasss.discrimination.Prejudice;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Les préjugés se voient : haut-de-forme et monocle pour les snobs, béret aux couleurs de leur région pour les chauvins. */
public class AccessoriesLayer extends RenderLayer<VillagerRenderState, VillagerModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Discrimination.MOD_ID, "textures/entity/accessoires.png");

    private final ModelPart monocle;
    private final ModelPart topHat;
    private final ModelPart beret;

    public AccessoriesLayer(RenderLayerParent<VillagerRenderState, VillagerModel> parent) {
        super(parent);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition monocleData = root.addOrReplaceChild("monocle", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, -7.5f, -4.3f, 4, 4, 0),
            PartPose.ZERO);
        monocleData.addOrReplaceChild("chain", CubeListBuilder.create().texOffs(48, 0).addBox(-3.6f, -3.5f, -4.25f, 1, 5, 0), PartPose.ZERO);
        PartDefinition hatData = root.addOrReplaceChild("top_hat", CubeListBuilder.create(), PartPose.ZERO);
        hatData.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 20).addBox(-5.5f, -11, -5.5f, 11, 1, 11), PartPose.ZERO);
        hatData.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 32).addBox(-3.5f, -17, -3.5f, 7, 6, 7), PartPose.ZERO);
        PartDefinition beretData = root.addOrReplaceChild("beret", CubeListBuilder.create().texOffs(0, 8).addBox(-4.5f, -11.5f, -4.5f, 9, 2, 9),
            PartPose.rotation(0, 0, 0.12f));
        beretData.addOrReplaceChild("pom", CubeListBuilder.create().texOffs(40, 8).addBox(-0.5f, -12.5f, -0.5f, 1, 1, 1), PartPose.ZERO);
        ModelPart model = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        monocle = model.getChild("monocle");
        topHat = model.getChild("top_hat");
        beret = model.getChild("beret");
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, VillagerRenderState state, float yRot, float xRot) {
        if (state.isInvisible || state.isBaby) return;
        Prejudice prejudice = ((AccessoryRenderState) state).discrimination$getPrejudice();
        if (prejudice != Prejudice.SNOB && prejudice != Prejudice.CHAUVIN) return;
        RenderType type = RenderTypes.entityCutout(TEXTURE);
        int overlay = OverlayTexture.NO_OVERLAY;

        poseStack.pushPose();
        getParentModel().getHead().translateAndRotate(poseStack);
        if (prejudice == Prejudice.SNOB) {
            collector.submitModelPart(monocle, poseStack, type, light, overlay, null);
            collector.submitModelPart(topHat, poseStack, type, light, overlay, null);
        } else {
            int color = 0xFF000000 | Origin.color(state.villagerData.type().unwrapKey().orElse(null));
            collector.submitModelPart(beret, poseStack, type, light, overlay, null, color);
        }
        poseStack.popPose();
    }
}
