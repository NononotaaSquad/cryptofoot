package squad.nononotaa.backend.dto

import jakarta.validation.constraints.Email
import squad.nononotaa.backend.model.ScheduleIntervalUnit
import squad.nononotaa.backend.model.SchedulePreset
import squad.nononotaa.backend.util.IntegerOnly
import squad.nononotaa.backend.util.NarrowingIdentifier
import squad.nononotaa.backend.util.TrimToNull
import squad.nononotaa.backend.util.ValidWalletAddress
import tools.jackson.databind.annotation.JsonDeserialize

@NarrowingIdentifier
data class CreateCaseRequest(
    @param:JsonDeserialize(using = TrimToNull::class) val nickname: String? = null,
    @param:JsonDeserialize(using = TrimToNull::class) val login: String? = null,
    @param:JsonDeserialize(using = TrimToNull::class) @field:Email val email: String? = null,
    @param:JsonDeserialize(using = TrimToNull::class) val messengerId: String? = null,
    @param:JsonDeserialize(using = TrimToNull::class) val profileUrl: String? = null,
    @param:JsonDeserialize(using = TrimToNull::class) val fullName: String? = null,
    @param:JsonDeserialize(using = TrimToNull::class) @field:ValidWalletAddress val walletAddress: String? = null,
)

data class UpdateScheduleRequest @JvmOverloads constructor(
    val enabled: Boolean,
    val cadence: SchedulePreset? = null,
    @param:JsonDeserialize(using = IntegerOnly::class) val intervalValue: Long? = null,
    val intervalUnit: ScheduleIntervalUnit? = null,
)
