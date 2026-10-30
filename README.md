# Online Examination System

A full-stack **Online Examination System** built with **Spring Boot, Java, Thymeleaf, Spring Security, PostgreSQL, Razorpay and Google Gemini AI**.

The system provides separate Admin and Student workflows for managing students, subjects, exams, questions, results, paid course enrollment and AI-powered assistance.

---

## 📌 Project Overview

The Online Examination System is designed to manage the complete examination workflow:

- Student registration and login
- Admin authentication and administration
- Student management
- Subject/course management
- Free and paid subjects
- Razorpay payment integration
- Course enrollment and subscription expiry
- Exam creation and management
- Question management
- Online exam attempts
- Automatic result handling
- Email communication
- AI chatbot for student assistance
- Secure role-based access using Spring Security

---

## 🚀 Main Features

### 👨‍💼 Admin Features

The Admin dashboard provides centralized management of the examination system.

- Admin login
- Dashboard
- Create/manage administrator accounts
- Manage students
- Delete student accounts
- Reset student passwords
- Manage subjects/courses
- Create and manage exams
- Assign exams to subjects
- Add, edit and delete questions
- View examination results
- View payment/enrollment records
- View successful payments and revenue
- Email center for sending emails to students
- Role-based access control

---

### 👨‍🎓 Student Features

Students can use the system to access subjects, enroll in courses and take examinations.

- Student registration
- Student login/logout
- Student dashboard
- View available subjects
- Access free subjects
- Purchase paid subjects
- Razorpay online payment
- Course enrollment
- Subscription/access expiry handling
- View available exams
- Attempt exams
- Submit answers
- View examination results
- AI Exam Assistant chatbot

---

### 🤖 AI Exam Assistant

The project includes an AI-powered chatbot using **Google Gemini**.

Students can ask questions related to:

- Examinations
- Exam instructions
- Subjects
- Course enrollment
- Payments
- Results
- Java
- Python
- CSE
- Programming
- Data Structures
- Algorithms
- Databases
- Web Development

The chatbot is integrated into the student interface and communicates with the backend through a secure API endpoint.

---

### 💳 Razorpay Payment Integration

Paid subjects/courses can be purchased through Razorpay.

Payment workflow:

1. Student selects a paid subject.
2. Backend creates a Razorpay order.
3. Razorpay Checkout processes the payment.
4. Payment details are returned to the application.
5. Backend verifies the Razorpay signature.
6. Successful enrollment is stored in PostgreSQL.
7. Subscription expiry is calculated according to the selected duration.

Supported subscription types include:

- FREE
- MONTHLY
- YEARLY
- LIFETIME

Razorpay should be configured using environment variables. Never commit API secrets to GitHub.

---

### 📧 Email Center

Administrators can send emails from the Admin Email Center.

The application uses Gmail SMTP with:

- SMTP host: `smtp.gmail.com`
- SMTP port: `587`
- STARTTLS
- SMTP authentication

Email credentials are supplied through environment variables.

---

## 🛠️ Technology Stack

### Backend

- Java 17
- Spring Boot 3.2.0
- Spring MVC
- Spring Security
- Spring Data JPA
- Hibernate
- Maven

### Frontend

- Thymeleaf
- HTML5
- CSS3
- JavaScript

### Database

- PostgreSQL

### Integrations

- Razorpay
- Google Gemini AI
- Gmail SMTP

### Development Tools

- Git
- GitHub
- IntelliJ IDEA / VS Code
- pgAdmin
- Maven Wrapper

---

## 📂 Project Structure

```text
online-exam-system-master/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/exam/
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── model/
│   │   │       ├── repository/
│   │   │       └── service/
│   │   │
│   │   └── resources/
│   │       ├── static/
│   │       ├── templates/
│   │       └── application.properties
│   │
│   └── test/
│
├── uploads/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

## ⚙️ Requirements

Install the following before running the project:

- Java 17
- PostgreSQL
- Git
- Internet connection for external services
- A Google Gemini API key
- Razorpay Test Mode credentials
- Gmail SMTP credentials if email functionality is required

Maven does not need to be installed separately because the project includes the Maven Wrapper.

---

# 🗄️ PostgreSQL Database Setup

Create a PostgreSQL database named:

```text
online_exam_db
```

The application uses:

```text
Host: localhost
Port: 5432
Database: online_exam_db
Username: postgres
```

The project uses Hibernate:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Therefore, the application can update the database schema without requiring the database to be recreated every time.

---

# 🔐 Environment Variables

Sensitive credentials should not be written directly into source code.

Configure the following environment variables before starting the application:

```text
DB_PASSWORD
MAIL_USERNAME
MAIL_PASSWORD
RAZORPAY_KEY_ID
RAZORPAY_KEY_SECRET
GEMINI_API_KEY
```

The application properties use these variables instead of storing secrets directly.

### Windows PowerShell example

```powershell
$env:DB_PASSWORD="your_postgresql_password"
$env:MAIL_USERNAME="your_gmail_address"
$env:MAIL_PASSWORD="your_gmail_app_password"
$env:RAZORPAY_KEY_ID="your_razorpay_test_key"
$env:RAZORPAY_KEY_SECRET="your_razorpay_test_secret"
$env:GEMINI_API_KEY="your_gemini_api_key"
```

Do not commit real credentials or API keys to GitHub.

---

# ▶️ How to Run the Project

## 1. Open the project directory

Open PowerShell in the project root:

```text
online-exam-system-master
```

---

## 2. Configure PostgreSQL

Make sure PostgreSQL is running and the database exists:

```text
online_exam_db
```

---

## 3. Configure environment variables

Set the required environment variables described above.

---

## 4. Start the Spring Boot application

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Alternatively, the project can be opened in an IDE and run as a Spring Boot application.

---

## 5. Open the application

The application runs on port:

```text
7890
```

Open:

```text
http://localhost:7890
```

---

# 🔑 Authentication and Roles

The application uses Spring Security with role-based authorization.

### ADMIN

Administrators can access:

```text
/admin/**
```

### STUDENT

Students can access:

```text
/student/**
/exam/**
```

The login system redirects users according to their role.

---

# 🧪 Examination Workflow

The typical examination workflow is:

```text
Admin
  ↓
Create Subject
  ↓
Create Exam
  ↓
Assign Exam to Subject
  ↓
Add Questions
  ↓
Publish/Make Exam Available
  ↓
Student
  ↓
Access Subject
  ↓
Attempt Exam
  ↓
Submit Answers
  ↓
View Result
```

---

# 💰 Paid Course Workflow

```text
Student
  ↓
Select Paid Subject
  ↓
Create Razorpay Order
  ↓
Razorpay Checkout
  ↓
Payment
  ↓
Signature Verification
  ↓
Course Enrollment
  ↓
Access Subject
  ↓
Subscription Expiry
```

---

# 🤖 Chatbot Workflow

```text
Student
  ↓
Enter Question
  ↓
Student Dashboard Chatbot
  ↓
/api/chatbot/message
  ↓
GeminiService
  ↓
Google Gemini API
  ↓
AI Response
  ↓
Chatbot UI
```

The application also handles temporary AI-service failures and retries the Gemini request before returning a fallback response.

---

# 📧 Email Workflow

```text
Admin
  ↓
Email Center
  ↓
Enter Recipient
  ↓
Enter Subject
  ↓
Enter Message
  ↓
Gmail SMTP
  ↓
Email Sent
```

---

# 🔒 Security

Security features include:

- Spring Security authentication
- BCrypt password hashing
- Role-based authorization
- CSRF protection
- Secure login/logout
- Protected Admin routes
- Protected Student routes
- Razorpay payment signature verification
- Environment-based secret configuration

---

# 🧩 Important API Endpoints

### Authentication

```text
/login
/register
/logout
```

### Payment

```text
POST /api/payment/create-order/{subjectId}
POST /api/payment/verify
```

### Chatbot

```text
POST /api/chatbot/message
```

### Admin

```text
/admin/dashboard
/admin/students
/admin/exams
/admin/questions
/admin/results
/admin/payments
/admin/email-center
```

### Student

```text
/student/dashboard
/student/**
/exam/**
```

---

# 🗃️ Main Backend Components

The project contains components for:

- Users
- Subjects
- Exams
- Questions
- Exam Attempts
- Student Answers
- Exam Results
- Course Enrollments
- Payments
- Authentication
- Email
- AI Chatbot

The application follows a layered Spring Boot architecture:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
JPA / Hibernate
    ↓
PostgreSQL
```

---

# 🧑‍💻 Development

### Build the project

```powershell
.\mvnw.cmd clean package
```

### Run the project

```powershell
.\mvnw.cmd spring-boot:run
```

### Check Git status

```powershell
git status
```

### Commit changes

```powershell
git add .
git commit -m "Update project"
```

### Push changes to GitHub

```powershell
git push
```

---

# 📌 Configuration File

The main application configuration is:

```text
src/main/resources/application.properties
```

It contains configuration for:

- Application port
- PostgreSQL
- JPA/Hibernate
- File uploads
- Gmail SMTP
- Razorpay
- Environment variables

Secrets should always remain outside the repository.

---

# 📦 Deployment Notes

Before deploying the application:

1. Configure PostgreSQL.
2. Configure all required environment variables.
3. Configure Razorpay production credentials if moving from test mode.
4. Configure Gmail SMTP credentials.
5. Configure Gemini API access.
6. Build the Spring Boot application.
7. Deploy the generated application to the target server.

Never expose:

```text
DB_PASSWORD
MAIL_PASSWORD
RAZORPAY_KEY_SECRET
GEMINI_API_KEY
```

in source code or public repositories.

---

# 📝 Project Status

The project currently includes:

- ✅ Admin authentication
- ✅ Student authentication
- ✅ Student management
- ✅ Subject/course management
- ✅ Exam management
- ✅ Question management
- ✅ Online examination
- ✅ Results
- ✅ Free and paid subjects
- ✅ Razorpay payment integration
- ✅ Course enrollment
- ✅ Subscription expiry
- ✅ Email Center
- ✅ AI Exam Assistant chatbot
- ✅ PostgreSQL database
- ✅ Spring Security
- ✅ Git/GitHub version control

---

# 👥 Intended Users

### Administrator

Manages:

- Students
- Subjects
- Exams
- Questions
- Results
- Payments
- Emails

### Student

Uses:

- Subjects
- Course enrollment
- Payments
- Exams
- Results
- AI Exam Assistant

---

# 📄 License

This project is developed as an academic/project implementation.

Copyright © 2026.
