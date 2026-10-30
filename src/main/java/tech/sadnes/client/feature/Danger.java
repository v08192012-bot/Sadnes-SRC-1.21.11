package tech.sadnes.client.feature;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import tech.sadnes.client.server.MarkerLevel;
import tech.sadnes.client.server.MarkerServer;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Danger {
    MarkerServer[] reasons();

    MarkerLevel level() default MarkerLevel.WARNING;
}
