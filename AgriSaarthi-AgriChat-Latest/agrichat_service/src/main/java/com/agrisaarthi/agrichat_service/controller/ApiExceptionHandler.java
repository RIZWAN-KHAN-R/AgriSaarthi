package com.agrisaarthi.agrichat_service.controller;

import com.agrisaarthi.agrichat_service.service.AiAnswerService;
import com.agrisaarthi.agrichat_service.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid request");

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage() != null ? ex.getMessage() : "Invalid argument");
    }

    @ExceptionHandler({
            AiAnswerService.AiUnavailableException.class,
            ChatService.IntegrationUnavailableException.class,
            RestClientException.class
    })
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ProblemDetail integration(Exception ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "A required service is unavailable. Check the service configuration and try again.");
    }
}
