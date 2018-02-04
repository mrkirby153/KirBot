package me.mrkirby153.KirBot.infraction

import me.mrkirby153.KirBot.database.models.Column
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.PrimaryKey
import me.mrkirby153.KirBot.database.models.Table
import me.mrkirby153.KirBot.database.models.Timestamps
import java.sql.Timestamp

@Timestamps(false)
@Table("infractions")
class Infraction : Model() {

    @PrimaryKey
    var id: Int = 0

    @Column("user_id")
    var userId: String = ""

    @Column("issuer")
    var issuerId: String? = null

    var guild: String = ""

    @Column("type")
    private var typeRaw: String = ""

    var reason: String? = null

    var active: Boolean = true

    @Column("revoked_at")
    var revokedAt: Timestamp? = null

    var type: InfractionType = InfractionType.UNKNOWN
        get() = InfractionType.getType(this.typeRaw)
        set(type) {
            this.typeRaw = type.internalName
            field = type
        }
}