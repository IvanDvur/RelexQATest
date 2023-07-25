import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pojo.LoginCredentials;
import org.junit.jupiter.api.Test;
import pojo.User;
import utils.RandomStringGenerator;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;

public class SessionTests {

    private final String BASE_URL = "http://test-microcam.relex.ru:40000";
//    Валидные данные
    private final LoginCredentials adminCredentialsEntity = new LoginCredentials(TestConfig.adminUsername,TestConfig.adminPassword,"","");
//    Невалидные данные
    private final LoginCredentials notValidEntity = new LoginCredentials("Ad","blabla","","");
//    Несуществующий пользователь
    private final LoginCredentials nonExistingEntity = new LoginCredentials("someNonExistingUsername","someNonExistingPassword","","");
    private final ObjectMapper om = new ObjectMapper();

    //  2.1 Логинимся под админом, проверяем код ответа, и роль
    @Test
    public void whenLoginWithAdminCredentialsExpect() throws JsonProcessingException {
        String admin = om.writeValueAsString(adminCredentialsEntity);
        String response = given()
                .contentType("application/json")
                .body(admin)
                .when()
                .post(BASE_URL + "/login")
                .then().statusCode(200).body("roles",hasItems("ADMIN"))
                .extract().response().asString();
    }

    // 2.2
    @Test
    public void whenLoginWithUserCredentialsExpectRoleUser() throws JsonProcessingException {
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
//        Логинимся по созданным пользователем и проверяем роль
        String userToken = given()
                .contentType("application/json")
                .body(userLoginCredentials)
                .when()
                .post(BASE_URL + "/login")
                .then().statusCode(200).body("roles",hasItems("USER"))
                .extract().body().jsonPath().getString("access_token");
//        Чистим базу
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .delete(BASE_URL + "/users/" + userId)
                .then()
                .statusCode(200);
    }


    @Test
    public void whenLoginWithNotValidCredentialsExpect() throws JsonProcessingException {
        String notValidCredentials = om.writeValueAsString(notValidEntity);
        given()
                .contentType("application/json")
                .body(notValidCredentials)
                .when()
                .post(BASE_URL + "/login")
                .then().statusCode(400);

    }

    // 2.3
    @Test
    public void whenLoginWithNonExistingCredentialsExpect() throws JsonProcessingException {
        String notExistingCredentials = om.writeValueAsString(nonExistingEntity);
        given()
                .contentType("application/json")
                .body(notExistingCredentials)
                .when()
                .post(BASE_URL + "/login")
                .then().statusCode(401);
    }


}
