package dev.ultreon.devicesnext.util;

import java.lang.reflect.Method;

public interface FuncObject {
    default Object call(int ptr, Arguments args) {
        Class<? extends FuncObject> aClass = getClass();
        for (Method method : aClass.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(FuncPtr.class) || method.getAnnotation(FuncPtr.class).value() != ptr)
                continue;
            try {
                Object invoke = method.invoke(this, args);
                if (invoke == null)
                    return Unit.INSTANCE;
                else if (invoke instanceof String)
                    return invoke;
                else if (invoke instanceof Number)
                    return invoke;
                else if (invoke instanceof Boolean)
                    return invoke;
                else if (invoke instanceof Character)
                    return invoke;
                else
                    throw new IllegalArgumentException("Invalid return type: " + invoke.getClass());
            } catch (ReflectiveOperationException e) {
                this.fatal(e.getMessage());
            } catch (Exception e) {
                this.setError(e.getMessage());
            }
        }
        return null;
    }

    void fatal(String message);

    void setError(String message);
}
