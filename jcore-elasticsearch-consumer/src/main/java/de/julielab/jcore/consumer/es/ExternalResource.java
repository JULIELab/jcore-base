/**
 * 
 */
package de.julielab.jcore.consumer.es;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author faessler
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExternalResource {
	/**
	 * The key to which external resources bind to. If no key is set, the name of the annotated field will be used.
	 * 
	 * @return the key;
	 */
	String key();

	/**
	 * <p>
	 * If the external resource provider does follow the Java bean conventions of naming its getters for a property
	 * named <tt>propName</tt> as <tt>getPropName()</tt>, it is sufficient to deliver the name of the respective
	 * resource property.
	 * </p>
	 * <p>
	 * Otherwise, {@link #methodName()} may be used to specify the getter method to be called to retrieve the desired
	 * external resource.
	 * </p>
	 * <p>
	 * If the getter method can be unambiguously derived by the type of the annotated field the resource should be
	 * assigned to, i.e. the type of this field exactly matches the return type of the respective shared resource
	 * object's getter method, neither value is required.
	 * </p>
	 * 
	 * @return
	 */
	String property() default "";

	/**
	 * The getter method name to get the desired external resource as more precise alternative to {@link #property()}.
	 * <p>
	 * If the getter method can be unambiguously derived by the type of the annotated field the resource should be
	 * assigned to, i.e. the type of this field exactly matches the return type of the respective shared resource
	 * object's getter method, neither value is required.
	 * </p>
	 * 
	 * @return
	 */
	String methodName() default "";
}
