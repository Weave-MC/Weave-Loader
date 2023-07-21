package net.weavemc.weave.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate methods with this to make them event listeners.
 * To work, methods annotated with this annotation must have 1 parameter,
 * that parameter being the event type they listen for.
 *
 * @see Event
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {}
