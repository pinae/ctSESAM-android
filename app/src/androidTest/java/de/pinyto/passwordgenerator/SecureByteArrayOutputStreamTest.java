package de.pinyto.passwordgenerator;

import junit.framework.TestCase;

import java.lang.reflect.Field;

/**
 * Testing that the extended class really deletes the buffer.
 */
public class SecureByteArrayOutputStreamTest extends TestCase {

    public void testEmptyBuffer() {
        SecureByteArrayOutputStream stream = new SecureByteArrayOutputStream();
        stream.write(0x12);
        stream.write(0x34);
        stream.write(0x56);
        assertEquals(0x12, stream.toByteArray()[0]);
        assertEquals(0x34, stream.toByteArray()[1]);
        assertEquals(0x56, stream.toByteArray()[2]);
        stream.emptyBuffer();
        assertEquals(3, stream.size());
        Class targetClass = stream.getClass();
        try {
            Field field = targetClass.getSuperclass().getDeclaredField("buf");
            field.setAccessible(true);
            byte[] buffer = (byte[]) field.get(stream);
            assertEquals(0x00, buffer[0]);
            assertEquals(0x00, buffer[1]);
            assertEquals(0x00, buffer[2]);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            int count = 0;
            for (Field found : targetClass.getDeclaredFields()) {
                assertEquals("buf", found.getName());
                count++;
            }
            assertTrue(count > 0);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        stream.reset();
        assertEquals(0, stream.size());
    }

}
