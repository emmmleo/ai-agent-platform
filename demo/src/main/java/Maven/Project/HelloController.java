package Maven.Project;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HelloController {
    @GetMapping("/ping")
    public String ping() { return "pong"; }
}
