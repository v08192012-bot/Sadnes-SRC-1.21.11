package tech.sadnes.client.render;

public enum Side {
    LEFT {
        @Override
        public void expand(RenderUtil renderUtil, float f) {
            renderUtil.setX(renderUtil.getX() - f);
            renderUtil.setWidth(renderUtil.getWidth() + f);
        }
    },
    RIGHT {
        @Override
        public void expand(RenderUtil renderUtil, float f) {
            renderUtil.setWidth(renderUtil.getWidth() + f);
        }
    },
    TOP {
        @Override
        public void expand(RenderUtil renderUtil, float f) {
            renderUtil.setY(renderUtil.getY() - f);
            renderUtil.setHeight(renderUtil.getHeight() + f);
        }
    },
    BOTTOM {
        @Override
        public void expand(RenderUtil renderUtil, float f) {
            renderUtil.setHeight(renderUtil.getHeight() + f);
        }
    },
    ALL {
        @Override
        public void expand(RenderUtil renderUtil, float f) {
            renderUtil.setX(renderUtil.getX() - f);
            renderUtil.setY(renderUtil.getY() - f);
            renderUtil.setWidth(renderUtil.getWidth() + f * 2.0F);
            renderUtil.setHeight(renderUtil.getHeight() + f * 2.0F);
        }
    };

    public abstract void expand(RenderUtil renderUtil, float f);
}
