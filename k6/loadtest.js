import http from "k6/http";
import { check, group } from "k6";
import * as crypto from "k6/crypto";
import { SharedArray } from "k6/data";

// --- Configuration ---
const SECRET_KEY = __ENV.SECRET_KEY || "default-insecure-key";

// --- Data Pools (using SharedArray for better memory efficiency) ---

const leaderEmails = new SharedArray("leaderEmails", function () {
    return ["alice@example.com", "bob@example.com", "carol@example.com", "dave@example.com"];
});

const teamNames = new SharedArray("teamNames", function() {
    return ["Code Wizards", "Data Dynamos", "Binary Brigade", "Pixel Pioneers", "Syntax Strikers"];
});

const tracks = new SharedArray("tracks", function() {
    return ["Hardware", "Software$Hospitality$BedSys", "Software$Fintech$PaymentGateway", "Software$Healthcare$PatientRecord"];
});

const firstNames = new SharedArray("firstNames", function() {
    return ["Alex", "Jordan", "Priya", "Mohammed", "Chen", "Fatima"];
});

const lastNames = new SharedArray("lastNames", function() {
    return ["Smith", "Gupta", "Lee", "Khan", "Wang", "Garcia"];
});

const institutions = new SharedArray("institutions", function() {
    return ["SSN College of Engineering", "IIT Madras", "NIT Trichy", "Stanford University", "MIT"];
});

const departments = new SharedArray("departments", function() {
    return ["Computer Science", "Electronics", "Information Technology", "Mechanical", "Design"];
});

const genders = new SharedArray("genders", function() {
    return ["Male", "Female"];
});
const eventIds = [ 31, 25, 32, 33, 34, 28, 38, 39, 41, 53, 55, 54, 52, 42, 40, 43, 45, 46, 59, 14,
    57, 51, 48, 56, 61, 64, 63, 58, 69, 47, 68, 72, 74, 71, 73, 75, 76, 62, 66, 36,
    70, 80, 79, 81, 83, 19, 90, 85, 89, 86, 87, 84, 88 ];


// --- k6 Options ---

export let options = {
    stages: [
        { duration: "10s", target: 15 }, // Ramp up to 20 VUs
        { duration: "15s", target: 20 },  // Stay at 20 VUs
        { duration: "10s", target: 0 },   // Ramp down
    ],
    thresholds: {
        http_req_failed: ["rate<0.01"], // HTTP errors should be less than 1%
        http_req_duration: ["p(95)<500"], // 95% of requests should complete under 500ms
    },
};

// --- Helper Function for Authentication ---

/**
 * Generates HMAC-SHA256 signature headers.
 */
function generateAuthHeaders(secret) {
    const timestamp = Math.floor(Date.now() / 1000).toString();
    const signature = crypto.hmac("sha256", secret, timestamp, "base64");

    return {
        "Content-Type": "application/json",
        "X-Timestamp": timestamp,
        "X-Signature": signature,
    };
}

// --- Main Test Logic ---

export default function () {
    const headers = generateAuthHeaders(SECRET_KEY);

    // --- Generate common data for both requests ---
    const paymentId = `pay_techfit${__VU}_iter${__ITER}`;
    const createdAt = new Date().toISOString();
    const leaderEmail = leaderEmails[Math.floor(Math.random() * leaderEmails.length)];
    const leaderPhone = `9${Math.floor(100000000 + Math.random() * 900000000)}`;


    // --- 1. ORIGINAL PAYMENT API (NOW COMMENTED OUT) ---
    group("API - Payment Flow", function () {
        const email = leaderEmails[Math.floor(Math.random() * leaderEmails.length)];
        const name = `User ${Math.floor(Math.random() * 10000)}`;
        const phone = `9${Math.floor(100000000 + Math.random() * 900000000)}`;
        const eventId = eventIds[Math.floor(Math.random() * eventIds.length)];

        const paymentPayload = JSON.stringify({
            emailID: email,
            name: name,
            paymentID: paymentId,
            phoneNumber: phone,
            createdAt: createdAt,
            eventBookingDetails: [{ "1": eventId }],
            type: "t",
        });

        const res = http.post(
            "http://localhost:8080/api/payment",
            paymentPayload,
            { headers: headers }
        );

        check(res, {
            "Payment request is successful (status 201)": (r) => r.status === 201,
        });
    });


    // // --- 2. NEW HACKATHON PAYMENT API ---
    // group("API - Hackathon Payment Flow", function () {
    //     // --- Helper function to get random item from an array ---
    //     const randomItem = (arr) => arr[Math.floor(Math.random() * arr.length)];
    //
    //     // --- Generate dynamic member data ---
    //     let member_emails = {}, member_names = {}, member_institutions = {};
    //     let member_phones = {}, member_gender = {}, member_department = {}, member_yearofstudy = {};
    //
    //     for (let i = 1; i <= 3; i++) {
    //         const fname = randomItem(firstNames);
    //         const lname = randomItem(lastNames);
    //         member_emails[i] = `${fname.toLowerCase()}.${lname.toLowerCase()}${Math.floor(Math.random() * 1000)}@example.com`;
    //         member_names[i] = `${fname} ${lname}`;
    //         member_institutions[i] = randomItem(institutions);
    //         member_phones[i] = `9${Math.floor(100000000 + Math.random() * 900000000)}`;
    //         member_gender[i] = randomItem(genders);
    //         member_department[i] = randomItem(departments);
    //         member_yearofstudy[i] = (Math.floor(Math.random() * 4) + 1).toString();
    //     }
    //
    //     // --- Construct the final payload ---
    //     const hackathonPayload = JSON.stringify({
    //         leader_email: leaderEmail,
    //         team_name: `${randomItem(teamNames)} ${__VU}`,
    //         track: randomItem(tracks),
    //         paymentID: paymentId,
    //         createdAt: createdAt,
    //         member_emails,
    //         member_names,
    //         member_institutions,
    //         member_phones,
    //         member_gender,
    //         member_department,
    //         member_yearofstudy,
    //     });
    //
    //     const res = http.post(
    //         "http://localhost:8080/api/hackathon-payment",
    //         hackathonPayload,
    //         { headers: headers }
    //     );
    //
    //     check(res, {
    //         "Hackathon payment is successful (status 201)": (r) => r.status === 201,
    //     });
    // });


    // --- 3. RECEIPT REQUEST (STILL ACTIVE) ---
    group("API - Receipt Flow", function () {
        const receiptPayload = JSON.stringify({
            emailID: leaderEmail, // Use leader's email for the receipt
            paymentID: paymentId,
            paidOn: createdAt,
            method: "Card",
            amount: "300.00", // Example fixed amount for hackathon
            phoneNumber: leaderPhone,
        });

        const receiptRes = http.post(
            "http://localhost:8080/restricted/receipt",
            receiptPayload,
            { headers: headers }
        );

        check(receiptRes, {
            "Receipt request is successful (status 200)": (r) => r.status === 201,
        });
    });
}