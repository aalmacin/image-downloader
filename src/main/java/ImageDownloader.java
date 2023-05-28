import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ImageDownloader {
    private static final Logger logger = Logger.getLogger(ImageDownloader.class.getName());
    private static final Path LOG_FILE_PATH = Paths.get("logs");

    interface Counter {
        void increment();

        int getCount();
    }

    public static void main(String[] args) throws IOException {
        String logFilePath = LOG_FILE_PATH.resolve("log.txt").toString();
        FileHandler fileHandler = new FileHandler(logFilePath);
        fileHandler.setFormatter(new SimpleFormatter());
        final Counter completedCounter = new Counter() {
            int count = 0;

            public void increment() {
                count++;
            }

            @Override
            public int getCount() {
                return count;
            }
        };
        logger.addHandler(fileHandler);

        if (args.length != 2) {
            logger.severe("Usage: java ImageDownloader <url> <filename>");
            System.exit(1);
        }

        String tsvFilePath = args[0];
        String imagesFolderPath = args[1];

        CSVParser csvParser = new CSVParserBuilder().withSeparator('\t')
                .build();

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(tsvFilePath))
                .withCSVParser(csvParser)
                .withSkipLines(0)
                .build()) {
            List<String[]> imageUrls = reader.readAll();
            List<String> failedUrls = new ArrayList<>();
            imageUrls.forEach((row) -> {
                if (row.length == 3) {
                    try {
                        File file = new File(getFileName(
                                imagesFolderPath,
                                row[1]
                        ));
                        if (!file.exists()) {
                            logger.info("Downloading: " + row[0]);
                            logger.info("Saving to: " + row[1]);
                            byte[] imageBytes = downloadImage(row[0]);
                            byte[] pngBytes = convertToPNG(imageBytes);
                            saveImage(pngBytes, row[1], imagesFolderPath);
                            completedCounter.increment();
                            logger.info("Completed: " + completedCounter.getCount());
                        } else {
                            completedCounter.increment();
                            logger.info("File already exists: " + row[1]);
                            logger.info("Completed: " + completedCounter.getCount());
                        }
                    } catch (MalformedURLException e) {
                        logger.severe("Error downloading image: " + row[0]);
                        failedUrls.add(row[2]);
                    } catch (IOException e) {
                        logger.severe("Error saving image: " + row[1]);
                        failedUrls.add(row[2]);
                    } catch (Exception e) {
                        logger.severe("Error: " + e.getMessage());
                        failedUrls.add(row[2]);
                    }
                } else {
                    logger.severe("Error row size: " + Arrays.toString(row));
                }
            });
            if (!failedUrls.isEmpty()) {
                saveFailedImage(String.join("\n", failedUrls));
            }
        } catch (FileNotFoundException e) {
            logger.severe("File not found: " + tsvFilePath);
            System.exit(1);
        } catch (IOException e) {
            logger.severe("Error reading file: " + tsvFilePath);
            System.exit(1);
        } catch (CsvException e) {
            logger.severe("Error parsing file: " + tsvFilePath);
            System.exit(1);
        }
        logger.info("Completed: " + completedCounter.getCount());
    }

    private static void saveFailedImage(String content) {
        String failedImagesFilePath = "failed_images.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(failedImagesFilePath))) {
            writer.write(content);
            System.out.println("Data written to the file successfully.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file: " + e.getMessage());
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
        String imgFileName = getFileName(imagesFolderPath, fileName.trim());
        try (FileOutputStream fos = new FileOutputStream(imgFileName.trim())) {
            fos.write(pngBytes);
        } catch (IOException e) {
            logger.severe("Error saving image: " + imgFileName);
        }
    }

    private static byte[] downloadImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream in = url.openStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream();
        ) {
            byte[] b = new byte[2048];
            int bytesRead;
            while ((bytesRead = in.read(b)) != -1) {
                out.write(b, 0, bytesRead);
            }
            return out.toByteArray();
        }
    }

    //    private static byte[] convertToPNG(byte[] bytes) throws IOException, ImageReadException, ImageWriteException {
//        BufferedImage bufferedImage = Imaging.getBufferedImage(bytes);
//
//        return Imaging.writeImageToBytes(
//                bufferedImage,
//                ImageFormats.PNG,
//                null
//        );
//    }

    public static byte[] convertToPNG(byte[] imageBytes) throws IOException {
        // Read the image from the byte array
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        BufferedImage image = ImageIO.read(inputStream);

        // Create an output stream for the PNG image
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Write the image as PNG to the output stream
        ImageIO.write(image, "PNG", outputStream);

        // Retrieve the PNG byte array
        byte[] pngBytes = outputStream.toByteArray();

        // Clean up resources
        outputStream.close();
        inputStream.close();

        return pngBytes;
    }
}
