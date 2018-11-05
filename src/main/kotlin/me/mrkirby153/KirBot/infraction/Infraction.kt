package me.mrkirby153.KirBot.infraction

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import java.sql.Timestamp

@Table("infractions")
class Infraction : Model() {

    init {
        this.timestamps = false
    }

    @PrimaryKey
    var id: Long? = null

    @Column("user_id")
    var userId: String = ""

    @Column("issuer")
    var issuerId: String? = null

    var guild: String = ""

    @Column("type")
    private var typeRaw: String = ""

    var reason: String? = null

    var active: Boolean = true

    var metadata: String? = null

    @Column("expires_at")
    var expiresAt: Timestamp? = null

    var type: InfractionType
        get() = InfractionType.getType(this.typeRaw)
        set(type) {
            this.typeRaw = type.internalName
        }

    fun revoke() {
        this.active = false
        save()
    }
}