package squad.nononotaa.backend.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import squad.nononotaa.backend.analysis.web.WhitelistProperties

@Configuration
@EnableConfigurationProperties(WhitelistProperties::class)
class WebSourceConfig
