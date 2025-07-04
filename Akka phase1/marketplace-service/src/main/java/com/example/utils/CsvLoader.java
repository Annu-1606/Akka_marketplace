package com.example.utils;

import akka.actor.ActorRef;
import com.example.messages.ProductMessages;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;




public class CsvLoader {
    public static void loadProducts(ActorRef productRegion) {
        try {
            InputStream inputStream = CsvLoader.class.getClassLoader().getResourceAsStream("products.csv");

            if (inputStream == null) {
                System.err.println(" Error: products.csv file not found in resources folder!");
                return;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            br.readLine(); // Skip header

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 5) {  
                    String id = values[0];
                    String name = values[1];
                    String description = values[2];  
                    double price = Double.parseDouble(values[3]);
                    int stock_quantity = Integer.parseInt(values[4]);

                    // Send each product to ProductRegionActor
                    productRegion.tell(new ProductMessages.RegisterProduct(id, name, description, price, stock_quantity), ActorRef.noSender());
                    System.out.println(" Registered product: " + name + " (" + description + ")");
                }
            }
            System.out.println(" All products loaded and registered.");
        } catch (Exception e) {
            System.err.println(" Error loading products: " + e.getMessage());
        }
    }
}
