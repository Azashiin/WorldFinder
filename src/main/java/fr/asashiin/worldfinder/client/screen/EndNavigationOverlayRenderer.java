package fr.asashiin.worldfinder.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;

final class EndNavigationOverlayRenderer {
	private static final int RADIUS_BLOCKS = 1000;
	private static final int RADIUS_FILL = 0x1822D3EE;
	private static final int RADIUS_OUTLINE = 0xAA67E8F9;

	private EndNavigationOverlayRenderer() {
	}

	static void draw(GuiGraphicsExtractor graphics, int mapLeft, int mapTop, int mapWidth, int mapHeight, int centerX, int centerY, double blockScale) {
		int radius = (int)Math.round(RADIUS_BLOCKS * blockScale);
		if (radius <= 0 || centerX + radius < mapLeft || centerX - radius > mapLeft + mapWidth
				|| centerY + radius < mapTop || centerY - radius > mapTop + mapHeight) {
			return;
		}
		drawFilledCircle(graphics, mapLeft, mapTop, mapWidth, mapHeight, centerX, centerY, radius);
		drawCircleOutline(graphics, mapLeft, mapTop, mapWidth, mapHeight, centerX, centerY, radius);
	}

	private static void drawFilledCircle(GuiGraphicsExtractor graphics, int mapLeft, int mapTop, int mapWidth, int mapHeight, int centerX, int centerY, int radius) {
		int minY = Math.max(mapTop, centerY - radius);
		int maxY = Math.min(mapTop + mapHeight, centerY + radius + 1);
		int step = radius > 180 ? 2 : 1;
		double radiusSquared = (double)radius * radius;
		for (int y = minY; y < maxY; y += step) {
			double dy = y + step * 0.5D - centerY;
			double halfSquared = radiusSquared - dy * dy;
			if (halfSquared < 0.0D) {
				continue;
			}
			int halfWidth = (int)Math.floor(Math.sqrt(halfSquared));
			int left = Math.max(mapLeft, centerX - halfWidth);
			int right = Math.min(mapLeft + mapWidth, centerX + halfWidth + 1);
			if (right > left) {
				graphics.fill(left, y, right, Math.min(maxY, y + step), RADIUS_FILL);
			}
		}
	}

	private static void drawCircleOutline(GuiGraphicsExtractor graphics, int mapLeft, int mapTop, int mapWidth, int mapHeight, int centerX, int centerY, int radius) {
		int samples = Math.max(96, Math.min(1440, (int)Math.ceil(Math.PI * radius)));
		for (int i = 0; i < samples; i++) {
			double angle = i * Math.PI * 2.0D / samples;
			int x = centerX + (int)Math.round(Math.cos(angle) * radius);
			int y = centerY + (int)Math.round(Math.sin(angle) * radius);
			if (x >= mapLeft && x < mapLeft + mapWidth && y >= mapTop && y < mapTop + mapHeight) {
				graphics.fill(x, y, x + 1, y + 1, RADIUS_OUTLINE);
			}
		}
	}
}
