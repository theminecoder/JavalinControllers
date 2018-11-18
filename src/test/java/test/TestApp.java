package test;

import io.javalin.Javalin;
import me.theminecoder.web.javalin.JavalinController;

public class TestApp {

    public static void main(String[] args) {
        Javalin app = Javalin.create().disableStartupBanner().enableRouteOverview("/").start(3000);
        app.exception(NullPointerException.class, (ex, ctx) -> {
            ctx.status(404).result("Not Found");
            ex.printStackTrace();
        });
        JavalinController.registerController(TestController.class, app);
    }

}
