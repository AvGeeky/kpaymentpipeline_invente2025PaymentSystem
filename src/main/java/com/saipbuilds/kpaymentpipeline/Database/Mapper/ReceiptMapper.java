package com.saipbuilds.kpaymentpipeline.Database.Mapper;
import com.saipbuilds.kpaymentpipeline.Database.Entities.Receipt;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReceiptMapper {

    @Insert("INSERT INTO receipts(payment_id, paid_on, method, email, phone) " +
            "VALUES(#{paymentId}, #{paidOn}, #{method}, #{email}, #{phone})")
    void insertReceipt(Receipt receipt);

    @Select("SELECT * FROM receipts WHERE payment_id = #{paymentId}")
    Receipt findByPaymentId(String paymentId);

    @Select("SELECT * FROM receipts")
    List<Receipt> findAll();
}

