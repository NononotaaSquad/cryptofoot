package squad.nononotaa.backend.analysis.web

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Playwright
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import squad.nononotaa.backend.util.logger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class PlaywrightFactory(
    private val props: WhitelistProperties,
) {
    private val lock = ReentrantLock()
    private var playwright: Playwright? = null
    private var browser: Browser? = null

    fun browser(): Browser = lock.withLock {
        if (browser == null || !browser!!.isConnected()) {
            logger.info("Starting Playwright browser headless={}", props.headless)
            playwright = Playwright.create()
            browser = playwright!!.chromium().launch(
                com.microsoft.playwright.BrowserType.LaunchOptions()
                    .setHeadless(props.headless),
            )
            logger.info("Playwright browser started")
        }
        return browser!!
    }

    @PreDestroy
    fun shutdown() {
        lock.withLock {
            try {
                browser?.close()
                playwright?.close()
                logger.info("Playwright browser stopped")
            } catch (e: Exception) {
                logger.warn("Error stopping Playwright: {}", e.message)
            }
            browser = null
            playwright = null
        }
    }

    companion object {
        private val logger = logger()
    }
}
