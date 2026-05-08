# StaffLaunch Portal

A full-stack, role-based Enterprise Portal designed to streamline employee onboarding, IT asset management, and internal knowledge distribution. 

This project was built to transition a standard organizational directory into an interactive, self-serve platform for both Administrators and Employees.

## 🚀 Features

**Authentication & Security**
* Role-Based Access Control (RBAC) separating `ROLE_ADMIN` and `ROLE_USER`.
* Secure password hashing using BCrypt.
* Registration workflow requiring Administrator approval before account activation.

**Administrator Dashboard**
* **Approvals:** Review and authorize new employee account requests.
* **Directory Management:** Full CRUD operations for organizational personnel records.
* **IT Asset Pre-Allotment:** Assign hardware bundles to employees during profile creation.
* **ITSM Helpdesk:** View, categorize, and resolve technical support tickets raised by employees.

**Employee Self-Service Portal**
* **Profile Management:** Secure self-editing of contact details and credentials.
* **IT Asset Desk:** View assigned hardware, voluntarily claim received assets, and digitally acknowledge receipt.
* **Ticketing System:** Report technical issues (with specific categories) and track resolution status.
* **Knowledge Base:** An interactive, interactive resource library featuring company SOPs and manuals, complete with progress tracking (Pending/Completed).

## 🛠️ Tech Stack

* **Backend:** Java 17, Spring Boot, Spring Security, Spring Data JPA
* **Frontend:** Thymeleaf, HTML5, Bootstrap 5, FontAwesome, JavaScript
* **Database:** H2 In-Memory Database (or easily swappable to MySQL/PostgreSQL)
* **Build Tool:** Maven

## ⚙️ How to Run Locally

1. Ensure you have **Java 17** and **Maven** installed.
2. Clone the repository.
3. Open a terminal in the root directory and run:
   ```bash
   mvn spring-boot:run
4. Access the application in your browser at http://localhost:8080.

## Default Administrator Credentials:

* **Email/Username:** admin@drait.edu.in / admin
* **Password:** kodnest123
(Note: A default admin is seeded automatically upon startup).
