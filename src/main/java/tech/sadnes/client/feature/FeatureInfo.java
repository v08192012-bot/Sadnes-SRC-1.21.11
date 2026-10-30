package tech.sadnes.client.feature;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FeatureInfo {
    String name();

    String[] aliases() default {};

    String description() default "Description is missing";

    String displayName() default "";

    Category category();

    Subcategory subcategory() default Subcategory.NONE;

    int[] keys() default {};

    BindMode[] bindTypes() default {};

    boolean autoEnabled() default false;

    boolean allowDisable() default true;

    boolean hidden() default false;
}
