package com.sena.akka.homework.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;

public class AnalyzeUtils {

    //password cracking
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

    //linear combination
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

    public static int sum(int[] passwords, int[] prefixes) {
        int sum = 0;
        for (int i = 0; i < passwords.length; i++)
            sum += passwords[i] * prefixes[i];
        return sum;
    }

    //dna analysis
    public static int longestOverlapPartner(int thisIndex, List<String> sequences) {
        int bestOtherIndex = -1;
        String bestOverlap = "";
        for (int otherIndex = 0; otherIndex < sequences.size(); otherIndex++) {
            if (otherIndex == thisIndex)
                continue;

            String longestOverlap = longestOverlap(sequences.get(thisIndex), sequences.get(otherIndex));

            if (bestOverlap.length() < longestOverlap.length()) {
                bestOverlap = longestOverlap;
                bestOtherIndex = otherIndex;
            }
        }
        return bestOtherIndex;
    }

    private static String longestOverlap(String str1, String str2) {
        if (str1.isEmpty() || str2.isEmpty())
            return "";

        if (str1.length() > str2.length()) {
            String temp = str1;
            str1 = str2;
            str2 = temp;
        }

        int[] currentRow = new int[str1.length()];
        int[] lastRow = str2.length() > 1 ? new int[str1.length()] : null;
        int longestSubstringLength = 0;
        int longestSubstringStart = 0;

        for (int str2Index = 0; str2Index < str2.length(); str2Index++) {
            char str2Char = str2.charAt(str2Index);
            for (int str1Index = 0; str1Index < str1.length(); str1Index++) {
                int newLength;
                if (str1.charAt(str1Index) == str2Char) {
                    newLength = str1Index == 0 || str2Index == 0 ? 1 : lastRow[str1Index - 1] + 1;

                    if (newLength > longestSubstringLength) {
                        longestSubstringLength = newLength;
                        longestSubstringStart = str1Index - (newLength - 1);
                    }
                } else {
                    newLength = 0;
                }
                currentRow[str1Index] = newLength;
            }
            int[] temp = currentRow;
            currentRow = lastRow;
            lastRow = temp;
        }
        return str1.substring(longestSubstringStart, longestSubstringStart + longestSubstringLength);
    }

    //hash mining
    public static String findHash(int content, String prefix, int prefixLength) {
        StringBuilder fullPrefixBuilder = new StringBuilder();
        for (int i = 0; i < prefixLength; i++)
            fullPrefixBuilder.append(prefix);

        Random rand = new Random(13);

        String fullPrefix = fullPrefixBuilder.toString();
        int nonce = 0;
        while (true) {
            nonce = rand.nextInt();
            String hash = hash(content + nonce);
            if (hash.startsWith(fullPrefix)) {
                return hash;
            }
        }
    }
}
