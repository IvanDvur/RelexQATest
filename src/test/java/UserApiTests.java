import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pojo.LoginCredentials;
import pojo.User;
import utils.RandomStringGenerator;
import org.junit.jupiter.api.Test;


import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class UserApiTests {

    private final LoginCredentials adminCredentialsEntity = new LoginCredentials(TestConfig.adminUsername, TestConfig.adminPassword, "", "");
    private final ObjectMapper om = new ObjectMapper();
    private final String BASE_URL = "http://test-microcam.relex.ru:40000";

    //    4.1.2.1-6
    @Test
    public void whenCreateUserWithInvalidDataExpectBadRequest() throws JsonProcessingException {
        String login = RandomStringGenerator.generateRandomUsername(10);
        String notValidLogin = RandomStringGenerator.generateRandomUsername(2);
        // Отсутствует хотя бы одно обязательное поле
        User userEntityWithoutMandatoryField = new User(null, "Sergeevich", "Dvurechenskiy",
                login, "user@example.com", "SoME_PAssword1!!");
//        Невалидный формат Email
        User userEntityWithNotValidEmail = new User("Ivan", "Sergeevich", "Dvurechenskiy",
                login, "userexamplecom", "SoME_PAssword1!!");
//        Невалидное имя
        User userEntityWithNotValidFirstName = new User("", "Sergeevich", "Dvurechenskiy",
                login, "user@example.com", "SoME_PAssword1!!");
//        Невалидная фамилия
        User userEntityWithNotValidLastName = new User("Ivan", "Sergeevich", "Dvurec^hen__skiy",
                login, "user@example.com", "SoME_PAssword1!!");
//        Невалидный логин(2 символа)
        User userEntityWithNotValidLogin = new User("Ivan", "Sergeevich", "Dvurechenskiy",
                notValidLogin, "user@example.com", "SoME_PAssword1!!");
//        Невалидный пароль(нет цифры)
        User userEntityWithNotValidPassword = new User(null, "Sergeevich", "Dvurechenskiy",
                login, "user@example.com", "SoME_PAssword!!");

        given()
                .contentType("application/json")
                .body(om.writeValueAsString(userEntityWithoutMandatoryField))
                .when().post(BASE_URL + "/users")
                .then().statusCode(400);
        given()
                .contentType("application/json")
                .body(om.writeValueAsString(userEntityWithNotValidEmail))
                .when().post(BASE_URL + "/users")
                .then().statusCode(400);
//        БАГ(?): Можно создать пользователя с именем из одной буквы
        given()
                .contentType("application/json")
                .body(om.writeValueAsString(userEntityWithNotValidFirstName))
                .when().post(BASE_URL + "/users")
                .then().statusCode(400);
        given()
                .contentType("application/json")
                .body(om.writeValueAsString(userEntityWithNotValidLastName))
                .when().post(BASE_URL + "/users")
                .then().statusCode(400);
        given()
                .contentType("application/json")
                .body(om.writeValueAsString(userEntityWithNotValidLogin))
                .when().post(BASE_URL + "/users")
                .then().statusCode(400);
        given()
                .contentType("application/json")
                .body(om.writeValueAsString(userEntityWithNotValidPassword))
                .when().post(BASE_URL + "/users")
                .then().statusCode(400);
    }

    //      4.1.3 Пытаемся создать пользователя с уже существующим аккаунтом
    @Test
    public void whenCreateUserWithExistingLoginExpectConflict() throws JsonProcessingException {
        String login = RandomStringGenerator.generateRandomUsername(10);
        User userEntity1 = new User("Ivan", "Sergeevich", "Dvurechenskiy",
                login, "user@example.com", "SoME_PAssword1!!");
        User userEntity2 = new User("Ivan", "Ivanovich", "Ivanov",
                login, "user2@example.com", "SoME_PAssword1323!!");
        LoginCredentials loginCredentials = new LoginCredentials(login, "SoME_PAssword1!!", "", "");
        String userId = given()
                .contentType("application/json")
                .body(om.writeValueAsString(userEntity1))
                .when().post(BASE_URL + "/users")
                .then().statusCode(201).extract().body().jsonPath().getString("uuid");
        given()
                .contentType("application/json")
                .body(om.writeValueAsString(userEntity2))
                .when().post(BASE_URL + "/users")
                .then().statusCode(409);

        String user1Token = given()
                .contentType("application/json")
                .body(om.writeValueAsString(loginCredentials))
                .when().post(BASE_URL + "/login")
                .then().statusCode(200).extract().body().jsonPath().getString("access_token");
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + user1Token)
                .body(om.writeValueAsString(loginCredentials))
                .when().delete(BASE_URL + "/users/" + userId)
                .then().statusCode(200);

    }

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
                .when().post(BASE_URL + "/users")
                .then().statusCode(201).extract().body().jsonPath().getString("uuid");
//        Логинимся по созданным пользователем
        String userToken = given()
                .contentType("application/json")
                .body(userLoginCredentials)
                .when().post(BASE_URL + "/login")
                .then().statusCode(200).extract().body().jsonPath().getString("access_token");
//         Сверяем тело (получаем доступ к своему же аккаунту)
        given()
                .header("Authorization", "Bearer " + userToken)
                .when().get(BASE_URL + "/users/" + userId)
                .then()
                .statusCode(200).body(
                        "firstName", equalTo("Ivan"),
                        "lastName", equalTo("Dvurechenskiy"),
                        "email", equalTo("user@example.com"));
//        Логинимся под админом
        String adminToken = given()
                .contentType("application/json")
                .body(om.writeValueAsString(adminCredentialsEntity))
                .when().post(BASE_URL + "/login")
                .then().statusCode(200).extract().body().jsonPath().getString("access_token");

//        Проверяем, что админ может получить доступ к пользователю
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().get(BASE_URL + "/users/" + userId)
                .then()
                .statusCode(200).body(
                        "firstName", equalTo("Ivan"),
                        "lastName", equalTo("Dvurechenskiy"),
                        "email", equalTo("user@example.com"));

//        Удаляем пользователя
        given()
                .header("Authorization", "Bearer " + userToken)
                .when().delete(BASE_URL + "/users/" + userId)
                .then()
                .statusCode(200);

//        Проверяем, что пользователь удалён
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().get(BASE_URL + "/users/" + userId)
                .then().statusCode(404);

    }

    //   4.2.2, 4.3.3, 4.4.2
    @Test
    public void whenUserIsLoggedInExpectHimToManageOnlyHisOwnAccount() throws JsonProcessingException {
        String login1 = RandomStringGenerator.generateRandomUsername(10);
        String login2 = RandomStringGenerator.generateRandomUsername(10);
        User userEntity1 = new User("Ivan", "Sergeevich", "Dvurechenskiy",
                login1, "user1@example.com", "SoME_PAssword1!!");
        User userEntity2 = new User("Ivan", "Ivanovich", "Ivanov",
                login2, "user2@example.com", "SoME_PAssword2!!");
        LoginCredentials userLoginCredentials1 = new LoginCredentials(login1, "SoME_PAssword1!!", "", "");
        LoginCredentials userLoginCredentials2 = new LoginCredentials(login2, "SoME_PAssword2!!", "", "");
        String userData1 = om.writeValueAsString(userEntity1);
        String userData2 = om.writeValueAsString(userEntity2);

//        Создаём пользователя 1
        String userId1 = given()
                .contentType("application/json")
                .body(userData1)
                .when().post(BASE_URL + "/users")
                .then().statusCode(201).extract().body().jsonPath().getString("uuid");
//        Создаём пользователя 2
        String userId2 = given()
                .contentType("application/json")
                .body(userData2)
                .when().post(BASE_URL + "/users")
                .then().statusCode(201).extract().body().jsonPath().getString("uuid");
//        Логинимся под пользователем 1
        String userToken1 = given()
                .contentType("application/json")
                .body(userLoginCredentials1)
                .when().post(BASE_URL + "/login")
                .then().statusCode(200).extract().body().jsonPath().getString("access_token");
//        Логинимся под пользователем 2
        String userToken2 = given()
                .contentType("application/json")
                .body(userLoginCredentials2)
                .when().post(BASE_URL + "/login")
                .then().statusCode(200).extract().body().jsonPath().getString("access_token");
//        Получаем информацию о другом пользователе
        given()
                .header("Authorization", "Bearer " + userToken1)
                .when().get(BASE_URL + "/users/" + userId2)
                .then()
                .statusCode(404);
//        Пытаемся удалить другого пользователя
        given()
                .header("Authorization", "Bearer " + userToken1)
                .when().delete(BASE_URL + "/users/" + userId2)
                .then()
                .statusCode(404);
//        Пытаемся удалить несуществующего пользователя
        given()
                .header("Authorization", "Bearer " + userToken1)
                .when().delete(BASE_URL + "/users/" + UUID.randomUUID())
                .then()
                .statusCode(404);
//        Пытаемся изменить данные о другом пользователе
        given()
                .header("Authorization", "Bearer " + userToken1).contentType("application/json")
                .body("""
                        {
                          "firstName": "DnLXjDFjwJp",
                          "middleName": "ubHlLYNlyHi",
                          "lastName": "kKLYpSsXWaWelMSE-",
                          "login": "gdWvy[/H;{>k]W?nE9u",
                          "email": "use45453r@example.com",
                          "password": "b;gamnIFJ2v!6>K>uU<v_lPU)g"
                        }""")
                .when().patch(BASE_URL + "/users/" + userId2)
                .then()
                .statusCode(404);

//        Удаляём пользователя 1
        given()
                .header("Authorization", "Bearer " + userToken1)
                .when().delete(BASE_URL + "/users/" + userId1)
                .then()
                .statusCode(200);
//        Удаляём пользователя 2
        given()
                .header("Authorization", "Bearer " + userToken2)
                .when().delete(BASE_URL + "/users/" + userId2)
                .then()
                .statusCode(200);
    }


}
