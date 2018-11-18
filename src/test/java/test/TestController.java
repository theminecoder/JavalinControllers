package test;

import io.javalin.Context;
import me.theminecoder.web.javalin.View;
import me.theminecoder.web.javalin.annotations.Controller;
import me.theminecoder.web.javalin.annotations.methods.Before;
import me.theminecoder.web.javalin.annotations.methods.GET;
import me.theminecoder.web.javalin.annotations.parameters.Query;
import me.theminecoder.web.javalin.annotations.parameters.RequestContext;
import me.theminecoder.web.javalin.annotations.parameters.conditions.NotNull;
import me.theminecoder.web.javalin.annotations.parameters.conditions.Range;
import me.theminecoder.web.javalin.annotations.parameters.conditions.Regex;

import java.util.Optional;

@Controller("test")
public class TestController extends ParentTestController {

    @Before("/*")
    public void beforeTest() {
        System.out.println("pls");
    }

    @GET("view")
    public View view() {
        return new View("test.md");
    }

    @GET("viewdata")
    public View view(@Query("data") @NotNull String data) {
        return new View("testdata.mustache").withData("test", "test").withData("data", data);
    }

    @GET
    public String test(@RequestContext Context ctx) {
        return ctx.req.getRemoteHost();
    }

    @GET("hello")
    public String helloTest(
            @Query(value = "name") @Regex("[A-Za-z ]+") Optional<String> who,
            @Query(value = "test", defaultValue = "1") @Range(min = 1, max = 10) int test
    ) {
        return "Hello " + who + ". Number is " + test;
    }

    @GET("err")
    public void exceptionTest() {
        throw new NullPointerException("test");
    }

}
