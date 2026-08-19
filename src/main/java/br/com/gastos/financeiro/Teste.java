package br.com.gastos.financeiro;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teste")
public class Teste {

    @GetMapping()
    public String hello(){
        return "Hello world";
    }
}
