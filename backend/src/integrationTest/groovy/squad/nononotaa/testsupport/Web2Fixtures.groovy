package squad.nononotaa.testsupport

import java.time.Duration
import squad.nononotaa.backend.config.Web2Properties

/** Controlled material in the shape of the real services' answers, and properties pointing the sources at a local stub. */
class Web2Fixtures {

    static final String BTC = '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
    static final String BECH32 = 'bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4'
    static final String ETH = '0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed'

    static Web2Properties.GitHub github(StubWeb2Server server, String token = '', Duration timeout = Duration.ofMillis(500), int maxLogins = 3) {
        new Web2Properties.GitHub(true, URI.create(server.url()), token, timeout, 262144, maxLogins)
    }

    static Web2Properties.Bitcointalk bitcointalk(StubWeb2Server server, Duration timeout = Duration.ofMillis(500), int maxPosts = 20) {
        new Web2Properties.Bitcointalk(true, URI.create(server.url()), 'cryptofoot-test/1.0', timeout, 1048576, 3, maxPosts, Duration.ZERO)
    }

    static String githubUser(String login = 'Octo-Dev', String bio = "Builder. Donate BTC to ${BTC}") {
        """{"login":"${login}","name":"Octo Dev","bio":"${bio}","blog":"","company":null,"location":"Warsaw"}"""
    }

    static String longReadme() {
        'Hi, this is my profile README. ' + ('I write about open source and about tooling for forensic analysts. ' * 8) + "Donations welcome at ${ETH} anytime."
    }

    static String bitcointalkProfile(String name = 'Satoshi_Fan', String address = BTC, String signature = "Tip me: ${BECH32}") {
        """<html><body><table>
<tr><td><b>Name: </b></td><td>${name}</td></tr>
<tr><td><b>Position: </b></td><td>Member</td></tr>
<tr><td><b>Bitcoin address: </b></td><td>${address}</td></tr>
<tr><td><b>Signature: </b></td><td>${signature}</td></tr>
</table></body></html>"""
    }

    static String bitcointalkPost(String message, String author, String body) {
        """<tr><td class="windowbg"><a name="msg${message}"></a><table><tr>
<td class="poster_info"><b><a href="https://bitcointalk.org/index.php?action=profile;u=9${message}">${author}</a></b></td>
<td class="td_headerandpost"><div class="subject"><a href="https://bitcointalk.org/index.php?topic=555.msg${message}#msg${message}">Re: sale</a></div>
<div class="post" id="msg_${message}">${body}</div></td></tr></table></td></tr>"""
    }

    static String bitcointalkThread(List<String> posts) {
        "<html><body><table>${posts.join('\n')}</table></body></html>"
    }
}
