import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.imaging.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class ImageDownloader {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ImageDownloader <url> <filename>");
            System.exit(1);
        }

        String tsvFilePath = args[0];
        String imagesFolderPath = args[1];

        CSVParser csvParser = new CSVParserBuilder().withSeparator('\t').build();

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(tsvFilePath))
                .withCSVParser(csvParser)
                .build()) {
            List<String[]> imageUrls = reader.readAll();
            imageUrls.forEach((row) -> {
                if (row.length == 2) {
                    System.out.println("Downloading: " + row[0]);
                    System.out.println("Saving to: " + row[1]);
                    try {
                        File file = new File(getFileName(
                                imagesFolderPath,
                                row[1]
                        ));
                        if (!file.exists()) {
                            byte[] imageBytes = downloadImage(row[0]);
                            byte[] pngBytes = convertToPNG(imageBytes);
                            saveImage(pngBytes, row[1], imagesFolderPath);
                        } else {
                            System.out.println("File already exists: " + row[1]);
                        }
                    } catch (MalformedURLException e) {
                        System.out.println("Error downloading image: " + row[0]);
                    } catch (ImageWriteException e) {
                        System.out.println("Error writing image: " + row[1]);
                    } catch (IOException e) {
                        System.out.println("Error saving image: " + row[1]);
                    } catch (ImageReadException e) {
                        System.out.println("Error reading image: " + row[0]);
                    }
                } else {
                    System.out.println("Error row size: " + Arrays.toString(row));
                }
            });
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + tsvFilePath);
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Error reading file: " + tsvFilePath);
            System.exit(1);
        } catch (CsvException e) {
            System.out.println("Error parsing file: " + tsvFilePath);
            System.exit(1);
        }
    }

    private static String getFileName(String imagesFolderPath, String fileName) {
        return imagesFolderPath + "/" + fileName + ".png";
    }

    private static void saveImage(byte[] pngBytes, String fileName, String imagesFolderPath) {
        File imagesFolder = new File(imagesFolderPath);
        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs();
        }
        String imgFileName = getFileName(imagesFolderPath, fileName);
        try (FileOutputStream fos = new FileOutputStream(imgFileName)) {
            fos.write(pngBytes);
        } catch (IOException e) {
            System.out.println("Error saving image: " + imgFileName);
        }
    }

    private static byte[] downloadImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        try(InputStream in = url.openStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
        ){
            byte[] b = new byte[2048];
            int bytesRead;
            while ((bytesRead = in.read(b)) != -1) {
                out.write(b, 0, bytesRead);
            }
            return out.toByteArray();
        }
    }

    private static byte[] convertToPNG(byte[] bytes) throws IOException, ImageReadException, ImageWriteException {
        BufferedImage bufferedImage = Imaging.getBufferedImage(bytes);

        return Imaging.writeImageToBytes(
                bufferedImage,
                ImageFormats.PNG,
                null
        );
    }
}
