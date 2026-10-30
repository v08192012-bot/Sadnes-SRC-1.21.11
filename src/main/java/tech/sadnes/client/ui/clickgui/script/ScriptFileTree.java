package tech.sadnes.client.ui.clickgui.script;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Util;
import net.minecraft.util.Util.OperatingSystem;
import org.joml.Vector2f;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.editor.FilePreview;
import tech.sadnes.client.ui.IInteractable;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.component.TextFieldComponent;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class ScriptFileTree implements IInteractable, IRenderable, IMinecraft {
    public static final float ROW_HEIGHT = 13.0F;
    private static final float HALF_ROW_HEIGHT = 6.5F;
    private static final float MENU_ROW_HEIGHT = 15.0F;
    private static final float MENU_PADDING = 4.0F;
    private static final float MENU_MIN_WIDTH = 108.0F;
    private static final float DRAG_THRESHOLD = 3.0F;
    private final Consumer<Path> fileOpener;
    private final Runnable refreshCallback;
    private final Runnable beforeChangeCallback;
    private final List<ScriptFileTree.TreeNode> nodes = new ArrayList<>();
    private final Set<Path> collapsed = new HashSet<>();
    private final TextFieldComponent nameField = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.0F, "scripts.tree.name", false, false);
    private Script script;
    private Path selectedPath;
    private float x;
    private float y;
    private float width;
    private float height;
    private float scrollOffset;
    private ScriptFileTree.EditMode editMode = ScriptFileTree.EditMode.NONE;
    private Path editTarget;
    private List<ScriptFileTree.MenuEntry> menuEntries = List.of();
    private boolean menuOpen;
    private float menuX;
    private float menuY;
    private float menuWidth;
    private float menuHeight;
    private final Animation menuAnimation = new Animation();
    private Path pressedPath;
    private double pressX;
    private double pressY;
    private Path draggedPath;
    private Path dropTarget;

    public ScriptFileTree(Consumer<Path> consumer, Runnable runnable, Runnable runnable2) {
        this.fileOpener = consumer;
        this.refreshCallback = runnable;
        this.beforeChangeCallback = runnable2;
    }

    public void setScript(Script script2) {
        this.script = script2;
        this.collapsed.clear();
        this.scrollOffset = 0.0F;
        this.cancelEdit();
        this.closeContextMenu();
    }

    public void setSelectedPath(Path path) {
        this.selectedPath = path;
    }

    public boolean isEditing() {
        return this.editMode != ScriptFileTree.EditMode.NONE;
    }

    public void render(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2, float f5) {
        if (this.script != null) {
            this.x = f;
            this.y = f2;
            this.width = f3;
            this.height = f4;
            this.rebuildNodes();
            this.clampScroll();
            new RenderUtil(drawContext, f + f3 - 1.0F, f2, 1.0F, f4).rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getOutline(), 0.4F), f5));
            if (ScissorStack.push(drawContext, f, f2, f3 - 2.0F, f4)) {
                try {
                    float f6 = f2 + 2.0F - this.scrollOffset;

                    for (ScriptFileTree.TreeNode treeNode : this.nodes) {
                        if (f6 + 13.0F >= f2 && f6 <= f2 + f4) {
                            this.renderNode(drawContext, treeNode, i, i2, f6, f5);
                        }

                        f6 += 13.0F;
                        if (this.hasInputBelow(treeNode)) {
                            this.renderNameField(drawContext, f6, f5);
                            f6 += 13.0F;
                        }
                    }
                } finally {
                    ScissorStack.pop(drawContext);
                }
            }

            this.renderContextMenu(drawContext, i, i2, f5);
            if (this.draggedPath != null) {
                this.renderDragGhost(drawContext, i, i2, f5);
            }
        }
    }

    private boolean hasInputBelow(ScriptFileTree.TreeNode treeNode) {
        return this.editMode != ScriptFileTree.EditMode.NONE && this.editMode != ScriptFileTree.EditMode.RENAME ? treeNode.path().equals(this.editTarget) : false;
    }

    private void renderNode(DrawContext drawContext, ScriptFileTree.TreeNode treeNode, int i, int i2, float f, float f2) {
        boolean flag;
        if (!treeNode.directory()) {
            flag = treeNode.path().equals(this.selectedPath);
        } else {
            flag = false;
        }

        boolean flag2 = this.isHovered(i, i2, this.x, f, this.width - 2.0F, 13.0);
        boolean flag3;
        if (treeNode.directory()) {
            flag3 = treeNode.path().equals(this.dropTarget);
        } else {
            flag3 = false;
        }

        boolean flag4;
        if (this.editMode == ScriptFileTree.EditMode.RENAME) {
            flag4 = treeNode.path().equals(this.editTarget);
        } else {
            flag4 = false;
        }

        if (flag4) {
            this.renderNameField(drawContext, f, f2);
        } else {
            if (flag || flag2 || flag3) {
                int i3 = flag3 ? Theme.getPrimary() : (flag ? Theme.getPrimary() : Theme.getSurfaceDark());
                float f3 = flag3 ? 0.3F : (flag ? 0.18F : 0.1F);
                new RenderUtil(drawContext, this.x + 1.0F, f, this.width - 4.0F, 13.0F)
                    .rect(ColorUtil.multiplyAlpha(Theme.withAlpha(i3, f3), f2), Radius.all(3.0F));
            }

            float f4 = 3.0F + treeNode.depth() * 7.0F;
            Icon icon2 = treeNode.directory() ? (this.collapsed.contains(treeNode.path()) ? Icon.CARET_RIGHT : Icon.FOLDER) : (treeNode.text() ? Icon.CODE : Icon.PACKAGE);
            int i4 = ColorUtil.multiplyAlpha(
                Theme.withAlpha(treeNode.directory() ? Theme.getPrimary() : Theme.getSecondaryText(), treeNode.directory() ? 0.8F : 0.55F), f2
            );
            int i5 = ColorUtil.multiplyAlpha(
                Theme.withAlpha(flag ? Theme.getTextColor() : Theme.getSecondaryText(), treeNode.directory() ? 0.8F : (treeNode.text() ? 0.92F : 0.6F)), f2
            );
            new RenderUtil(drawContext, this.x + f4, f, this.width, 13.0F)
                .icon(icon2, 0.0F, 2.5F, 8.0F, i4)
                .textMiddleLeft(Fonts.INTER_MEDIUM, this.truncate(treeNode.path().getFileName().toString(), this.width - f4 - 16.0F), 11.0F, 0.0F, i5, 6.5F);
        }
    }

    private void renderNameField(DrawContext drawContext, float f, float f2) {
        float f3 = this.x + 4.0F;
        float f4 = this.width - 10.0F;
        GuiRenderUtil.drawOutlined(drawContext, f3, f, f4, 13.0F, f2 * 0.9F, 3.0F);
        this.nameField.render(drawContext, new Vector2f(f3 + 4.0F, f + 6.5F - 3.0F), ColorUtil.multiplyAlpha(Theme.getTextColor(), f2), f4 - 8.0F);
    }

    private void renderDragGhost(DrawContext drawContext, int i, int i2, float f) {
        String string = this.draggedPath.getFileName().toString();
        float f2 = Fonts.INTER_MEDIUM.getWidth(string, 6.5F) + 16.0F;
        GuiRenderUtil.drawOutlined(drawContext, i + 6.0F, i2 + 6.0F, f2, 13.0F, f * 0.9F, 3.0F)
            .icon(Files.isDirectory(this.draggedPath) ? Icon.FOLDER : Icon.CODE, 4.0F, 3.0F, 7.0F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f))
            .textMiddleLeft(Fonts.INTER_MEDIUM, string, 14.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.getTextColor(), f), 6.5F);
    }

    private void renderContextMenu(DrawContext drawContext, int i, int i2, float f) {
        this.menuAnimation.update();
        this.menuAnimation.animate(this.menuOpen ? 1.0 : 0.0, this.menuOpen ? 0.25 : 0.15, this.menuOpen ? Easings.BACK_OUT : Easings.QUAD_IN);
        if (this.menuEntries.isEmpty() || !this.menuOpen && this.menuAnimation.get() <= 0.001) {
            if (!this.menuOpen) {
                this.menuEntries = List.of();
            }
        } else {
            float f2 = (float)Math.min(this.menuAnimation.get(), 1.0);
            float f3 = f * f2;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, this.menuX + this.menuWidth / 2.0F, this.menuY + this.menuHeight / 2.0F, 0.9F + 0.1F * f2);
            GuiRenderUtil.drawPopup(drawContext, this.menuX, this.menuY, this.menuWidth, this.menuHeight, f3, 6.0F);
            int i3 = 0;

            for (int i4 = this.menuEntries.size(); i3 < i4; i3++) {
                ScriptFileTree.MenuEntry menuEntry = this.menuEntries.get(i3);
                float f4 = this.menuX + 4.0F;
                float f5 = this.menuY + 4.0F + i3 * 15.0F;
                boolean flag;
                if (this.menuOpen) {
                    flag = this.isHovered(i, i2, f4, f5, this.menuWidth - 8.0F, 15.0);
                } else {
                    flag = false;
                }

                if (flag) {
                    GuiRenderUtil.drawHighlight(drawContext, f4, f5, this.menuWidth - 8.0F, 15.0F, f3, Radius.all(3.0F));
                }

                int i5 = menuEntry.danger()
                    ? ColorUtil.multiplyAlpha(ColorUtil.rgb(255, 96, 96), f3)
                    : ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), flag ? 1.0F : 0.85F), f3);
                new RenderUtil(drawContext, f4, f5, this.menuWidth, 15.0F)
                    .icon(
                        menuEntry.icon(),
                        3.0F,
                        3.5F,
                        8.0F,
                        ColorUtil.multiplyAlpha(Theme.withAlpha(menuEntry.danger() ? ColorUtil.rgb(255, 120, 120) : Theme.getSecondaryText(), 0.8F), f3)
                    )
                    .textMiddleLeft(Fonts.INTER_MEDIUM, menuEntry.label(), 15.0F, 0.0F, i5, 6.0F);
            }

            MatrixUtil.pop(drawContext);
        }
    }

    private void rebuildNodes() {
        this.nodes.clear();
        this.addNode(this.script.getSourceDir(), 0);
        this.addNode(this.script.getAssetsDir(), 0);
    }

    private void addNode(Path path, int i) {
        boolean flag = Files.isDirectory(path);
        this.nodes.add(new ScriptFileTree.TreeNode(i, path, flag, !flag && FilePreview.isTextFile(path)));
        if (flag && !this.collapsed.contains(path) && i < 4) {
            try (Stream<Path> stream = Files.list(path)) {
                for (Path path2 : stream.sorted(
                        Comparator.<Path, Boolean>comparing(path3 -> !Files.isDirectory(path3) ? true : false)
                            .thenComparing(path4 -> path4.getFileName().toString().toLowerCase(Locale.ROOT))
                    )
                    .toList()) {
                    this.addNode(path2, i + 1);
                }
            } catch (IOException ioException) {
            }
        }
    }

    private ScriptFileTree.TreeNode getNodeAt(double d, double d2) {
        if (!this.isHovered(d, d2, this.x, this.y, this.width, this.height)) {
            return null;
        }

        int i = (int)((d2 - this.y - 2.0 + this.scrollOffset) / 13.0);
        return i >= 0 && i < this.nodes.size() ? this.nodes.get(i) : null;
    }

    private List<ScriptFileTree.MenuEntry> buildMenuEntries(ScriptFileTree.TreeNode treeNode) {
        ArrayList arrayList = new ArrayList(6);
        Path path2 = treeNode.path();
        boolean flag;
        if (!path2.equals(this.script.getSourceDir())) {
            flag = path2.equals(this.script.getAssetsDir());
        } else {
            flag = true;
        }

        if (treeNode.directory()) {
            arrayList.add(
                new ScriptFileTree.MenuEntry(
                    Icon.PLUS, Language.translate("scripts.tree.new_file", "Новый файл"), false, () -> this.beginEdit(ScriptFileTree.EditMode.NEW_FILE, path2)
                )
            );
            arrayList.add(
                new ScriptFileTree.MenuEntry(
                    Icon.FOLDER,
                    Language.translate("scripts.tree.new_folder", "Новая папка"),
                    false,
                    () -> this.beginEdit(ScriptFileTree.EditMode.NEW_FOLDER, path2)
                )
            );
        } else {
            arrayList.add(new ScriptFileTree.MenuEntry(Icon.CLONE, Language.translate("scripts.tree.duplicate", "Дублировать"), false, () -> this.duplicate(path2)));
        }

        if (!flag) {
            arrayList.add(
                new ScriptFileTree.MenuEntry(
                    Icon.FONT, Language.translate("scripts.tree.rename", "Переименовать"), false, () -> this.beginEdit(ScriptFileTree.EditMode.RENAME, path2)
                )
            );
        }

        arrayList.add(new ScriptFileTree.MenuEntry(Icon.SEARCH, Language.translate("scripts.tree.reveal", "Показать в проводнике"), false, () -> {
            OperatingSystem operatingSystem = Util.getOperatingSystem();
            if (treeNode.directory()) {
                operatingSystem.open(path2.toFile());
            } else {
                operatingSystem.open(path2.getParent().toFile());
            }
        }));
        if (!flag) {
            arrayList.add(new ScriptFileTree.MenuEntry(Icon.TRASH, Language.translate("scripts.tree.delete", "Удалить"), true, () -> this.delete(path2)));
        }

        return arrayList;
    }

    private void beginEdit(ScriptFileTree.EditMode editMode2, Path path) {
        this.editMode = editMode2;
        this.editTarget = path;
        this.nameField.setText(editMode2 == ScriptFileTree.EditMode.RENAME ? path.getFileName().toString() : "");
        this.nameField.setFocused(true);
        if (editMode2 != ScriptFileTree.EditMode.RENAME) {
            this.collapsed.remove(path);
        }
    }

    private void cancelEdit() {
        this.editMode = ScriptFileTree.EditMode.NONE;
        this.editTarget = null;
        this.nameField.setText("");
        this.nameField.setFocused(false);
    }

    private void commitEdit() {
        this.beforeChangeCallback.run();
        String string = this.nameField.getText().trim();
        ScriptFileTree.EditMode editMode2 = this.editMode;
        Path path = this.editTarget;
        this.cancelEdit();
        if (!string.isEmpty() && path != null && !string.contains("/") && !string.contains("\\")) {
            try {
                switch (editMode2) {
                    case NEW_FILE:
                        Path path2 = path.resolve(string);
                        if (Files.exists(path2)) {
                            this.warnExists(string);
                            return;
                        }

                        Files.createFile(path2);
                        this.fileOpener.accept(path2);
                        break;
                    case NEW_FOLDER:
                        Path path3 = path.resolve(string);
                        if (Files.exists(path3)) {
                            this.warnExists(string);
                            return;
                        }

                        Files.createDirectories(path3);
                        break;
                    case RENAME:
                        Path path4 = path.resolveSibling(string);
                        if (Files.exists(path4)) {
                            this.warnExists(string);
                            return;
                        }

                        Files.move(path, path4);
                        if (path.equals(this.selectedPath)) {
                            this.fileOpener.accept(path4);
                        }
                }

                this.refreshCallback.run();
            } catch (IOException ioException) {
                NotificationUtil.error(
                    NotificationSources.scripts(), Language.translate("scripts.tree.failed", "Операция не удалась") + ": " + ioException.getMessage()
                );
            }
        }
    }

    private void warnExists(String string) {
        NotificationUtil.warning(NotificationSources.scripts(), String.format(Language.translate("scripts.tree.exists", "«%s» уже существует"), string));
    }

    private void duplicate(Path path) {
        String string = path.getFileName().toString();
        int i = string.lastIndexOf(46);
        String string2 = i > 0 ? string.substring(0, i) : string;
        String string3 = i > 0 ? string.substring(i) : "";

        try {
            Path path2 = path.resolveSibling(string2 + "_copy" + string3);

            for (int i2 = 2; Files.exists(path2); i2++) {
                path2 = path.resolveSibling(string2 + "_copy" + i2 + string3);
            }

            Files.copy(path, path2, StandardCopyOption.COPY_ATTRIBUTES);
            this.refreshCallback.run();
        } catch (IOException ioException) {
            NotificationUtil.error(NotificationSources.scripts(), Language.translate("scripts.tree.failed", "Операция не удалась") + ": " + ioException.getMessage());
        }
    }

    private void delete(Path path) {
        this.beforeChangeCallback.run();

        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path2 -> {
                try {
                    Files.deleteIfExists(path2);
                } catch (IOException ioException) {
                }
            });
        } catch (IOException ioException2) {
            NotificationUtil.error(NotificationSources.scripts(), Language.translate("scripts.tree.failed", "Операция не удалась") + ": " + ioException2.getMessage());
            return;
        }

        if (path.equals(this.selectedPath) || this.selectedPath != null && this.selectedPath.startsWith(path)) {
            this.fileOpener.accept(this.script.getMainFile());
        }

        this.refreshCallback.run();
    }

    private void move(Path path, Path path2) {
        this.beforeChangeCallback.run();
        if (path2 != null && !path.equals(path2) && !path2.startsWith(path)) {
            if (path.getParent() == null || !path.getParent().equals(path2)) {
                Path path3 = path2.resolve(path.getFileName());
                if (Files.exists(path3)) {
                    this.warnExists(path3.getFileName().toString());
                } else {
                    try {
                        Files.move(path, path3);
                        if (path.equals(this.selectedPath)) {
                            this.fileOpener.accept(path3);
                        }

                        this.refreshCallback.run();
                    } catch (IOException ioException) {
                        NotificationUtil.error(
                            NotificationSources.scripts(), Language.translate("scripts.tree.failed", "Операция не удалась") + ": " + ioException.getMessage()
                        );
                    }
                }
            }
        }
    }

    public boolean mouseClicked(Click click, boolean flag) {
        double d = click.x();
        double d2 = click.y();
        if (this.menuOpen) {
            if (!this.isHovered(d, d2, this.menuX, this.menuY, this.menuWidth, this.menuHeight)) {
                this.closeContextMenu();
                return true;
            }

            int i = (int)((d2 - this.menuY - 4.0) / 15.0);
            List list = this.menuEntries;
            this.closeContextMenu();
            if (i >= 0 && i < list.size()) {
                ((ScriptFileTree.MenuEntry)list.get(i)).action().run();
            }
        } else {
            if (!this.isHovered(d, d2, this.x, this.y, this.width, this.height)) {
                if (this.editMode != ScriptFileTree.EditMode.NONE) {
                    this.commitEdit();
                }

                return false;
            }

            if (this.editMode != ScriptFileTree.EditMode.NONE) {
                this.commitEdit();
                return true;
            }

            ScriptFileTree.TreeNode treeNode = this.getNodeAt(d, d2);
            if (treeNode == null) {
                return true;
            }

            if (this.isRightButton(click.button())) {
                this.openContextMenu(treeNode, d, d2);
                return true;
            }

            if (!this.isLeftButton(click.button())) {
                return true;
            }

            if (!treeNode.directory()) {
                this.pressedPath = treeNode.path();
                this.pressX = d;
                this.pressY = d2;
                this.fileOpener.accept(treeNode.path());
                return true;
            }

            if (!this.collapsed.remove(treeNode.path())) {
                this.collapsed.add(treeNode.path());
                return true;
            }
        }

        return true;
    }

    public boolean mouseDragged(Click click, double d, double d2) {
        if (this.pressedPath == null) {
            return false;
        }

        if (this.draggedPath == null && Math.abs(click.x() - this.pressX) + Math.abs(click.y() - this.pressY) > 3.0) {
            this.draggedPath = this.pressedPath;
        }

        if (this.draggedPath == null) {
            return false;
        } else {
            ScriptFileTree.TreeNode treeNode = this.getNodeAt(click.x(), click.y());
            if (treeNode == null) {
                this.dropTarget = null;
                return true;
            } else if (treeNode.directory()) {
                this.dropTarget = treeNode.path();
                return true;
            } else {
                this.dropTarget = treeNode.path().getParent();
                return true;
            }
        }
    }

    public boolean mouseReleased(Click click) {
        if (this.draggedPath != null && this.dropTarget != null) {
            this.move(this.draggedPath, this.dropTarget);
        }

        boolean flag = this.draggedPath != null;
        this.pressedPath = null;
        this.draggedPath = null;
        this.dropTarget = null;
        return flag;
    }

    public boolean mouseScrolled(double d, double d2, double d3) {
        if (!this.isHovered(d, d2, this.x, this.y, this.width, this.height)) {
            return false;
        }

        if (this.menuOpen) {
            this.closeContextMenu();
        }

        this.scrollOffset -= (float)d3 * 13.0F * 2.0F;
        this.clampScroll();
        return true;
    }

    public boolean keyPressed(KeyInput keyInput) {
        if (this.menuOpen && keyInput.key() == 256) {
            this.closeContextMenu();
            return true;
        }

        if (this.editMode == ScriptFileTree.EditMode.NONE) {
            return false;
        }

        switch (keyInput.key()) {
            case 256:
                this.cancelEdit();
                return true;
            case 257:
            case 335:
                this.commitEdit();
                return true;
            default:
                this.nameField.keyPressed(keyInput.key());
                return true;
        }
    }

    public boolean charTyped(CharInput charInput) {
        if (this.editMode == ScriptFileTree.EditMode.NONE) {
            return false;
        }

        this.nameField.charTyped(charInput.codepoint());
        return true;
    }

    private void openContextMenu(ScriptFileTree.TreeNode treeNode, double d, double d2) {
        this.menuEntries = this.buildMenuEntries(treeNode);
        this.menuOpen = true;
        this.menuAnimation.forceValue(0.0);
        this.menuWidth = 108.0F;

        for (ScriptFileTree.MenuEntry menuEntry : this.menuEntries) {
            this.menuWidth = Math.max(this.menuWidth, Fonts.INTER_MEDIUM.getWidth(menuEntry.label(), 6.0F) + 26.0F);
        }

        this.menuHeight = this.menuEntries.size() * 15.0F + 8.0F;
        this.menuX = GuiRenderUtil.clampToScreenX((float)d, this.menuWidth);
        this.menuY = GuiRenderUtil.clampToScreenY((float)d2, this.menuHeight);
    }

    private void closeContextMenu() {
        this.menuOpen = false;
    }

    public boolean isContextMenuOpen() {
        return this.menuOpen;
    }

    private void clampScroll() {
        float f = this.nodes.size() * 13.0F + 4.0F + (this.editMode != ScriptFileTree.EditMode.NONE ? 13.0F : 0.0F);
        this.scrollOffset = Math.clamp(this.scrollOffset, 0.0F, Math.max(0.0F, f - this.height));
    }

    private String truncate(String string, float f) {
        if (!(f <= 0.0F) && !(Fonts.INTER_MEDIUM.getWidth(string, 6.5F) <= f)) {
            StringBuilder builder = new StringBuilder();
            int i = 0;

            for (int i2 = string.length(); i < i2 && !(Fonts.INTER_MEDIUM.getWidth(builder.toString() + string.charAt(i) + "…", 6.5F) > f); i++) {
                builder.append(string.charAt(i));
            }

            return builder + "…";
        } else {
            return string;
        }
    }

    enum EditMode {
        NONE,
        NEW_FILE,
        NEW_FOLDER,
        RENAME;
    }

    record MenuEntry(Icon icon, String label, boolean danger, Runnable action) {
    }

    record TreeNode(int depth, Path path, boolean directory, boolean text) {
    }
}
