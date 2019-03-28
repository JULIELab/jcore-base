package de.julielab.jcore.consumer.es;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceAccessException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-thread object holding per-thread filter instances to avoid concurrency
 * issues. Thus, each instance of the ElasticSearch consumer has its own filter
 * registry. Resource sharing is done by making the UimaContext available, and
 * with it the shared resources defined for the consumer AE.
 * 
 * @author faessler
 * 
 */
public class FilterRegistry {
	private Map<Class<?>, FilterBoard> filterBoardMap;
	private UimaContext context;

	public FilterRegistry(UimaContext aContext) {
		this.context = aContext;
		this.filterBoardMap = new HashMap<>();
	}

	public void addFilterBoard(Class<?> filterBoardClass, FilterBoard filterBoard) {
		if (filterBoardMap.keySet().contains(filterBoardClass))
			throw new IllegalArgumentException("The filter registry already contains a filter board named \"" + filterBoardClass
					+ "\" and cannot add another filter board with the same name.");
		injectExternalResources(filterBoard, filterBoard.getClass());

		filterBoard.setupFilters();
		filterBoardMap.put(filterBoardClass, filterBoard);
	}

	@SuppressWarnings("unchecked")
	public <T extends FilterBoard> T getFilterBoard(Class<T> filterBoardClass) {
		FilterBoard filterBoard = filterBoardMap.get(filterBoardClass);
		if (null == filterBoard)
			throw new IllegalArgumentException(
					"No filter board class \"" + filterBoardClass + "\" could be found in the filter board registry.");
		return (T) filterBoard;
	}

	public void addFilterBoards(String[] filterBoardClasses) {
		for (int i = 0; i < filterBoardClasses.length; i++) {
			try {
				String className = filterBoardClasses[i];
				Class<?> filterBoardClass = Class.forName(className);
				FilterBoard filterBoard = (FilterBoard) filterBoardClass.newInstance();
				filterBoard.setUimaContext(context);

				addFilterBoard(filterBoardClass, filterBoard);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	protected void injectExternalResources(FilterBoard filterBoard, Class<?> filterBoardClass) {
		Field[] declaredFields = filterBoardClass.getDeclaredFields();
		for (int i = 0; i < declaredFields.length; i++) {
			Field field = declaredFields[i];
			ExternalResource annotation = field.getAnnotation(ExternalResource.class);
			if (null != annotation) {
				try {
					Object resourceObject = context.getResourceObject(annotation.key());
					if (null == resourceObject)
						throw new IllegalStateException("Cannot inject external resource bound to key \""
								+ annotation.key() + "\" to field " + filterBoard.getClass().getName() + "#"
								+ field.getName()
								+ ": The resource was not found at the specified key, i.e. no resource was bound to the key.");
					String methodName = annotation.methodName();
					Method resourceGetterMethod = null;

					// get the name of the resource getter method, if the name
					// or a property name is given (to derive
					// the method
					// name by prepending 'get' and capitalizing the property
					// name, e.g. 'hypernyms' -> 'getHypernyms')
					// If no name can be derived, try to find the right method
					// by unique return type
					if (StringUtils.isBlank(methodName)) {
						String property = annotation.property();
						if (StringUtils.isBlank(property)) {
							// noone told us how the method is called to get the
							// resource. Perhaps there is only a
							// single
							// public method present so it's unambigous anyway
							Method[] publicMethods = resourceObject.getClass().getMethods();
							if (publicMethods.length == 1) {
								resourceGetterMethod = publicMethods[0];
							} else {
								// more than one field; try to disambiguate by
								// the type of the field where the external
								// resource should be injected into
								for (int j = 0; j < publicMethods.length; j++) {
									Method publicMethod = publicMethods[j];
									if (publicMethod.getReturnType().equals(field.getType())
											&& resourceGetterMethod == null)
										resourceGetterMethod = publicMethod;
									else if (publicMethod.getReturnType().equals(field.getType()))
										throw new IllegalStateException("The annotation " + annotation
												+ " does neither define the methodName nor a property to derive the method name from (by prefix with 'get') and the shared resource object \""
												+ resourceObject + "\" has more than one public method of return type "
												+ field.getType()
												+ ". It must be specified which method to use in order to retrieve the external resource.");
								}
							}
						} else {
							methodName = "get" + StringUtils.capitalize(property);
						}
						if (StringUtils.isBlank(methodName) && null == resourceGetterMethod)
							throw new IllegalStateException("The annotation " + annotation
									+ " does neither define the methodName nor a property to derive the method name from (by prefix with 'get') and the shared resource object \""
									+ resourceObject
									+ "\"has more than one public method. It must be specified which method to use in order to retrieve the external resource.");
					}

					if (null == resourceGetterMethod)
						resourceGetterMethod = resourceObject.getClass().getMethod(methodName);

					// get the external resource
					Object externalResource = resourceGetterMethod.invoke(resourceObject);
					// inject the resource into the field
					field.setAccessible(true);
					field.set(filterBoard, externalResource);
					field.setAccessible(false);
				} catch (ResourceAccessException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}

			}
		}
	}
}
