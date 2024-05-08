package roomescape.web.controller;

import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import roomescape.core.dto.auth.TokenRequest;
import roomescape.core.dto.reservation.ReservationRequest;
import roomescape.core.dto.reservationtime.ReservationTimeRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {"spring.config.location = classpath:application-test.yml"})
class ReservationControllerTest {
    private static final String TOMORROW = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE);
    private static final String DAY_AFTER_TOMORROW = LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_DATE);
    private static final String EMAIL = "test@email.com";
    private static final String PASSWORD = "password";

    private String accessToken;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        accessToken = RestAssured
                .given().log().all()
                .body(new TokenRequest(EMAIL, PASSWORD))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when().post("/login")
                .then().log().cookies().extract().cookie("token");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "abc"})
    @DisplayName("예약 생성 시, date의 형식이 올바르지 않으면 예외가 발생한다.")
    void validateReservationWithDateFormat(final String date) {
        ReservationRequest request = new ReservationRequest(date, 1L, 1L);

        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("예약 생성 시, date가 이미 지난 날짜면 예외가 발생한다.")
    void validateReservationWithPastDate() {
        ReservationRequest request = new ReservationRequest("2020-10-10", 1L, 1L);

        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("예약 생성 시, date는 오늘이고 time은 이미 지난 시간이면 예외가 발생한다.")
    void validateReservationWithTodayPastTime() {
        ReservationTimeRequest timeRequest = new ReservationTimeRequest(
                LocalTime.now().minusMinutes(1).format(DateTimeFormatter.ofPattern("HH:mm")));

        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .contentType(ContentType.JSON)
                .body(timeRequest)
                .when().post("/times")
                .then().log().all()
                .statusCode(201);

        ReservationRequest reservationRequest = new ReservationRequest(
                LocalDate.now().format(DateTimeFormatter.ISO_DATE),
                4L, 1L);

        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .contentType(ContentType.JSON)
                .body(reservationRequest)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }


    @Test
    @DisplayName("예약 생성 시, timeId가 null이면 예외가 발생한다.")
    void validateReservationWithNullTimeId() {
        ReservationRequest request = new ReservationRequest(TOMORROW, null, 1L);

        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("예약 생성 시, timeId 값으로 찾을 수 있는 시간이 없으면 예외가 발생한다.")
    void validateReservationWithTimeIdNotFound() {
        ReservationRequest request = new ReservationRequest(TOMORROW, 4L, 1L);

        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("예약 생성 시, 해당 날짜와 시간에 예약 내역이 있으면 예외가 발생한다.")
    void validateReservationWithDuplicatedDateAndTime() {
        ReservationRequest request = new ReservationRequest(TOMORROW, 1L, 1L);

        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(201);

        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("예약 생성 시, themeId가 null이면 예외가 발생한다.")
    void validateReservationWithNullThemeId() {
        ReservationRequest request = new ReservationRequest(TOMORROW, 1L, null);

        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("예약 생성 시, themeId 값으로 찾을 수 있는 테마가 없으면 예외가 발생한다.")
    void validateReservationWithThemeIdNotFound() {
        ReservationRequest request = new ReservationRequest(TOMORROW, 1L, 0L);

        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("모든 예약 내역을 조회한다.")
    void findAllReservations() {
        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .when().get("/reservations")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @DisplayName("예약을 삭제한다.")
    void validateReservationDelete() {
        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .when().delete("/reservations/1")
                .then().log().all()
                .statusCode(204);
    }

    @Test
    @DisplayName("조건에 따라 예약을 조회한다.")
    void findReservationsByCondition() {
        ReservationRequest request = new ReservationRequest(TOMORROW, 1L, 1L);

        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(201);

        ReservationRequest request2 = new ReservationRequest(DAY_AFTER_TOMORROW, 1L, 1L);

        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .contentType(ContentType.JSON)
                .body(request2)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(201);

        RestAssured.given().log().all()
                .cookies("token", accessToken)
                .queryParams(
                        "member", 1L,
                        "theme", 1L,
                        "date-from", TOMORROW,
                        "date-to", DAY_AFTER_TOMORROW
                )
                .when().get("/reservations")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(2));
    }

    @Test
    @DisplayName("토큰이 유효하지 않을 경우 예외가 발생한다.")
    void validateToken() {
        ReservationRequest request = new ReservationRequest(TOMORROW, 1L, 1L);

        RestAssured.given().log().all()
                .cookies("token", "invalid-token")
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(401);
    }
}
