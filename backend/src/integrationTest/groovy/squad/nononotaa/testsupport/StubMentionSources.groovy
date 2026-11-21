package squad.nononotaa.testsupport

import java.time.Instant
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import squad.nononotaa.backend.analysis.CaseIdentifier
import squad.nononotaa.backend.analysis.MentionSource
import squad.nononotaa.backend.analysis.RawMention

// outside the scanned base package, so only specs that @Import this configuration get these sources
class StubOkSource implements MentionSource {
    String sourceName = 'stub-ok'
    boolean synthetic = true

    @Override
    List<RawMention> findMentions(List<CaseIdentifier> identifiers) {
        [
            new RawMention(
            'https://stub-ok.example/post/1',
            Instant.parse('2026-11-21T10:00:00Z'),
            'post by CryptoNick: send BTC to 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa or ETH to 0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed'),
            new RawMention(
            'https://stub-ok.example/post/2',
            Instant.parse('2026-11-21T10:00:00Z'),
            'unrelated post about weather'),
        ]
    }
}

class StubEmptySource implements MentionSource {
    String sourceName = 'stub-empty'
    boolean synthetic = true

    @Override
    List<RawMention> findMentions(List<CaseIdentifier> identifiers) {
        []
    }
}

class StubFailingSource implements MentionSource {
    String sourceName = 'stub-failing'
    boolean synthetic = true

    @Override
    List<RawMention> findMentions(List<CaseIdentifier> identifiers) {
        throw new RuntimeException('source down')
    }
}

class StubSlowSource implements MentionSource {
    String sourceName = 'stub-slow'
    boolean synthetic = true

    @Override
    List<RawMention> findMentions(List<CaseIdentifier> identifiers) {
        Thread.sleep(30000)
        []
    }
}

@Configuration
class StubMentionSources {
    @Bean
    MentionSource stubOkSource() {
        new StubOkSource()
    }

    @Bean
    MentionSource stubEmptySource() {
        new StubEmptySource()
    }

    @Bean
    MentionSource stubFailingSource() {
        new StubFailingSource()
    }

    @Bean
    MentionSource stubSlowSource() {
        new StubSlowSource()
    }
}
