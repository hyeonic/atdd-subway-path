package wooteco.subway.domain;

import java.util.List;
import java.util.Objects;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.WeightedMultigraph;

public class Path {

    private final DijkstraShortestPath dijkstraShortestPath;

    public Path(Sections sections) {
        WeightedMultigraph<Station, DefaultWeightedEdge> graph = new WeightedMultigraph(DefaultWeightedEdge.class);
        fillVertexes(graph, sections.getStations());
        fillEdges(graph, sections.getValue());

        this.dijkstraShortestPath = new DijkstraShortestPath(graph);
    }

    private void fillVertexes(WeightedMultigraph<Station, DefaultWeightedEdge> graph, List<Station> stations) {
        for (Station station : stations) {
            graph.addVertex(station);
        }
    }

    private void fillEdges(WeightedMultigraph<Station, DefaultWeightedEdge> graph, List<Section> sections) {
        for (Section section : sections) {
            graph.setEdgeWeight(graph.addEdge(section.getUpStation(), section.getDownStation()), section.getDistance());
        }
    }

    public List<Station> getVertexes(Station source, Station target) {
        GraphPath<Station, DefaultWeightedEdge> shortestPath = getShortestPath(source, target);

        return shortestPath.getVertexList();
    }

    private GraphPath getPath(Station source, Station target, DijkstraShortestPath dijkstraShortestPath) {
        try {
            return dijkstraShortestPath.getPath(source, target);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("노선에 등록되지 않는 지하철역입니다.");
        }
    }

    public int getWeight(Station source, Station target) {
        GraphPath<Station, DefaultWeightedEdge> shortestPath = getShortestPath(source, target);

        return (int) shortestPath.getWeight();
    }

    private GraphPath<Station, DefaultWeightedEdge> getShortestPath(Station source, Station target) {
        GraphPath<Station, DefaultWeightedEdge> shortestPath = getPath(source, target, dijkstraShortestPath);

        if (Objects.isNull(shortestPath)) {
            throw new IllegalArgumentException("경로가 존재하지 않습니다.");
        }

        return shortestPath;
    }
}
