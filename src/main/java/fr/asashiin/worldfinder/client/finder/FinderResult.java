package fr.asashiin.worldfinder.client.finder;

public record FinderResult(String label, int blockX, int blockY, int blockZ, double distance, String note) {
	public String coordinates() {
		return blockX + " " + blockY + " " + blockZ;
	}
}
