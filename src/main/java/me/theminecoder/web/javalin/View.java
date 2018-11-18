package me.theminecoder.web.javalin;

import java.util.HashMap;
import java.util.Map;

public class View {

    private String viewName;
    private Map<String, Object> data = new HashMap<>();

    public View(String viewName) {
        this.viewName = viewName;
    }

    public String getViewName() {
        return viewName;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public View withData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

}
