package me.theminecoder.web.javalin;

import io.javalin.*;
import me.theminecoder.web.javalin.annotations.Controller;
import me.theminecoder.web.javalin.annotations.methods.*;
import me.theminecoder.web.javalin.annotations.parameters.*;
import me.theminecoder.web.javalin.annotations.parameters.conditions.NotNull;
import me.theminecoder.web.javalin.annotations.parameters.conditions.Range;
import me.theminecoder.web.javalin.annotations.parameters.conditions.Regex;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JavalinController {

    private static final Map<Class<? extends Annotation>, Function<Javalin, BiConsumer<String, Handler>>> annotationMethodMap = new HashMap<>();
    private static final Map<Class<? extends Annotation>, Method> annotationValueMethodMap = new HashMap<>();

    private static final Map<Class<? extends Annotation>, ParameterMapper<?>> parameterMappers = new HashMap<>();
    private static final Map<Class<? extends Annotation>, BiPredicate<?, Object>> parameterValidators = new HashMap<>();

    static {
        annotationMethodMap.put(GET.class, app -> app::get);
        annotationMethodMap.put(POST.class, app -> app::post);
        annotationMethodMap.put(PATCH.class, app -> app::patch);
        annotationMethodMap.put(PUT.class, app -> app::put);
        annotationMethodMap.put(DELETE.class, app -> app::delete);

        annotationMethodMap.put(HEAD.class, app -> app::head);

        annotationMethodMap.put(Before.class, app -> app::before);
        annotationMethodMap.put(After.class, app -> app::after);

        registerParameterMapper(RequestContext.class, (ctx, annotation, type) -> ctx);
        registerParameterMapper(Query.class, (ctx, annotation, type) -> {
            //noinspection StringEquality
            String value = ctx.queryParam(annotation.value(), annotation.defaultValue() == Query.NO_DEFAULT ? null : annotation.defaultValue());
            if (value == null) return null;
            if (String.class.isAssignableFrom(type)) return value;

            if (byte.class.isAssignableFrom(type) || Byte.class.isAssignableFrom(type)) {
                return Byte.valueOf(value);
            }

            if (char.class.isAssignableFrom(type) || Character.class.isAssignableFrom(type)) {
                if (value.length() != 1) throw new BadRequestResponse("Invalid character provided.");
                return value.charAt(0);
            }

            if (short.class.isAssignableFrom(type) || Short.class.isAssignableFrom(type)) {
                return Short.valueOf(value);
            }

            if (int.class.isAssignableFrom(type) || Integer.class.isAssignableFrom(type)) {
                return Integer.valueOf(value);
            }

            if (long.class.isAssignableFrom(type) || Long.class.isAssignableFrom(type)) {
                return Long.valueOf(value);
            }

            if (float.class.isAssignableFrom(type) || Float.class.isAssignableFrom(type)) {
                return Float.valueOf(value);
            }

            if (double.class.isAssignableFrom(type) || Double.class.isAssignableFrom(type)) {
                return Double.valueOf(value);
            }

            if (boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
                return Boolean.valueOf(value);
            }

            throw new IllegalArgumentException("Parameter type must be a primitive or String");
        });
        registerParameterMapper(QueryMap.class, (ctx, annotation, type) -> ctx.queryParamMap());
        registerParameterMapper(Header.class, (ctx, annotation, type) -> ctx.queryParam(annotation.value()));
        registerParameterMapper(Path.class, (ctx, annotation, type) -> ctx.pathParam(annotation.value()));
        registerParameterMapper(HeaderMap.class, (ctx, annotation, type) -> ctx.headerMap());

        registerParameterValidator(NotNull.class, (annotation, obj) -> obj != null);
        registerParameterValidator(Range.class, (annotation, obj) -> {
            if (!(obj instanceof Number)) return false;
            Number number = (Number) obj;
            if (number.doubleValue() < annotation.min())
                return false;
            if (number.doubleValue() > annotation.max())
                return false;
            return true;
        });
        registerParameterValidator(Regex.class, (annotation, obj) -> {
            if (!(obj instanceof String)) return false;
            return Pattern.matches(annotation.value(), (String) obj);
        });
    }

    public static void registerController(Class<?> controller, Javalin app) {
        actuallyRegisterController(controller, null, app);
    }

    public static void registerController(Object controller, Javalin app) {
        actuallyRegisterController(controller.getClass(), controller, app);
    }

    public static <T extends Annotation> void registerParameterMapper(Class<T> type, ParameterMapper<T> mapperFunction) {
        if (parameterMappers.containsKey(type)) {
            throw new IllegalStateException("Parameter mapper already exists!");
        }

        parameterMappers.put(type, mapperFunction);
    }

    public static <T extends Annotation> void registerParameterValidator(Class<T> type, BiPredicate<T, Object> validatorFunction) {
        if (parameterValidators.containsKey(type)) {
            throw new IllegalStateException("Parameter validator already exists!");
        }

        parameterValidators.put(type, validatorFunction);
    }

    private static void actuallyRegisterController(Class<?> controllerClass, Object controllerObject, Javalin app) {
        Controller controller = controllerClass.getAnnotation(Controller.class);
        if (controller == null) {
            throw new IllegalArgumentException("Controller doesn't have the @Controller annotation");
        }

        Consumer<Method> registerMethod = method -> {
            List<Annotation> methodAnnotations = annotationMethodMap.keySet().stream()
                    .map(method::getAnnotation)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (methodAnnotations.size() <= 0) {
                return;
            }

            if (methodAnnotations.size() > 1) {
                throw new IllegalStateException("Method \"" + method + "\" must have only 1 controller method annotation");
            }

            Annotation annotation = methodAnnotations.get(0);

            String routeString = "/";

            String controllerPath = controller.value().trim();
            if (controllerPath.startsWith("/")) controllerPath = controllerPath.substring(1);
            if (controllerPath.endsWith("/"))
                controllerPath = controllerPath.substring(0, controllerPath.length() - 1);

            routeString += controllerPath;

            Method valueMethod = annotationValueMethodMap.computeIfAbsent(annotation.getClass(), clz -> {
                try {
                    return clz.getMethod("value");
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            });

            String methodPath;
            try {
                methodPath = (String) valueMethod.invoke(annotation);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }

            methodPath = methodPath.trim();
            if (methodPath.startsWith("/")) methodPath = methodPath.substring(1);
            if (methodPath.endsWith("/")) methodPath = methodPath.substring(0, methodPath.length() - 1);

            if (methodPath.length() >= 1) methodPath = "/" + methodPath;
            routeString += methodPath;

            annotationMethodMap.get(annotation.annotationType()).apply(app).accept(routeString, ctx -> callMethod(ctx, controllerClass, controllerObject, method));
            System.out.println("Registered " + annotation.annotationType().getSimpleName() + " method " + method + " on " + routeString);
        };

        Arrays.stream(controllerClass.getMethods()).filter(method -> method.getAnnotation(Before.class) != null).forEach(registerMethod);
        Arrays.stream(controllerClass.getMethods()).filter(method -> method.getAnnotation(Before.class) == null && method.getAnnotation(After.class) == null).forEach(registerMethod);
        Arrays.stream(controllerClass.getMethods()).filter(method -> method.getAnnotation(After.class) != null).forEach(registerMethod);
    }

    private static final Map<Class, Class> primitiveObjectTypes = new HashMap<Class, Class>() {{
        put(byte.class, Byte.class);
        put(char.class, Character.class);
        put(short.class, Short.class);
        put(int.class, Integer.class);
        put(long.class, Long.class);
        put(float.class, Float.class);
        put(double.class, Double.class);
        put(boolean.class, Boolean.class);
    }};

    private static void callMethod(Context ctx, Class<?> controllerClass, Object controller, Method method) {
        Object actualController = ctx.attribute("controller");
        if (actualController == null) {
            if (controller == null) {
                try {
                    controller = controllerClass.newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new InternalServerErrorResponse("Error creating controller object");
                }
            }
            actualController = controller;
            ctx.attribute("controller", actualController);
        }

        List<Object> args = new ArrayList<>(method.getParameterCount());
        Arrays.stream(method.getParameters()).forEach(parameter -> {
            Object arg;
            Class argClass = parameter.getType();
            boolean optional = false;

            if (parameter.getType() == Optional.class) {
                argClass = (Class) ((ParameterizedType) parameter.getParameterizedType()).getActualTypeArguments()[0];
                optional = true;
            }

            if (primitiveObjectTypes.containsKey(argClass)) argClass = primitiveObjectTypes.get(argClass);

            Class<? extends Annotation> valueAnnotation = parameterMappers.keySet().stream()
                    .filter(annotation -> parameter.getAnnotation(annotation) != null)
                    .findFirst().orElse(null);
            if (valueAnnotation == null) {
                args.add(optional ? Optional.empty() : null);
                return;
            }

            arg = ((ParameterMapper<Annotation>) parameterMappers.get(valueAnnotation)).map(ctx, parameter.getAnnotation(valueAnnotation), argClass);

            if (!Arrays.stream(parameter.getAnnotations()).allMatch(annotation -> {
                if (!parameterValidators.containsKey(annotation.annotationType())) return true;
                return ((BiPredicate<Annotation, Object>) parameterValidators.get(annotation.annotationType())).test(annotation, arg);
            })) {
                if (optional) {
                    args.add(Optional.empty());
                    return;
                }

                throw new BadRequestResponse("Validation failed");
            }

            if (arg == null) {
                args.add(optional ? Optional.empty() : null);
                return;
            }

            if (!argClass.isAssignableFrom(arg.getClass())) {
                throw new IllegalStateException("Error assigning argument. Expected type: " + argClass + " Got type: " + arg.getClass());
            }

            args.add(optional ? Optional.of(arg) : arg);
        });

        try {
            Object response = method.invoke(actualController, args.toArray());

            if ((method.getReturnType() == Void.class && (method.getAnnotation(Before.class) != null || method.getAnnotation(After.class) != null)) //Bypass pipeline methods
                    && (ctx.resultStream() != null || ctx.resultFuture() != null)) {
                return;
            }

            if (response instanceof String) {
                ctx.result((String) response);
                return;
            }

            if (response instanceof View) {
                View view = (View) response;
                ctx.render("/" + view.getViewName(), view.getData());
                return;
            }

            if (response == null) {
                ctx.result("null");
                return;
            }

            ctx.json(response);
        } catch (IllegalAccessException e) {
            throw new InternalServerErrorResponse("Error invoking controller method");
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof HttpResponseException) {
                throw (HttpResponseException) e.getTargetException();
            }
            if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) e.getTargetException();
            }
            throw new RuntimeException(e.getTargetException());
        }
    }

}
