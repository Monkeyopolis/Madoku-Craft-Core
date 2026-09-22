package madoku.craft.java.core.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Core-owned player levels screen backed by the optional Levels menu provider. */
public final class LevelsMenuScreen extends Screen {
	private static final Identifier BACKGROUND_SMALL_TEXTURE = texture("levels-menu/small-levels-container.png");
	private static final Identifier BACKGROUND_NORMAL_TEXTURE = texture("levels-menu/normal-levels-container.png");
	private static final Identifier BACKGROUND_LARGE_TEXTURE = texture("levels-menu/large-levels-container.png");
	private static final Identifier BACKGROUND_SCROLLING_TEXTURE = texture("levels-menu/scrolling-levels-container.png");
	private static final Identifier EMPTY_EXPERIENCE_TEXTURE = texture("levels-menu/empty-experience-bar.png");
	private static final Identifier FULL_EXPERIENCE_TEXTURE = texture("levels-menu/full-experience-bar.png");
	private static final Identifier CONFIRM_TEXTURE = texture("shared-ui/confirm-button.png");
	private static final Identifier CONFIRM_HIGHLIGHTED_TEXTURE = texture("shared-ui/confirm-button-highlighted.png");
	private static final Identifier EXIT_TEXTURE = texture("shared-ui/exit-button.png");
	private static final Identifier EXIT_HIGHLIGHTED_TEXTURE = texture("shared-ui/exit-button-highlighted.png");
	private static final Identifier SCROLLER_TEXTURE = texture("shared-ui/scroller.png");
	private static final Identifier SCROLLER_HIGHLIGHTED_TEXTURE = texture("shared-ui/scroller-highlighted.png");
	private static final int TEXTURE_SIZE = 256;
	private static final int BACKGROUND_RENDER_SIZE = 256;
	private static final int PANEL_WIDTH = 176;
	private static final int SCROLLING_PANEL_WIDTH = 196;
	private static final int PANEL_HEIGHT_SMALL = 100;
	private static final int PANEL_HEIGHT_NORMAL = 140;
	private static final int PANEL_HEIGHT_LARGE = 180;
	private static final int EXIT_SIZE = 12;
	private static final int EXIT_RIGHT_INSET = 5;
	private static final int EXIT_TOP_INSET = 7;
	private static final int HEADER_CENTER_X = 88;
	private static final int HEADER_SEPARATOR_Y = 15;
	private static final int HEADER_TEXT_GAP = 2;
	private static final int XP_BAR_X = 21;
	private static final int XP_BAR_Y = 33;
	private static final int XP_BAR_WIDTH = 134;
	private static final int XP_BAR_HEIGHT = 5;
	private static final int XP_TEXT_ABOVE_GAP = 1;
	private static final int XP_TEXT_BELOW_GAP = 2;
	private static final int STAT_ENTRY_TOP = 54;
	private static final int STAT_ENTRY_WIDTH = 78;
	private static final int STAT_ENTRY_HEIGHT = 32;
	private static final int STAT_ENTRY_ROW_GAP = 8;
	private static final int STAT_LEFT_ENTRY_X = 7;
	private static final int STAT_RIGHT_ENTRY_X = 93;
	private static final int STAT_BUTTON_X = 63;
	private static final int STAT_BUTTON_Y = 10;
	private static final int STAT_TEXT_CENTER_X = 42;
	private static final int STAT_SEPARATOR_Y = 15;
	private static final int STAT_SEPARATOR_HEIGHT = 2;
	private static final int STAT_TITLE_GAP = 2;
	private static final int STAT_LEVEL_GAP = 2;
	private static final int TEXT_COLOR = 0xFF404040;
	private static final int SUBTEXT_COLOR = 0xFF555555;
	private static final float HEADER_TEXT_SCALE = 0.9F;
	private static final float INFO_TEXT_SCALE = 0.8F;
	private static final float STAT_TITLE_SCALE = 0.7F;
	private static final float STAT_LEVEL_SCALE = 0.7F;
	private static final int ATTRIBUTE_VIEWPORT_X = 4;
	private static final int ATTRIBUTE_VIEWPORT_Y = 54;
	private static final int ATTRIBUTE_VIEWPORT_WIDTH = 169;
	private static final int ATTRIBUTE_VIEWPORT_HEIGHT = 112;
	private static final int SCROLLER_X = 180;
	private static final int SCROLLER_Y = 33;
	private static final int SCROLLER_WIDTH = 10;
	private static final int SCROLLER_HEIGHT = 15;
	private static final int SCROLLER_TRACK_HEIGHT = 133;
	private static final int SCROLL_STEP = 16;
	private int scrollOffset;
	private boolean draggingScroller;

	public LevelsMenuScreen() {
		super(Component.translatable("menu.madoku-craft.levels.title"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreenAndShow(null);
	}

	@Override
	protected void init() {
		this.addRenderableOnly((guiGraphics, mouseX, mouseY, partialTick) -> renderMenu(guiGraphics, mouseX, mouseY));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 1) return super.mouseClicked(event, doubleClick);

		LevelsMenuProvider.Snapshot snapshot = LevelsMenuAPIManager.snapshot();
		List<LevelsMenuProvider.Stat> stats = visibleStats(snapshot);
		int panelX = panelX(snapshot);
		int panelY = panelY(snapshot);
		if (contains(exitX(panelX, snapshot), panelY + EXIT_TOP_INSET, EXIT_SIZE, EXIT_SIZE, event.x(), event.y())) {
			returnToMainMenu();
			return true;
		}

		if (isScrolling(stats) && scrollerBounds(panelX, panelY, stats.size()).contains(event.x(), event.y())) {
			draggingScroller = true;
			return true;
		}

		int currentScrollOffset = scrollOffset(snapshot);
		for (int index = 0; index < stats.size(); index++) {
			if (statButtonBounds(panelX, panelY, index, currentScrollOffset).contains(event.x(), event.y())
				&& (!isScrolling(stats) || attributeViewportBounds(panelX, panelY).contains(event.x(), event.y()))) {
				LevelsMenuProvider.Stat stat = stats.get(index);
				if (canUpgrade(snapshot, stat)) LevelsMenuAPIManager.requestUpgrade(stat.id());
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 1 && draggingScroller) {
			draggingScroller = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (event.button() == 1 && draggingScroller) {
			LevelsMenuProvider.Snapshot snapshot = LevelsMenuAPIManager.snapshot();
			int maxScrollOffset = maxScrollOffset(visibleStats(snapshot).size());
			if (maxScrollOffset > 0) {
				scrollOffset = clampScrollOffset(
					scrollOffset + (int) Math.round(deltaY * maxScrollOffset / (double) (SCROLLER_TRACK_HEIGHT - SCROLLER_HEIGHT)),
					maxScrollOffset
				);
			}
			return true;
		}
		return super.mouseDragged(event, deltaX, deltaY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		LevelsMenuProvider.Snapshot snapshot = LevelsMenuAPIManager.snapshot();
		List<LevelsMenuProvider.Stat> stats = visibleStats(snapshot);
		if (!isScrolling(stats)) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

		int panelX = panelX(snapshot);
		int panelY = panelY(snapshot);
		if (!attributeViewportBounds(panelX, panelY).contains(mouseX, mouseY)) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}

		int wheelDelta = (int) Math.round(scrollY * SCROLL_STEP);
		if (wheelDelta == 0 && scrollY != 0.0D) wheelDelta = scrollY > 0.0D ? 1 : -1;
		scrollOffset = clampScrollOffset(scrollOffset - wheelDelta, maxScrollOffset(stats.size()));
		return true;
	}

	private void renderMenu(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		guiGraphics.fill(RenderPipelines.GUI, 0, 0, this.width, this.height, 0x88000000);

		LevelsMenuProvider.Snapshot snapshot = LevelsMenuAPIManager.snapshot();
		int panelX = panelX(snapshot);
		int panelY = panelY(snapshot);
		guiGraphics.blit(
			RenderPipelines.GUI_TEXTURED,
			backgroundTexture(snapshot),
			panelX,
			panelY,
			0.0F,
			0.0F,
			BACKGROUND_RENDER_SIZE,
			BACKGROUND_RENDER_SIZE,
			TEXTURE_SIZE,
			TEXTURE_SIZE
		);

		Minecraft client = Minecraft.getInstance();
		String username = snapshot.hasData()
			? snapshot.username()
			: client.player == null ? "" : client.player.getName().getString();
		String levelText = Component.translatable("menu.madoku-craft.levels.level", snapshot.level()).getString();
		drawScaledCenteredText(guiGraphics, username, panelX + HEADER_CENTER_X, panelY + headerNameY(), HEADER_TEXT_SCALE, TEXT_COLOR);

		boolean exitHovered = contains(exitX(panelX, snapshot), panelY + EXIT_TOP_INSET, EXIT_SIZE, EXIT_SIZE, mouseX, mouseY);
		guiGraphics.blit(
			RenderPipelines.GUI_TEXTURED,
			exitHovered ? EXIT_HIGHLIGHTED_TEXTURE : EXIT_TEXTURE,
			exitX(panelX, snapshot),
			panelY + EXIT_TOP_INSET,
			0.0F,
			0.0F,
			EXIT_SIZE,
			EXIT_SIZE,
			EXIT_SIZE,
			EXIT_SIZE
		);

		guiGraphics.blit(
			RenderPipelines.GUI_TEXTURED,
			EMPTY_EXPERIENCE_TEXTURE,
			panelX + XP_BAR_X,
			panelY + XP_BAR_Y,
			0.0F,
			0.0F,
			XP_BAR_WIDTH,
			XP_BAR_HEIGHT,
			XP_BAR_WIDTH,
			XP_BAR_HEIGHT
		);
		int filledWidth = experienceWidth(snapshot);
		if (filledWidth > 0) {
			guiGraphics.enableScissor(panelX + XP_BAR_X, panelY + XP_BAR_Y, panelX + XP_BAR_X + filledWidth, panelY + XP_BAR_Y + XP_BAR_HEIGHT);
			guiGraphics.blit(
				RenderPipelines.GUI_TEXTURED,
				FULL_EXPERIENCE_TEXTURE,
				panelX + XP_BAR_X,
				panelY + XP_BAR_Y,
				0.0F,
				0.0F,
				XP_BAR_WIDTH,
				XP_BAR_HEIGHT,
				XP_BAR_WIDTH,
				XP_BAR_HEIGHT
			);
			guiGraphics.disableScissor();
		}

		String pointsText = Component.translatable("menu.madoku-craft.levels.points", snapshot.availablePoints()).getString();
		String experienceText = Component.translatable("menu.madoku-craft.levels.experience", snapshot.currentXp(), snapshot.requiredXp()).getString();
		int experienceInfoY = panelY + experienceTextY();
		int levelInfoY = panelY + experienceInfoYAboveBar();
		drawScaledLeftText(guiGraphics, levelText, panelX + XP_BAR_X, levelInfoY, INFO_TEXT_SCALE, TEXT_COLOR);
		drawScaledRightText(guiGraphics, pointsText, panelX + XP_BAR_X + XP_BAR_WIDTH, levelInfoY, INFO_TEXT_SCALE, TEXT_COLOR);
		drawCenteredExperienceText(guiGraphics, experienceText, panelX + XP_BAR_X + XP_BAR_WIDTH / 2, experienceInfoY, INFO_TEXT_SCALE, SUBTEXT_COLOR);

		List<LevelsMenuProvider.Stat> stats = visibleStats(snapshot);
		int currentScrollOffset = scrollOffset(snapshot);
		boolean scrolling = isScrolling(stats);
		if (scrolling) {
			guiGraphics.enableScissor(
				panelX + ATTRIBUTE_VIEWPORT_X,
				panelY + ATTRIBUTE_VIEWPORT_Y,
				panelX + ATTRIBUTE_VIEWPORT_X + ATTRIBUTE_VIEWPORT_WIDTH,
				panelY + ATTRIBUTE_VIEWPORT_Y + ATTRIBUTE_VIEWPORT_HEIGHT
			);
		}
		for (int index = 0; index < stats.size(); index++) {
			LevelsMenuProvider.Stat stat = stats.get(index);
			Bounds entry = statBounds(panelX, panelY, index, currentScrollOffset);
			if (entry.y() + entry.height() <= panelY + ATTRIBUTE_VIEWPORT_Y || entry.y() >= panelY + ATTRIBUTE_VIEWPORT_Y + ATTRIBUTE_VIEWPORT_HEIGHT) continue;
			boolean active = canUpgrade(snapshot, stat);
			boolean hovered = statButtonBounds(panelX, panelY, index, currentScrollOffset).contains(mouseX, mouseY);
			guiGraphics.blit(
				RenderPipelines.GUI_TEXTURED,
				stat.rowTexture(),
				entry.x(),
				entry.y(),
				0.0F,
				0.0F,
				STAT_ENTRY_WIDTH,
				STAT_ENTRY_HEIGHT,
				STAT_ENTRY_WIDTH,
				STAT_ENTRY_HEIGHT
			);
			drawScaledCenteredText(guiGraphics, stat.label(), entry.x() + STAT_TEXT_CENTER_X, entry.y() + statTitleY(), STAT_TITLE_SCALE, TEXT_COLOR);
			drawScaledCenteredText(guiGraphics, stat.level() + "/" + stat.maxLevel(), entry.x() + STAT_TEXT_CENTER_X, entry.y() + statLevelY(), STAT_LEVEL_SCALE, SUBTEXT_COLOR);
			guiGraphics.blit(
				RenderPipelines.GUI_TEXTURED,
				active && hovered ? CONFIRM_HIGHLIGHTED_TEXTURE : CONFIRM_TEXTURE,
				entry.x() + STAT_BUTTON_X,
				entry.y() + STAT_BUTTON_Y,
				0.0F,
				0.0F,
				EXIT_SIZE,
				EXIT_SIZE,
				EXIT_SIZE,
				EXIT_SIZE
			);
		}
		if (scrolling) {
			guiGraphics.disableScissor();
			Bounds scroller = scrollerBounds(panelX, panelY, stats.size());
			boolean scrollerHovered = scroller.contains(mouseX, mouseY);
			guiGraphics.blit(
				RenderPipelines.GUI_TEXTURED,
				scrollerHovered || draggingScroller ? SCROLLER_HIGHLIGHTED_TEXTURE : SCROLLER_TEXTURE,
				scroller.x(),
				scroller.y(),
				0.0F,
				0.0F,
				SCROLLER_WIDTH,
				SCROLLER_HEIGHT,
				SCROLLER_WIDTH,
				SCROLLER_HEIGHT
			);
		}

		if (!snapshot.hasData()) {
			drawScaledCenteredText(
				guiGraphics,
				Component.translatable("menu.madoku-craft.levels.waiting").getString(),
				panelX + HEADER_CENTER_X,
				panelY + 44,
				INFO_TEXT_SCALE,
				SUBTEXT_COLOR
			);
		}
	}

	private Identifier backgroundTexture(LevelsMenuProvider.Snapshot snapshot) {
		int statCount = visibleStats(snapshot).size();
		if (statCount <= 2) return BACKGROUND_SMALL_TEXTURE;
		if (statCount <= 4) return BACKGROUND_NORMAL_TEXTURE;
		if (statCount <= 6) return BACKGROUND_LARGE_TEXTURE;
		return BACKGROUND_SCROLLING_TEXTURE;
	}

	private int panelY(LevelsMenuProvider.Snapshot snapshot) {
		return (this.height - panelHeight(snapshot)) / 2;
	}

	private void returnToMainMenu() {
		Minecraft.getInstance().setScreenAndShow(new MenuScreen());
	}

	private int panelHeight(LevelsMenuProvider.Snapshot snapshot) {
		int statCount = visibleStats(snapshot).size();
		if (statCount <= 2) return PANEL_HEIGHT_SMALL;
		if (statCount <= 4) return PANEL_HEIGHT_NORMAL;
		return PANEL_HEIGHT_LARGE;
	}

	private List<LevelsMenuProvider.Stat> visibleStats(LevelsMenuProvider.Snapshot snapshot) {
		if (!snapshot.hasData()) return List.of();
		return snapshot.stats();
	}

	private Bounds statBounds(int panelX, int panelY, int index, int currentScrollOffset) {
		int row = index / 2;
		int column = index % 2;
		return new Bounds(
			panelX + (column == 0 ? STAT_LEFT_ENTRY_X : STAT_RIGHT_ENTRY_X),
			panelY + STAT_ENTRY_TOP + row * (STAT_ENTRY_HEIGHT + STAT_ENTRY_ROW_GAP) - currentScrollOffset,
			STAT_ENTRY_WIDTH,
			STAT_ENTRY_HEIGHT
		);
	}

	private Bounds statButtonBounds(int panelX, int panelY, int index, int currentScrollOffset) {
		Bounds entry = statBounds(panelX, panelY, index, currentScrollOffset);
		return new Bounds(entry.x() + STAT_BUTTON_X, entry.y() + STAT_BUTTON_Y, EXIT_SIZE, EXIT_SIZE);
	}

	private Bounds attributeViewportBounds(int panelX, int panelY) {
		return new Bounds(panelX + ATTRIBUTE_VIEWPORT_X, panelY + ATTRIBUTE_VIEWPORT_Y, ATTRIBUTE_VIEWPORT_WIDTH, ATTRIBUTE_VIEWPORT_HEIGHT);
	}

	private Bounds scrollerBounds(int panelX, int panelY, int statCount) {
		int maxScrollOffset = maxScrollOffset(statCount);
		int currentScrollOffset = clampScrollOffset(scrollOffset, maxScrollOffset);
		int travel = SCROLLER_TRACK_HEIGHT - SCROLLER_HEIGHT;
		int thumbOffset = maxScrollOffset == 0 ? 0 : Math.round(currentScrollOffset * travel / (float) maxScrollOffset);
		return new Bounds(panelX + SCROLLER_X, panelY + SCROLLER_Y + thumbOffset, SCROLLER_WIDTH, SCROLLER_HEIGHT);
	}

	private boolean isScrolling(List<LevelsMenuProvider.Stat> stats) {
		return stats.size() > 6;
	}

	private int scrollOffset(LevelsMenuProvider.Snapshot snapshot) {
		int maxScrollOffset = maxScrollOffset(visibleStats(snapshot).size());
		scrollOffset = clampScrollOffset(scrollOffset, maxScrollOffset);
		return scrollOffset;
	}

	private int maxScrollOffset(int statCount) {
		int rowCount = (statCount + 1) / 2;
		int contentHeight = Math.max(0, rowCount * (STAT_ENTRY_HEIGHT + STAT_ENTRY_ROW_GAP) - STAT_ENTRY_ROW_GAP);
		return Math.max(0, contentHeight - ATTRIBUTE_VIEWPORT_HEIGHT);
	}

	private int clampScrollOffset(int value, int maximum) {
		return Math.max(0, Math.min(value, maximum));
	}

	private int headerNameY() {
		return HEADER_SEPARATOR_Y - HEADER_TEXT_GAP - scaledTextHeight(HEADER_TEXT_SCALE) + 2;
	}

	private int experienceInfoYAboveBar() {
		return XP_BAR_Y - XP_TEXT_ABOVE_GAP - scaledTextHeight(INFO_TEXT_SCALE);
	}

	private int experienceTextY() {
		return XP_BAR_Y + XP_BAR_HEIGHT + XP_TEXT_BELOW_GAP;
	}

	private int statTitleY() {
		return STAT_SEPARATOR_Y - STAT_TITLE_GAP - scaledTextHeight(STAT_TITLE_SCALE);
	}

	private int statLevelY() {
		return STAT_SEPARATOR_Y + STAT_SEPARATOR_HEIGHT + STAT_LEVEL_GAP;
	}

	private int scaledTextHeight(float scale) {
		return Math.round(this.font.lineHeight * scale);
	}

	private boolean canUpgrade(LevelsMenuProvider.Snapshot snapshot, LevelsMenuProvider.Stat stat) {
		return snapshot.hasData() && snapshot.availablePoints() > 0 && stat.level() < stat.maxLevel();
	}

	private int experienceWidth(LevelsMenuProvider.Snapshot snapshot) {
		if (snapshot.requiredXp() <= 0) return 0;
		return Math.max(0, Math.min(XP_BAR_WIDTH, Math.round(snapshot.currentXp() / (float) snapshot.requiredXp() * XP_BAR_WIDTH)));
	}

	private int panelX(LevelsMenuProvider.Snapshot snapshot) {
		return (this.width - panelWidth(snapshot)) / 2;
	}

	private int panelWidth(LevelsMenuProvider.Snapshot snapshot) {
		return isScrolling(visibleStats(snapshot)) ? SCROLLING_PANEL_WIDTH : PANEL_WIDTH;
	}

	private int exitX(int panelX, LevelsMenuProvider.Snapshot snapshot) {
		return panelX + panelWidth(snapshot) - EXIT_RIGHT_INSET - EXIT_SIZE;
	}

	private void drawScaledLeftText(GuiGraphicsExtractor guiGraphics, String text, int x, int y, float scale, int color) {
		if (text == null || text.isEmpty()) return;
		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().scale(scale, scale);
		guiGraphics.text(this.font, text, Math.round(x / scale), Math.round(y / scale), color, false);
		guiGraphics.pose().popMatrix();
	}

	private void drawScaledRightText(GuiGraphicsExtractor guiGraphics, String text, int rightX, int y, float scale, int color) {
		if (text == null || text.isEmpty()) return;
		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().scale(scale, scale);
		int sourceRightX = (int) Math.ceil(rightX / scale);
		guiGraphics.text(this.font, text, sourceRightX - this.font.width(text), Math.round(y / scale), color, false);
		guiGraphics.pose().popMatrix();
	}

	private void drawScaledCenteredText(GuiGraphicsExtractor guiGraphics, String text, int centerX, int y, float scale, int color) {
		if (text == null || text.isEmpty()) return;
		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().scale(scale, scale);
		int sourceCenterX = Math.round(centerX / scale);
		guiGraphics.text(this.font, text, Math.round(sourceCenterX - this.font.width(text) / 2.0F), Math.round(y / scale), color, false);
		guiGraphics.pose().popMatrix();
	}

	private void drawCenteredExperienceText(GuiGraphicsExtractor guiGraphics, String text, int centerX, int y, float scale, int color) {
		if (text == null || text.isEmpty()) return;
		int slashIndex = text.indexOf('/');
		if (slashIndex < 0) {
			drawScaledCenteredText(guiGraphics, text, centerX, y, scale, color);
			return;
		}

		String currentText = text.substring(0, slashIndex).trim();
		String requiredText = text.substring(slashIndex + 1).trim();
		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().scale(scale, scale);
		int sourceCenterX = Math.round(centerX / scale);
		int slashWidth = this.font.width("/");
		int spaceWidth = this.font.width(" ");
		int slashX = Math.round(sourceCenterX - slashWidth / 2.0F);
		int sourceY = Math.round(y / scale);
		guiGraphics.text(this.font, currentText, slashX - spaceWidth - this.font.width(currentText), sourceY, color, false);
		guiGraphics.text(this.font, "/", slashX, sourceY, color, false);
		guiGraphics.text(this.font, requiredText, slashX + slashWidth + spaceWidth, sourceY, color, false);
		guiGraphics.pose().popMatrix();
	}

	private static boolean contains(int x, int y, int width, int height, double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static Identifier texture(String path) {
		return Identifier.fromNamespaceAndPath("madoku-craft", "textures/" + path);
	}

	private record Bounds(int x, int y, int width, int height) {
		private boolean contains(double mouseX, double mouseY) {
			return LevelsMenuScreen.contains(x, y, width, height, mouseX, mouseY);
		}
	}
}
