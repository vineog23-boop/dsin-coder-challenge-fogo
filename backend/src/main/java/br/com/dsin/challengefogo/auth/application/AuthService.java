package br.com.dsin.challengefogo.auth.application;

import br.com.dsin.challengefogo.auth.application.dto.LoginDtoRequest;
import br.com.dsin.challengefogo.auth.application.dto.LoginDtoResponse;
import br.com.dsin.challengefogo.auth.persistence.UserEntity;
import br.com.dsin.challengefogo.auth.persistence.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    // Comparado quando o usuário não existe, para o tempo de resposta não denunciar quem existe.
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("senha-inexistente");
    }

    @Transactional(readOnly = true)
    public LoginDtoResponse login(LoginDtoRequest request) {
        UserEntity user = userRepository.findByUsername(request.username()).orElse(null);
        String hash = user != null ? user.getPasswordHash() : dummyHash;

        if (!passwordEncoder.matches(request.password(), hash) || user == null) {
            throw new InvalidCredentialsException();
        }

        return new LoginDtoResponse(tokenService.issue(user.getUsername(), user.getRole()), user.getRole());
    }
}
