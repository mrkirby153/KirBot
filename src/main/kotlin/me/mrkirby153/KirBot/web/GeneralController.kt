package me.mrkirby153.KirBot.web

import ro.pippo.controller.Controller
import ro.pippo.controller.GET
import ro.pippo.controller.Produces

class GeneralController : Controller() {

    @GET
    @Produces(Produces.JSON)
    fun index(): AppInfo {
        return AppInfo("KirBot", 1)
    }

    data class AppInfo(val name: String, val apiVersion: Int)
}