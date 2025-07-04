package marketplace.marketplace;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CsvLoader {

    // Inner class to hold product details
    public static class ProductData {
        public final int id;
        public final String name;
        public final String description;
        public final int price;
        public final int stockQuantity;

        public ProductData(int id, String name, String description, int price, int stockQuantity) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.stockQuantity = stockQuantity;
        }
    }

    // Method to load products from a CSV file
    public static List<ProductData> loadProducts(String fileName) {
        List<ProductData> products = new ArrayList<>();

        try (InputStream is = CsvLoader.class.getResourceAsStream("/" + fileName)) {
            if (is == null) {
                throw new RuntimeException("Could not find file: " + fileName); // Throw error if file not found
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)); // Read the file with UTF-8 encoding

            String line;
            boolean firstLine = true; // Flag to skip the header line
            while ((line = br.readLine()) != null) {
                if (firstLine) { // Skip CSV header
                    firstLine = false;
                    continue;
                }
                String[] values = line.split(","); // Split line by commas
                if (values.length < 5) continue; // Skip incomplete lines

                // Parse product fields from CSV
                int id = Integer.parseInt(values[0].trim());
                String name = values[1].trim();
                String description = values[2].trim();
                int price = Integer.parseInt(values[3].trim());
                int stockQuantity = Integer.parseInt(values[4].trim());

                // Create a ProductData object and add it to the list
                ProductData product = new ProductData(id, name, description, price, stockQuantity);
                products.add(product);

                System.out.println("Loaded product: ID=" + id + ", Name=" + name); // Log the loaded product
            }
            System.out.println("Loaded all products"); // Log after loading all products
        } catch (Exception e) {
            e.printStackTrace(); // Print any exceptions
        }

        return products; // Return the list of loaded products
    }
}
