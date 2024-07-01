package uz.programmer.courseBot.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import uz.programmer.courseBot.exception.ExceptionResponse;

import java.util.List;

@Component
public class ValidationExceptionHandler {

    public ResponseEntity handleValidationException(BindingResult bindingResult) {
        StringBuilder stringBuilder = new StringBuilder();
        List<FieldError> fieldErrorList = bindingResult.getFieldErrors();
        fieldErrorList.forEach(stringBuilder::append);
        return new ResponseEntity(new ExceptionResponse(stringBuilder.toString()), HttpStatus.BAD_REQUEST);
    }
}
