package br.com.gastos.financeiro.infrastructure.googleAuth.Controller;

import br.com.gastos.financeiro.infrastructure.googleAuth.Service.GoogleAuthService;
import br.com.gastos.financeiro.infrastructure.googleAuth.Dto.GoogleUserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/auth/google")
public class AuthController {

    private final GoogleAuthService googleService;

    public AuthController(GoogleAuthService GoogleService) {
        this.googleService = GoogleService;
    }

    @PostMapping()
    public ResponseEntity<GoogleUserInfo> AuthWithGoogle(@RequestBody String token){
        GoogleUserInfo userInfo = googleService.dadosUser(token);
        if (userInfo != null) {
            return ResponseEntity.ok(userInfo);
        }
        return ResponseEntity.status(401).build();
    };
}
