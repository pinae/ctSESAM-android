package de.pinyto.passwordgenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * This class statically packs and unpacks String data.
 */
public class Packer {
    public static byte[] compress(String data) {
        byte[] encodedData;
        try {
            encodedData = data.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println("Compression error: UTF-8 is not supported. " +
                    "Using default encoding.");
            encodedData = data.getBytes();
        }

        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(encodedData);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        while (!deflater.finished()) {
            int byteCount = deflater.deflate(buf);
            baos.write(buf, 0, byteCount);
        }
        deflater.end();

        byte[] compressedData = baos.toByteArray();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        try {
            outputStream.write(ByteBuffer.allocate(4).putInt(encodedData.length).array());
        } catch (IOException e) {
            System.out.println("Compression error: Unable to write compressed data length.");
            e.printStackTrace();
        }
        try {
            outputStream.write(compressedData);
        } catch (IOException e) {
            System.out.println("Compression error: Unable to write compressed data.");
            e.printStackTrace();
        }

        return outputStream.toByteArray();
    }

    public static String decompress(byte[] data) {
        int length = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4)).getInt();
        if (length > 100000) {
            // This is a sanity check. More than 100kb of password settings make no sense.
            System.out.println("Decompression error: The trasferred length is too big.");
            return "";
        }
        Inflater inflater = new Inflater();
        inflater.setInput(data, 4, data.length-4);
        byte[] decompressedBytes = new byte[length];
        try {
            if (inflater.inflate(decompressedBytes) != length) {
                throw new AssertionError();
            }
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        inflater.end();
        try {
            return new String(decompressedBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println("Decompression error: UTF-8 is not supported. " +
                    "Using default encoding.");
            return new String(decompressedBytes);
        }
    }
}
