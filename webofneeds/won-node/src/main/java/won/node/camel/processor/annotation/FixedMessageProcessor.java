package won.node.camel.processor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies the annotated class as a message processor used to implement the obligatory
 * behavior for the specified message type and direction.
 *
 * Implementing classes may not generate and send new messages as this might cause ordering inconsistencies if new
 * messages are sent before the current message has been processed completely. If new messages are to be sent,
 * this is to be done in a FixedMessageReactionProcessor.
 *
 * This implementation is always executed before the corresponding FacetMessageProcessor.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FixedMessageProcessor
{
  String direction();
  String messageType();
}
