package com.demo.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author qiudong
 */
@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
    String value() default "";
}
