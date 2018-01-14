package me.mrkirby153.KirBot.database.models

@Target(AnnotationTarget.FIELD)
annotation class Column(val value: String)

@Target(AnnotationTarget.FIELD)
annotation class PrimaryKey

@Target(AnnotationTarget.CLASS)
annotation class Table(val value: String)

@Target(AnnotationTarget.CLASS)
annotation class Timestamps(val value: Boolean)