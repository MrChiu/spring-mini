package com.demo.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author qiudong
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {
    String value() default "";
}
