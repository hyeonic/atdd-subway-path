package wooteco.subway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import wooteco.subway.dao.StationDao;
import wooteco.subway.domain.Station;
import wooteco.subway.dto.LineRequest;
import wooteco.subway.dto.LineResponse;
import wooteco.subway.dto.LineUpdateRequest;
import wooteco.subway.dto.SectionRequest;
import wooteco.subway.dto.StationResponse;
import wooteco.subway.error.exception.NotFoundException;

@SpringBootTest
@Sql("/truncate.sql")
class LineServiceTest {

    private final LineService lineService;
    private final StationDao stationDao;

    @Autowired
    public LineServiceTest(LineService lineService, StationDao stationDao) {
        this.lineService = lineService;
        this.stationDao = stationDao;
    }

    @DisplayName("노선 저장과 관련된 기능")
    @TestFactory
    Stream<DynamicTest> dynamicTestFromSvaeStation() {
        Station 선릉역 = generateStation("선릉역");
        Station 잠실역 = generateStation("잠실역");

        return Stream.of(
                dynamicTest("노선을 저장한다.", () -> {
                    String name = "2호선";
                    String color = "bg-green-600";
                    LineRequest lineRequest = new LineRequest(
                            name, color, 선릉역.getId(), 잠실역.getId(), 10, 0);

                    LineResponse lineResponse = lineService.save(lineRequest);

                    assertAll(
                            () -> assertThat(lineResponse.getName()).isEqualTo(name),
                            () -> assertThat(lineResponse.getColor()).isEqualTo(color),
                            () -> assertThat(lineResponse.getStations().size()).isEqualTo(2)
                    );
                }),

                dynamicTest("중복된 이름의 노선을 저장할 경우 예외를 발생시킨다.", () -> {
                    String name = "2호선";
                    String color = "bg-green-600";
                    LineRequest lineRequest = new LineRequest(
                            name, color, 선릉역.getId(), 잠실역.getId(), 10, 0);

                    assertThatThrownBy(() -> lineService.save(lineRequest))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage(name + "은 이미 존재하는 노선 이름입니다.");
                }),

                dynamicTest("노선의 저장 시 상행과 하행이 같은 지하철역인 경우 예외를 던진다.", () -> {
                    Station 중동역 = generateStation("중동역");
                    String name = "3호선";
                    String color = "bg-green-600";
                    LineRequest lineRequest = new LineRequest(
                            name, color, 중동역.getId(), 중동역.getId(), 10, 0);

                    assertThatThrownBy(() -> lineService.save(lineRequest))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("상행과 하행은 같을 수 없습니다.");
                })
        );
    }

    @DisplayName("존재하지 않는 지하철역을 활용하여 노선을 등록할 경우 예외를 던진다.")
    @Test
    void 존재하지_않는_지하철역으로_노선_등록_예외발생() {
        LineRequest lineRequest = new LineRequest(
                "2호선", "bg-green-600", 1L, 2L, 10, 0);

        assertThatThrownBy(() -> lineService.save(lineRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(lineRequest.getUpStationId() + "의 지하철역은 존재하지 않습니다.");
    }

    @DisplayName("노선 조작과 관련된 기능")
    @TestFactory
    Stream<DynamicTest> dynamicTestFromStation() {
        Station 선릉역 = generateStation("선릉역");
        Station 잠실역 = generateStation("잠실역");

        String name1 = "2호선";
        String color1 = "bg-green-600";
        int distance1 = 7;
        LineResponse lineResponse1 = lineService.save(
                new LineRequest(name1, color1, 선릉역.getId(), 잠실역.getId(), distance1, 900));

        Station 중동역 = generateStation("중동역");
        Station 신도림역 = generateStation("신도림역");

        String name2 = "1호선";
        String color2 = "bg-blue-600";
        int distance2 = 10;
        LineResponse lineResponse2 = lineService.save(
                new LineRequest(name2, color2, 중동역.getId(), 신도림역.getId(), distance2, 0));

        return Stream.of(
                dynamicTest("노선을 조회한다.", () -> {
                    assertAll(
                            () -> assertThat(lineResponse1.getName()).isEqualTo(name1),
                            () -> assertThat(lineResponse1.getColor()).isEqualTo(color1)
                    );
                }),

                dynamicTest("다수의 노선을 조회한다.", () -> {
                    List<LineResponse> lines = lineService.findAll();

                    assertThat(lines.size()).isEqualTo(2);
                }),

                dynamicTest("존재하는 노선을 수정한다.", () -> {
                    String updateColor = "bg-blue-600";
                    LineUpdateRequest updateRequest = new LineUpdateRequest(name1, updateColor, 0);
                    lineService.update(lineResponse1.getId(), updateRequest);

                    LineResponse updatedResponse = lineService.findById(lineResponse1.getId());
                    assertAll(
                            () -> assertThat(updatedResponse.getName()).isEqualTo(name1),
                            () -> assertThat(updatedResponse.getColor()).isEqualTo(updateColor)
                    );
                }),

                dynamicTest("중복된 이름을 가진 노선으로 수정할 경우 예외를 던진다.", () -> {
                    String updateName = "2호선";
                    String updateColor = "bg-green-600";
                    LineUpdateRequest updateRequest = new LineUpdateRequest(updateName, updateColor, 0);

                    assertThatThrownBy(() -> lineService.update(lineResponse2.getId(), updateRequest))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage(updateName + "은 이미 존재하는 노선 이름입니다.");
                }),

                dynamicTest("노선을 삭제한다.", () -> {
                    lineService.deleteById(lineResponse1.getId());
                    lineService.deleteById(lineResponse2.getId());

                    assertThat(lineService.findAll().size()).isEqualTo(0);
                })
        );
    }

    @DisplayName("존재하지 않는 노선을 조회할 경우 예외를 발생시킨다.")
    @Test
    void 존재하지_않는_노선_조회_예외발생() {
        assertThatThrownBy(() -> lineService.findById(0L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(0 + "의 노선은 존재하지 않습니다.");
    }

    @DisplayName("존재하지 않는 노선을 수정하는 경우 예외를 던진다.")
    @Test
    void 존재하지_않는_노선_수정_예외발생() {
        String name = "2호선";
        String updateColor = "bg-blue-600";
        LineUpdateRequest updateRequest = new LineUpdateRequest(name, updateColor, 0);

        assertThatThrownBy(() -> lineService.update(0L, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(0 + "의 노선은 존재하지 않습니다.");
    }

    @DisplayName("존재하지 않는 노선을 삭제하는 경우 예외를 던진다.")
    @Test
    void 존재하지_않는_노선_삭제_예외발생() {
        assertThatThrownBy(() -> lineService.deleteById(0L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(0 + "의 노선은 존재하지 않습니다.");
    }

    @DisplayName("노선에 구간을 추가하는 기능을 확인한다.")
    @TestFactory
    Stream<DynamicTest> dynamicTestFromAddSection() {
        Long 신도림역Id = generateStation("신도림역").getId();
        Long 중동역Id = generateStation("중동역").getId();
        int basedDistance = 10;

        String name = "1호선";
        String color = "bg-blue-600";
        LineResponse line = generateLine(name, color, 신도림역Id, 중동역Id, basedDistance, 0);

        return Stream.of(
                dynamicTest("상행 종점이 같은 경우 가장 앞단의 구간 보다 길이가 크거나 같으면 예외를 던진다.", () -> {
                    Long 온수역Id = generateStation("온수역").getId();

                    SectionRequest sectionRequest = new SectionRequest(신도림역Id, 온수역Id, 10);
                    assertThatThrownBy(() -> lineService.addSection(line.getId(), sectionRequest))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("새로운 구간의 길이가 기존 구간의 길이 보다 크거나 같으므로 추가할 수 없습니다.");
                }),

                dynamicTest("상행 종점이 같은 경우 가장 앞단의 구간 보다 길이가 작으면 추가한다.", () -> {
                    Long 개봉역Id = generateStation("개봉역").getId();

                    SectionRequest sectionRequest = new SectionRequest(신도림역Id, 개봉역Id, 4);
                    assertDoesNotThrow(() -> lineService.addSection(line.getId(), sectionRequest));
                }),

                dynamicTest("상행 종점에 구간을 추가한다.", () -> {
                    Long 영등포역Id = generateStation("영등포역").getId();

                    SectionRequest sectionRequest = new SectionRequest(영등포역Id, 신도림역Id, 7);
                    assertDoesNotThrow(() -> lineService.addSection(line.getId(), sectionRequest));
                }),

                dynamicTest("노선 조회 시 등록된 지하철 목록을 확인할 수 있다.", () -> {
                    LineResponse response = lineService.findById(line.getId());

                    List<StationResponse> stations = response.getStations();

                    assertThat(stations.size()).isEqualTo(4);
                }),

                dynamicTest("상행 종점 추가 시 지하철이 존재하지 않는 경우 예외를 던진다.", () -> {
                    SectionRequest sectionRequest = new SectionRequest(0L, 신도림역Id, 7);

                    assertThatThrownBy(() -> lineService.addSection(line.getId(), sectionRequest))
                            .isInstanceOf(NotFoundException.class)
                            .hasMessage("0의 지하철역은 존재하지 않습니다.");
                }),

                dynamicTest("상행 종점 추가 시 상행역이 기존 노선에 존재하는 경우 예외를 던진다.", () -> {
                    SectionRequest sectionRequest = new SectionRequest(중동역Id, 신도림역Id, 7);

                    assertThatThrownBy(() -> lineService.addSection(line.getId(), sectionRequest))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("상행역과 하행역 모두 노선에 포함되어 있으므로 추가할 수 없습니다.");
                }),

                dynamicTest("하행 종점이 같은 경우 가장 뒷간의 구간 보다 길이가 크거나 같으면 예외를 던진다.", () -> {
                    Long upStationId = generateStation("역곡역").getId();

                    SectionRequest sectionRequest = new SectionRequest(upStationId, 중동역Id, 10);

                    assertThatThrownBy(() -> lineService.addSection(line.getId(), sectionRequest))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("새로운 구간의 길이가 기존 구간의 길이 보다 크거나 같으므로 추가할 수 없습니다.");
                }),

                dynamicTest("하행 종점이 같은 경우 가장 앞단의 구간보다 길이가 작으면 추가한다.", () -> {
                    Long upStationId = generateStation("부천역").getId();

                    SectionRequest sectionRequest = new SectionRequest(upStationId, 중동역Id, 3);

                    assertDoesNotThrow(() -> lineService.addSection(line.getId(), sectionRequest));
                }),

                dynamicTest("하행 종점에 구간을 추가한다.", () -> {
                    Long downStationId = generateStation("부평역").getId();

                    SectionRequest sectionRequest = new SectionRequest(중동역Id, downStationId, 10);

                    assertDoesNotThrow(() -> lineService.addSection(line.getId(), sectionRequest));
                }),

                dynamicTest("상행역과 하행역이 노선에 모두 존재하면 예외를 던진다.", () -> {
                    SectionRequest sectionRequest = new SectionRequest(신도림역Id, 중동역Id, 1);

                    assertThatThrownBy(() -> lineService.addSection(line.getId(), sectionRequest))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("상행역과 하행역 모두 노선에 포함되어 있으므로 추가할 수 없습니다.");
                }),

                dynamicTest("상행역과 하행역이 노선에 모두 존재하지 않으면 예외를 던진다.", () -> {
                    Long upStationId = generateStation("서울역").getId();
                    Long downStationId = generateStation("노량진역").getId();

                    SectionRequest sectionRequest = new SectionRequest(upStationId, downStationId, 1);

                    assertThatThrownBy(() -> lineService.addSection(line.getId(), sectionRequest))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("상행역과 하행역이 모두 노선에 포함되지 않으므로 추가할 수 없습니다.");
                })
        );
    }

    @DisplayName("구간 삭제 기능")
    @TestFactory
    Stream<DynamicTest> dynamicTestRemoveSection() {
        Long 신도림역 = generateStation("신도림역").getId();
        Long 온수역 = generateStation("온수역").getId();
        Long 역곡역 = generateStation("역곡역").getId();
        Long 부천역 = generateStation("부천역").getId();
        Long 중동역 = generateStation("중동역").getId();

        LineResponse response = generateLine("1호선", "bg-blue-600", 신도림역, 온수역, 10, 0);
        Long lineId = response.getId();

        lineService.addSection(lineId, new SectionRequest(온수역, 역곡역, 10));
        lineService.addSection(lineId, new SectionRequest(역곡역, 부천역, 10));
        lineService.addSection(lineId, new SectionRequest(부천역, 중동역, 10));

        return Stream.of(
                dynamicTest("중간에 위치한 역을 삭제한다.", () -> {
                    assertDoesNotThrow(() -> lineService.deleteSection(lineId, 온수역));
                }),

                dynamicTest("상행 종점의 구간을 삭제한다.", () -> {
                    assertDoesNotThrow(() -> lineService.deleteSection(lineId, 신도림역));
                }),

                dynamicTest("존재하지 않는 역을 삭제할 경우 예외를 던진다.", () -> {
                    assertThatThrownBy(() -> lineService.deleteSection(lineId, 신도림역))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("일치하는 구간이 존재하지 않습니다.");
                }),

                dynamicTest("하행 종점의 구간을 삭제한다.", () -> {
                    assertDoesNotThrow(() -> lineService.deleteSection(lineId, 중동역));
                }),

                dynamicTest("구간이 한개 뿐인 경우 예외를 던진다.", () -> {
                    assertThatThrownBy(() -> lineService.deleteSection(lineId, 온수역))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("구간이 1개만 존재하므로 삭제가 불가능 합니다.");
                })
        );
    }

    private LineResponse generateLine(
            String name, String color, Long upStationId, Long downStationId, int distance, int extraFare) {
        return lineService.save(new LineRequest(name, color, upStationId, downStationId, distance, extraFare));
    }

    private Station generateStation(String name) {
        return stationDao.save(new Station(name));
    }
}
