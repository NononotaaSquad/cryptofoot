package squad.nononotaa.backend.model

enum class AnalysisTrigger { MANUAL, SCHEDULED }

enum class AnalysisStatus { RUNNING, COMPLETED, PARTIAL, FAILED }

enum class SourceStatus { CHECKED, UNAVAILABLE, NOT_CHECKED }

enum class ExclusionReason { EXCHANGE, SMART_CONTRACT, MIXER, FUNDRAISER }

enum class ClassificationStatus { NOT_EXCLUDED, UNAVAILABLE }

enum class ReportLimitation {
    SOURCE_UNAVAILABLE,
    BLOCKCHAIN_DATA_UNAVAILABLE,
    CLASSIFICATION_UNAVAILABLE,
    MODEL_ASSESSMENT_UNAVAILABLE,
    ANALYSIS_INCOMPLETE,
}

enum class AddressActivityEventType { RECEIVED, SENT, SWAP, CASH_OUT_SIGNAL }

enum class AttributionChangeType { NEW, NOT_OBSERVED, CONFIDENCE_LEVEL_CHANGED, EXCLUSION_CHANGED }

enum class ComparabilityLimitation {
    IDENTIFIERS_DIFFER,
    SOURCE_COVERAGE_DIFFERS,
    NETWORKS_DIFFER,
    ALGORITHM_VERSION_DIFFERS,
    MODEL_VERSION_DIFFERS,
    PARTIAL_ANALYSIS,
}

enum class ScheduleAttemptOutcome { STARTED, SKIPPED_OVERLAP, SKIPPED_DOWNTIME, FAILED }

enum class SchedulePreset(val demo: Boolean = false) { DAILY, EVERY_6_HOURS, WEEKLY, DEMO_EVERY_MINUTE(true) }

// TODO E7.1: cover exactly these networks in the demo fixtures; a new network needs a validator branch and api-sync
enum class Network { BTC, ETH }

enum class AddressValidationFailure { MALFORMED, CHECKSUM_MISMATCH, UNSUPPORTED_NETWORK, NETWORK_MISMATCH }

enum class PremiseStatus { COUNTED, DISMISSED, FOR_REVIEW }

enum class DismissalReason { WARNING, DECLARED_BY_ANOTHER_PERSON, NO_PERSON_LINK, CONFLICTING_EMAIL }

enum class PremiseKind { OWN_PROFILE, OWN_DECLARATION, SALE_OR_SETTLEMENT, DISCUSSION, MASS_COMMENT }

enum class AssessedBy { RULE, MODEL }

enum class AuthorMatch { YES, NO, UNKNOWN }

enum class AssessmentMode { RULES_ONLY, WITH_MODEL }

/** The three questions of the question set `premise-questions-1`: `rodzaj_wpisu`, `deklaracja`, `ostrzezenie`. */
enum class ModelQuestion { KIND, DECLARATION, WARNING }
