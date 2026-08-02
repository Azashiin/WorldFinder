package fr.asashiin.worldfinder.common.map;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructureMarkerClustererTest {
    @Test
    void groupsNearbyMarkersAndKeepsDistantMarkersSeparate() {
        StructureMarker first = marker("village", 0, 0);
        StructureMarker second = marker("stronghold", 12, 5);
        StructureMarker distant = marker("monument", 80, 0);

        List<StructureMarkerClusterer.Cluster> clusters =
                StructureMarkerClusterer.cluster(List.of(first, second, distant), 24.0D);

        assertEquals(2, clusters.size());
        assertEquals(List.of(first, second), clusters.getFirst().markers());
        assertEquals(6.0D, clusters.getFirst().centerX());
        assertEquals(2.5D, clusters.getFirst().centerZ());
        assertEquals(List.of(distant), clusters.getLast().markers());
    }

    @Test
    void doesNotMergeGroupsThroughTransitiveMarkerChains() {
        List<StructureMarkerClusterer.Cluster> clusters = StructureMarkerClusterer.cluster(List.of(
                marker("first", -20, 0),
                marker("second", 0, 0),
                marker("third", 20, 0)
        ), 21.0D);

        assertEquals(2, clusters.size());
        assertEquals(List.of("first", "second"), clusters.getFirst().markers().stream()
                .map(StructureMarker::structureId).toList());
        assertEquals(List.of("third"), clusters.getLast().markers().stream()
                .map(StructureMarker::structureId).toList());
    }

    @Test
    void addsAnIconCloseToTheGroupsEvolvingCenter() {
        List<StructureMarkerClusterer.Cluster> clusters = StructureMarkerClusterer.cluster(List.of(
                marker("first", 0, 0),
                marker("second", 4, 0),
                marker("third", 6, 0)
        ), 4.0D);

        assertEquals(1, clusters.size());
        assertEquals(3, clusters.getFirst().markers().size());
        assertEquals(10.0D / 3.0D, clusters.getFirst().centerX());
    }

    @Test
    void producesTheSameGroupsRegardlessOfScanArrivalOrder() {
        StructureMarker first = marker("first", 0, 0);
        StructureMarker second = marker("second", 2, 0);
        StructureMarker third = marker("third", 4, 0);

        List<StructureMarkerClusterer.Cluster> forwards =
                StructureMarkerClusterer.cluster(List.of(first, second, third), 2.0D);
        List<StructureMarkerClusterer.Cluster> backwards =
                StructureMarkerClusterer.cluster(List.of(third, second, first), 2.0D);

        assertEquals(forwards, backwards);
        assertEquals(2, forwards.size());
    }

    @Test
    void hidesVisuallyCloseItemsAndKeepsTheLargestGroup() {
        List<StructureMarkerClusterer.Cluster> clusters = StructureMarkerClusterer.cluster(List.of(
                marker("single", 0, 0),
                marker("group-a", 10, 0),
                marker("group-b", 11, 0),
                marker("distant", 50, 0)
        ), 2.0D);

        List<StructureMarkerClusterer.Cluster> visible = StructureMarkerClusterer.declutter(clusters, 20.0D);

        assertEquals(2, visible.size());
        assertEquals(2, visible.getFirst().markers().size());
        assertEquals(List.of("distant"), visible.getLast().markers().stream()
                .map(StructureMarker::structureId).toList());
    }

    @Test
    void rejectsInvalidRadius() {
        assertThrows(IllegalArgumentException.class,
                () -> StructureMarkerClusterer.cluster(List.of(marker("village", 0, 0)), 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> StructureMarkerClusterer.declutter(List.of(), 0.0D));
    }

    private static StructureMarker marker(String id, int blockX, int blockZ) {
        return new StructureMarker(id, id, new MapPosition(blockX, blockZ));
    }
}
