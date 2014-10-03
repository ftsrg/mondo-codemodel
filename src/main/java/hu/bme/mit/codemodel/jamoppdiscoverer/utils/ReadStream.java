package hu.bme.mit.codemodel.jamoppdiscoverer.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ReadStream {

    public static String from(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static List<String> asLines(InputStream is) {
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line = null;

            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines;
    }
}
