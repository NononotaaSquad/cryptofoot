package squad.nononotaa.backend.service

import static squad.nononotaa.backend.model.ExclusionReason.EXCHANGE
import static squad.nononotaa.backend.model.ExclusionReason.FUNDRAISER
import static squad.nononotaa.backend.model.ExclusionReason.MIXER
import static squad.nononotaa.backend.model.Network.BTC
import static squad.nononotaa.backend.model.Network.ETH

import spock.lang.Shared
import spock.lang.Specification
import squad.nononotaa.backend.model.AddressValidation
import squad.nononotaa.backend.model.CanonicalAddress
import squad.nononotaa.backend.model.ExclusionLabel
import tools.jackson.module.kotlin.ExtensionsKt

class ExclusionLabelDirectorySpec extends Specification {

    @Shared
    def mapper = ExtensionsKt.jacksonObjectMapper()

    @Shared
    ExclusionLabelDirectory bundled = ExclusionLabelDirectory.load(
    mapper,
    resource('etherscan-labels.json'),
    resource('own-labels.json'))

    private static InputStream resource(String name) {
        ExclusionLabelDirectorySpec.getResourceAsStream("/exclusion-labels/$name")
    }

    def "maps tags #tags to #reason"() {
        expect:
        EtherscanTagMapping.INSTANCE.reasonFor(tags) == reason

        where:
        tags                                                     | reason
        ['kraken']                                               | EXCHANGE
        ['cold-wallet']                                          | EXCHANGE
        ['fiat-gateway']                                         | EXCHANGE
        ['tornado-cash']                                         | MIXER
        ['donate']                                               | FUNDRAISER
        ['crowdfunding']                                         | FUNDRAISER
        [
            'ofac-sanctions-lists',
            'tornado-cash',
            'binance'
        ]      | MIXER
        ['binance', 'donate']                                    | EXCHANGE
    }

    def "never excludes tags that can mark a private person: #tags"() {
        expect:
        EtherscanTagMapping.INSTANCE.reasonFor(tags) == null

        where:
        tags << [
            ['ofac-sanctions-lists'],
            ['gitcoin-grants'],
            ['maker-vault-owner'],
            ['buidlguidl-builders'],
            ['sybil-delegate'],
            ['airdrop-hunter'],
            ['charity'],
            ['dex'],
            []
        ]
    }

    def "finds #description in the bundled lists"() {
        expect:
        with(bundled.find(new CanonicalAddress(network, address))) {
            it.reason == reason
            it.name.contains(name)
            it.source.startsWith(source)
        }

        where:
        description               | network | address                                         | reason     | name                            | source
        'a Kraken wallet'         | ETH     | '0x267be1c1d684f78cb4f6a176c4911b741e4ffdc0'    | EXCHANGE   | 'Kraken 4'                      | 'Etherscan labels'
        'the Tornado Cash router' | ETH     | '0xd90e2f925da726b50c4ed8d0fb90ad053324f31b'    | MIXER      | 'Tornado.Cash: Router'          | 'Etherscan labels'
        'a donation address'      | ETH     | '0x165cd37b4c644c2921454429e7f9358d18a45e14'    | FUNDRAISER | 'Ukraine Crypto Donation'       | 'Etherscan labels'
        'a BTC exchange wallet'   | BTC     | '1NDyJtNTjmwk5xPNhjgAMu4HDHigtobu1s'            | EXCHANGE   | 'Binance.com'                   | 'Own label list'
    }

    def "the own list holds only addresses confirmed by a label service"() {
        expect:
        bundled.find(new CanonicalAddress(BTC, address)) == null

        where:
        address << [
            '34xp4vRoCGJym3xR7yCVPFHoCNxv4Twseo',
            'bc1qm34lsc65zpw79lxes69zkqmk6ee3ewf0j77s3h',
            'bc1qgdjqv0av3q56jvd82tkdjpy7gdp9ut8tlqmgrpmv24sq90ecnvqqjwvw97',
            '357a3So9CbsNfBBgFYACGvxxS6tMaDoa1P'
        ]
    }

    def "does not list #description"() {
        expect:
        bundled.find(new CanonicalAddress(ETH, address)) == null

        where:
        description                       | address
        'a Gitcoin grant recipient'       | '0xbd53a5df1fd60aba402cf346ee07ed8b058c17d0'
        'an OFAC-listed address only'     | '0x6f1ca141a28907f78ebaa64fb83a9088b02a8352'
        'a DEX router (left to bytecode)' | '0x7a250d5630b4cf539739df2c5dacb4c659f2488d'
    }

    def "every own list entry is a valid address in canonical form"() {
        given:
        def validator = new AddressValidator()
        def own = ExclusionLabelDirectory.fromOwnList(
                mapper.readValue(resource('own-labels.json'), ExclusionLabelDirectory.OwnList))

        expect:
        !own.isEmpty()
        own.keySet().every {
            validator.validate(it.address, it.network) == new AddressValidation.Valid(it)
        }
    }

    def "stores dataset addresses lowercased and keeps the first of letter-case duplicates"() {
        given:
        def entries = [
            '0xABCDEFabcdefABCDEFabcdefABCDEFabcdefABCD': new ExclusionLabelDirectory.EtherscanEntry('Upper', ['kraken']),
            '0xabcdefabcdefabcdefabcdefabcdefabcdefabcd': new ExclusionLabelDirectory.EtherscanEntry('Lower', ['kraken']),
            '0x1111111111111111111111111111111111111111': new ExclusionLabelDirectory.EtherscanEntry('', ['gitcoin-grants'])
        ]

        when:
        def labels = ExclusionLabelDirectory.fromEtherscan(entries)

        then:
        labels.size() == 1
        labels[new CanonicalAddress(ETH, '0xabcdefabcdefabcdefabcdefabcdefabcdefabcd')].name == 'Upper [kraken]'
    }

    def "an own entry overrides the dataset entry for the same address"() {
        given:
        def address = new CanonicalAddress(ETH, '0x165cd37b4c644c2921454429e7f9358d18a45e14')
        def dataset = [(address): new ExclusionLabel(FUNDRAISER, 'dataset', 'Etherscan labels')]
        def own = [(address): new ExclusionLabel(EXCHANGE, 'own', 'Own label list (ref)')]

        expect:
        new ExclusionLabelDirectory(dataset + own).find(address).name == 'own'
    }
}
