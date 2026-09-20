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
	private static final Identifier BACKGROUND_FOUR_TEXTURE = texture("player-levels/player-levels-four.png");
	private static final Identifier BACKGROUND_FIVE_TEXTURE = texture("player-levels/player-levels-five.png");
	private static final Identifier BACKGROUND_SIX_TEXTURE = texture("player-levels/player-levels-six.png");
	private static final Identifier EMPTY_EXPERIENCE_TEXTURE = texture("player-levels/empty-experience-bar.png");
	private static final Identifier FULL_EXPERIENCE_TEXTURE = texture("player-levels/full-experience-bar.png");
	private static final Identifier CONFIRM_TEXTURE = texture("shared-ui/confirm-button.png");
	private static final Identifier CONFIRM_HIGHLIGHTED_TEXTURE = texture("shared-ui/confirm-button-highlighted.png");
	private static final Identifier EXIT_TEXTURE = texture("shared-ui/exit-button.png");
	private static final Identifier EXIT_HIGHLIGHTED_TEXTURE = texture("shared-ui/exit-button-highlighted.png");
	private static final int TEXTURE_SIZE = 256;
	private static final int BACKGROUND_RENDER_SIZE = 256;
	private static final int PANEL_WIDTH = 176;
	private static final int PANEL_HEIGHT_FOUR = 128;
	private static final int PANEL_HEIGHT_EXPANDED = 168;
	private static final int EXIT_SIZE = 12;
	private static final int EXIT_RIGHT_INSET = 6;
	private static final int EXIT_TOP_INSET = 7;
	private static final int HEADER_CENTER_X = 88;
	private static final int HEADER_SEPARATOR_Y = 17;
	private static final int HEADER_TEXT_GAP = 2;
	private static final int XP_BAR_X = 16;
	private static final int XP_BAR_Y = 32;
	private static final int XP_BAR_WIDTH = 144;
	private static final int XP_BAR_HEIGHT = 5;
	private static final int XP_TEXT_ABOVE_GAP = 1;
	private static final int XP_TEXT_BELOW_GAP = 2;
	private static final int STAT_ENTRY_TOP = 56;
	private static final int STAT_ENTRY_WIDTH = 77;
	private static final int STAT_ENTRY_HEIGHT = 32;
	private static final int STAT_ENTRY_ROW_GAP = 8;
	private static final int STAT_LEFT_ENTRY_X = 7;
	private static final int STAT_RIGHT_ENTRY_X = 92;
	private static final int STAT_FIFTH_ENTRY_X = 49;
	private static final int STAT_BUTTON_X = 62;
	private static final int STAT_BUTTON_Y = 10;
	private static final int STAT_TEXT_CENTER_X = 40;
	private static final int STAT_SEPARATOR_Y = 15;
	private static final int STAT_SEPARATOR_HEIGHT = 2;
	private static final int STAT_TITLE_GAP = 2;
	private static final int STAT_LEVEL_GAP = 2;
	private static final int TEXT_COLOR = 0xFF404040;
	private static final int SUBTEXT_COLOR = 0xFF555555;
	private static final float HEADER_TEXT_SCALE = 0.9F;
	private static final float INFO_TEXT_SCALE = 0.8F;
	private static final float STAT_TITLE_SCALE = 0.8F;
	private static final float STAT_LEVEL_SCALE = 0.7F;

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
		int panelX = panelX();
		int panelY = panelY(snapshot);
		if (contains(exitX(panelX), panelY + EXIT_TOP_INSET, EXIT_SIZE, EXIT_SIZE, event.x(), event.y())) {
			returnToMainMenu();
			return true;
		}

		List<LevelsMenuProvider.Stat> stats = visibleStats(snapshot);
		for (int index = 0; index < stats.size(); index++) {
			if (statButtonBounds(panelX, panelY, index, stats.size()).contains(event.x(), event.y())) {
				LevelsMenuProvider.Stat stat = stats.get(index);
				if (canUpgrade(snapshot, stat)) LevelsMenuAPIManager.requestUpgrade(stat.id());
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void renderMenu(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		guiGraphics.fill(RenderPipelines.GUI, 0, 0, this.width, this.height, 0x88000000);

		LevelsMenuProvider.Snapshot snapshot = LevelsMenuAPIManager.snapshot();
		int panelX = panelX();
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

		boolean exitHovered = contains(exitX(panelX), panelY + EXIT_TOP_INSET, EXIT_SIZE, EXIT_SIZE, mouseX, mouseY);
		guiGraphics.blit(
			RenderPipelines.GUI_TEXTURED,
			exitHovered ? EXIT_HIGHLIGHTED_TEXTURE : EXIT_TEXTURE,
			exitX(panelX),
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
		drawScaledCenteredText(guiGraphics, experienceText, panelX + XP_BAR_X + XP_BAR_WIDTH / 2, experienceInfoY, INFO_TEXT_SCALE, SUBTEXT_COLOR);

		List<LevelsMenuProvider.Stat> stats = visibleStats(snapshot);
		for (int index = 0; index < stats.size(); index++) {
			LevelsMenuProvider.Stat stat = stats.get(index);
			Bounds entry = statBounds(panelX, panelY, index, stats.size());
			boolean active = canUpgrade(snapshot, stat);
			boolean hovered = statButtonBounds(panelX, panelY, index, stats.size()).contains(mouseX, mouseY);
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
		return switch (visibleStats(snapshot).size()) {
			case 5 -> BACKGROUND_FIVE_TEXTURE;
			case 6 -> BACKGROUND_SIX_TEXTURE;
			default -> BACKGROUND_FOUR_TEXTURE;
		};
	}

	private int panelY(LevelsMenuProvider.Snapshot snapshot) {
		return (this.height - panelHeight(snapshot)) / 2;
	}

	private void returnToMainMenu() {
		Minecraft.getInstance().setScreenAndShow(new MenuScreen());
	}

	private int panelHeight(LevelsMenuProvider.Snapshot snapshot) {
		return visibleStats(snapshot).size() >= 5 ? PANEL_HEIGHT_EXPANDED : PANEL_HEIGHT_FOUR;
	}

	private List<LevelsMenuProvider.Stat> visibleStats(LevelsMenuProvider.Snapshot snapshot) {
		if (!snapshot.hasData()) return List.of();
		return snapshot.stats().size() <= 6 ? snapshot.stats() : snapshot.stats().subList(0, 6);
	}

	private Bounds statBounds(int panelX, int panelY, int index, int statCount) {
		if (statCount == 5 && index == 4) {
			return new Bounds(panelX + STAT_FIFTH_ENTRY_X, panelY + STAT_ENTRY_TOP + (STAT_ENTRY_HEIGHT + STAT_ENTRY_ROW_GAP) * 2, STAT_ENTRY_WIDTH, STAT_ENTRY_HEIGHT);
		}
		int row = index / 2;
		int column = index % 2;
		return new Bounds(
			panelX + (column == 0 ? STAT_LEFT_ENTRY_X : STAT_RIGHT_ENTRY_X),
			panelY + STAT_ENTRY_TOP + row * (STAT_ENTRY_HEIGHT + STAT_ENTRY_ROW_GAP),
			STAT_ENTRY_WIDTH,
			STAT_ENTRY_HEIGHT
		);
	}

	private int headerNameY() {
		return HEADER_SEPARATOR_Y - HEADER_TEXT_GAP - scaledTextHeight(HEADER_TEXT_SCALE);
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

	private Bounds statButtonBounds(int panelX, int panelY, int index, int statCount) {
		Bounds entry = statBounds(panelX, panelY, index, statCount);
		return new Bounds(entry.x() + STAT_BUTTON_X, entry.y() + STAT_BUTTON_Y, EXIT_SIZE, EXIT_SIZE);
	}

	private boolean canUpgrade(LevelsMenuProvider.Snapshot snapshot, LevelsMenuProvider.Stat stat) {
		return snapshot.hasData() && snapshot.availablePoints() > 0 && stat.level() < stat.maxLevel();
	}

	private int experienceWidth(LevelsMenuProvider.Snapshot snapshot) {
		if (snapshot.requiredXp() <= 0) return 0;
		return Math.max(0, Math.min(XP_BAR_WIDTH, Math.round(snapshot.currentXp() / (float) snapshot.requiredXp() * XP_BAR_WIDTH)));
	}

	private int panelX() {
		return (this.width - PANEL_WIDTH) / 2;
	}

	private int exitX(int panelX) {
		return panelX + PANEL_WIDTH - EXIT_RIGHT_INSET - EXIT_SIZE;
	}

	private void drawScaledLeftText(GuiGraphicsExtractor guiGraphics, String text, int x, int y, float scale, int color) {
		if (text == null || text.isEmpty()) return;
		guiGraphics.pose().pushMatrix();
		guiGraphics.pose().scale(scale, scale);
		guiGraphics.text(this.font, text, Math.round(x / scale), Math.round(y / scale), color, false);
		guiGraphics.pose().popMatrix();
	}

	private void drawScaledRightText(GuiGraphicsExtractor guiGraphics, String text, int rightX, int y, float scale, int color) {
		drawScaledLeftText(guiGraphics, text, rightX - Math.round(this.font.width(text) * scale), y, scale, color);
	}

	private void drawScaledCenteredText(GuiGraphicsExtractor guiGraphics, String text, int centerX, int y, float scale, int color) {
		drawScaledLeftText(guiGraphics, text, Math.round(centerX - this.font.width(text) * scale / 2.0F), y, scale, color);
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
