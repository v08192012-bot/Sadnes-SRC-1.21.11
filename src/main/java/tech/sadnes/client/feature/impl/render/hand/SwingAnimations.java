package tech.sadnes.client.feature.impl.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import tech.sadnes.client.animation.Easings;

final class SwingAnimations {
    static final String OFF = "Off";
    static final String[] MODES = new String[]{"Off", "Swipe", "Down", "Smooth", "Flat", "Power", "Rebound", "Уклон", "Спад", "Tap"};

    private SwingAnimations() {
    }

    static boolean isSwingItem(ItemStack itemStack) {
        return itemStack.isIn(ItemTags.SWORDS) || itemStack.isOf(Items.MACE);
    }

    static void apply(MatrixStack matrixStack, String string, int i, float f, float f2, float f3) {
        float f4;
        float f5;
        label69: {
            label68: {
                label67: {
                    label66: {
                        float f6;
                        label65: {
                            label64: {
                                label63: {
                                    label62: {
                                        f4 = MathHelper.sin(f * f * (float) Math.PI) * f2;
                                        f5 = MathHelper.sin(MathHelper.sqrt(f) * (float) Math.PI) * f2;
                                        f6 = (float)(Math.sin(f * Math.PI) * 0.5) * f2;
                                        byte b = -1;
                                        switch (string.hashCode()) {
                                            case -1814666802:
                                                if (string.equals("Smooth")) {
                                                    break label67;
                                                }
                                                break;
                                            case -1548637429:
                                                if (string.equals("Rebound")) {
                                                    break label64;
                                                }
                                                break;
                                            case 83843:
                                                if (string.equals("Tap")) {
                                                    b = 8;
                                                }
                                                break;
                                            case 2136258:
                                                if (string.equals("Down")) {
                                                    break label68;
                                                }
                                                break;
                                            case 2192281:
                                                if (string.equals("Flat")) {
                                                    break label66;
                                                }
                                                break;
                                            case 32568002:
                                                if (string.equals("Спад")) {
                                                    break label62;
                                                }
                                                break;
                                            case 77306085:
                                                if (string.equals("Power")) {
                                                    break label65;
                                                }
                                                break;
                                            case 80301850:
                                                if (string.equals("Swipe")) {
                                                    break label69;
                                                }
                                                break;
                                            case 1011318115:
                                                if (string.equals("Уклон")) {
                                                    break label63;
                                                }
                                        }

                                        switch (b) {
                                            case 0:
                                                break label69;
                                            case 1:
                                                break label68;
                                            case 2:
                                                break label67;
                                            case 3:
                                                break label66;
                                            case 4:
                                                break label65;
                                            case 5:
                                                break label64;
                                            case 6:
                                                break label63;
                                            case 7:
                                                break;
                                            case 8:
                                                matrixStack.translate(i * 0.56F, -0.42F, -0.78F);
                                                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F));
                                                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
                                                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-110.0F));
                                                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotation(f5 * f3));
                                            default:
                                                return;
                                        }
                                    }

                                    matrixStack.translate(i * 0.56F, -0.32F - f5 / 2.0F, -0.72F);
                                    matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                                    matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                                    matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
                                    matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35 * i));
                                    return;
                                }

                                matrixStack.translate(i * 0.56F, -0.32F, -0.72F);
                                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f5 * 75.0F * i));
                                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f5 * -45.0F));
                                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
                                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35 * i));
                                return;
                            }

                            float f7 = (float)Easings.BACK_OUT.ease(f);
                            float f8 = MathHelper.sin(f7 * f7 * (float) Math.PI);
                            matrixStack.translate(i * 0.56F, -0.42F, -0.78F);
                            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F));
                            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
                            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-110.0F));
                            matrixStack.multiply(RotationAxis.NEGATIVE_Z.rotation(f8 * -f2));
                            matrixStack.multiply(RotationAxis.NEGATIVE_X.rotation(f8 * -f2));
                            return;
                        }

                        matrixStack.translate(i * 0.56F, -0.32F, -0.72F);
                        matrixStack.translate(-f6 * f6 * f4 * i, 0.0F, 0.0F);
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(61 * i));
                        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f5));
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f5 * f4 * -5.0F));
                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f5 * f4 * -30.0F));
                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f6 * -60.0F));
                        return;
                    }

                    float f9 = 0.2F * MathHelper.sin(MathHelper.sqrt(f) * (float) (Math.PI * 2));
                    matrixStack.translate(0.0F, f9 / 2.0F, 0.0F);
                    matrixStack.translate(i * 0.56F, -0.52F, -0.72F);
                    matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + f4 * -20.0F)));
                    matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * f5 * -20.0F));
                    matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f5 * -80.0F));
                    matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
                    return;
                }

                matrixStack.translate(i * 0.56F, -0.42F, -0.72F);
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + f4 * -20.0F)));
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * f5 * -20.0F));
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f5 * -80.0F));
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
                matrixStack.translate(0.0, -0.1, 0.0);
                return;
            }

            matrixStack.translate(i * 0.56F, -0.32F, -0.72F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f5 * -5.0F));
            matrixStack.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(f5 * -100.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f5 * -155.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
            return;
        }

        matrixStack.translate(0.56F * i, -0.32F, -0.72F);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(60 * i));
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60 * i));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f5 * f4 * -5.0F));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f5 * f4 * -120.0F));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
    }
}
