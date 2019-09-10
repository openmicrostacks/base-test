package de.a9d3.testing.testdata;

import de.a9d3.testing.GlobalStatics;
import de.a9d3.testing.method_extractor.GetterIsSetterExtractor;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class TestDataProvider {
    private static final Logger LOGGER = Logger.getLogger(TestDataProvider.class.getName());

    protected static final int LIST_ARRAY_ITEM_COUNT = 2;

    private Map<String, Function<String, Object>> providerMap;

    /**
     * This class will invoke classes with random data
     */
    public TestDataProvider() {
        this(Collections.emptyMap());
    }

    /**
     * This class will invoke classes with random data from a custom map
     *
     * @param customMap The elements in this map will override the keys of the default map
     */
    public TestDataProvider(Map<String, Function<String, Object>> customMap) {
        providerMap = getDefaultProviderMap();

        providerMap.putAll(customMap);
    }

    /**
     * This method defines the default functions which generate random values for the corresponding classes
     *
     * @return A map with key (string), {@literal (value(Function<String, Object>))}
     */
    public static Map<String, Function<String, Object>> getDefaultProviderMap() {
        Map<String, Function<String, Object>> map = new HashMap<>();

        map.put(boolean.class.getName(), x -> (x.hashCode() % 2 != 0));
        map.put(Boolean.class.getName(), map.get(boolean.class.getName()));

        map.put(char.class.getName(), x -> (char) (x.hashCode() % Character.MAX_VALUE));
        map.put(Character.class.getName(), map.get(char.class.getName()));

        map.put(byte.class.getName(), x -> (byte) (x.hashCode() % (Byte.MAX_VALUE - Byte.MIN_VALUE) - Byte.MAX_VALUE));
        map.put(Byte.class.getName(), map.get(byte.class.getName()));

        map.put(short.class.getName(), x -> (short) (x.hashCode() %
                (Short.MAX_VALUE - Short.MIN_VALUE) - Short.MAX_VALUE));
        map.put(Short.class.getName(), map.get(short.class.getName()));

        map.put(int.class.getName(), String::hashCode);
        map.put(Integer.class.getName(), map.get(int.class.getName()));

        map.put(long.class.getName(), x -> (long) x.hashCode() << 16);
        map.put(Long.class.getName(), map.get(long.class.getName()));

        map.put(float.class.getName(), x -> ((float) x.hashCode()) / 3);
        map.put(Float.class.getName(), map.get(float.class.getName()));

        map.put(double.class.getName(), x -> ((double) x.hashCode()) * 2 / 3);
        map.put(Double.class.getName(), map.get(double.class.getName()));

        map.put(String.class.getName(), x -> UUID.nameUUIDFromBytes(x.getBytes()).toString());

        // Other classes
        map.put(Instant.class.getName(), x -> Instant.ofEpochSecond(x.hashCode()));

        return map;
    }

    /**
     * This method will try to invoke the provided class.
     *
     * @param c                               The class which should be invoked
     * @param seed                            The seed which should be used to generate the constructor parameters
     * @param tryComplexConstructorIfPossible If true, try largest constructor first
     * @param <T>                             Return type
     * @return Initialized object
     */
    public <T> T fill(Class c, String seed, Boolean tryComplexConstructorIfPossible) {
        Function<String, Object> fun = providerMap.get(c.getName());
        if (fun != null) {
            return (T) fun.apply(seed);
        } else {
            LOGGER.finest("Could not find class in functionMap. Trying to invoke class by constructors.");
            return generateTestDataByNonStandardClass(c, seed, tryComplexConstructorIfPossible);
        }
    }

    protected <T> T generateTestDataByNonStandardClass(Class c, String seed, Boolean complex) {
        if (Collection.class.isAssignableFrom(c)) {
            return (T) invokeCollectionInstance(c, seed);
        } else if (Map.class.isAssignableFrom(c)) {
            return (T) invokeMapInstance(c, seed);
        } else if (c.isArray()) {
            Object objects = Array.newInstance(c.getComponentType(), LIST_ARRAY_ITEM_COUNT);
            for (int i = 0; i < Array.getLength(objects); i++) {
                Array.set(objects, i, fill(c.getComponentType(), seed + i, false));
            }

            return (T) objects;
        } else {
            return resolveComplexObject(c, seed, complex);
        }
    }

    private Collection invokeCollectionInstance(Class c, String seed) {
        Collection instance;

        // https://static.javatpoint.com/images/java-collection-hierarchy.png
        if (c.equals(List.class) || c.equals(Queue.class)) {
            instance = new LinkedList<>();
        } else if (c.equals(Set.class)) {
            instance = new HashSet<>();
        } else {
            instance = resolveComplexObject(c, seed, false);
        }

        for (int i = 0; i < LIST_ARRAY_ITEM_COUNT; i++) {
            instance.add(null);
        }

        return instance;
    }

    private Map invokeMapInstance(Class c, String seed) {
        Map instance = null;

        // https://static.javatpoint.com/images/core/java-map-hierarchy.png
        if (!c.equals(SortedMap.class) && c.equals(Map.class)) {
            instance = new HashMap();
            instance.put(null, null);
        } else if (c.equals(SortedMap.class) || c.equals(TreeMap.class)) { // TreeMap as it does not allow null values
            instance = new TreeMap();
        } else {
            instance = resolveComplexObject(c, seed, false);
            instance.put(null, null);
        }

        return instance;
    }

    private <T> T resolveComplexObject(Class c, String seed, Boolean tryComplexConstructor) {
        Constructor[] constructors = c.getConstructors();
        if (tryComplexConstructor) {
            Arrays.sort(constructors, Comparator.comparingInt(con -> -con.getParameterCount()));
        } else {
            Arrays.sort(constructors, Comparator.comparingInt(Constructor::getParameterCount));
        }

        for (Constructor single : constructors) {
            if (Arrays.stream(single.getParameterTypes()).noneMatch(aClass -> aClass.equals(c))) {
                try {
                    Object[] args = new Object[single.getParameterCount()];
                    for (int i = 0; i < single.getParameterCount(); i++) {
                        args[i] = fill(single.getParameterTypes()[i], seed + i, tryComplexConstructor);
                    }

                    if (args.length == 0) {
                        return (T) single.newInstance();
                    } else {
                        return (T) single.newInstance(args);
                    }
                } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
                    LOGGER.fine("Could not initialize constructor " + single + " of class " + c.getName() +
                            ". Trying other constructor if available.");
                }
            }
        }

        // Could not initialize class
        StringBuilder message = new StringBuilder();
        message.append("Could not initialize ");
        message.append(c.getName());

        if (c.getConstructors().length == 0) {
            message.append("\nClass has no constructors.\nPlease refer to ");
            message.append(GlobalStatics.GIT_REPO_MD);
            message.append(" to get an idea how to use customMaps to initialize the TestDataProvider");
        }
        LOGGER.warning(message.toString());


        return null;
    }

    /**
     * This method will initialize the provided class with null pointer for the mutable variables and random
     * values for the immutable variables (can't be null)
     *
     * @param c   Class which should be initialized
     * @param <T> Return type
     * @return Initialized class
     * @throws IllegalAccessException is thrown if the access to the class, field, method or constructor is not allowed.
     * @throws InvocationTargetException is thrown when the called method throws an exception.
     */
    public <T> T fillMutableWithNull(Class c) throws IllegalAccessException, InvocationTargetException {
        Object instance = fill(c, "123", false);

        for (Method method : GetterIsSetterExtractor.getSetter(c)) {
            if (!method.getParameterTypes()[0].isPrimitive()) {
                method.invoke(instance, new Object[]{null});
            }
        }

        return (T) instance;
    }
}
