package ai.exception;




import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handle(Exception ex) {

        return ResponseEntity
                .status(500)
                .body(ex.getMessage());
    }
}
