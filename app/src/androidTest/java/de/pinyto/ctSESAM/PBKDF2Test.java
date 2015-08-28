package de.pinyto.ctSESAM;

import junit.framework.TestCase;

import java.io.UnsupportedEncodingException;

/**
 * Unit tests for the PBKDF2_HMAC implementation
 */
public class PBKDF2Test extends TestCase {

    public void testSha512Hmac () {
        byte[] key;
        try {
            key = "secret".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            key = "secret".getBytes();
        }
        byte[] message;
        try {
            message = "message".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            message = "message".getBytes();
        }
        byte[] expectedDigest = new byte[] { 0x1B, (byte)0xBA, 0x58, 0x7C, 0x73, 0x0E, (byte)0xED,
                (byte)0xBA, 0x31, (byte)0xF5, 0x3A, (byte)0xBB, 0x0B, 0x6C, (byte)0xA5, (byte)0x89,
                (byte)0xE0, (byte)0x9D, (byte)0xE4, (byte)0xE8, (byte)0x94, (byte)0xEE, 0x45, 0x5E,
                0x61, 0x40, (byte)0x80, 0x73, (byte)0x99, 0x75, (byte)0x9A, (byte)0xDA, (byte)0xAF,
                (byte)0xA0, 0x69, (byte)0xEE, (byte)0xC7, (byte)0xC0, 0x16, 0x47, (byte)0xBB, 0x17,
                0x3D, (byte)0xCB, 0x17, (byte)0xF5, 0x5D, 0x22, (byte)0xAF, 0x49, (byte)0xA1,
                (byte)0x80, 0x71, (byte)0xB7, 0x48, (byte)0xC5, (byte)0xC2, (byte)0xED, (byte)0xD7,
                (byte)0xF7, (byte)0xA8, 0x29, (byte)0xC6, 0x32 };
        byte[] digest = PBKDF2.shaHMAC("SHA512", key, message);
        assertEquals(expectedDigest.length, digest.length);
        for (int i = 0; i < digest.length; i++) {
            assertEquals(digest[i], expectedDigest[i]);
        }
    }

    public void testSha512HmacEmptyMessage () {
        byte[] key;
        try {
            key = "secret".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            key = "secret".getBytes();
        }
        byte[] message;
        try {
            message = "".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            message = "".getBytes();
        }
        byte[] expectedDigest = new byte[] { (byte)0xB0, (byte)0xE9, 0x65, 0x0C, 0x5F, (byte)0xAF,
                (byte)0x9C, (byte)0xD8, (byte)0xAE, 0x02, 0x27, 0x66, 0x71, 0x54, 0x54, 0x24, 0x10,
                0x45, (byte)0x89, (byte)0xB3, 0x65, 0x67, 0x31, (byte)0xEC, 0x19, 0x3B, 0x25,
                (byte)0xD0, 0x1B, 0x07, 0x56, 0x1C, 0x27, 0x63, 0x7C, 0x2D, 0x4D, 0x68, 0x38,
                (byte)0x9D, 0x6C, (byte)0xF5, 0x00, 0x7A, (byte)0x86, 0x32, (byte)0xC2, 0x6E,
                (byte)0xC8, (byte)0x9B, (byte)0xA8, 0x0A, 0x01, (byte)0xC7, 0x7A, 0x6C, (byte)0xDD,
                0x38, (byte)0x9E, (byte)0xC2, (byte)0x8D, (byte)0xB4, 0x39, 0x01 };
        byte[] digest = PBKDF2.shaHMAC("SHA512", key, message);
        assertEquals(expectedDigest.length, digest.length);
        for (int i = 0; i < digest.length; i++) {
            assertEquals(digest[i], expectedDigest[i]);
        }
    }

    public void testSha512HmacEmptyKey () {
        byte[] key;
        try {
            key = "".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            key = "".getBytes();
        }
        byte[] message;
        try {
            message = "message".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            message = "message".getBytes();
        }
        byte[] expectedDigest = new byte[] { 0x08, (byte)0xFC, (byte)0xE5, 0x2F, 0x63, (byte)0x95,
                (byte)0xD5, (byte)0x9C, 0x2A, 0x3F, (byte)0xB8, (byte)0xAB, (byte)0xB2, (byte)0x81,
                (byte)0xD7, 0x4A, (byte)0xD6, (byte)0xF1, 0x12, (byte)0xB9, (byte)0xA9, (byte)0xC7,
                (byte)0x87, (byte)0xBC, (byte)0xEA, 0x29, 0x0D, (byte)0x94, (byte)0xDA, (byte)0xDB,
                (byte)0xC8, 0x2B, 0x2C, (byte)0xA3, (byte)0xE5, (byte)0xE1, 0x2B, (byte)0xF2, 0x27,
                0x7C, 0x7F, (byte)0xED, (byte)0xBB, 0x01, 0x54, (byte)0xD5, 0x49, 0x3E, 0x41,
                (byte)0xBB, 0x74, 0x59, (byte)0xF6, 0x3C, (byte)0x8E, 0x39, 0x55, 0x4E, (byte)0xA3,
                0x65, 0x1B, (byte)0x81, 0x24, (byte)0x92 };
        byte[] digest = PBKDF2.shaHMAC("SHA512", key, message);
        assertEquals(expectedDigest.length, digest.length);
        for (int i = 0; i < digest.length; i++) {
            assertEquals(digest[i], expectedDigest[i]);
        }
    }

    public void testSha512 () {
        byte[] salt;
        try {
            salt = "pepper".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            salt = "pepper".getBytes();
        }
        byte[] message;
        try {
            message = "message".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            message = "message".getBytes();
        }
        byte[] expected = new byte[] { 0x26, 0x46, (byte)0xf9, (byte)0xcc, (byte)0xb5, (byte)0x8d,
                0x21, 0x40, 0x68, 0x15, (byte)0xba, (byte)0xfc, 0x62, 0x24, 0x57, 0x71, (byte)0xbf,
                (byte)0x80, (byte)0xaa, (byte)0xa0, (byte)0x80, (byte)0xa6, 0x33, (byte)0xff, 0x1b,
                (byte)0xdd, 0x66, 0x0e, (byte)0xb4, 0x4f, 0x36, (byte)0x9a, (byte)0x89, (byte)0xda,
                0x48, (byte)0xfb, 0x04, 0x1c, 0x55, 0x51, (byte)0xa1, 0x18, (byte)0xde, 0x20,
                (byte)0xcf, (byte)0xb8, (byte)0xb9, 0x6b, (byte)0x92, (byte)0xe7, (byte)0xa9,
                (byte)0x94, 0x54, 0x25, (byte)0xba, (byte)0x88, (byte)0x9e, (byte)0x9a, (byte)0xd6,
                0x45, 0x61, 0x45, 0x22, (byte)0xeb };
        byte[] actual = PBKDF2.hmac("SHA512", message, salt, 3);
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    public void testSha512EmptySalt () {
        byte[] salt;
        try {
            salt = "".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            salt = "".getBytes();
        }
        byte[] message;
        try {
            message = "message".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            message = "message".getBytes();
        }
        byte[] expected = new byte[] { (byte)0xb8, (byte)0xec, 0x13, (byte)0xcf, (byte)0xc9,
                (byte)0xb9, (byte)0xd4, (byte)0x9c, (byte)0xa1, 0x14, 0x30, 0x18, (byte)0xce,
                (byte)0x84, 0x13, (byte)0xa9, 0x62, (byte)0xc0, (byte)0x9c, 0x00, 0x63,
                (byte)0xf3, 0x0a, 0x46, 0x6d, (byte)0xf8, 0x02, (byte)0x89, 0x74, 0x75,
                (byte)0xc5, 0x7f, 0x26, (byte)0x8d, (byte)0x91, (byte)0xcc, 0x56, (byte)0x8a,
                (byte)0xc1, (byte)0xb6, (byte)0xa9, (byte)0xf1, (byte)0x9b, 0x1a, 0x0d,
                (byte)0xb1, 0x0f, 0x30, 0x05, (byte)0x8f, (byte)0xb7, (byte)0xa4, 0x53,
                (byte)0xb2, 0x67, 0x50, 0x10, (byte)0xef, 0x2b, 0x5f, (byte)0x96, 0x48, 0x7a,
                (byte)0xd3 };
        byte[] actual = PBKDF2.hmac("SHA512", message, salt, 3);
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    public void testSha512EmptyMessage () {
        byte[] salt;
        try {
            salt = "pepper".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            salt = "pepper".getBytes();
        }
        byte[] message;
        try {
            message = "".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            message = "".getBytes();
        }
        byte[] expected = new byte[] { (byte)0x9d, (byte)0xd3, 0x31, (byte)0xfc, 0x67, 0x42, 0x1e,
                0x1d, (byte)0xce, 0x61, (byte)0x9c, (byte)0xbb, (byte)0xb5, 0x17, 0x17, 0x0e, 0x2d,
                (byte)0xc3, 0x25, 0x49, 0x1d, 0x34, 0x26, 0x42, 0x56, 0x30, (byte)0xc4, (byte)0xc0,
                0x1f, (byte)0xd0, (byte)0xec, (byte)0xa8, (byte)0xd8, (byte)0xf5, 0x35, (byte)0xd6,
                (byte)0xb0, 0x55, 0x5a, 0x2a, (byte)0xa4, 0x3e, (byte)0xfb, (byte)0xc9, 0x14, 0x1e,
                0x3d, (byte)0xd7, (byte)0xed, (byte)0xae, (byte)0xf8, (byte)0xb1, 0x27, (byte)0x8a,
                (byte)0xc3, 0x4e, (byte)0xab, (byte)0xfc, 0x2d, (byte)0xb7, 0x35, (byte)0xd9,
                (byte)0x92, (byte)0xee };
        byte[] actual = PBKDF2.hmac("SHA512", message, salt, 3);
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    public void testSha512LongMessage () {
        byte[] salt;
        try {
            salt = "pepper".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            salt = "pepper".getBytes();
        }
        String msgString = "ThisMessageIsLongerThanSixtyFourCharactersWhichLeadsToTheSituation" +
                "ThatTheMessageHasToBeHashedWhenCalculatingTheHmac";
        byte[] message;
        try {
            message = msgString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            message = msgString.getBytes();
        }
        byte[] expected = new byte[] { (byte)0xef, (byte)0xc8, (byte)0xe7, 0x34, (byte)0xed, 0x5b,
                0x56, 0x57, (byte)0xac, 0x22, 0x00, 0x46, 0x75, 0x4b, 0x7d, 0x1d, (byte)0xbe,
                (byte)0xa0, 0x09, (byte)0x83, (byte)0xf1, 0x32, 0x09, (byte)0xb1, (byte)0xec, 0x1d,
                0x0e, 0x41, (byte)0x8e, (byte)0x98, (byte)0x80, 0x7c, (byte)0xba, 0x10, 0x26,
                (byte)0xd3, (byte)0xed, 0x3f, (byte)0xa2, (byte)0xa0, (byte)0x9d, (byte)0xfa, 0x43,
                (byte)0xc0, 0x74, 0x44, 0x7b, (byte)0xf4, 0x77, 0x7e, 0x70, (byte)0xe4, (byte)0x99,
                (byte)0x9d, 0x29, (byte)0xd2, (byte)0xc2, (byte)0xf8, 0x4d, (byte)0xc5, 0x15, 0x02,
                (byte)0xa1, (byte)0x95 };
        byte[] actual = PBKDF2.hmac("SHA512", message, salt, 3);
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    public void testSha384 () {
        byte[] salt;
        try {
            salt = "salt".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            salt = "salt".getBytes();
        }
        byte[] message;
        try {
            message = "message".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            message = "message".getBytes();
        }
        byte[] expected = new byte[] { (byte)0xdc, (byte)0xbe, (byte)0xb0, (byte)0xb9, (byte)0x9a,
                0x4c, (byte)0xf4, (byte)0xd1, (byte)0xc9, (byte)0xc1, (byte)0xe8, (byte)0xf6, 0x30,
                (byte)0xf3, (byte)0xaa, (byte)0x86, 0x37, (byte)0xc8, (byte)0x90, 0x6f, 0x1c, 0x3e,
                0x1c, 0x78, (byte)0xfb, 0x4f, 0x46, 0x2b, 0x16, 0x0d, (byte)0xf2, 0x0f, 0x74, 0x35,
                (byte)0xbd, (byte)0xd6, (byte)0xa9, 0x04, (byte)0xdd, 0x3c, 0x3e, (byte)0xde, 0x7f,
                (byte)0xf0, 0x4b, (byte)0xc5, 0x3e, (byte)0x90 };
        byte[] actual = PBKDF2.hmac("SHA384", message, salt, 3);
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    public void testSha256 () {
        byte[] salt;
        try {
            salt = "salt".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            salt = "salt".getBytes();
        }
        byte[] message;
        try {
            message = "message".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            message = "message".getBytes();
        }
        byte[] expected = new byte[] { (byte)0xdb, 0x78, (byte)0xc5, 0x09, 0x14, 0x44, (byte)0x94,
                0x0f, (byte)0x96, 0x42, (byte)0xfc, (byte)0xe5, 0x19, 0x09, 0x7e, (byte)0xe7,
                (byte)0xad, (byte)0xfe, (byte)0xb3, 0x38, (byte)0xfd, 0x69, 0x70, (byte)0x85, 0x51,
                0x35, 0x53, (byte)0x90, 0x20, (byte)0xb5, 0x3f, (byte)0xad };
        byte[] actual = PBKDF2.hmac("SHA256", message, salt, 3);
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

}
