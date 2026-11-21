package squad.nononotaa.backend.service

import static squad.nononotaa.backend.model.ExclusionReason.EXCHANGE
import static squad.nononotaa.backend.model.Network.BTC
import static squad.nononotaa.backend.model.Network.ETH

import spock.lang.Specification
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.ExclusionReason
import squad.nononotaa.backend.model.KnownExcludedAddress

class OwnListClassificationSourceSpec extends Specification {

    def source = new OwnListClassificationSource(new AddressValidator())

    def "loads a valid, versioned list covering every exclusion reason"() {
        given:
        def all = source.findExcluded(allListedAddresses())

        expect:
        source.name == 'Own list v1'
        source.networks == [BTC, ETH] as Set
        all.values()*.reason as Set == ExclusionReason.values() as Set
    }

    def "matches canonical addresses only and returns nothing for unlisted ones"() {
        given:
        def binance = new CanonicalAddress(ETH, '0x28c6c06298d514db089934071355e5743bf21d60')
        def personal = new CanonicalAddress(ETH, '0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed')

        expect:
        source.findExcluded([binance, personal]) == [(binance): new KnownExcludedAddress(EXCHANGE, 'Binance 14 (hot wallet)')]
        source.findExcluded([]) == [:]
    }

    private static List<CanonicalAddress> allListedAddresses() {
        def validator = new AddressValidator()
        OwnListClassificationSource.getResourceAsStream("/$OwnListClassificationSource.LIST_PATH").readLines()
                .findAll { it.trim() && !it.startsWith('#') }
                .collect {
                    validator.validate(it.split(',')[1], null).canonicalAddress
                }
    }
}
