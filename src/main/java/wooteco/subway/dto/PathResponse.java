package wooteco.subway.dto;

import java.util.List;

public class PathResponse {

    private List<StationResponse> stations;
    private int distance;

    private PathResponse() {
    }

    public PathResponse(List<StationResponse> stations, int distance) {
        this.stations = stations;
        this.distance = distance;
    }

    public List<StationResponse> getStations() {
        return stations;
    }

    public int getDistance() {
        return distance;
    }
}
