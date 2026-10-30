package tech.sadnes.client.feature.impl.misc.autobuild;

import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.util.color.ColorUtil;

public final class SchematicPreviewRenderer {
    private final SchematicFileManager schematicFiles;
    private String loadedFile;
    private Vec3i size;

    public void loadSize(String string) {
        if (string == null) {
            this.loadedFile = null;
            this.size = null;
        } else if (!string.equals(this.loadedFile)) {
            this.loadedFile = string;
            this.size = null;
            this.schematicFiles.loadSchematic(this.schematicFiles.getSchematicsDir().resolve(string)).whenComplete((iStaticSchematic, throwable) -> {
                boolean flag;
                if (throwable == null) {
                    flag = string.equals(this.loadedFile);
                } else {
                    flag = false;
                }

                if (flag) {
                    this.size = new Vec3i(iStaticSchematic.widthX(), iStaticSchematic.heightY(), iStaticSchematic.lengthZ());
                }
            });
        }
    }

    public Box getPreviewBox(BlockPos blockPos, boolean flag) {
        if (this.size == null) {
            return null;
        }

        int i = flag ? this.size.getZ() : this.size.getX();
        int i2 = flag ? this.size.getX() : this.size.getZ();
        return new Box(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + i, blockPos.getY() + this.size.getY(), blockPos.getZ() + i2);
    }

    public static void drawBox(Render3DUtil.BoxBatch boxBatch, MatrixStack matrixStack, Vec3d vec3d, Box box, int i, float f) {
        if (ProjectionUtil.isVisible(box)) {
            float f2 = (float)(box.minX - vec3d.x);
            float f3 = (float)(box.minY - vec3d.y);
            float f4 = (float)(box.minZ - vec3d.z);
            float f5 = (float)(box.maxX - vec3d.x);
            float f6 = (float)(box.maxY - vec3d.y);
            float f7 = (float)(box.maxZ - vec3d.z);
            boolean flag = box.contains(vec3d);
            if (!flag) {
                boxBatch.fill(matrixStack, f2, f3, f4, f5, f6, f7, ColorUtil.multiplyAlpha(i, 0.08F * f));
            }

            boxBatch.outline(matrixStack, f2, f3, f4, f5, f6, f7, ColorUtil.multiplyAlpha(i, f));
        }
    }

    public static void drawBlockMarkers(Render3DUtil.BoxBatch boxBatch, MatrixStack matrixStack, Vec3d vec3d, LongList longList, int i) {
        int i2 = ColorUtil.multiplyAlpha(i, 0.55F);
        int i3 = Math.min(longList.size(), 1500);

        for (int i4 = 0; i4 < i3; i4++) {
            long l = longList.getLong(i4);
            int i5 = BlockPos.unpackLongX(l);
            int i6 = BlockPos.unpackLongY(l);
            int i7 = BlockPos.unpackLongZ(l);
            if (ProjectionUtil.isVisible(i5 + 0.5, i6 + 0.5, i7 + 0.5)) {
                float f = (float)(i5 - vec3d.x);
                float f2 = (float)(i6 - vec3d.y);
                float f3 = (float)(i7 - vec3d.z);
                boxBatch.outline(matrixStack, f + 0.3F, f2 + 0.3F, f3 + 0.3F, f + 0.7F, f2 + 0.7F, f3 + 0.7F, i2);
            }
        }
    }

    public SchematicPreviewRenderer(SchematicFileManager schematicFileManager) {
        this.schematicFiles = schematicFileManager;
    }
}
