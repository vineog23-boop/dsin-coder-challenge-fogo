package br.com.dsin.challengefogo.auth.web;

import br.com.dsin.challengefogo.auth.application.AuthService;
import br.com.dsin.challengefogo.auth.application.dto.LoginDtoRequest;
import br.com.dsin.challengefogo.auth.application.dto.LoginDtoResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginDtoResponse login(@RequestBody @Valid LoginDtoRequest request) {
        return authService.login(request);
    }
}
