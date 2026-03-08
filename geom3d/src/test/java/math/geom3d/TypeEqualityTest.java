/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package math.geom3d;

import io.github.classgraph.ClassGraph;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 * @author peter
 */
@RunWith(Parameterized.class)
public class TypeEqualityTest {

    private final Class<?> clazz;

    public TypeEqualityTest(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> getClasses() {
        List<Object[]> out = new ArrayList<>();
//        for (Class<?> c : new ClassGraph().acceptPackages(TypeEqualityTest.class.getPackage().getName()).scan().getAllClasses().loadClasses()) {
//            if (!(isTest(c) || isUtilities(c))) {
//                out.add(new Object[]{c});
//            }
//        }
        return out;
    }

    static boolean isTest(Class<?> c) {
        if (junit.framework.TestCase.class.isAssignableFrom(c)) {
            return true;
        }
        for (Method m : c.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Test.class)) {
                return true;
            }
        }
        return false;
    }

    static boolean isUtilities(Class<?> c) {
        if (Throwable.class.isAssignableFrom(c)) {
            return true;
        }
        for (Method m : c.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void test() {
        EqualsVerifier.forClass(clazz).usingGetClass().suppress(Warning.NONFINAL_FIELDS).verify();
    }

}
