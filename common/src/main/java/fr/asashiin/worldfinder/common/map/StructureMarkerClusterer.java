package fr.asashiin.worldfinder.common.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Groups nearby structure markers without depending on the viewport's pan offset. */
public final class StructureMarkerClusterer {
    private StructureMarkerClusterer() {
    }

    /**
     * Groups markers around nearby, incrementally updated centroids.
     *
     * <p>Input markers are processed in a deterministic coordinate and identifier order. Each
     * marker joins the nearest cluster whose current centre is within {@code radiusBlocks}, or
     * starts a new cluster when none is close enough. The returned list and each cluster's marker
     * list are immutable.</p>
     *
     * @param markers non-null markers to group
     * @param radiusBlocks positive, finite maximum distance to a cluster centre
     * @return immutable clusters; empty when {@code markers} is empty
     * @throws NullPointerException if the marker list or one of its elements is {@code null}
     * @throws IllegalArgumentException if {@code radiusBlocks} is non-finite or not positive
     */
    public static List<Cluster> cluster(List<StructureMarker> markers, double radiusBlocks) {
        Objects.requireNonNull(markers, "markers");
        if (!Double.isFinite(radiusBlocks) || radiusBlocks <= 0.0D) {
            throw new IllegalArgumentException("Cluster radius must be finite and positive");
        }
        if (markers.isEmpty()) return List.of();

        List<StructureMarker> orderedMarkers = new ArrayList<>(markers);
        orderedMarkers.sort(Comparator.comparingInt((StructureMarker marker) -> marker.position().blockX())
                .thenComparingInt(marker -> marker.position().blockZ())
                .thenComparing(StructureMarker::structureId)
                .thenComparing(StructureMarker::label)
                .thenComparing(StructureMarker::blockY, Comparator.nullsLast(Integer::compareTo)));
        List<MutableCluster> clusters = new ArrayList<>();
        Map<Cell, List<Integer>> cells = new HashMap<>();
        double radiusSquared = radiusBlocks * radiusBlocks;

        for (StructureMarker marker : orderedMarkers) {
            long cellX = cellCoordinate(marker.position().blockX(), radiusBlocks);
            long cellZ = cellCoordinate(marker.position().blockZ(), radiusBlocks);
            int nearestCluster = -1;
            double nearestDistanceSquared = Double.POSITIVE_INFINITY;
            for (long offsetZ = -1; offsetZ <= 1; offsetZ++) {
                for (long offsetX = -1; offsetX <= 1; offsetX++) {
                    List<Integer> candidates = cells.get(new Cell(cellX + offsetX, cellZ + offsetZ));
                    if (candidates == null) continue;
                    for (int clusterIndex : candidates) {
                        MutableCluster candidate = clusters.get(clusterIndex);
                        double deltaX = marker.position().blockX() - candidate.centerX();
                        double deltaZ = marker.position().blockZ() - candidate.centerZ();
                        double distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
                        if (distanceSquared <= radiusSquared && distanceSquared < nearestDistanceSquared) {
                            nearestCluster = clusterIndex;
                            nearestDistanceSquared = distanceSquared;
                        }
                    }
                }
            }
            if (nearestCluster >= 0) {
                MutableCluster cluster = clusters.get(nearestCluster);
                Cell previousCell = new Cell(cellCoordinate(cluster.centerX(), radiusBlocks),
                        cellCoordinate(cluster.centerZ(), radiusBlocks));
                cluster.add(marker);
                Cell updatedCell = new Cell(cellCoordinate(cluster.centerX(), radiusBlocks),
                        cellCoordinate(cluster.centerZ(), radiusBlocks));
                if (!previousCell.equals(updatedCell)) {
                    List<Integer> previousCellClusters = cells.get(previousCell);
                    if (previousCellClusters != null) {
                        previousCellClusters.remove(Integer.valueOf(nearestCluster));
                        if (previousCellClusters.isEmpty()) cells.remove(previousCell);
                    }
                    cells.computeIfAbsent(updatedCell, ignored -> new ArrayList<>()).add(nearestCluster);
                }
            } else {
                int clusterIndex = clusters.size();
                clusters.add(new MutableCluster(marker));
                cells.computeIfAbsent(new Cell(cellX, cellZ), ignored -> new ArrayList<>()).add(clusterIndex);
            }
        }
        List<Cluster> result = new ArrayList<>(clusters.size());
        for (MutableCluster cluster : clusters) {
            result.add(new Cluster(List.copyOf(cluster.markers()), cluster.centerX(), cluster.centerZ()));
        }
        return List.copyOf(result);
    }

    /**
     * Removes visually overlapping clusters while favouring clusters with more markers.
     *
     * <p>Clusters are considered in descending marker count, then centre-coordinate order. A
     * candidate is hidden when its centre is strictly closer than {@code minimumDistanceBlocks}
     * to a retained cluster.</p>
     *
     * @param clusters non-null clusters to filter
     * @param minimumDistanceBlocks positive, finite minimum distance between retained centres
     * @return immutable list of retained clusters
     * @throws NullPointerException if {@code clusters} is {@code null}
     * @throws IllegalArgumentException if {@code minimumDistanceBlocks} is non-finite or not positive
     */
    public static List<Cluster> declutter(List<Cluster> clusters, double minimumDistanceBlocks) {
        Objects.requireNonNull(clusters, "clusters");
        if (!Double.isFinite(minimumDistanceBlocks) || minimumDistanceBlocks <= 0.0D) {
            throw new IllegalArgumentException("Declutter distance must be finite and positive");
        }
        if (clusters.isEmpty()) return List.of();

        List<Cluster> prioritized = new ArrayList<>(clusters);
        prioritized.sort(Comparator.comparingInt((Cluster cluster) -> cluster.markers().size()).reversed()
                .thenComparingDouble(Cluster::centerX)
                .thenComparingDouble(Cluster::centerZ));
        List<Cluster> kept = new ArrayList<>();
        Map<Cell, List<Integer>> cells = new HashMap<>();
        double minimumDistanceSquared = minimumDistanceBlocks * minimumDistanceBlocks;

        for (Cluster cluster : prioritized) {
            long cellX = cellCoordinate(cluster.centerX(), minimumDistanceBlocks);
            long cellZ = cellCoordinate(cluster.centerZ(), minimumDistanceBlocks);
            boolean hidden = false;
            for (long offsetZ = -1; offsetZ <= 1 && !hidden; offsetZ++) {
                for (long offsetX = -1; offsetX <= 1 && !hidden; offsetX++) {
                    List<Integer> neighbours = cells.get(new Cell(cellX + offsetX, cellZ + offsetZ));
                    if (neighbours == null) continue;
                    for (int neighbourIndex : neighbours) {
                        Cluster neighbour = kept.get(neighbourIndex);
                        double deltaX = cluster.centerX() - neighbour.centerX();
                        double deltaZ = cluster.centerZ() - neighbour.centerZ();
                        if (deltaX * deltaX + deltaZ * deltaZ < minimumDistanceSquared) {
                            hidden = true;
                            break;
                        }
                    }
                }
            }
            if (!hidden) {
                int keptIndex = kept.size();
                kept.add(cluster);
                cells.computeIfAbsent(new Cell(cellX, cellZ), ignored -> new ArrayList<>()).add(keptIndex);
            }
        }
        return List.copyOf(kept);
    }

    private static long cellCoordinate(double blockCoordinate, double radiusBlocks) {
        return (long)Math.floor(blockCoordinate / radiusBlocks);
    }

    private record Cell(long x, long z) {
    }

    private static final class MutableCluster {
        private final List<StructureMarker> markers = new ArrayList<>();
        private double sumX;
        private double sumZ;

        private MutableCluster(StructureMarker marker) {
            add(marker);
        }

        private void add(StructureMarker marker) {
            markers.add(marker);
            sumX += marker.position().blockX();
            sumZ += marker.position().blockZ();
        }

        private List<StructureMarker> markers() {
            return markers;
        }

        private double centerX() {
            return sumX / markers.size();
        }

        private double centerZ() {
            return sumZ / markers.size();
        }
    }

    /**
     * Immutable structure marker group and its horizontal centroid.
     *
     * @param markers non-empty marker list
     * @param centerX centroid block X coordinate
     * @param centerZ centroid block Z coordinate
     */
    public record Cluster(List<StructureMarker> markers, double centerX, double centerZ) {
        /**
         * Creates a cluster and snapshots its marker list.
         *
         * @throws NullPointerException if {@code markers} or one of its elements is {@code null}
         * @throws IllegalArgumentException if {@code markers} is empty
         */
        public Cluster {
            markers = List.copyOf(markers);
            if (markers.isEmpty()) throw new IllegalArgumentException("A structure cluster cannot be empty");
        }
    }
}
