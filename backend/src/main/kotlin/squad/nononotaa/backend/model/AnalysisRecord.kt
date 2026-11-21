package squad.nononotaa.backend.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import squad.nononotaa.backend.analysis.AnalysisResult
import squad.nononotaa.backend.analysis.CaseIdentifier
import java.time.Instant

@Document("analyses")
@CompoundIndex(def = "{'caseId': 1, 'startedAt': -1}")
data class AnalysisRecord(
    @Id val id: String? = null,
    @Version val version: Long? = null,
    val caseId: String,
    val trigger: AnalysisTrigger,
    val status: AnalysisStatus,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val identifiers: List<CaseIdentifier>,
    val networks: List<Network>,
    val result: AnalysisResult? = null,
)
