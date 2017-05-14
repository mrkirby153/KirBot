package me.mrkirby153.KirBot;

import me.mrkirby153.KirBot.web.WebApp;
import ro.pippo.controller.*;
import ro.pippo.controller.extractor.Param;

@Path("/channels")
public class ChanController extends Controller {


    @GET("/api/{id: [0-9]+}")
    @Named("api.get")
    @Produces(Produces.JSON)
    public WebApp.Response get(@Param int id) {
        return new WebApp.Response(false, "ID: "+id);
    }
}
