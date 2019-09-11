package uk.gov.ida.hub.policy.domain.serialization;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.assertj.core.api.Assertions.assertThat;

class StateSerializationHelpers {

    static void assertDeserializedForm(String actualBase64Form, Object expectedObject) {
        System.out.println("Expected: "+serialize(expectedObject));
        assertThat(expectedObject).isInstanceOf(Serializable.class);
        assertThat(deserialize(actualBase64Form))
            .as(
                "Deserialized object didn't match the expected form. Expected serialized form was:\n\n%s\n\n",
                serialize(expectedObject)
            ).isEqualToComparingFieldByField(expectedObject);
    }

    private static String serialize(Object input) {
        try (
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
        ) {
            objectOutputStream.writeObject(input);
            return Base64.encodeBase64String(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static Object deserialize(String input) {
        byte[] bytes = decodeBase64(input);
        try (
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            ObjectInputStream objectInputStream = new ObjectInputStream(in)
        ) {
            return objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException exception) {
            throw new AssertionError(
                "Deserialization failure - this probably means you've made a change to the serialized " +
                "form of your class.\nThis is probably a breaking change and could cause issues during deployment.\n" +
                "If the change is deliberate you should update the test and the serialVersionUID field on the class.\n\n" +
                "See the inner exception for details, or see https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html to learn about Java serialization.",
                exception
            );
        }
    }
}
