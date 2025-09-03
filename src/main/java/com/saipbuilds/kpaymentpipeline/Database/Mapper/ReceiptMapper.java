package com.saipbuilds.kpaymentpipeline.Database.Mapper;
import com.saipbuilds.kpaymentpipeline.Database.Entities.Receipt;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReceiptMapper {


    @Insert("INSERT INTO receipts(payment_id, paid_on, method, email, phone, amount) " +
            "VALUES(#{paymentId}, #{paidOn}, #{method}, #{email}, #{phone}, #{amount}) " +
            "ON CONFLICT(payment_id) DO NOTHING")
    void insertReceipt(Receipt receipt);

    @Select("SELECT * FROM receipts WHERE payment_id = #{paymentId}")
    Receipt findByPaymentId(String paymentId);

    @Select("SELECT * FROM receipts")
    List<Receipt> findAll();

    @Select("SELECT COALESCE((SELECT passGenerated FROM receipts WHERE payment_id = #{paymentId}), false)")
    boolean isPassGenerated(String paymentId);


    @Update("UPDATE receipts SET passGenerated = TRUE WHERE payment_id = #{paymentId}")
    void markPassGeneratedTrue(String paymentId);
}

