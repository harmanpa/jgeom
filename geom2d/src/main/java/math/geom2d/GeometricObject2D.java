/**
 * File: 	GeometricObject2D.java
 * Project: javaGeom
 *
 * Distributed under the LGPL License.
 *
 * Created: 26 sept. 2010
 */
package math.geom2d;

import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.DoubleStream;

/**
 * Grouping interface for all objects operating on Euclidean plane. This
 * includes shapes, boxes, transforms, vectors...
 *
 * @author dlegland
 *
 */
public interface GeometricObject2D {

    /**
     * Checks if the two objects are similar up to a given threshold value. This
     * method can be used to compare the results of geometric computations, that
     * introduce errors due to numerical computations.
     *
     * @param obj the object to compare
     * @param eps a threshold value, for example the minimal coordinate
     * difference
     * @return true if both object have the same value up to the threshold
     */
    public boolean almostEquals(GeometricObject2D obj, double eps);

    public static boolean equals(GeometricObject2D a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (!a.getClass().equals(b.getClass())) {
            return false;
        }
        return almostEquals(a, (GeometricObject2D) b, Tolerance2D.get());
    }

    public static boolean almostEquals(GeometricObject2D a, GeometricObject2D b, double eps) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (!a.getClass().equals(b.getClass())) {
            return false;
        }
        return almostEquals(a, b, a.getClass(), eps);
    }

    static boolean almostEquals(GeometricObject2D a, GeometricObject2D b, Class<? extends GeometricObject2D> type, double eps) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        Field[] fields = type.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                if (double.class.equals(field.getType())) {
                    if (Tolerance2D.compare(field.getDouble(a), field.getDouble(b), eps) != 0) {
                        return false;
                    }
                } else if (Double.class.equals(field.getType())) {
                    if (Tolerance2D.compare((Double) field.get(a), (Double) field.get(b), eps) != 0) {
                        return false;
                    }
                } else if (GeometricObject2D.class.isAssignableFrom(field.getType())) {
                    if (!almostEquals((GeometricObject2D) field.get(a), (GeometricObject2D) field.get(b), (Class<? extends GeometricObject2D>) field.getType(), eps)) {
                        return false;
                    }
                } else if (Collection.class.isAssignableFrom(field.getType())) {
                    Type collected = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    if (collected instanceof Class && GeometricObject2D.class.isAssignableFrom((Class<?>) collected)) {
                        if (!almostEquals((Collection<GeometricObject2D>) field.get(a), (Collection<GeometricObject2D>) field.get(b), eps)) {
                            return false;
                        }
                    } else if (collected instanceof TypeVariable) {
                        TypeVariable<?> typeVariableImpl = (TypeVariable<?>) collected;
                        GenericDeclaration genericDeclaration = typeVariableImpl.getGenericDeclaration();
                        if (genericDeclaration instanceof Class && GeometricObject2D.class.isAssignableFrom((Class<?>) genericDeclaration)) {
                            if (!almostEquals((Collection<GeometricObject2D>) field.get(a), (Collection<GeometricObject2D>) field.get(b), eps)) {
                                return false;
                            }
                        } else {
                            if (!Objects.equals(field.get(a), field.get(b))) {
                                return false;
                            }
                        }
                    } else {
                        if (!Objects.equals(field.get(a), field.get(b))) {
                            return false;
                        }
                    }
                } else {
                    if (!Objects.equals(field.get(a), field.get(b))) {
                        return false;
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                return false;
            }
        }
        if (GeometricObject2D.class.isAssignableFrom(type.getSuperclass())) {
            if (!almostEquals(a, b, (Class<? extends GeometricObject2D>) type.getSuperclass(), eps)) {
                return false;
            }
        } else if (fields.length == 0) {
            // This is a fudge to handle the equalsVerifier tests
            return a.hashCode() == b.hashCode();
        }
        return true;
    }

    public static boolean almostEquals(Collection<GeometricObject2D> a, Collection<GeometricObject2D> b, double eps) {
        // FIXME: Doesn't work for Set, but do we need that?
        if (a == null || b == null) {
            return a == b;
        }
        if (!a.getClass().equals(b.getClass())) {
            return false;
        }
        if (a.size() != b.size()) {
            return false;
        }
        Iterator<GeometricObject2D> itA = a.iterator();
        Iterator<GeometricObject2D> itB = b.iterator();
        while (itA.hasNext() & itB.hasNext()) {
            if (!almostEquals(itA.next(), itB.next(), eps)) {
                return false;
            }
        }
        return true;
    }

    public static int hash(int c, int m, double... fields) {
        return c + DoubleStream.of(fields).mapToInt(f -> Tolerance2D.hash(f)).map(v -> v * m).sum();
    }
}
