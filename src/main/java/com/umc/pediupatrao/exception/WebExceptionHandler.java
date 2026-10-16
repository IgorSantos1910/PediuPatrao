package com.umc.pediupatrao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

// Tratamento de erros das telas
@ControllerAdvice(annotations = Controller.class)
public class WebExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado(RecursoNaoEncontradoException ex, Model model) {
        model.addAttribute("mensagem", ex.getMessage());
        return "erro";
    }

    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public String regraNegocio(RegraNegocioException ex, Model model) {
        model.addAttribute("mensagem", ex.getMessage());
        return "erro";
    }
}
