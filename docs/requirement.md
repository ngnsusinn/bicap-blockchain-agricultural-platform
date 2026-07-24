● English: Blockchain integration in clean agricultural production
● Vietnamese: Tích hợp Blockchain trong sản xuất nông sản sạch
● Abbreviation: BICAP
a. Context:
The Blockchain Agricultural Integration Project was launched to address the growing domestic demand for clean and traceable agricultural products. Many small and medium-sized farms and agricultural cooperatives in Vietnam’s agricultural regions face challenges in monitoring, managing production processes, and meeting food safety standards. Additionally, today’s consumers seek clear information about the production processes of the products they consume.

In response to this need, the project leverages Blockchain technology to offer a solution for tracking agricultural product origins from farm to table. With a user-friendly design that easily integrates into existing production workflows, this solution helps farms increase transparency while adapting to the varying production conditions across regions, such as climate and farming practices.

The project aims to foster trust and transparency for consumers while contributing to the sustainable development of Vietnam’s agricultural sector.

b. Proposed Solutions: he project integrates Blockchain in the clean agricultural production process to solve the following problems
● Optimize the monitoring of agricultural production processes.
● Implement product traceability using blockchain technology.
● Provide QR codes to support consumers in checking the origin of agricultural products
● Optimize costs and efficiency through analysis and forecasting.
● Connect farms with retail distributors.
● Functional requirement
o Farm Management (Web App)
• Register and log in to your account.
• Update owner personal information.
• Update Business License and information of farm.
• Purchase a package to use services.
• Payment of purchasing a package
• View processes of farming seasons.
• View detail of farming seasons.
• Create a farming season (information is saved into blockchain).
• Updating processes of farming seasons (information is saved into blockchain).
• Export a farming season.
• Generate QR Code with every export farming season (information is saved into blockchain).
• Register to push to the trading floor.
• View registration of pushing to the trading floor.
• Handle requests to buy agricultural products from Retailers.
• View information of Retailers that do contract.
• View and view detail of shipping processes.
• View reports of shipping processes.
• Get notifications of Retailer’s reports.
• Get notifications of Shipper’s reports
• Get notifications about temperature, humidity, pH during the day.
• Send reports to the admin.

o Retailer (Web App)
• Register and log in to your account.
• Update owner personal information.
• Update Business License and information of farm.
• Search for agricultural products on the trading floor.
• View detail agricultural products.
• Scan QR Code to retrieve product information about processes of seasons.
• Create a request to order agricultural products.
• Pay the deposit to order agricultural products.
• Cancel request to order agricultural products
• View history of orders.
• View detail and status of buying request.
• Get notification from Farm Management.
• Send notification to Farm Management.
• View and view detail processings shipping.
• Get notifications from Shippers.
• Confirm that products are shipped completely.
• Upload images products that are shipped completely.
• Get notifications from shippers.
• Send reports to the admin.

o Ship Driver (Mobile App)
• View and view detail of your shipments.
• Update the processes of shipments.
• Scan QR Code to track information of products when completely come farms.
• Confirm that completely to receive products.
• Confirm that completely to give products to retailers.
• Send reports to Shipping Manager.

o Shipping Manager (Web App)
• View successful orders between Retailers and Farm Managements.
• Create a shipment for every successful order.
• Cancel created-shipment.
• View processes of shipment.
• Management transportation vehicles (Create, Update, Delete, View)
• Management transportation drivers (Create, Update, Delete, View)
• Send reports to the admin.
• Send notifications to Farm Managements, Retailers.
• View reports from ship Driver.

○ Admin (Web App)
● Admin can create, view, edit, and delete other admin accounts, assigning roles and permissions as needed.
● Admin can view, approve, or reject new farm registrations to ensure legitimacy.
● Admin can access and manage farm details, including certification, contact information, and location.
● Admin can oversee all products registered on the platform, managing product categories, descriptions, and ensuring data accuracy.
● Admin can deploy, update, and manage smart contracts to maintain accuracy and transparency of product tracking data on the blockchain.

○ Guest (Web Application/Mobile App)
● Guest can receive general notifications about the platform, such as new product updates, educational articles, or events related to sustainable agriculture.
● Guest can use search and filter options to easily locate products by criteria like origin, product type, certification, and availability.
● Guest can access articles, videos, and other educational content related to agriculture, sustainable farming practices, and food safety.

● Non-functional requirement:
○ The system should be able to scale flexibly to handle a large number of users and data queries from multiple sources. Components like AWS/Google Cloud, Docker, and Redis 8.6 should be configured for seamless scaling.
○ The blockchain (VeChainThor) should support multiple concurrent transactions as IoT data volume increases or when product information requests surge.
○ The blockchain must ensure transparency and immutability of product origin data. VeChainThor encryption standards should be used to secure information, with access restricted based on roles (admin, retailer, transporter).
(*) 3.2. Main proposal content (including result and product)
a. Theory and practice (document):
● Students should apply the software development process and UML 2.0 in the modeling system.
● The documents include User Requirements, Software Requirement Specifications, Architecture Design, Detail Design, System Implementation, Testing Document, Installation Guide, source code, and deployable software packages.
● Server-side technologies:
○ Server: C# .NET, NodeJS
○ Database Design: MySQL 5.7.41, Redis 8.6
● Client-side technologies:
○ Web Client: ReactJS/NextJs
○ Mobile App: React native.
● Blockchain technologies:
○ Libraries: Solidity, JavaScript/TypeScript
○ Platform: VeChainThor.
○ Development Tools: VeChain ToolChain, VeChain Sync, VeChain Stats

b. Products:
○ Mobile application for Shipping Driver, Guest
○ Web API for System.
○ Web app for Admin, Farm Manager, Retailer, Shipping Manager.
c. Proposed Tasks:
○ Task package 1: Develop the Web application for the Admin system.
○ Task package 2: Develop the Web application for the Farm Management.
○ Task package 3: Develop the Web application for Retailer.
○ Task package 4: Develop the Web application for Shipping Management.
○ Task package 5: Develop the App Mobile for Shipping Driver.
○ Task package 6: Develop the App application (or Mobile App) for Guest.
○ Task package 7: Develop the Web API for the system.
○ Task package 8: Build – Deploy and Test the system.
○ Task package 9: Prepare all the required documents: System analysis and Design, Test plan, Installation manual, User manual.