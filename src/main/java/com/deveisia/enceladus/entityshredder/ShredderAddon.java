package com.deveisia.enceladus.entityshredder;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

/**
 * GTCEu 에 이 모드를 "애드온"으로 등록한다.
 * <p>
 * GTCEu 의 동적 리소스 팩(GTDynamicResourcePack)은 등록된 애드온의 modId 네임스페이스만 서빙한다.
 * 애드온으로 등록되지 않으면 {@code entityshredder:} 네임스페이스의 블록스테이트/모델이 아예 로드되지 않아
 * 컨트롤러가 missing texture 로 깨지고, 해치를 케이싱으로 덮는 texture_overrides 도 적용되지 않는다.
 * <p>
 * 반드시 public 무인자 생성자를 가져야 한다(AddonFinder 가 리플렉션으로 생성).
 */
@GTAddon
public class ShredderAddon implements IGTAddon {

    @Override
    public GTRegistrate getRegistrate() {
        return EntityShredder.REGISTRATE;
    }

    @Override
    public void initializeAddon() {
        EntityShredder.LOGGER.info("[{}] GTCEu addon initialized", EntityShredder.MOD_ID);
    }

    @Override
    public String addonModId() {
        return EntityShredder.MOD_ID;
    }
}
