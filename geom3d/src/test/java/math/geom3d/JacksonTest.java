/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package math.geom3d;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

/**
 *
 * @author peter
 */
public class JacksonTest {

    @Test
    public void testPoint() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(new Point3D()));
        System.out.println(mapper.readValue(mapper.writeValueAsString(new Point3D()), Point3D.class));
    }

    @Test
    public void testVector() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(new Vector3D()));
        System.out.println(mapper.readValue(mapper.writeValueAsString(new Vector3D()), Vector3D.class));
    }
}
