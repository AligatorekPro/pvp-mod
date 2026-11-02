package dev.alek.pinktools;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ThrownItemEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrajectoryRenderer {
    private record Physics(double gravity, double drag, double waterDrag, int maxSteps) {}
    private record Label(Vec3d pos, String text, int color) {}

    private static Physics physics(Entity e) {
        if (e instanceof PersistentProjectileEntity) return new Physics(0.05, 0.99, 0.6, 300);   // strzały, trident
        if (e instanceof AbstractWindChargeEntity) return new Physics(0.0, 1.0, 1.0, 120);       // wind charge
        if (e instanceof ExperienceBottleEntity) return new Physics(0.07, 0.99, 0.8, 300);
        if (e instanceof ThrownItemEntity t) {                                                    // perły, śnieżki, jajka, mikstury
            ItemStack s = t.getStack();
            if (s.getItem() instanceof PotionItem) return new Physics(0.05, 0.99, 0.8, 300);
            return new Physics(0.03, 0.99, 0.8, 300);
        }
        return null;
    }

    public static void render(WorldRenderContext ctx) {
        Config cfg = Config.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (!cfg.trajectories || world == null || mc.player == null || ctx.matrixStack() == null) return;

        float delta = ctx.tickCounter().getTickDelta(false);
        Vec3d cam = ctx.camera().getPos();
        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider.Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer lines = imm.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = ms.peek();
        List<Label> labels = new ArrayList<>();

        for (Entity e : world.getEntities()) {
            if (!(e instanceof ProjectileEntity proj)) continue;
            Physics p = physics(e);
            if (p == null) continue;

            Vec3d vel = e.getVelocity();
            if (vel.lengthSquared() < 0.02) continue; // wbita strzała / stoi

            Entity owner = proj.getOwner();
            boolean mine = owner == mc.player;
            int color = mine ? cfg.ownColor : cfg.otherColor;

            Vec3d start = e.getLerpedPos(delta);
            List<Vec3d> pts = new ArrayList<>();
            pts.add(start);
            Vec3d pos = start;
            Vec3d v = vel;
            BlockHitResult hit = null;
            int ticks = 0;

            for (int i = 0; i < p.maxSteps(); i++) {
                Vec3d next = pos.add(v);
                BlockHitResult h = world.raycast(new RaycastContext(
                        pos, next,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        e));
                ticks = i + 1;
                if (h.getType() != HitResult.Type.MISS) {
                    pts.add(h.getPos());
                    hit = h;
                    break;
                }
                pts.add(next);
                pos = next;
                boolean water = world.getFluidState(BlockPos.ofFloored(pos)).isIn(FluidTags.WATER);
                v = v.multiply(water ? p.waterDrag() : p.drag()).subtract(0, p.gravity(), 0);
                if (pos.y < world.getBottomY() - 20) break;
            }

            for (int i = 0; i < pts.size() - 1; i++) {
                line(lines, entry, pts.get(i).subtract(cam), pts.get(i + 1).subtract(cam), color);
            }

            if (hit != null) {
                Box b = new Box(hit.getBlockPos()).expand(0.003).offset(-cam.x, -cam.y, -cam.z);
                box(lines, entry, b, color);

                String time = String.format(Locale.ROOT, "%.1fs", ticks / 20.0);
                String text = (mine || owner == null) ? time : owner.getName().getString() + " " + time;
                labels.add(new Label(hit.getPos(), text, color));
            }
        }

        imm.draw(RenderLayer.getLines());

        TextRenderer tr = mc.textRenderer;
        for (Label l : labels) {
            ms.push();
            ms.translate(l.pos().x - cam.x, l.pos().y - cam.y + 0.6, l.pos().z - cam.z);
            ms.multiply(ctx.camera().getRotation());
            ms.scale(0.03f, -0.03f, 0.03f);
            float x = -tr.getWidth(l.text()) / 2f;
            tr.draw(l.text(), x, 0, 0xFF000000 | l.color(), false,
                    ms.peek().getPositionMatrix(), imm,
                    TextRenderer.TextLayerType.SEE_THROUGH, 0x66000000, 0xF000F0);
            ms.pop();
        }
        imm.draw();
    }

    private static void line(VertexConsumer vc, MatrixStack.Entry e, Vec3d a, Vec3d b, int color) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, bl = color & 0xFF;
        Vec3d n = b.subtract(a);
        double len = n.length();
        if (len < 1.0E-6) return;
        n = n.multiply(1.0 / len);
        vc.vertex(e, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, 255).normal(e, (float) n.x, (float) n.y, (float) n.z);
        vc.vertex(e, (float) b.x, (float) b.y, (float) b.z).color(r, g, bl, 255).normal(e, (float) n.x, (float) n.y, (float) n.z);
    }

    private static void box(VertexConsumer vc, MatrixStack.Entry e, Box b, int color) {
        Vec3d[] c = {
                new Vec3d(b.minX, b.minY, b.minZ), new Vec3d(b.maxX, b.minY, b.minZ),
                new Vec3d(b.maxX, b.minY, b.maxZ), new Vec3d(b.minX, b.minY, b.maxZ),
                new Vec3d(b.minX, b.maxY, b.minZ), new Vec3d(b.maxX, b.maxY, b.minZ),
                new Vec3d(b.maxX, b.maxY, b.maxZ), new Vec3d(b.minX, b.maxY, b.maxZ)
        };
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        for (int[] ed : edges) line(vc, e, c[ed[0]], c[ed[1]], color);
    }
}
