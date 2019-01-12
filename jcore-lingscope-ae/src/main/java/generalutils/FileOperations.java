package generalutils;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

public class FileOperations {
    public static List<String> readFile(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            return br.lines().collect(Collectors.toList());
        }
    }

    public static void writeFile(String filename, List<String> contents) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for (String s : contents) {
                bw.write(s);
                bw.newLine();
            }
        }
    }
}
