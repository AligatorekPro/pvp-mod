package dev.alek.pinktools;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class PinkToolsScreen extends Screen {
    private static final int MAX_SHOWN = 8;

    private TextFieldWidget nickInput;
    private int listTop;

    public PinkToolsScreen() {
        super(Text.literal("PinkTools"));
    }

    @Override
    protected void init() {
        Config c = Config.get();
        int left = width / 2 - 160;
        int y = 30;

        // nicki
        addDrawableChild(toggle(left, y, 120, "Nicki", () -> c.nickHighlight, v -> c.nickHighlight = v));
        addDrawableChild(colorField(left + 128, y, 72, c.nickColor, v -> c.nickColor = v, "Kolor nicków (hex)"));

        // trajektorie
        y += 24;
        addDrawableChild(toggle(left, y, 120, "Trajektorie", () -> c.trajectories, v -> c.trajectories = v));
        addDrawableChild(colorField(left + 128, y, 72, c.ownColor, v -> c.ownColor = v, "Moje trajektorie (hex)"));
        addDrawableChild(colorField(left + 204, y, 72, c.otherColor, v -> c.otherColor = v, "Trajektorie innych (hex)"));

        // trafienie
        y += 24;
        addDrawableChild(toggle(left, y, 120, "Hit highlight", () -> c.hitHighlight, v -> c.hitHighlight = v));
        addDrawableChild(colorField(left + 128, y, 72, c.hitColor, v -> c.hitColor = v, "Kolor trafienia (hex)"));

        // fastplace
        y += 24;
        addDrawableChild(toggle(left, y, 120, "FastPlace", () -> c.fastPlace, v -> c.fastPlace = v));
        addDrawableChild(new CpsSlider(left + 128, y, 148, 20));

        // dodawanie nicków
        y += 32;
        nickInput = new TextFieldWidget(textRenderer, left, y, 190, 20, Text.literal("Nick"));
        nickInput.setMaxLength(16);
        nickInput.setPlaceholder(Text.literal("nick kolegi..."));
        addDrawableChild(nickInput);
        addDrawableChild(ButtonWidget.builder(Text.literal("Dodaj"), b -> addNick())
                .dimensions(left + 198, y, 78, 20).build());

        // lista nicków
        y += 26;
        listTop = y;
        List<String> nicks = c.nicks;
        for (int i = 0; i < Math.min(nicks.size(), MAX_SHOWN); i++) {
            String nick = nicks.get(i);
            addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                        c.nicks.remove(nick);
                        Config.save();
                        clearAndInit();
                    })
                    .dimensions(left + 254, listTop + i * 22, 22, 20).build());
        }
    }

    private void addNick() {
        Config c = Config.get();
        String n = nickInput.getText().trim();
        if (n.matches("[A-Za-z0-9_]{1,16}") && !c.isFriend(n)) {
            c.nicks.add(n);
            Config.save();
            clearAndInit();
        }
    }

    private ButtonWidget toggle(int x, int y, int w, String name, BooleanSupplier get, Consumer<Boolean> set) {
        return ButtonWidget.builder(label(name, get.getAsBoolean()), b -> {
            boolean nv = !get.getAsBoolean();
            set.accept(nv);
            b.setMessage(label(name, nv));
        }).dimensions(x, y, w, 20).build();
    }

    private static Text label(String name, boolean on) {
        return Text.literal(name + ": " + (on ? "ON" : "OFF")).formatted(on ? Formatting.GREEN : Formatting.RED);
    }

    private TextFieldWidget colorField(int x, int y, int w, int color, IntConsumer setter, String tip) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(tip));
        f.setMaxLength(7);
        f.setText(String.format("#%06X", color & 0xFFFFFF));
        f.setEditableColor(0xFF000000 | color);
        f.setTooltip(Tooltip.of(Text.literal(tip)));
        f.setChangedListener(s -> {
            Integer parsed = parseHex(s);
            if (parsed != null) {
                setter.accept(parsed);
                f.setEditableColor(0xFF000000 | parsed);
            }
        });
        return f;
    }

    private static Integer parseHex(String s) {
        String h = s.startsWith("#") ? s.substring(1) : s;
        if (h.length() != 6) return null;
        try {
            return Integer.parseInt(h, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);

        int left = width / 2 - 160;
        List<String> nicks = Config.get().nicks;
        for (int i = 0; i < Math.min(nicks.size(), MAX_SHOWN); i++) {
            ctx.drawTextWithShadow(textRenderer, nicks.get(i), left + 4, listTop + i * 22 + 6, 0xFFFFFFFF);
        }
        if (nicks.size() > MAX_SHOWN) {
            ctx.drawTextWithShadow(textRenderer, "+" + (nicks.size() - MAX_SHOWN) + " więcej (usuń kogoś, żeby zobaczyć)",
                    left + 4, listTop + MAX_SHOWN * 22 + 4, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            close();
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && nickInput != null && nickInput.isFocused()) {
            addNick();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        Config.save();
    }

    private static class CpsSlider extends SliderWidget {
        CpsSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Text.empty(), (Config.get().cps - 1) / 14.0);
            updateMessage();
        }

        private int cps() {
            return 1 + (int) Math.round(value * 14);
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("CPS: " + cps()));
        }

        @Override
        protected void applyValue() {
            Config.get().cps = cps();
        }
    }
}
