package wooteco.subway.service;

import static java.util.stream.Collectors.toList;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wooteco.subway.dao.SectionDao;
import wooteco.subway.domain.Fare;
import wooteco.subway.domain.Line;
import wooteco.subway.domain.Path;
import wooteco.subway.domain.Section;
import wooteco.subway.domain.Station;
import wooteco.subway.dto.PathResponse;
import wooteco.subway.dto.StationResponse;

@Transactional(readOnly = true)
@Service
public class PathService {

    private static final int DEFAULT_EXTRA_FARE = 0;
    private final SectionDao sectionDao;
    private final StationService stationService;

    public PathService(SectionDao sectionDao, StationService stationService) {
        this.sectionDao = sectionDao;
        this.stationService = stationService;
    }

    public PathResponse findPath(Long sourceId, Long targetId, int age) {
        Station source = stationService.findById(sourceId).toStation();
        Station target = stationService.findById(targetId).toStation();

        Path shortestPath = new Path(sectionDao.findAll());
        List<StationResponse> stationResponses = convertToStationResponse(source, target, shortestPath);

        int shortestDistance = shortestPath.getDistance(source, target);
        Fare fare = new Fare(shortestDistance, age, getMaxExtraFare(source, target, shortestPath));

        return new PathResponse(stationResponses, shortestDistance, fare.calculateFare());
    }

    private int getMaxExtraFare(Station source, Station target, Path shortestPath) {
        List<Section> sections = shortestPath.getSections(source, target);
        return sections.stream()
                .map(Section::getLine)
                .mapToInt(Line::getExtraFare)
                .max()
                .orElse(DEFAULT_EXTRA_FARE);
    }

    private List<StationResponse> convertToStationResponse(Station source, Station target, Path shortestPath) {
        return shortestPath.getStations(source, target)
                .stream()
                .map(StationResponse::new)
                .collect(toList());
    }
}
