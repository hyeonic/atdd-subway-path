package wooteco.subway.service;

import static java.util.stream.Collectors.toList;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wooteco.subway.dao.SectionDao;
import wooteco.subway.domain.Fare;
import wooteco.subway.domain.Path;
import wooteco.subway.domain.Section;
import wooteco.subway.domain.Sections;
import wooteco.subway.domain.Station;
import wooteco.subway.dto.PathResponse;
import wooteco.subway.dto.StationResponse;

@Transactional(readOnly = true)
@Service
public class PathService {

    private final SectionDao sectionDao;
    private final StationService stationService;

    public PathService(SectionDao sectionDao, StationService stationService) {
        this.sectionDao = sectionDao;
        this.stationService = stationService;
    }

    public PathResponse findPath(Long sourceId, Long targetId, int age) {
        Station source = stationService.findById(sourceId).toStation();
        Station target = stationService.findById(targetId).toStation();

        List<Section> sections = sectionDao.findAll();
        Path path = new Path(new Sections(sections));

        List<StationResponse> stations = path.getVertexes(source, target)
                .stream()
                .map(StationResponse::new)
                .collect(toList());

        Fare fare = new Fare(path.getWeight(source, target));
        return new PathResponse(stations, path.getWeight(source, target), fare.calculate());
    }
}
