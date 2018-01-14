package me.mrkirby153.KirBot.database.models

import java.sql.Connection

interface ConnectionFactory {

    fun getConnection(): Connection
}