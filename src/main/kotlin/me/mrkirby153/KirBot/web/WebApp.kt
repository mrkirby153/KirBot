package me.mrkirby153.KirBot.web

import ro.pippo.controller.ControllerApplication

class WebApp : ControllerApplication() {

    override fun onInit() {
        addControllers(GeneralController(), ApiController())
    }

    data class Response(val success: Boolean, val message: String?)
}