package test;

import io.javalin.Context;
import me.theminecoder.web.javalin.annotations.methods.After;
import me.theminecoder.web.javalin.annotations.methods.Before;
import me.theminecoder.web.javalin.annotations.parameters.RequestContext;

public class ParentTestController {

    private long requestStart;

    @Before
    public void before() {
        requestStart = System.currentTimeMillis();
    }

    @After
    public void after(@RequestContext Context ctx) {
        System.out.println(ctx.ip() + " " + ctx.status() + " " + ctx.url() + " " + (System.currentTimeMillis() - requestStart) + "ms");
    }

}
