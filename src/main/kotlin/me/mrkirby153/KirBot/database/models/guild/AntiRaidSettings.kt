package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model

@Table("anti_raid_settings")
class AntiRaidSettings : Model() {

    var id = ""

    var enabled = false
    var count = 0L
    var period = 0L
    var action = "NOTHING"

    @Column("alert_role")
    var alertRole: String? = null

    @Column("alert_channel")
    var alertChannel: String? = null

    @Column("quiet_period")
    var quietPeriod = 0L
}