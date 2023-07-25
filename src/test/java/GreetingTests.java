import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

class GreetingTests {

    private final String baseUrl = "http://test-microcam.relex.ru:40000";

    // 1.1
    @Test
    public void whenRequestToGreetingGetHelloWorld() {
        given()
                .when()
                .get(baseUrl + "/greet")
                .then()
                .statusCode(200)
                .body(Matchers.equalToObject("\"Hello, world!\""));
    }
}
