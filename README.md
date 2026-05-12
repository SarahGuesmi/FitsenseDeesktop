# FitSense - Smart Fitness, Nutrition & Mental Health Platform (Java Version)
<img width="500" height="500" alt="sport-hero" src="https://github.com/user-attachments/assets/e242ee5d-6c5c-4664-87a9-34c1e6eee43b" />

---

## Overview

FitSense is a smart desktop platform developed in Java to help users manage their fitness activities, nutrition habits, and mental well-being.

The application allows users to:
- Track workouts and physical activities
- Monitor calories and daily water intake
- Follow personalized nutrition and training programs
- Manage user profiles and system roles
- Monitor mental health and well-being
- Submit workout feedback and ratings

FitSense integrates physical health, nutrition management, mental wellness, and feedback analysis tools into a single digital platform.

This project was developed as part of the PIDEV – 3rd Year Engineering Program at **Esprit School of Engineering** (Academic Year 2025-2026).

---

## Features

### **User Management**
- User registration and authentication
- Role management (Admin, Coach, User)
- Profile management
- Password reset system
- Secure authentication

### **Nutrition Management**
- Daily calorie tracking
- Water intake monitoring
- Nutrition goals management
- Healthy recipes consultation
- Nutrition dashboard

### **Fitness & Workout Management**
- Workout program management
- Exercise tracking
- Physical activity monitoring
- Personalized fitness recommendations

### **Feedback Module**
- Automatic feedback form displayed after workout completion
- Workout rating system
- User comments and experience sharing
- Feedback submission and management
- Coach access to users’ feedback and ratings
- Feedback analysis and performance evaluation
- User satisfaction monitoring
- Workout improvement recommendations

### **Mental Health Monitoring**
- Mood tracking
- Stress level monitoring
- Mental well-being evaluation
- Wellness suggestions

### **Dashboard**
- Personalized dashboard
- Fitness statistics
- Nutrition progress visualization
- Health analytics
- Feedback analytics

---

## Tech Stack

### Frontend
- JavaFX
- CSS
- FXML
- Scene Builder

### Backend
- Java
- JDBC
- Maven

### Database
- MySQL

### Tools
- GitHub
- GitHub Education
- IntelliJ IDEA
- NetBeans
- Scene Builder
- XAMPP

---

## Architecture

FitSense follows a layered architecture using Java technologies.

### **Model**
- Java entities and classes
- Database interactions using JDBC
- Data persistence management

### **View**
- JavaFX interfaces using FXML
- Responsive UI design with CSS
- Interactive desktop application components

### **Controller**
- Java controllers managing business logic
- Event handling and user interactions

This architecture ensures scalability, maintainability, and modular development.

---

## Contributors
- Ayari Farah
- Sarra Guesmi
- Ranim Chelly
- Nour Ammar
- Aziz Zarrouk  

Students of **Esprit School of Engineering**

---

## Academic Context

Developed at **Esprit School of Engineering – Tunisia**

Program: PIDEV – 3rd Year Engineering Program  
Academic Year: 2025-2026

This academic project allows students to apply Java desktop application development concepts, database design, and software engineering principles while building an innovative digital health platform.

---

## Getting Started

1. **Clone the repository**:  
   `git clone https://github.com/your-repository/fitsense-java.git`

2. **Install dependencies**:  
   Open the project using IntelliJ IDEA or NetBeans and install Maven dependencies.

3. **Configure the database**:  
   - Create a MySQL database named `fitsense`.  
   - Import the SQL script provided in the project.  
   - Update database credentials in the configuration file.

   Example:
   ```java
   String url = "jdbc:mysql://localhost:3306/fitsense";
   String user = "root";
   String password = "";
   ```

4. **Run the application**:  
   Launch the JavaFX application from your IDE.

5. **Test**:  
   Create an account and explore the platform features.

---

## Application Workflow

1. User creates an account or logs into the platform.
2. User accesses workout and nutrition programs.
3. User tracks fitness activities and mental well-being.
4. After completing a workout, a feedback form automatically appears.
5. User submits ratings and comments about the workout experience.
6. Coaches analyze users’ feedback and ratings to improve workout quality.
7. Dashboard displays statistics, progress, and analytics.

---

## Acknowledgments

Special thanks to:
- **Esprit School of Engineering** for providing the academic framework and resources.
- The GitHub Education Program for supporting student projects.
