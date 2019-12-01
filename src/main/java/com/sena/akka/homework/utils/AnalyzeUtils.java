package com.sena.akka.homework.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AnalyzeUtils {
    public static int unhash(String hexHash) {
        for (int i = 100000; i < 1000000; i++)
            if (hash(i).equals(hexHash))
                return i;
        throw new RuntimeException("Cracking failed for " + hexHash);
    }

    private static String hash(int number) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(String.valueOf(number).getBytes("UTF-8"));

            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < hashedBytes.length; i++) {
                stringBuffer.append(Integer.toString((hashedBytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            return stringBuffer.toString();
        }
        catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static int[] solve(int[] passwords, long from, long to) {
        for (long a = from; a < to; a++) {
            String binary = Long.toBinaryString(a);

            int[] prefixes = new int[passwords.length];
            for (int i = 0; i < prefixes.length; i++)
                prefixes[i] = 1;

            int i = 0;
            for (int j = binary.length() - 1; j >= 0; j--) {
                if (binary.charAt(j) == '1')
                    prefixes[i] = -1;
                i++;
            }

            if (sum(passwords, prefixes) == 0)
                return prefixes;
        }

        return new int[0];
    }

    //Prefixes
    public static int sum(int[] passwords, int[] prefixes) {
        int sum = 0;
        for (int i = 0; i < passwords.length; i++)
            sum += passwords[i] * prefixes[i];
        return sum;
    }
}
