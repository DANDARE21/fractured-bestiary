package net.dandare21.fracturedutils.client.gui;

import net.dandare21.fracturedutils.sound.sequence.MusicSequenceEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

import java.util.List;

public class MusicSequenceListWidget extends ObjectSelectionList<MusicSequenceListWidget.MusicEntryRow> {
    private final MusicSequenceScreen screen;
    private final int topPos;
    private final int bottomPos;

    private int draggingIndex = -1;
    private double dragMouseX = 0;
    private double dragMouseY = 0;
    private int targetDropIndex = -1;

    public MusicSequenceListWidget(MusicSequenceScreen screen, Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraft, width, height, top, bottom, itemHeight);
        this.screen = screen;
        this.topPos = top;
        this.bottomPos = bottom;
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
    }

    public void updateEntries(List<MusicSequenceEntry> entries) {
        this.clearEntries();
        for (int i = 0; i < entries.size(); i++) {
            this.addEntry(new MusicEntryRow(screen, entries.get(i), i, entries.size()));
        }
    }

    public int getTopPos() {
        return topPos;
    }

    public int getItemHeight() {
        return itemHeight;
    }

    public int getDraggingIndex() {
        return draggingIndex;
    }

    @Override
    public int getRowWidth() {
        return this.width - 24;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getRowLeft() + this.getRowWidth() + 6;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int rowLeft = getRowLeft();
            int rowWidth = getRowWidth();
            int listTop = this.topPos;
            int listBottom = this.bottomPos;

            if (mouseX >= rowLeft && mouseX <= rowLeft + rowWidth && mouseY >= listTop && mouseY <= listBottom) {
                int relativeY = (int) (mouseY - listTop + this.getScrollAmount());
                int index = relativeY / this.itemHeight;

                if (index >= 0 && index < this.children().size()) {
                    MusicEntryRow entry = this.children().get(index);
                    if (entry.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                    this.draggingIndex = index;
                    this.dragMouseX = mouseX;
                    this.dragMouseY = mouseY;
                    this.targetDropIndex = index;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingIndex != -1) {
            this.dragMouseX = mouseX;
            this.dragMouseY = mouseY;

            int listTop = this.topPos;
            int relativeY = (int) (mouseY - listTop + this.getScrollAmount());
            int row = (relativeY + this.itemHeight / 2) / this.itemHeight;
            this.targetDropIndex = Math.max(0, Math.min(this.children().size(), row));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingIndex != -1) {
            if (targetDropIndex != -1 && targetDropIndex != draggingIndex && targetDropIndex != draggingIndex + 1) {
                int toIndex = targetDropIndex;
                if (toIndex > draggingIndex) {
                    toIndex--;
                }
                screen.moveEntry(draggingIndex, toIndex);
            }
            draggingIndex = -1;
            targetDropIndex = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (draggingIndex != -1 && targetDropIndex != -1) {
            int rowLeft = getRowLeft();
            int rowWidth = getRowWidth();
            int lineY = this.topPos - (int) getScrollAmount() + (targetDropIndex * this.itemHeight);

            if (lineY >= this.topPos && lineY <= this.bottomPos) {
                guiGraphics.fill(rowLeft, lineY - 1, rowLeft + rowWidth, lineY + 1, 0xFF00E5FF);
            }

            MusicEntryRow dragged = this.children().get(draggingIndex);
            int floatX = (int) dragMouseX - 100;
            int floatY = (int) dragMouseY - (this.itemHeight / 2);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 300);
            dragged.renderFloating(guiGraphics, floatX, floatY, rowWidth, this.itemHeight);
            guiGraphics.pose().popPose();
        }
    }

    public static class MusicEntryRow extends ObjectSelectionList.Entry<MusicEntryRow> {
        private final MusicSequenceScreen screen;
        private final MusicSequenceEntry entry;
        private final int index;
        private final int totalCount;

        private final CyberpunkButton editBtn;
        private final CyberpunkButton deleteBtn;
        private final CyberpunkButton upBtn;
        private final CyberpunkButton downBtn;

        public MusicEntryRow(MusicSequenceScreen screen, MusicSequenceEntry entry, int index, int totalCount) {
            this.screen = screen;
            this.entry = entry;
            this.index = index;
            this.totalCount = totalCount;

            this.editBtn = new CyberpunkButton(0, 0, 40, 16, Component.literal("EDIT"), b -> screen.openEditEntryModal(index));
            this.deleteBtn = new CyberpunkButton(0, 0, 45, 16, Component.literal("DELETE"), b -> screen.deleteEntry(index), 0xFFFF3366, false);
            this.upBtn = new CyberpunkButton(0, 0, 16, 16, Component.literal("▲"), b -> screen.moveEntry(index, index - 1));
            this.downBtn = new CyberpunkButton(0, 0, 16, 16, Component.literal("▼"), b -> screen.moveEntry(index, index + 1));

            this.upBtn.active = (index > 0);
            this.downBtn.active = (index < totalCount - 1);
        }

        public MusicSequenceEntry getEntry() {
            return entry;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            Font font = Minecraft.getInstance().font;

            int bgColor = isHovered ? 0xEE0D2235 : 0xEE091420;
            int borderColor = isHovered ? 0xFF00E5FF : 0x4400E5FF;
            guiGraphics.fill(left, top, left + width, top + height - 2, bgColor);

            guiGraphics.fill(left, top, left + width, top + 1, borderColor);
            guiGraphics.fill(left, top + height - 3, left + width, top + height - 2, borderColor);
            guiGraphics.fill(left, top, left + 1, top + height - 2, borderColor);
            guiGraphics.fill(left + width - 1, top, left + width, top + height - 2, borderColor);

            // Index & drag indicator
            guiGraphics.drawString(font, "≡ #" + (index + 1), left + 6, top + 8, 0xFF88AAFF, false);

            // Timestamp badge (e.g. 2500ms or 00:02.500)
            long ts = entry.getTimestampMs();
            long sec = ts / 1000;
            long msRem = ts % 1000;
            String timeStr = String.format("%02d:%02d.%03d (%dms)", sec / 60, sec % 60, msRem, ts);
            guiGraphics.drawString(font, timeStr, left + 50, top + 8, 0xFFFFD700, false);

            // Type badge
            String typeStr = "[" + (entry.getActionType() != null ? entry.getActionType() : "COMMAND") + "]";
            guiGraphics.drawString(font, typeStr, left + 190, top + 8, 0xFF00E5FF, false);

            // Command / Payload preview snippet
            String cmdText = entry.getCommand();
            if (cmdText.length() > 30) {
                cmdText = cmdText.substring(0, 27) + "...";
            }
            guiGraphics.drawString(font, cmdText, left + 280, top + 8, 0xFFE0E0E0, false);

            // Description note preview
            if (!entry.getDescription().isEmpty()) {
                String desc = "(" + entry.getDescription() + ")";
                if (desc.length() > 25) {
                    desc = desc.substring(0, 22) + "...";
                }
                guiGraphics.drawString(font, desc, left + 460, top + 8, 0xFF8899AA, false);
            }

            // Render buttons
            int btnY = top + (height - 2 - 16) / 2;
            int rightX = left + width - 6;

            deleteBtn.setX(rightX - 45);
            deleteBtn.setY(btnY);
            deleteBtn.render(guiGraphics, mouseX, mouseY, partialTick);

            editBtn.setX(rightX - 45 - 4 - 40);
            editBtn.setY(btnY);
            editBtn.render(guiGraphics, mouseX, mouseY, partialTick);

            downBtn.setX(rightX - 45 - 4 - 40 - 4 - 16);
            downBtn.setY(btnY);
            downBtn.render(guiGraphics, mouseX, mouseY, partialTick);

            upBtn.setX(rightX - 45 - 4 - 40 - 4 - 16 - 2 - 16);
            upBtn.setY(btnY);
            upBtn.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        public void renderFloating(GuiGraphics guiGraphics, int x, int y, int width, int height) {
            Font font = Minecraft.getInstance().font;
            guiGraphics.fill(x, y, x + width, y + height - 2, 0xEE0A2A3F);
            guiGraphics.fill(x, y, x + width, y + 1, 0xFF00E5FF);
            guiGraphics.fill(x, y + height - 3, x + width, y + height - 2, 0xFF00E5FF);
            guiGraphics.fill(x, y, x + 1, y + height - 2, 0xFF00E5FF);
            guiGraphics.fill(x + width - 1, y, x + width, y + height - 2, 0xFF00E5FF);

            String timeStr = entry.getTimestampMs() + "ms";
            guiGraphics.drawString(font, "≡ Moving Entry #" + (index + 1) + " [" + timeStr + "]", x + 8, y + 8, 0xFFFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (editBtn.mouseClicked(mouseX, mouseY, button)) return true;
            if (deleteBtn.mouseClicked(mouseX, mouseY, button)) return true;
            if (upBtn.mouseClicked(mouseX, mouseY, button)) return true;
            if (downBtn.mouseClicked(mouseX, mouseY, button)) return true;
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal("Music Sequence Entry " + (index + 1));
        }
    }
}
