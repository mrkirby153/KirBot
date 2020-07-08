package com.mrkirby153.kirbot.entity.repo

import com.mrkirby153.kirbot.entity.PanelUser
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface PanelUserRepository : CrudRepository<PanelUser, String> {

    @Query("SELECT panel_user FROM PanelUser panel_user WHERE panel_user.admin = true")
    fun getGlobalAdmins(): List<PanelUser>
}