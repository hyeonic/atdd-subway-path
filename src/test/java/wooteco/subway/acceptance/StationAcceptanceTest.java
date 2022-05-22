package wooteco.subway.acceptance;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import wooteco.subway.dto.StationRequest;
import wooteco.subway.dto.StationResponse;

public class StationAcceptanceTest extends AcceptanceTest {

    @DisplayName("지하철역 관리 기능을 확인한다.")
    @TestFactory
    Stream<DynamicTest> dynamicTestFromStation() {
        return Stream.of(
                dynamicTest("이름을 활용하여 지하철역을 생성한다.", () -> {
                    ExtractableResponse<Response> response = generateStation("강남역");

                    assertAll(
                            () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value()),
                            () -> assertThat(response.header("Location")).isNotBlank()
                    );
                }),

                dynamicTest("기존에 존재하는 지하철역 이름으로 지하철역을 생성한다.", () -> {
                    ExtractableResponse<Response> response = generateStation("강남역");

                    assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                }),

                dynamicTest("지하철역 목록을 조회한다.", () -> {
                    generateStation("역삼역");

                    ExtractableResponse<Response> response = RestAssured.given().log().all()
                            .when()
                            .get("/stations")
                            .then().log().all()
                            .extract();

                    List<StationResponse> stations = response.jsonPath().getList(".", StationResponse.class);
                    assertAll(
                            () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                            () -> assertThat(stations.size()).isEqualTo(2)
                    );
                }),

                dynamicTest("지하철역을 제거한다.", () -> {
                    ExtractableResponse<Response> response = RestAssured.given().log().all()
                            .when()
                            .get("/stations")
                            .then().log().all()
                            .extract();

                    List<Long> stationIds = response.jsonPath()
                            .getList(".", StationResponse.class)
                            .stream()
                            .map(StationResponse::getId)
                            .collect(toList());

                    List<Integer> statusCodes = stationIds.stream()
                            .map(this::deleteStation)
                            .map(ExtractableResponse::statusCode)
                            .collect(toList());
                    List<Integer> expectedStatusCodes = IntStream.range(0, stationIds.size())
                            .mapToObj(ignored -> HttpStatus.NO_CONTENT.value())
                            .collect(toList());
                    assertThat(statusCodes).containsAll(expectedStatusCodes);
                })
        );
    }

    private ExtractableResponse<Response> generateStation(String name) {
        return RestAssured.given().log().all()
                .body(new StationRequest(name))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/stations")
                .then().log().all()
                .extract();
    }

    private ExtractableResponse<Response> deleteStation(Long id) {
        return RestAssured.given().log().all()
                .when()
                .delete("/stations/" + id)
                .then().log().all()
                .extract();
    }
}
