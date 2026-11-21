package squad.nononotaa.backend.util

import org.springframework.core.annotation.AliasFor
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RestController
@RequestMapping
annotation class ApiController(
    @get:AliasFor(annotation = RequestMapping::class, attribute = "path")
    val path: String,
    @get:AliasFor(annotation = RequestMapping::class, attribute = "version")
    val version: String = "1",
)
