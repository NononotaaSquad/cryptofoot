package squad.nononotaa.backend

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.client.RestTestClient
import org.testcontainers.mongodb.MongoDBContainer
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
abstract class BaseSpec extends Specification {

    static final MongoDBContainer MONGODB = new MongoDBContainer('mongo:8')

    static {
        MONGODB.start()
    }

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add('MONGODB_URI') { MONGODB.getReplicaSetUrl('test') }
    }

    @Autowired
    protected RestTestClient client
}
