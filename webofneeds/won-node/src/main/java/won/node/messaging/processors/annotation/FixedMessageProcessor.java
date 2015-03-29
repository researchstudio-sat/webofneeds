package won.node.messaging.processors.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies the annotated class as a message processor used to implement the obligatory
 * behavior for the specified message type and direction.
 * This implementation is always executed before the corresponding FacetMessageProcessor.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FixedMessageProcessor
{
  String direction();
  String messageType();
}
