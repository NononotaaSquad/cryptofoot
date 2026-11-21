package squad.nononotaa.backend.model

enum class AnalysisTrigger { MANUAL, SCHEDULED }

enum class AnalysisStatus { RUNNING, COMPLETED, PARTIAL, FAILED }

enum class SourceStatus { CHECKED, UNAVAILABLE }

enum class ExclusionReason { EXCHANGE, SMART_CONTRACT, MIXER, FUNDRAISER }

enum class ClassificationStatus { NOT_EXCLUDED, UNAVAILABLE }

enum class ReportLimitation { SOURCE_UNAVAILABLE, BLOCKCHAIN_DATA_UNAVAILABLE, CLASSIFICATION_UNAVAILABLE, ANALYSIS_INCOMPLETE }

enum class AddressActivityEventType { RECEIVED, SENT, SWAP, CASH_OUT_SIGNAL }

enum class AttributionChangeType { NEW, NOT_OBSERVED, CONFIDENCE_LEVEL_CHANGED, EXCLUSION_CHANGED }

enum class ComparabilityLimitation { IDENTIFIERS_DIFFER, SOURCE_COVERAGE_DIFFERS, ALGORITHM_VERSION_DIFFERS, PARTIAL_ANALYSIS }

enum class ScheduleAttemptOutcome { STARTED, SKIPPED_OVERLAP, FAILED }
