## Technical Architecture of the Backend of Mall System

Modern shopping malls require robust, distributed architectures to handle high traffic, complex transactions, and real-time data processing. Here’s a breakdown of a typical cloud-native microservices architecture:

### 1. Presentation Layer:
•  Responsive UIs built with Vue.js (web) , optimized for performance.

### 2. Application Layer (Microservices):
• Decoupled Services: Independent components handle core functions:

  Product Catalog: Manages SKUs, pricing, and categories
  
  Inventory Service: Real-time stock tracking
  
  Shopping Cart: Redis-backed ephemeral storage
  
  Order Management: Orchestrates checkout flows
  
  Payment Gateway: Integrates with Stripe/PayPal


### 3. Data Layer:
• Polyglot Persistence:

  Relational DBs (MsqlSQL): Handle transactions (orders, payments) with ACID compliance.
  
  Search Engines (Elasticsearch): Enable faceted product discovery.

  Caches (Redis): Accelerate session management and hot data.
  
• Event Streaming: Kafka processes real-time events (e.g., "order placed") for async communication.



