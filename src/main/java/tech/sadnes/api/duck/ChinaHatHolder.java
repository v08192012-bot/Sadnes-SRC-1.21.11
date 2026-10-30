package tech.sadnes.api.duck;

import tech.sadnes.client.render.ChinaHatPhysics;

public interface ChinaHatHolder {
    ChinaHatPhysics sadnes$chinaHat();

    void sadnes$setChinaHat(ChinaHatPhysics chinaHatPhysics);

    boolean sadnes$chinaHatHelmet();

    void sadnes$setChinaHatHelmet(boolean flag);
}
