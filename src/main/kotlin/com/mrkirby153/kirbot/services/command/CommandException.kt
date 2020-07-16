package com.mrkirby153.kirbot.services.command

import java.lang.RuntimeException

class CommandException(val msg: String) : RuntimeException(msg)