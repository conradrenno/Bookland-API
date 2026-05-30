# BOOKLAND - A Book Store Management System

## Introduction
Bookland should be an API for an ecommerce to sell books online. It provides functionalities for managing books,
customers, and orders. The API is built using Spring Boot, with PostgreSQL as the database.

## Context
The main goal of Bookland is to provide a robust and scalable backend for an online bookstore. The system should
allow customers to browse and purchase books, while also providing administrative functionalities for managing
inventory and orders.

## Architecture
The architecture of Bookland must follow a Clean Architecture approach combined with DDD and SOLID principles where I want a very purist design
with internal controllers(that will be used by interface controllers that are responsible for the endpoints) to orchestrate
domain usecases and with no core domain dependencies on external frameworks(the idea is to be able to create a jar for the
domain and use it in other projects without any dependency issues).

### Important: 
This project must observe the DDD principles, so I want it to have every domain encapsulated in its own module with its own entities, 
repositories, services, and controllers(all following Clean Architecture). The domain layer should not have any dependencies on the infrastructure 
layer, and all interactions with external systems (like databases) should be done through interfaces defined in the 
domain layer. I want the domains easily turnable into microservices in the future if needed, so they should be designed 
to be as independent as possible.

## User Profiles
1. **Customer**: Can browse books, add them to the cart, and place orders.
2. **Admin**: Can manage books, view orders, and handle customer inquiries.
3. **Guest**: Can browse books but cannot make purchases or access account features.

## Domains
1. **Catalog**: Manages book information, including details and availability.
2. **Orders**: Handles order processing, including order creation, payment, and fulfillment.
3. **Authentication**: Manages user authentication and authorization, ensuring secure access to the system.
4. **Reviews**: Allows customers to leave reviews and ratings for books they have purchased.
5. **Inventory**: Manages the stock levels and availability of books in the store.
6. **Wishlist**: Allows customers to create and manage a wishlist of books they are interested in purchasing in the future.
7. **User** : Manages user profiles, including registration, login, and profile management.

## Database
Initially, we will use H2 for development and testing purposes, but the system should be designed to easily switch to PostgreSQL for production. The database will include tables for users, books, orders, reviews, inventory, and wishlists.

## Tests
Unit tests and integration tests must be implemented (using JUnit and Mockito) to ensure the reliability and correctness of the system's functionality.

## Stack
- **Backend**: Spring Boot
- **Database**: H2 (development), PostgreSQL (production)
- **Testing**: JUnit, Mockito
- **Build Tool**: Maven
- **Version Control**: Git
- **API Documentation**: Swagger/OpenAPI
- **Security**: Spring Security with JWT for authentication and authorization
- **Docker**: For containerization and deployment(docker-compose.yml with volume for database persistence)