package net.weavemc.loader.api.event

/**
 * Annotate methods with this to make them event listeners.
 * To work, methods annotated with this annotation must have 1 parameter,
 * that parameter being the event type they listen for.
 *
 * @see Event
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
public annotation class SubscribeEvent
