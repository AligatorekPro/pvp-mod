package dev.alek.pinktools;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Podświetla gracza na chwilę, ale tylko jeśli trafienie naprawdę weszło
 * (serwer wysłał animację obrażeń). Blok tarczą = brak obrażeń = brak podświetlenia.
 */
public class HitHighlight {
    private static final long DURATION_MS = 350;

    private static final Map<Integer, Long> active = new HashMap<>();
    // id -> {wiek w tickach, hurtTime w momencie ataku}
    private static final Map<Integer, int[]> pending = new HashMap<>();

    public static ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity target, EntityHitResult hit) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (world.isClient && Config.get().hitHighlight && player == mc.player
                && target instanceof PlayerEntity other) {
            pending.put(target.getId(), new int[]{0, other.hurtTime});
        }
        return ActionResult.PASS;
    }

    public static void tick(MinecraftClient mc) {
        if (mc.world == null) {
            pending.clear();
            active.clear();
            return;
        }
        long now = System.currentTimeMillis();
        active.values().removeIf(t -> t < now);

        Iterator<Map.Entry<Integer, int[]>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, int[]> en = it.next();
            Entity e = mc.world.getEntityById(en.getKey());
            if (!(e instanceof LivingEntity le)) {
                it.remove();
                continue;
            }
            int[] d = en.getValue();
            if (le.hurtTime > d[1]) {
                active.put(en.getKey(), now + DURATION_MS);
                it.remove();
            } else if (++d[0] > 6) {
                it.remove();
            }
        }
    }

    public static boolean isActive(Entity e) {
        Long t = active.get(e.getId());
        return t != null && t >= System.currentTimeMillis();
    }
}
