package tech.sadnes.client.render;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiElementRenderState;

public final class GuiOverlapUtil {
    private static final int CELL_SHIFT = 6;
    private static final int LINEAR_THRESHOLD = 8;
    private static final Map<List<?>, GuiOverlapUtil.SpatialGrid> GRIDS = new IdentityHashMap<>();

    private GuiOverlapUtil() {
    }

    public static boolean overlapsAny(ScreenRect screenRect, List<? extends GuiElementRenderState> list) {
        if (list == null) {
            return false;
        }

        int i = list.size();
        if (i <= 8) {
            for (int i2 = 0; i2 < i; i2++) {
                ScreenRect screenRect2 = ((GuiElementRenderState)list.get(i2)).bounds();
                if (screenRect2 != null && screenRect2.intersects(screenRect)) {
                    return true;
                }
            }

            return false;
        } else {
            GuiOverlapUtil.SpatialGrid spatialGrid = GRIDS.get(list);
            if (spatialGrid == null || spatialGrid.indexedCount > i) {
                spatialGrid = new GuiOverlapUtil.SpatialGrid();
                GRIDS.put(list, spatialGrid);
            }

            spatialGrid.update(list);
            return spatialGrid.overlaps(screenRect, list);
        }
    }

    public static void reset() {
        GRIDS.clear();
    }

    static long cellKey(int i, int i2) {
        return (long)i2 << 32 | i & 4294967295L;
    }

    static final class SpatialGrid {
        private final Long2ObjectOpenHashMap<IntArrayList> cells = new Long2ObjectOpenHashMap<>();
        private final IntArrayList largeElements = new IntArrayList();
        int indexedCount;

        void update(List<? extends GuiElementRenderState> list) {
            int i = list.size();

            for (int i2 = this.indexedCount; i2 < i; i2++) {
                ScreenRect screenRect = ((GuiElementRenderState)list.get(i2)).bounds();
                if (screenRect != null && screenRect.width() > 0 && screenRect.height() > 0) {
                    int i3 = screenRect.getLeft() >> 6;
                    int i4 = screenRect.getRight() - 1 >> 6;
                    int i5 = screenRect.getTop() >> 6;
                    int i6 = screenRect.getBottom() - 1 >> 6;
                    if (i4 - i3 <= 16 && i6 - i5 <= 16) {
                        for (int i7 = i5; i7 <= i6; i7++) {
                            for (int i8 = i3; i8 <= i4; i8++) {
                                this.cells.computeIfAbsent(GuiOverlapUtil.cellKey(i8, i7), l -> new IntArrayList(8)).add(i2);
                            }
                        }
                    } else {
                        this.largeElements.add(i2);
                    }
                }
            }

            this.indexedCount = i;
        }

        boolean overlaps(ScreenRect screenRect, List<? extends GuiElementRenderState> list) {
            if (screenRect.width() > 0 && screenRect.height() > 0) {
                int i = screenRect.getLeft() >> 6;
                int i2 = screenRect.getRight() - 1 >> 6;
                int i3 = screenRect.getTop() >> 6;
                int i4 = screenRect.getBottom() - 1 >> 6;
                if (i2 - i <= 16 && i4 - i3 <= 16) {
                    for (int i5 = 0; i5 < this.largeElements.size(); i5++) {
                        ScreenRect screenRect2 = ((GuiElementRenderState)list.get(this.largeElements.getInt(i5))).bounds();
                        if (screenRect2 != null && screenRect2.intersects(screenRect)) {
                            return true;
                        }
                    }

                    for (int i6 = i3; i6 <= i4; i6++) {
                        for (int i7 = i; i7 <= i2; i7++) {
                            IntArrayList intArrayList = this.cells.get(GuiOverlapUtil.cellKey(i7, i6));
                            if (intArrayList != null) {
                                for (int i8 = 0; i8 < intArrayList.size(); i8++) {
                                    ScreenRect screenRect3 = ((GuiElementRenderState)list.get(intArrayList.getInt(i8))).bounds();
                                    if (screenRect3 != null && screenRect3.intersects(screenRect)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    for (int i9 = 0; i9 < list.size(); i9++) {
                        ScreenRect screenRect4 = ((GuiElementRenderState)list.get(i9)).bounds();
                        if (screenRect4 != null && screenRect4.intersects(screenRect)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
    }
}
