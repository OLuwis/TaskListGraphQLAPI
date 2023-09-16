package com.luwis.application.mutations;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.luwis.application.todo.TodoModel;
import com.luwis.application.todo.TodoRepository;
import com.luwis.application.todo.exceptions.TodoNotFoundException;

@AutoConfigureHttpGraphQlTester
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DeleteTodo {
    
    @Value("${JWT_SECRET}")
    private String secret;
    
    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private HttpGraphQlTester tester;

    @Test
    void shouldReturnTodoDeletionMessage() {
        long id = 1;

        String token = JWT.create()
        .withClaim("id", id)
        .withClaim("username", "Luis")
        .withIssuer("Luwis")
        .sign(Algorithm.HMAC256(secret));

        TodoModel todo = new TodoModel("Title", "Desc", id);
        TodoModel savedTodo = todoRepository.save(todo);

        tester.mutate()
        .header("Authorization", "Bearer " + token)
        .build()
        .documentName("deleteTodo")
        .variable("id", savedTodo.getId())
        .execute()
        .path("$['data']['deleteTodo']", path -> {
            String message = path.entity(String.class).get();

            assertEquals(message, "Todo Deleted!");
        });
    }

    @Test
    void shouldReturnTodoNotFound() {
        long id = 1;

        String token = JWT.create()
        .withClaim("id", id)
        .withClaim("username", "Luis")
        .withIssuer("Luwis")
        .sign(Algorithm.HMAC256(secret));

        tester.mutate()
        .header("Authorization", "Bearer " + token)
        .build()
        .documentName("deleteTodo")
        .variable("id", 1)
        .execute()
        .path("$['errors'][0]", path -> {
            TodoNotFoundException exception = new TodoNotFoundException();

            String errorMessage = path.path("['message']").entity(String.class).get();

            String errorType = path.path("['extensions']['classification']").entity(String.class).get();

            assertAll(
                "shouldReturnInvalidTitle",
                
                () -> assertEquals(errorMessage, exception.getMessage()),
                () -> assertEquals(errorType, exception.getType())
            );
        });
    }

}