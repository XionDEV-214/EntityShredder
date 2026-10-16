package com.deveisia.enceladus.entityshredder.ui;

import com.deveisia.enceladus.entityshredder.data.EntityUtil;
import com.deveisia.enceladus.entityshredder.machine.EntityShredderMachine;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EntitySelectorFancyConfigurator implements IFancyUIProvider {
    /** 선택 버튼 높이 (기존 14의 1.5배). */
    private static final int ROW_H = 21;
    private static final int LIST_W = 150;
    /** 검색에서 제외된 행을 스크롤 영역 밖으로 치워 두는 위치 (콘텐츠 좌표). */
    private static final int HIDDEN_Y = -100000;

    private final EntityShredderMachine machine;

    private DraggableScrollableWidgetGroup list;
    private final List<Row> rows = new ArrayList<>();

    public EntitySelectorFancyConfigurator(EntityShredderMachine machine) {
        this.machine = machine;
    }

    /** 한 엔티티의 선택 버튼(클릭 영역 + 표시 영역)과 검색용 문자열. */
    private static final class Row {
        final String haystack;
        final Widget click;
        final Widget view;

        Row(String haystack, Widget click, Widget view) {
            this.haystack = haystack;
            this.click = click;
            this.view = view;
        }

        void place(int y) {
            click.setSelfPosition(new Position(0, y));
            view.setSelfPosition(new Position(0, y));
        }

        void setEnabled(boolean enabled) {
            click.setActive(enabled);
            view.setActive(enabled);
        }
    }

    @Override
    public Widget createMainPage(FancyMachineUIWidget widget) {
        rows.clear();

        var root = new WidgetGroup(0, 0, 176, 166);
        root.setBackground(GuiTextures.BACKGROUND_INVERSE);

        root.addWidget(new LabelWidget(8, 6,
                () -> Component.translatable("entityshredder.gui.entity_selector.selected",
                        machine.getSelectedEntitySet().size()).getString()).setDropShadow(false));

        root.addWidget(new ButtonWidget(112, 3, 56, 14,
                new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON,
                        new TextTexture("entityshredder.gui.entity_selector.clear").setWidth(56)),
                cd -> machine.clearEntities()));

        // 검색창: 입력은 클라이언트에서만 처리한다(패킷 없음). 위젯 트리는 바꾸지 않고 위치만 옮긴다.
        var search = new TextFieldWidget(8, 20, 160, 14, null, this::applyFilter);
        search.setClientSideWidget();
        search.setMaxStringLength(64);
        search.setHoverTooltips("entityshredder.gui.entity_selector.search");
        root.addWidget(search);

        list = new DraggableScrollableWidgetGroup(8, 38, 160, 120);
        list.setYScrollBarWidth(8);
        list.setYBarStyle(GuiTextures.SLIDER_BACKGROUND_VERTICAL, GuiTextures.BUTTON);
        root.addWidget(list);

        List<ResourceLocation> ids = EntityUtil.ids(machine.isRemote());
        for (int i = 0; i < ids.size(); i++) {
            addEntityRow(0, i * ROW_H, LIST_W, ids.get(i));
        }

        return root;
    }

    private void addEntityRow(int x, int y, int width, ResourceLocation id) {
        final String label = EntityUtil.displayName(id);
        var click = new ButtonWidget(x, y, width, ROW_H, IGuiTexture.EMPTY, cd -> machine.toggleEntity(id));
        var view = new ImageWidget(x, y, width, ROW_H,
                () -> new GuiTextureGroup(
                        ResourceBorderTexture.BUTTON_COMMON.copy()
                                .setColor(machine.isEntitySelected(id) ? ColorPattern.CYAN.color : -1),
                        new TextTexture(label).setWidth(width).setType(TextTexture.TextType.ROLL)));
        list.addWidget(click);
        list.addWidget(view);
        rows.add(new Row((label + " " + id).toLowerCase(Locale.ROOT), click, view));
    }

    /** 이름 또는 ID에 검색어가 포함된 엔티티만 위에서부터 채워 보여준다. */
    private void applyFilter(String query) {
        if (list == null) return;
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        list.setScrollYOffset(0);
        int y = 0;
        for (Row row : rows) {
            boolean match = q.isEmpty() || row.haystack.contains(q);
            row.setEnabled(match);
            // 위치를 바꿀 때마다 스크롤 오프셋이 보정될 수 있으므로 매번 현재 값을 반영한다.
            row.place((match ? y : HIDDEN_Y) - list.getScrollYOffset());
            if (match) y += ROW_H;
        }
        list.setScrollYOffset(0);
    }

    @Override
    public IGuiTexture getTabIcon() {
        return new ItemStackTexture(Items.ZOMBIE_HEAD);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("entityshredder.gui.entity_selector.title");
    }

    @Override
    public List<Component> getTabTooltips() {
        return List.of(Component.translatable("entityshredder.gui.entity_selector.tooltip"));
    }

    @Override
    public boolean hasPlayerInventory() {
        return false;
    }
}
