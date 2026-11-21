package squad.nononotaa.backend.model

enum class AnalysisTrigger { MANUAL, SCHEDULED }

enum class AnalysisStatus { RUNNING, COMPLETED, PARTIAL, FAILED }

enum class SourceStatus { CHECKED, UNAVAILABLE, NOT_CHECKED }

enum class ExclusionReason { EXCHANGE, SMART_CONTRACT, MIXER, FUNDRAISER }

enum class ClassificationStatus { NOT_EXCLUDED, UNAVAILABLE }

enum class ReportLimitation { SOURCE_UNAVAILABLE, BLOCKCHAIN_DATA_UNAVAILABLE, CLASSIFICATION_UNAVAILABLE, ANALYSIS_INCOMPLETE }

enum class AddressActivityStatus { AVAILABLE, UNAVAILABLE, NOT_CHECKED }

enum class CashOutSignalKind { EXCHANGE, P2P, PAYMENT_GATEWAY, CRYPTO_CARD, STABLECOIN_SWAP }

enum class AddressActivityCoverageLimitation { RECENT_TRANSACTIONS_ONLY, LABELLED_COUNTERPARTIES_ONLY, INTERNAL_TRANSFERS_NOT_COUNTED }

enum class TransferDirection { IN, OUT }

enum class AttributionChangeType { NEW, NOT_OBSERVED, CONFIDENCE_LEVEL_CHANGED, EXCLUSION_CHANGED }

enum class ComparabilityLimitation { IDENTIFIERS_DIFFER, SOURCE_COVERAGE_DIFFERS, NETWORKS_DIFFER, ALGORITHM_VERSION_DIFFERS, PARTIAL_ANALYSIS }

enum class ScheduleAttemptOutcome { STARTED, SKIPPED_OVERLAP, SKIPPED_DOWNTIME, FAILED }

enum class SchedulePreset(val demo: Boolean = false) { DAILY, EVERY_6_HOURS, WEEKLY, DEMO_EVERY_MINUTE(true) }

// TODO E7.1: cover exactly these networks in the demo fixtures; a new network needs a validator branch and api-sync
enum class Network { BTC, ETH }

enum class AddressValidationFailure { MALFORMED, CHECKSUM_MISMATCH, UNSUPPORTED_NETWORK, NETWORK_MISMATCH }
