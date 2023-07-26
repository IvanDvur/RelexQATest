import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pojo.LoginCredentials;
import pojo.User;
import utils.RandomStringGenerator;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;

public class AdminApiTests {

    private final String BASE_URL = "http://test-microcam.relex.ru:40000";
    private String token;
    private final LoginCredentials loginCredentialsEntity = new LoginCredentials(TestConfig.adminUsername, TestConfig.adminPassword, "", "");
    private final ObjectMapper om = new ObjectMapper();

    //    3.1 Запрос на получение пользователей из аккаунта с ролью ADMIN
    @Test
    public void whenLoginWithAdminCredentialsExpectToAccessAllUsers() throws JsonProcessingException {
        String admin = om.writeValueAsString(loginCredentialsEntity);
        String token = given()
                .contentType("application/json")
                .body(admin)
                .when()
                .post(BASE_URL + "/login")
                .then().statusCode(200)
                .extract().body().jsonPath().getString("access_token");

        List allUsers = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(BASE_URL + "/users/all")
                .then()
                .statusCode(200).extract().response().as(List.class);
        System.out.println(allUsers);
    }

    //    3.2 Запрос на получение пользователей при отсутствии авторизации
    @Test
    public void whenUnauthorizedExpectToDenyAllUsers() {
        given()
                .when()
                .get(BASE_URL + "/users/all")
                .then()
                .statusCode(401);
    }

//    3.3 Запрос на получение всех пользователей от сущности с ролью USER
    @Test
    public void whenInsufficentPrivilegesExpectToDenyAllUsers() throws JsonProcessingException {
        String login = RandomStringGenerator.generateRandomUsername(10);
        User userEntity = new User("Ivan", "Sergeevich", "Dvurechenskiy", login,
                "user@example.com", "SoME_PAssword1!!");
        LoginCredentials userLoginCredentials = new LoginCredentials(login,"SoME_PAssword1!!","","");
        String userData = om.writeValueAsString(userEntity);

        String userId = given()
                .contentType("application/json")
                .body(userData).when().post(BASE_URL + "/users")
                .then().statusCode(201).extract().body().jsonPath().getString("uuid");

        String userToken = given()
                .contentType("application/json")
                .body(userLoginCredentials)
                .when()
                .post(BASE_URL + "/login")
                .then().statusCode(200)
                .extract().body().jsonPath().getString("access_token");

        given().header("Authorization", "Bearer " + userToken)
                .when()
                .get(BASE_URL + "/users/all")
                .then()
                .statusCode(403);

        given().header("Authorization", "Bearer " + userToken)
                .when()
                .delete(BASE_URL + "/users/"+userId)
                .then()
                .statusCode(200);

    }

}
