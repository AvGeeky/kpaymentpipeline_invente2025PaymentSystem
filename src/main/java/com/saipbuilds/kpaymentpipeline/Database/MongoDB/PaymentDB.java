package com.saipbuilds.kpaymentpipeline.Database.MongoDB;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.saipbuilds.kpaymentpipeline.DTO.PaymentReceivedEvent;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class PaymentDB {

    static String uri = System.getenv("API_KEY");
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> collection;

    static {
        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .build();
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("InventePaymentDump"); // Database name
            collection = database.getCollection("Invente"); // Collection name
            log.info("Connected to MongoDB successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean insertPayment(
            PaymentReceivedEvent event
    )
    {
        try {
            Document paymentDoc = new Document("emailID", event.getEmailID())
                    .append("paymentID", event.getPaymentID())
                    .append("name", event.getName())
                    .append("phoneNumber", event.getPhoneNumber())
                    .append("createdAt", event.getCreatedAt())


                    .append("type", event.getType())
                    .append("assigned_by", event.getAssigned_by())
                    .append("institution", event.getInstitution());

            List<Map<String,Integer>> convertedList = event.getEventBookingDetails().stream()
                    .map(map -> map.entrySet().stream()
                            .collect(Collectors.toMap(
                                    e -> String.valueOf(e.getKey()),
                                    Map.Entry::getValue)))
                    .collect(Collectors.toList());
            paymentDoc.append("eventBookingDetails", convertedList);

            collection.insertOne(paymentDoc);
            log.info("Payment inserted successfully into MONGODB: " + event.getPaymentID());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
