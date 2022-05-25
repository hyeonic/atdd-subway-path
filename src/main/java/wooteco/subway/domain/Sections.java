package wooteco.subway.domain;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import wooteco.subway.domain.line.Line;

public class Sections {

    private final LinkedList<Section> value;

    public Sections(List<Section> value) {
        this.value = new LinkedList<>(value);
    }

    public void append(Section section) {
        validateSameSection(section);
        validateContainsStation(section);
        validateNotContainsStation(section);

        if (isUpTerminus(section)) {
            value.addFirst(section);
            return;
        }

        if (isDownTerminus(section)) {
            value.addLast(section);
            return;
        }

        decideForkedLoad(section);
    }

    private void validateSameSection(Section section) {
        if (value.stream().anyMatch(section::isSameUpAndDownStation)) {
            throw new IllegalArgumentException("구간이 노선에 이미 등록되어 추가할 수 없습니다.");
        }
    }

    private void validateContainsStation(Section section) {
        if (existsStation(section.getUpStation()) && existsStation(section.getDownStation())) {
            throw new IllegalArgumentException("상행역과 하행역 모두 노선에 포함되어 있으므로 추가할 수 없습니다.");
        }
    }

    private void validateNotContainsStation(Section section) {
        if (!existsStation(section.getUpStation()) && !existsStation(section.getDownStation())) {
            throw new IllegalArgumentException("상행역과 하행역이 모두 노선에 포함되지 않으므로 추가할 수 없습니다.");
        }
    }

    private boolean isUpTerminus(Section section) {
        return section.getDownStation().equals(getTerminusUpStation()) && !existsStation(section.getUpStation());
    }

    private boolean isDownTerminus(Section section) {
        return section.getUpStation().equals(getTerminusDownStation()) && !existsStation(section.getDownStation());
    }

    private void decideForkedLoad(Section section) {
        if (isFrontForkedLoad(section)) {
            changeFrontSection(section);
            return;
        }

        if (isBackForkedLoad(section)) {
            changeBackSection(section);
            return;
        }

        throw new IllegalArgumentException("새로운 구간의 길이가 기존 구간의 길이 보다 크거나 같으므로 추가할 수 없습니다.");
    }

    private boolean isFrontForkedLoad(Section section) {
        if (existsStation(section.getUpStation())) {
            Section frontSection = findByUpStation(section.getUpStation());
            return frontSection.getDistance() > section.getDistance();
        }

        return false;
    }

    private void changeFrontSection(Section section) {
        Section frontSection = findByUpStation(section.getUpStation());
        frontSection.changeUpStation(section);

        int frontIndex = value.indexOf(frontSection);
        value.remove(frontSection);
        value.add(frontIndex, section);
        value.add(frontIndex + 1, frontSection);
    }

    private boolean isBackForkedLoad(Section section) {
        if (existsStation(section.getDownStation())) {
            Section backSection = findByDownStation(section.getDownStation());
            return backSection.getDistance() > section.getDistance();
        }

        return false;
    }

    private void changeBackSection(Section section) {
        Section backSection = findByDownStation(section.getDownStation());
        backSection.changeDownStation(section);

        int backIndex = value.indexOf(backSection);
        value.add(backIndex + 1, section);
    }

    private boolean existsStation(Station station) {
        return value.stream()
                .anyMatch(section -> section.existsStation(station));
    }

    public void remove(Station station) {
        if (value.size() == 1) {
            throw new IllegalArgumentException("구간이 1개만 존재하므로 삭제가 불가능 합니다.");
        }

        if (getTerminusUpStation().equals(station)) {
            value.removeFirst();
            return;
        }

        if (getTerminusDownStation().equals(station)) {
            value.removeLast();
            return;
        }

        removeMiddleStation(station);
    }

    private Station getTerminusUpStation() {
        Section frontSection = value.getFirst();
        return frontSection.getUpStation();
    }

    private Station getTerminusDownStation() {
        Section backSection = value.getLast();
        return backSection.getDownStation();
    }

    private void removeMiddleStation(Station station) {
        Section frontSection = findByDownStation(station);
        Section backSection = findByUpStation(station);

        int frontIndex = value.indexOf(frontSection);

        Line line = frontSection.getLine();
        Station upStation = frontSection.getUpStation();
        Station downStation = backSection.getDownStation();
        int distance = frontSection.getDistance() + backSection.getDistance();

        value.remove(frontSection);
        value.remove(backSection);

        value.add(frontIndex, new Section(line, upStation, downStation, distance));
    }

    private Section findByUpStation(Station station) {
        return value.stream()
                .filter(section -> section.getUpStation().equals(station))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("일치하는 구간이 존재하지 않습니다."));
    }

    private Section findByDownStation(Station station) {
        return value.stream()
                .filter(section -> section.getDownStation().equals(station))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("일치하는 구간이 존재하지 않습니다."));
    }

    public List<Station> getStations() {
        List<Station> stations = value.stream()
                .map(Section::getUpStation)
                .collect(toList());

        Section lastSection = value.getLast();

        return Stream.concat(stations.stream(), Stream.of(lastSection.getDownStation()))
                .collect(toUnmodifiableList());
    }

    public List<Section> getValue() {
        return Collections.unmodifiableList(value);
    }
}
