import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pojo.LoginCredentials;
import pojo.User;
import utils.RandomStringGenerator;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class UserApiTests {

    private final LoginCredentials adminCredentialsEntity = new LoginCredentials(TestConfig.adminUsername, TestConfig.adminPassword, "", "");
    private final ObjectMapper om = new ObjectMapper();
    private final String BASE_URL = "http://test-microcam.relex.ru:40000";

    //    4.1.1, 4.2.1, 4.3.1, 4.2.5
    @Test
    public void whenCreateUserExpectToFindItInDataBase() throws JsonProcessingException {
        String login = RandomStringGenerator.generateRandomUsername(10);
        User userEntity = new User("Ivan", "Sergeevich", "Dvurechenskiy",
                login, "user@example.com", "SoME_PAssword1!!");
        LoginCredentials userLoginCredentials = new LoginCredentials(login, "SoME_PAssword1!!", "", "");
        String userData = om.writeValueAsString(userEntity);
//        Создаём пользователя
        String userId = given()
                .contentType("application/json")
                .body(userData)
                .when()
                .post(BASE_URL + "/users")
                .then().statusCode(201).extract().body().jsonPath().getString("uuid");
//        Логинимся по созданным пользователем
        String userToken = given()
                .contentType("application/json")
                .body(userLoginCredentials)
                .when()
                .post(BASE_URL + "/login")
                .then().statusCode(200).extract().body().jsonPath().getString("access_token");

//         Сверяем тело
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(BASE_URL + "/users/" + userId)
                .then()
                .statusCode(200).body(
                        "firstName", equalTo("Ivan"),
                        "lastName", equalTo("Dvurechenskiy"),
                        "email", equalTo("user@example.com"));
//        Удаляем пользователя
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete(BASE_URL + "/users/" + userId)
                .then()
                .statusCode(200);
        String adminToken = given()
                .contentType("application/json")
                .body(om.writeValueAsString(adminCredentialsEntity))
                .when()
                .post(BASE_URL + "/login")
                .then().statusCode(200).extract().body().jsonPath().getString("access_token");
//        Проверяем, что пользователь удалён
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get(BASE_URL + "/users/" + userId)
                .then().statusCode(404);

    }

    //   4.2.1, 4.2.3
    @Test
    public void whenUserIsLoggedInExpectHimToManageOnlyHisOwnAccount() throws JsonProcessingException {
        String randomLogin = RandomStringGenerator.generateRandomUsername(10);
        User userEntity = new User("Ivan", "Sergeevich", "Dvurechenskiy",
                randomLogin, "user@example.com", "SoME_PAssword1!!");
        String user = om.writeValueAsString(userEntity);
        LoginCredentials userCredentials = new LoginCredentials(randomLogin, "SoME_PAssword1!!", "", "");

//        Создаём пользователя
        String response = given()
                .contentType("application/json")
                .body(user)
                .when()
                .post(BASE_URL + "/users")
                .then().statusCode(201).extract().response().asString();

        JsonPath jsonPath = new JsonPath(response);
        String userId = jsonPath.getString("uuid");

//        Вход в аккаунт с ролью USER
        String userResponse = given()
                .when()
                .contentType("application/json")
                .body(userCredentials)
                .when()
                .post(BASE_URL + "/login")
                .then().statusCode(200)
                .extract().response().asString();
        String userToken = new JsonPath(userResponse).getString("access_token");

//        Получаем информацию о "себе"
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(BASE_URL + "/users/" + userId).then().statusCode(200);

//        Получаем информацию о несуществующем пользователе
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get(BASE_URL + "/users/" + UUID.randomUUID())
                .then().statusCode(404);

//        Удаляем свой аккаунт
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + userToken)
                .when().delete(BASE_URL + "/users/" + userId)
                .then().statusCode(200);
    }

    public void whenCreateNotValidUserExpectBadRequest() {

    }
}
