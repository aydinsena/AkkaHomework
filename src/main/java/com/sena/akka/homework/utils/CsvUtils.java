package com.sena.akka.homework.utils;

import com.sena.akka.homework.actor.MasterGuardian;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvUtils {
    public static List<String> readCsv(String filename) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)) {
            if (in == null) {
                throw new FileNotFoundException("Could not get the resource " + filename);
            }
            Stream<String> content = new BufferedReader(new InputStreamReader(in)).lines();
            return content.collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Could not load resource " + filename);
        }
    }

    public static List<MasterGuardian.CsvEntry> readCsvAsCsvEntries(String filename) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)) {
            if (in == null) {
                throw new FileNotFoundException("Could not get the resource " + filename);
            }
            Stream<String> content = new BufferedReader(new InputStreamReader(in)).lines();


            return content
                    .map(x -> x.split(";"))
                    .filter(x -> {
                        try {
                            int i = Integer.parseInt(x[0]);
                        } catch (NumberFormatException nfe) {
                            return false;
                        }
                        return true;
                    })
                    .map(split -> new MasterGuardian.CsvEntry(Integer.parseInt(split[0]), split[1], split[2], split[3]))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Could not load resource " + filename);
        }
    }
}
