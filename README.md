# BSK All-in-One Clinic Management System

## Overview

BSK All-in-One Clinic Management System is a comprehensive, desktop-based application designed to streamline clinic operations. Built with Java Swing for the user interface and leveraging the Netty framework for client-server communication, it supports multiple connected computers, allowing for efficient patient management, appointment booking, medical check-ups, and billing.

## Key Features

*   **User Authentication & Management:** Secure login for clinic staff.
*   **Patient Queue Management:** Real-time updates for patient queues displayed on a TV screen and within the application.
*   **Check-Up Module (`CheckUpPage`):
    *   Detailed patient information entry and management.
    *   Symptom recording, diagnosis, and notes.
    *   Prescription management for medicines and services.
    *   Integration with `MedicineDialog` for adding medicines to prescriptions.
    *   Patient history tracking.
    *   Ability to call patients to specific rooms.
*   **Invoice Generation (`MedicineInvoice`):
    *   Dynamic PDF generation for medical invoices (prescriptions).
    *   Includes detailed sections for both medicines and services.
    *   Enhanced medicine information: dosage, notes, quantity with units.
    *   Professional PDF layout with clinic logo, patient/doctor details, and itemized costs.
    *   VND currency formatting with thousand separators.
    *   **NEW:** Check-Up ID barcode at the top of the invoice for quick scanning at checkout.
    *   Resizable PDF preview dialog with improved scroll sensitivity.
*   **Checkout & Billing Module (`CheckoutPage`):
    *   View queue of patients who have completed check-ups and are awaiting payment.
    *   Create new standalone bills for customers purchasing items without a check-up.
    *   **NEW:** Barcode scanning field to quickly add items to a bill (backend lookup pending).
    *   **NEW:** `StandaloneMedicineDialog` for adding medicines to standalone bills, featuring name and barcode search.
    *   Itemized bill display with subtotal, discount, and grand total.
    *   Functionality for processing payments and printing bills (backend and PDF generation pending).
*   **Dashboard (`DashboardPage`):
    *   Centralized overview of clinic statistics (placeholder data currently).
    *   Summary cards for key metrics.
*   **Modernized User Interface:**
    *   Consistent top navigation bar across `DashboardPage`, `CheckUpPage`, and `CheckoutPage`.
    *   Navigation items include icons and text, with visual states for hover and active selection.
    *   Improved layout with taller navigation bar and better spacing.
    *   User-friendly pop-up menus for user profile actions (logout, etc.).
*   **Data Management:**
    *   Functionality for managing patient data, inventory (stubbed), and user accounts (stubbed).
*   **Networking:** Utilizes Netty for robust client-server communication for data synchronization and requests.

## Core Technologies

*   **Java Swing:** For the graphical user interface (GUI).
*   **iTextPDF:** For generating PDF invoices and other documents.
*   **Netty:** For client-server networking and real-time communication.
*   **SLF4J (with a logging backend like Logback/Log4j):** For application logging.

## Main Modules / Pages

*   `MainFrame.java`: The main application window orchestrating page navigation.
*   `LandingPage.java`: Initial page for login/authentication.
*   `DashboardPage.java`: Overview and statistics.
*   `CheckUpPage.java`: For managing patient check-ups and prescriptions.
*   `CheckoutPage.java`: For handling billing and payments.
*   `PatientDataPage.java`: (Assumed) For managing patient records.
*   `InventoryPage.java`: (Assumed) For managing clinic inventory.
*   `UserPage.java`: (Assumed) For managing system users.
*   `InfoPage.java`: (Assumed) For clinic information or application help.

## Setup & Usage

1.  **Prerequisites:** Java Development Kit (JDK) installed (version based on project requirements).
2.  **Dependencies:** Ensure all Maven/Gradle dependencies (like iText, Netty, SLF4J, etc.) are correctly configured in your `pom.xml` or `build.gradle` file.
3.  **Running the Server:** Start the Netty server application.
4.  **Running the Client:** Launch the client application (`MainFrame`).
5.  Log in with appropriate staff credentials.
6.  Navigate through the different modules using the top navigation bar to manage clinic operations.

## Future Enhancements (TODOs from recent development)

*   **Checkout Page Backend:** Implement network requests for:
    *   Fetching the queue of patients awaiting payment.
    *   Loading full bill details for selected patients.
    *   Looking up medicines by barcode.
    *   Processing payments and updating bill status.
    *   Updating inventory after sales.
*   **Standalone Medicine Dialog:** Ensure medicine data from `GetMedInfoResponse` includes barcodes and indices are handled correctly.
*   **Bill Printing:** Develop a PDF generation class for sales bills/receipts from the `CheckoutPage`.
*   Complete backend logic for all `//TODO` comments in `CheckoutPage.java` and `StandaloneMedicineDialog.java`.
*   Implement actual data fetching and display for the `DashboardPage`.
*   Flesh out `PatientDataPage`, `InventoryPage`, `UserPage`, and `InfoPage` functionalities.

This README provides a general overview. For specific class details and logic, please refer to the source code documentation and comments.
