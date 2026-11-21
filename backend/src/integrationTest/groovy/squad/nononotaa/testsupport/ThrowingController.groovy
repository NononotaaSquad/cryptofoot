package squad.nononotaa.testsupport

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.dao.DuplicateKeyException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import squad.nononotaa.backend.exception.BadRequestException
import squad.nononotaa.backend.exception.ConflictException
import squad.nononotaa.backend.exception.NotFoundException
import squad.nononotaa.backend.exception.ValidationException
import squad.nononotaa.backend.util.ApiController

class Contact {
    @NotBlank
    String name

    @Email
    String email

    Integer age
}

// outside the scanned base package, so only specs that @Import it expose these endpoints
@ApiController(path = '/errors')
class ThrowingController {
    @GetMapping('/bad-request')
    String badRequest() {
        throw new BadRequestException('bad input')
    }

    @GetMapping('/not-found')
    String notFound() {
        throw new NotFoundException('missing item')
    }

    @GetMapping('/conflict')
    String conflict() {
        throw new ConflictException('duplicate item')
    }

    @GetMapping('/duplicate')
    String duplicate() {
        throw new DuplicateKeyException('E11000 secret index detail')
    }

    @GetMapping('/validation')
    String validation() {
        throw new ValidationException('invalid item')
    }

    @GetMapping('/unexpected')
    String unexpected() {
        throw new IllegalStateException('secret internal detail')
    }

    @PostMapping('/body')
    Map<String, Object> body(@RequestBody Map<String, Object> payload) {
        payload
    }

    @PostMapping('/contact')
    Contact contact(@Valid @RequestBody Contact contact) {
        contact
    }

    @GetMapping('/param')
    int param(@RequestParam @Min(1L) int page) {
        page
    }
}
