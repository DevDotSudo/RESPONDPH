# CHAPTER III: METHODOLOGY

## RESPOND-PH: Resilient Emergency System for Prioritized Operations, Notification Dissemination in the Philippines
### A Geospatial System for Vulnerability Assessment and Localized Emergency Communication

---

## 3.1 Introduction to the Methodology

This chapter presents the comprehensive methodology employed in the design, development, testing, and deployment of RESPOND-PH, a geospatial desktop information system for vulnerability assessment, prioritized disaster aid distribution, and localized emergency communication in the Philippines. The system was developed specifically for the Municipality of Banate, Iloilo, Philippines, as a proof of concept intended for scalable adoption by Philippine local government units (LGUs) in disaster response operations.

The development methodology follows the **Agile-Iterative Software Development Life Cycle (SDLC)** model, which was selected for its capacity to accommodate the evolving requirements inherent to disaster management information systems. The iterative nature of Agile allowed for continuous refinement of the vulnerability scoring algorithms, clustering mechanisms, and communication modules through successive sprints of development, testing, and stakeholder feedback.

This chapter is organized according to the sequential and iterative phases of the SDLC: (1) Planning and Feasibility Study, (2) Requirements Analysis, (3) System Design, (4) Implementation and Coding, (5) Testing and Quality Assurance, (6) Deployment and Maintenance, and (7) Evaluation. Each phase is discussed in exhaustive technical detail, documenting every design decision, algorithmic choice, architectural pattern, and implementation strategy employed throughout the project.

---

## 3.2 Research Design

This study employed a **Design Science Research (DSR)** methodology, a problem-solving paradigm that seeks to create and evaluate IT artifacts intended to solve identified organizational problems. DSR was chosen because the primary objective of RESPOND-PH is to design and construct a novel information system artifact—a geospatial desktop application—that addresses the identified gap in vulnerability-based, prioritized emergency response communication within Philippine LGUs.

The DSR framework, as articulated by Hevner et al. (2004) and Peffers et al. (2007), prescribes the following activities, each of which was rigorously followed in this study:

1. **Problem Identification and Motivation** — Identifying the deficiency in existing disaster response systems within Philippine municipalities, specifically the absence of vulnerability-based beneficiary prioritization and offline emergency communication capabilities.

2. **Objectives of the Solution** — Defining the functional and non-functional objectives that the RESPOND-PH system must achieve, including automated vulnerability scoring, machine-learning-based clustering, geospatial mapping, and offline SMS dissemination.

3. **Design and Development** — Constructing the IT artifact (the RESPOND-PH desktop application) using established software engineering principles, design patterns, and algorithmic foundations.

4. **Demonstration** — Deploying the system in a controlled environment simulating real-world disaster scenarios in the Municipality of Banate.

5. **Evaluation** — Assessing the artifact's efficacy through functional testing, unit testing, integration testing, and user acceptance testing.

6. **Communication** — Documenting and disseminating the design knowledge through this dissertation.

---

## 3.3 Software Development Life Cycle (SDLC) Model: Agile-Iterative

### 3.3.1 Justification for the Agile-Iterative Model

The Agile-Iterative SDLC model was selected as the overarching development framework for RESPOND-PH based on the following considerations:

1. **Evolving Requirements** — Disaster management systems inherently involve complex, multi-stakeholder requirements that cannot be fully specified at the outset. The vulnerability indicators, scoring weights, and clustering parameters required iterative calibration with domain experts from the Municipal Disaster Risk Reduction and Management Office (MDRRMO).

2. **Incremental Delivery** — The modular architecture of RESPOND-PH (comprising distinct modules for beneficiary profiling, vulnerability assessment, disaster mapping, aid distribution, evacuation planning, and SMS communication) lent itself to incremental development, where each module could be developed, tested, and refined independently before integration.

3. **Rapid Prototyping** — The GSM hardware integration (SIM800C module) and geospatial mapping components required rapid prototyping and physical testing cycles that aligned with Agile sprint structures.

4. **Stakeholder Engagement** — Regular sprint reviews with LGU personnel ensured that the system aligned with operational workflows in Philippine municipal disaster response.

### 3.3.2 Sprint Structure

The development was organized into **12 iterative sprints**, each lasting approximately **2–3 weeks**, spanning the full development period. The sprint allocation was as follows:

| Sprint | Duration | Focus Area |
|--------|----------|------------|
| Sprint 1 | 2 weeks | Project setup, database schema design, technology stack selection |
| Sprint 2 | 2 weeks | Authentication module, admin management, session handling |
| Sprint 3 | 3 weeks | Beneficiary profiling module (CRUD operations, data encryption) |
| Sprint 4 | 2 weeks | Family members module and dependency ratio computation |
| Sprint 5 | 3 weeks | Vulnerability indicator scoring framework and HMIS integration |
| Sprint 6 | 3 weeks | Household score calculation engine and cascade update system |
| Sprint 7 | 3 weeks | Disaster management module and disaster damage assessment |
| Sprint 8 | 3 weeks | Fuzzy C-Means (FCM) and K-Means clustering algorithms for aid prioritization |
| Sprint 9 | 3 weeks | Geospatial mapping module (OpenStreetMap integration, tile rendering) |
| Sprint 10 | 3 weeks | SMS communication module (SIM800C GSM hardware + API integration) |
| Sprint 11 | 2 weeks | Evacuation planning and site allocation module |
| Sprint 12 | 3 weeks | Dashboard, AI news generation, reporting/printing, and final integration |

---

## 3.4 Phase 1: Planning and Feasibility Study

### 3.4.1 Problem Domain Analysis

The planning phase commenced with an extensive analysis of the problem domain — disaster response operations in Philippine municipalities. The following key deficiencies were identified through literature review, field observation, and interviews with MDRRMO personnel:

1. **Lack of Vulnerability-Based Prioritization** — Existing disaster response systems in Philippine LGUs distribute aid on a first-come-first-served basis or through manual assessment, without systematic quantification of individual or household vulnerability.

2. **Absence of Geospatial Context** — Aid distribution and evacuation planning decisions are made without geospatial visualization of beneficiary locations, disaster-affected zones, and evacuation site proximity.

3. **Communication Infrastructure Dependency** — During disasters, internet and cellular data infrastructure frequently fails, rendering internet-dependent communication systems inoperable. There is a critical need for offline communication capability.

4. **Language Barriers** — Emergency alerts disseminated in English or Filipino may not be effectively understood by populations in linguistically diverse regions. For the target area (Banate, Iloilo), Hiligaynon is the primary language of daily communication.

5. **Data Privacy Concerns** — Beneficiary data includes sensitive personal information (health status, disability type, income level, etc.) that requires robust encryption to comply with the Philippine Data Privacy Act of 2012 (Republic Act No. 10173).

### 3.4.2 Feasibility Assessment

#### 3.4.2.1 Technical Feasibility

A comprehensive technical feasibility assessment was conducted to evaluate the viability of the proposed system:

**Programming Language and Platform:**
- **Java 17 (LTS)** was selected as the primary programming language due to its platform independence, mature ecosystem, robust standard library for cryptographic operations, and strong support for desktop GUI development via JavaFX.
- **JavaFX 13** was selected as the GUI framework for its modern, CSS-styleable UI components, FXML-based declarative layout, and canvas-based rendering capabilities necessary for the mapping module.

**Database Management System:**
- **MySQL** was selected as the relational database management system (RDBMS) due to its widespread adoption, reliability, and compatibility with Java through the MySQL Connector/J JDBC driver (version 9.3.0).

**Build System:**
- **Apache Maven** was selected as the build automation and dependency management tool, utilizing a `pom.xml` configuration file for reproducible builds and centralized dependency resolution.

**Clustering Algorithms:**
- The technical feasibility of implementing **Fuzzy C-Means (FCM)** and **K-Means** clustering algorithms in Java was confirmed through proof-of-concept implementations using Apache Commons Math (version 3.6.1) and custom implementations.

**Hardware Communication:**
- The **SIM800C GSM module** was identified as the hardware platform for offline SMS communication. Technical feasibility was confirmed through prototyping with the **jSerialComm** library (version 2.11.4) for serial port communication.

**Geospatial Mapping:**
- **OpenStreetMap (OSM)** was identified as the mapping provider, with offline tile rendering achieved through pre-downloaded tile sets for the Municipality of Banate.

**AI Integration:**
- **Anthropic Claude API** (Claude Sonnet 4.5) was selected for AI-powered news generation and contextual emergency message composition, accessed via the Anthropic Java SDK (version 2.14.0).

**Data Security:**
- **AES-128-GCM (Galois/Counter Mode)** encryption was selected for at-rest data protection, with **BCrypt** hashing for password storage, both of which are implemented natively in Java's `javax.crypto` package.

#### 3.4.2.2 Economic Feasibility

The economic feasibility assessment considered the following factors:

- **Open-Source Software Stack** — Java, JavaFX, MySQL, OpenStreetMap, and Apache Maven are all open-source or freely available, minimizing software licensing costs.
- **Hardware Costs** — The SIM800C GSM module costs approximately ₱350–500 per unit, making it economically accessible to LGUs.
- **Infrastructure Requirements** — The system operates as a desktop application, eliminating the need for web hosting infrastructure. MySQL runs on a local server.
- **API Costs** — The Anthropic Claude API and SkySMS API operate on a pay-per-use model, with costs proportional to actual usage.

#### 3.4.2.3 Operational Feasibility

Operational feasibility was assessed by evaluating the end-users' (MDRRMO personnel) capacity to adopt and operate the system:

- **Desktop Application** — A desktop application was chosen over a web application to ensure operation during internet outages, which are common during disasters.
- **Intuitive GUI** — JavaFX's modern UI components, combined with FontAwesome icons (via FontAwesomeFX version 4.7.0-9.1.2) and custom CSS styling, provide a familiar and intuitive interface for non-technical users.
- **Splash Screen Loading** — A progressive loading mechanism (splash screen with progress bar) was implemented to provide visual feedback during system initialization, improving user confidence.

### 3.4.3 Technology Stack Summary

The complete technology stack selected during the planning phase is documented in the following table:

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Programming Language | Java (JDK) | 17 (LTS) | Core application logic |
| GUI Framework | JavaFX (with FXML) | 13 | Desktop user interface |
| Build Tool | Apache Maven | — | Dependency management and build automation |
| Database | MySQL | — | Persistent data storage |
| JDBC Driver | MySQL Connector/J | 9.3.0 | Database connectivity |
| Clustering Library | Apache Commons Math | 3.6.1 | Mathematical operations for FCM/K-Means |
| Serial Communication | jSerialComm | 2.11.4 | SIM800C GSM module communication |
| Encryption (Hashing) | jBCrypt | 0.4 | Password hashing (BCrypt) |
| JSON Processing | org.json | 20251224 | JSON data parsing |
| JSON Processing | Gson | 2.13.2 | JSON serialization/deserialization |
| JSON Processing | Jackson Databind | 2.21.0 | Object-JSON mapping |
| HTML Parsing | Jsoup | 1.22.1 | RSS feed and HTML content parsing |
| AI SDK | Anthropic Java SDK | 2.14.0 | Claude AI integration for news generation |
| AI SDK | Google GenAI | 1.38.0 | Google Generative AI integration |
| Icons | FontAwesomeFX | 4.7.0-9.1.2 | Icon set for UI components |
| UI Enhancement | FlatLaf | 3.6 | Modern look-and-feel theme |
| Date Picker | JDatePicker | 2.0.3 | Calendar-based date selection |
| Calendar | JCalendar | 1.4 | Date management utilities |
| PDF Generation | iText 7 Core | 9.5.0 | PDF report generation |
| Spreadsheet | Apache POI (OOXML) | 5.5.1 | Excel report generation |
| Collections | Google Guava | 33.0.0-jre | Enhanced collection utilities |
| Testing | JUnit Jupiter | 5.10.3 | Unit and integration testing |

---

## 3.5 Phase 2: Requirements Analysis

### 3.5.1 Functional Requirements

Through iterative requirements elicitation sessions with MDRRMO stakeholders, the following functional requirements were identified and documented:

#### 3.5.1.1 Module FR-01: Authentication and Session Management

| FR ID | Requirement Description |
|-------|------------------------|
| FR-01.1 | The system shall provide a splash screen with progressive loading indicators during initialization. |
| FR-01.2 | The system shall authenticate administrators using username and BCrypt-hashed password verification. |
| FR-01.3 | The system shall maintain user sessions through a singleton SessionManager, tracking the currently authenticated administrator. |
| FR-01.4 | The system shall preload all FXML scenes and dialog resources during splash screen initialization to ensure responsive navigation. |
| FR-01.5 | The system shall initialize all application services (Login, Admin, Beneficiary, Family Member, Disaster, Disaster Damage, Aid Type, Evacuation Site, Dashboard, Vulnerability Indicator, Disaster Mapping, Evacuation Site Mapping) during startup via the AppLoader. |
| FR-01.6 | The system shall establish a database connection through a thread-safe singleton DBConnection class using double-checked locking. |

#### 3.5.1.2 Module FR-02: Administrator Management

| FR ID | Requirement Description |
|-------|------------------------|
| FR-02.1 | The system shall allow creation of new administrator accounts with encrypted personal information (username, first name, middle name, last name). |
| FR-02.2 | The system shall support CRUD (Create, Read, Update, Delete) operations on administrator profiles. |
| FR-02.3 | The system shall validate password strength and enforce password matching during account creation. |
| FR-02.4 | The system shall encrypt administrator personal data using AES-128-GCM before database storage. |

#### 3.5.1.3 Module FR-03: Beneficiary Profiling

| FR ID | Requirement Description |
|-------|------------------------|
| FR-03.1 | The system shall capture comprehensive beneficiary profiles including: first name, middle name, last name, birth date, barangay, gender, marital status, solo parent status, latitude, longitude, mobile number, disability type, health condition, clean water access, sanitation facility, house type, ownership status, employment status, monthly income, educational level, and digital access. |
| FR-03.2 | The system shall encrypt all personally identifiable information (PII) using AES-128-GCM encryption before storage. |
| FR-03.3 | The system shall automatically compute an age-based vulnerability score upon beneficiary registration. |
| FR-03.4 | The system shall support geolocation capture through interactive map-based coordinate selection. |
| FR-03.5 | The system shall validate Philippine mobile phone numbers (10-15 digits with optional leading '+'). |
| FR-03.6 | The system shall support search, filtering, and pagination of beneficiary records. |
| FR-03.7 | The system shall decrypt beneficiary names for display purposes using the CryptographyManager singleton. |

#### 3.5.1.4 Module FR-04: Family Member Management

| FR ID | Requirement Description |
|-------|------------------------|
| FR-04.1 | The system shall associate family members with beneficiary households. |
| FR-04.2 | The system shall capture family member demographic data (name, age, gender, marital status, disability, health condition, employment, education) with encryption. |
| FR-04.3 | The system shall compute age-based vulnerability scores for each family member. |
| FR-04.4 | The system shall trigger automatic household score recalculation when family members are added, modified, or removed. |

#### 3.5.1.5 Module FR-05: Vulnerability Indicator Scoring Framework

| FR ID | Requirement Description |
|-------|------------------------|
| FR-05.1 | The system shall maintain configurable vulnerability indicator weights for 16 scoring dimensions: age, gender, marital status, solo parent status, disability type, health condition, clean water access, sanitation facilities, house construction type, ownership status, damage severity, employment status, monthly income, education level, digital access, and dependency ratio. |
| FR-05.2 | The system shall allow administrators to update vulnerability scoring weights through the VulnerabilityIndicator module. |
| FR-05.3 | The system shall support granular sub-categories within each dimension (e.g., 9 disability types, 6 health conditions, 7 income brackets, 6 education levels). |
| FR-05.4 | The system shall trigger cascading recalculation of all household scores and aid scores when vulnerability indicators are modified. |

#### 3.5.1.6 Module FR-06: Household Score Calculation Engine

| FR ID | Requirement Description |
|-------|------------------------|
| FR-06.1 | The system shall compute a composite household vulnerability score based on all 16 vulnerability dimensions. |
| FR-06.2 | The system shall calculate averaged scores across all household members for gender, marital status, disability, health condition, employment, and education dimensions. |
| FR-06.3 | The system shall compute a dependency ratio based on the number of vulnerable members (children, elderly, disabled, chronically ill) versus able-bodied adults in the household. |
| FR-06.4 | The system shall apply dependency ratio scoring: ≥2.00 → 1.00 (Severe), ≥1.00 → 0.67 (High), ≥0.50 → 0.33 (Moderate), <0.50 → 0.00 (Low). |
| FR-06.5 | The system shall support disaster-specific score calculation, allowing different damage severity scores per disaster event. |
| FR-06.6 | The system shall support batch recalculation of all household scores with cascade updates to aid distribution scores (AidHouseholdScoreCascadeUpdater). |

#### 3.5.1.7 Module FR-07: Disaster Management

| FR ID | Requirement Description |
|-------|------------------------|
| FR-07.1 | The system shall support CRUD operations for disaster events with type classification and naming. |
| FR-07.2 | The system shall encrypt disaster type and name data before storage. |
| FR-07.3 | The system shall support disaster circle mapping (lat/lon/radius) for defining affected geographic areas. |

#### 3.5.1.8 Module FR-08: Disaster Damage Assessment

| FR ID | Requirement Description |
|-------|------------------------|
| FR-08.1 | The system shall record house damage severity per beneficiary per disaster, with five severity levels: No Visible Damage, Minor Damage, Moderate Damage, Severe Damage, and Destruction/Collapse. |
| FR-08.2 | The system shall trigger automatic household score recalculation when damage records are created or updated (DisasterDamageUpdateHandler). |
| FR-08.3 | The system shall encrypt damage severity classifications before storage. |

#### 3.5.1.9 Module FR-09: Aid Distribution with Machine Learning Clustering

| FR ID | Requirement Description |
|-------|------------------------|
| FR-09.1 | The system shall implement **Fuzzy C-Means (FCM)** clustering for soft beneficiary prioritization, assigning membership degrees (μᵢⱼ) to multiple clusters. |
| FR-09.2 | The system shall implement **K-Means** clustering (with K-Means++ initialization) for hard beneficiary prioritization as an alternative algorithm. |
| FR-09.3 | The system shall classify clusters into three priority labels: "High Priority," "Moderate Priority," and "Low Priority" based on average cluster score ranking. |
| FR-09.4 | The system shall prioritize aid distribution by selecting beneficiaries from the highest priority cluster first, then filling remaining capacity from lower-priority clusters. |
| FR-09.5 | The system shall support configurable FCM parameters: fuzziness coefficient (m, default=2.0), convergence threshold (default=1e-5), and maximum iterations (default=300). |
| FR-09.6 | The system shall support configurable K-Means parameters: maximum iterations (default=100) and convergence threshold (default=0.001). |
| FR-09.7 | The system shall support aid record management including aid type, quantity, cost, provider, and distribution date. |
| FR-09.8 | The system shall generate printable aid distribution reports with beneficiary lists and distribution summaries via JavaFX printing and iText/POI export. |

#### 3.5.1.10 Module FR-10: Geospatial Mapping

| FR ID | Requirement Description |
|-------|------------------------|
| FR-10.1 | The system shall render interactive OpenStreetMap tiles on a JavaFX Canvas with pan and zoom controls. |
| FR-10.2 | The system shall support zoom levels from 13 (regional) to 20 (street-level) with smooth tile interpolation. |
| FR-10.3 | The system shall display beneficiary locations as interactive markers on the map, with click-to-inspect functionality. |
| FR-10.4 | The system shall display evacuation site locations as distinct markers on the map. |
| FR-10.5 | The system shall render disaster-affected areas as circular overlays defined by center coordinates and radius. |
| FR-10.6 | The system shall draw municipality boundary polygons on the map (Banate boundary with 30 vertex coordinates). |
| FR-10.7 | The system shall support coordinate validation ensuring points fall within the application's geographic bounds (North: 11.1166, South: 10.9842, West: 122.6585, East: 122.9348). |
| FR-10.8 | The system shall implement Haversine formula-based distance calculation for geographic proximity analysis. |
| FR-10.9 | The system shall identify beneficiaries within disaster circle zones through geographic intersection calculations. |
| FR-10.10 | The system shall clamp map panning to prevent navigation beyond the defined geographic bounds. |

#### 3.5.1.11 Module FR-11: SMS Communication

| FR ID | Requirement Description |
|-------|------------------------|
| FR-11.1 | The system shall support **dual-mode SMS sending**: (a) offline via SIM800C GSM hardware modem, and (b) online via SkySMS API. |
| FR-11.2 | The system shall auto-detect available serial ports for GSM modem connection. |
| FR-11.3 | The system shall configure the GSM modem using AT commands (ATE0, AT+CMGF=1, AT+CNMI=2,2,0,0,0) for text mode SMS operation. |
| FR-11.4 | The system shall support bulk SMS sending with configurable inter-message delay, retry logic, and cancellation support. |
| FR-11.5 | The system shall log all SMS send operations (recipient, message, status, timestamp) to the database. |
| FR-11.6 | The system shall provide real-time connection status indicators for the GSM modem. |
| FR-11.7 | The system shall support beneficiary selection for targeted SMS (all beneficiaries, by barangay, by disaster zone, or manual selection). |
| FR-11.8 | The system shall support customizable evacuation alert messages stored persistently through CustomEvacMessageManager. |
| FR-11.9 | The system shall enforce a 320-character SMS message limit with real-time character counting. |

#### 3.5.1.12 Module FR-12: AI-Powered News Generation

| FR ID | Requirement Description |
|-------|------------------------|
| FR-12.1 | The system shall generate contextual emergency news summaries using the Anthropic Claude AI (Claude Sonnet 4.5). |
| FR-12.2 | The system shall fetch real-time articles from Philippine RSS news feeds (GMA Network, PhilStar, Panay News, Daily Guardian). |
| FR-12.3 | The system shall support category-based news generation: local news, national news, weather, politics, health, law, crime. |
| FR-12.4 | The system shall format AI-generated news within 280–320 characters for SMS-ready output. |
| FR-12.5 | The system shall display 5 news slot options with streaming progress indicators, allowing administrators to select and send AI-generated news as SMS. |
| FR-12.6 | The system shall support cancellation of in-progress AI news generation. |

#### 3.5.1.13 Module FR-13: Evacuation Planning

| FR ID | Requirement Description |
|-------|------------------------|
| FR-13.1 | The system shall manage evacuation sites with name, location (lat/lon), and capacity attributes. |
| FR-13.2 | The system shall automatically allocate beneficiaries to evacuation sites based on: (a) vulnerability score ranking (highest priority first), (b) household size (entire household assigned together), and (c) remaining site capacity. |
| FR-13.3 | The system shall compute geographic distances between beneficiary locations and evacuation sites using the Haversine formula (GeoDistanceCalculator). |
| FR-13.4 | The system shall prevent double-assignment of beneficiaries to the same evacuation site for the same disaster. |
| FR-13.5 | The system shall track and display real-time evacuation site occupancy. |

#### 3.5.1.14 Module FR-14: Dashboard

| FR ID | Requirement Description |
|-------|------------------------|
| FR-14.1 | The system shall display summary statistics: total beneficiaries, total disasters, total aid distributions, and total evacuation sites. |
| FR-14.2 | The system shall display an interactive map showing all beneficiary locations, evacuation sites, and the municipal boundary. |
| FR-14.3 | The system shall provide a search-by-name functionality to locate and center on specific beneficiaries on the map. |
| FR-14.4 | The system shall display beneficiary detail panels upon double-click selection, showing family members and vulnerability information. |
| FR-14.5 | The system shall display real-time date and time. |
| FR-14.6 | The system shall provide one-click navigation to management modules (Beneficiary, Disaster, Aid, Evacuation). |

#### 3.5.1.15 Module FR-15: Settings

| FR ID | Requirement Description |
|-------|------------------------|
| FR-15.1 | The system shall allow configuration of application preferences through a settings interface. |
| FR-15.2 | The system shall allow reconfiguration of API keys and connection parameters. |

### 3.5.2 Non-Functional Requirements

| NFR ID | Category | Requirement Description |
|--------|----------|------------------------|
| NFR-01 | Security | All PII shall be encrypted at rest using AES-128-GCM with 12-byte random IV per encryption operation. |
| NFR-02 | Security | Passwords shall be hashed using BCrypt with salt before storage. |
| NFR-03 | Security | Encryption keys shall be loaded from external configuration files, not hardcoded in source code. |
| NFR-04 | Performance | The splash screen shall load the full application within 10 seconds on standard hardware. |
| NFR-05 | Performance | Map tile rendering shall support smooth panning and zooming at 30+ FPS. |
| NFR-06 | Performance | FCM clustering shall converge within 300 iterations for datasets up to 10,000 beneficiaries. |
| NFR-07 | Reliability | The system shall operate without internet connectivity for core functions (SMS via GSM, offline maps). |
| NFR-08 | Usability | The system shall provide consistent error handling with user-friendly alert dialogs (AlertDialogManager). |
| NFR-09 | Maintainability | The system shall follow the DAO pattern for all database operations, ensuring separation of concerns. |
| NFR-10 | Scalability | The database schema shall support multiple concurrent disaster events with independent scoring and allocation. |
| NFR-11 | Compliance | The system shall comply with the Philippine Data Privacy Act of 2012 (R.A. 10173) regarding PII handling. |
| NFR-12 | Availability | The system shall gracefully handle database connection failures and GSM modem disconnections with appropriate error messages. |

---

## 3.6 Phase 3: System Design

### 3.6.1 Architectural Design

#### 3.6.1.1 Overall Architecture: Layered Architecture with MVC

RESPOND-PH employs a **Layered Architecture** combined with the **Model-View-Controller (MVC)** design pattern, enhanced by a **Service Layer** and **Data Access Object (DAO)** pattern. This architecture ensures clean separation of concerns, testability, and maintainability.

The layers are organized as follows:

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ FXML Views   │  │ CSS Styles   │  │ Controllers  │      │
│  │ (41 files)   │  │ (42 files)   │  │ (JavaFX)     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
├─────────────────────────────────────────────────────────────┤
│                    SERVICE LAYER                            │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Business Logic, Validation, Encryption/Decryption   │    │
│  │ Clustering Algorithms (FCM, K-Means)                │    │
│  │ Vulnerability Scoring, Household Score Calculation   │    │
│  └─────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│                    DATA ACCESS LAYER                        │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ DAO Interfaces → DAO Implementations (JDBC)         │    │
│  └─────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│                    INFRASTRUCTURE LAYER                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ MySQL DB     │  │ SIM800C GSM  │  │ OSM Tiles    │      │
│  │ (JDBC)       │  │ (jSerialComm)│  │ (Canvas)     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
├─────────────────────────────────────────────────────────────┤
│                    EXTERNAL SERVICES                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Anthropic AI │  │ SkySMS API   │  │ RSS Feeds    │      │
│  │ (Claude)     │  │ (HTTP)       │  │ (HTTP/XML)   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

#### 3.6.1.2 Package Structure

The Java source code is organized into **23 packages** within the `com.ionres.respondph` namespace, totaling **191 Java source files**:

| Package | Contents | Purpose |
|---------|----------|---------|
| `com.ionres.respondph` | `RESPONDPH.java` | Application entry point (JavaFX Application) |
| `com.ionres.respondph.admin` | Controller, DAO, Model, Service (7 files) | Administrator management |
| `com.ionres.respondph.admin.login` | Controller, DAO, Service (5 files) | Authentication |
| `com.ionres.respondph.aid` | Controller, DAO, Model, Service, FCM, K-Means (10 files) | Aid distribution with clustering |
| `com.ionres.respondph.aid_type` | Controller, DAO, Model, Service (8 files) | Aid type classification |
| `com.ionres.respondph.aidType_and_household_score` | Calculator, Updater, DAO (4 files) | Cross-module score synchronization |
| `com.ionres.respondph.beneficiary` | Controller, DAO, Model, Service (7+ files) | Beneficiary profiling |
| `com.ionres.respondph.common` | Controllers, DAOs, Models, Services (14 files) | Shared models and services |
| `com.ionres.respondph.dashboard` | Controller, DAO, Model, Service (6 files) | Dashboard and statistics |
| `com.ionres.respondph.database` | `DBConnection.java` | Database connection singleton |
| `com.ionres.respondph.disaster` | Controller, DAO, Model, Service (8 files) | Disaster event management |
| `com.ionres.respondph.disaster_damage` | Controller, DAO, Model, Service (7 files) | Damage assessment |
| `com.ionres.respondph.disaster_mapping` | Controller, DAO, Service (5 files) | Geospatial disaster mapping |
| `com.ionres.respondph.evacuation_plan` | Controller, DAO, Model, Service, GeoCalc (13 files) | Evacuation planning |
| `com.ionres.respondph.evac_site` | Controller, DAO, Model, Service (7 files) | Evacuation site management |
| `com.ionres.respondph.exception` | Custom exception classes (5 files) | Error handling framework |
| `com.ionres.respondph.familymembers` | Controller, DAO, Model, Service (7 files) | Family member management |
| `com.ionres.respondph.household_score` | Calculator, DAO, Model (4 files) | Vulnerability score engine |
| `com.ionres.respondph.main` | `MainFrameController.java` | Main navigation frame |
| `com.ionres.respondph.sendsms` | Controller, DAO, Model, Service (14 files) | SMS communication |
| `com.ionres.respondph.settings` | `SettingsController.java` | Application configuration |
| `com.ionres.respondph.splash` | `SplashScreenController.java` | Application loading screen |
| `com.ionres.respondph.util` | 28 utility classes | Cross-cutting utilities |
| `com.ionres.respondph.vulnerability_indicator` | Controller, DAO, Model, Service (6 files) | Vulnerability configuration |

#### 3.6.1.3 Design Patterns Employed

The following software design patterns were systematically employed throughout the system:

**1. Singleton Pattern:**
- `DBConnection` — Thread-safe database connection manager using double-checked locking:
  ```
  getInstance() → synchronized block → null check → new DBConnection()
  ```
- `SMSSender` — Global GSM modem controller
- `SessionManager` — Authenticated user session tracking
- `CryptographyManager` — Shared AES encryption instance
- `ConfigLoader` — Application configuration loader (static initializer)
- `AppContext` — Central service registry with static fields

**2. Data Access Object (DAO) Pattern:**
Every module follows a strict DAO pattern:
```
Interface (e.g., BeneficiaryDAO)
    └── Implementation (e.g., BeneficiaryDAOImpl)
```
This pattern abstracts the data persistence mechanism from the business logic, facilitating unit testing with mock implementations and enabling future migration to alternative persistence frameworks.

**3. Service Layer Pattern:**
Every module implements business logic through a service interface and implementation:
```
Interface (e.g., BeneficiaryService)
    └── Implementation (e.g., BeneficiaryServiceImpl)
```
The service layer orchestrates DAO operations, applies encryption/decryption, performs validation, and manages transactions.

**4. Model-View-Controller (MVC):**
- **Model** — Plain Java objects (POJOs) representing domain entities (e.g., `BeneficiaryModel`, `DisasterModel`, `HouseholdScoreModel`)
- **View** — FXML files (41 total) defining the declarative UI layout
- **Controller** — JavaFX controller classes annotated with `@FXML` bindings (e.g., `BeneficiaryController`, `DashboardController`)

**5. Observer Pattern:**
- `DashboardRefresher` — Observer registry for cross-module UI refresh triggers
- `SMSSender.SendResult` listeners — Event-driven notification of SMS send outcomes via `Consumer<SendResult>` callbacks
- `SessionManager.setOnSessionChanged()` — Session change notification callback
- `UpdateTrigger` — Cascade update notification system

**6. Factory Pattern:**
- `ExceptionFactory` — Centralized exception creation with descriptive categorization (`passwordMismatch()`, `duplicate()`, `failedToCreate()`, `failedToUpdate()`, `failedToDelete()`, `entityNotFound()`)
- `DialogManager` — Preloaded dialog instantiation factory

**7. Strategy Pattern:**
- Dual clustering algorithm implementation (`FCMAidDistribution` and `KMeansAidDistribution`) allows runtime selection of the clustering strategy for aid prioritization.

**8. Builder Pattern:**
- `MessageCreateParams.builder()` in the Anthropic AI integration for constructing API requests.

### 3.6.2 Database Design

#### 3.6.2.1 Entity-Relationship Model

The MySQL database schema was designed to support the following primary entities and their relationships:

**Core Entities:**

1. **admin** — Stores administrator accounts with encrypted personal data and BCrypt-hashed passwords.
2. **beneficiary** — Stores beneficiary profiles with encrypted PII and geolocation coordinates.
3. **family_members** — Stores family member records linked to beneficiary households (one-to-many relationship with beneficiary).
4. **disaster** — Stores disaster events with encrypted type and name classifications.
5. **disaster_damage** — Stores damage assessment records linking beneficiaries to disasters (many-to-many relationship with severity classification).
6. **disaster_circle** — Stores geographic disaster zone definitions (lat/lon/radius) linked to disasters.
7. **household_score** — Stores computed vulnerability scores per beneficiary per disaster (16 dimensional scores + composite).
8. **vulnerability_indicator** — Stores configurable scoring weights for all vulnerability dimensions.
9. **aid_type** — Stores classifications of aid resources (food, medical, shelter, etc.).
10. **aid** — Stores aid distribution records linking beneficiaries, disasters, and aid types.
11. **evac_site** — Stores evacuation site information with capacity and geolocation.
12. **evac_plan** — Stores evacuation allocation records linking beneficiaries to evacuation sites per disaster.
13. **sms_log** — Stores SMS transmission records (recipient, message, status, timestamp).
14. **custom_evac_message** — Stores administrator-customized evacuation alert templates.

#### 3.6.2.2 Data Encryption Architecture

A critical design decision was the implementation of **field-level encryption** for all personally identifiable information (PII) stored in the database. The encryption architecture operates as follows:

**Encryption Algorithm:** AES-128-GCM (Galois/Counter Mode)
- **Key Size:** 128 bits (16 bytes)
- **IV Size:** 12 bytes (96 bits)
- **Tag Length:** 128 bits
- **Key Encoding:** Base64

**Encrypted Storage Format:**
```
Base64(IV) + ":" + Base64(Ciphertext + AuthTag)
```

Each encryption operation generates a **fresh random 12-byte Initialization Vector (IV)** using `java.security.SecureRandom`, ensuring that identical plaintext values produce different ciphertext values. This design prevents frequency analysis attacks and provides semantic security.

**Fields Encrypted:**
- Administrator: username, first name, middle name, last name
- Beneficiary: first name, middle name, last name, gender, marital status, solo parent status, disability type, health condition, clean water access, sanitation facility, house type, ownership status, employment status, monthly income, educational level, digital access, latitude, longitude, mobile number
- Family Members: first name, middle name, last name, gender, marital status, disability type, health condition, employment status, educational level
- Disaster: disaster type, disaster name
- Disaster Circle: latitude, longitude, radius
- Disaster Damage: house damage severity

**Password Hashing:** Separately, administrator passwords are hashed using **BCrypt** (via the jBCrypt library, version 0.4), which incorporates automatic salt generation and configurable work factor, making brute-force attacks computationally infeasible.

**Key Management:**
The AES secret key is stored in the `Outlet.properties` configuration file under the `secretKey` property and loaded at runtime through the `ConfigLoader` utility class. The `CryptographyManager` singleton ensures a single `Cryptography` instance is shared across the application, avoiding redundant key initialization.

#### 3.6.2.3 Database Connection Management

Database connectivity is managed through the `DBConnection` singleton class, which:

1. Loads JDBC configuration (driver, URL, user, password) from `config/Outlet.properties` at initialization time.
2. Implements thread-safe singleton instantiation using double-checked locking (`synchronized (DBConnection.class)`).
3. Returns a **fresh JDBC connection** on every `getConnection()` call, with callers responsible for closing connections via try-with-resources blocks.
4. Registers the MySQL JDBC driver using `Class.forName(driver)`.

### 3.6.3 User Interface Design

#### 3.6.3.1 FXML-Based Declarative UI

The user interface is composed of **41 FXML files** organized into module-specific directories:

| Module | View Files | Dialog Files |
|--------|-----------|--------------|
| Authentication | SplashScreen.fxml, Login.fxml | — |
| Main Frame | MainScreen.fxml | — |
| Dashboard | Dashboard.fxml | — |
| Admin | ManageAdmins.fxml | AddAdminDialog.fxml, EditAdminDialog.fxml |
| Beneficiary | ManageBeneficiaries.fxml | AddBeneficiariesDialog.fxml, EditBeneficiariesDialog.fxml |
| Family Members | FamilyMembers.fxml | AddFamilyMemberDialog.fxml, EditFamilyMemberDialog.fxml |
| Disaster | Disaster.fxml | AddDisasterDialog.fxml, EditDisasterDialog.fxml |
| Disaster Damage | DisasterDamage.fxml | AddDisasterDamageDialog.fxml, EditDisasterDamageDialog.fxml |
| Disaster Mapping | DisasterMapping.fxml | BeneficiariesInCircleDialog, AllocateBeneficiariesToEvacSite, EvacuationAllocationDialog, EvacSiteMapping |
| Aid | Aid.fxml | AddAidDialog.fxml, PreviewDistributionDialog, PrintAidDialog |
| Aid Type | AidType.fxml | AddAidTypeDialog.fxml, EditAidTypeDialog.fxml |
| Evacuation Site | EvacSite.fxml | AddEvacSiteDialog.fxml, EditEvacSiteDialog.fxml |
| Evacuation Plan | EvacuationPlan.fxml | EvacuationPlanPrinting |
| SMS | SendSMS.fxml | BeneficiarySelectionDialog |
| Mapping | — | MapDialog.fxml |
| Vulnerability Indicator | VulnerabilityIndicator.fxml | — |
| Settings | Settings.fxml | — |

#### 3.6.3.2 CSS Styling Architecture

The visual design is implemented through **42 CSS stylesheet files**, organized in a modular structure mirroring the view hierarchy. Each module has a dedicated stylesheet, and dialog windows have separate CSS files for scoped styling. Key CSS design decisions include:

- **Custom navigation buttons** with active state styling (`.nav-button-active`, `.nav-button-child-active`)
- **Consistent color scheme** aligned with disaster response visual standards
- **FontAwesome icon integration** for intuitive iconography
- **Responsive table styling** with alternating row colors for data-heavy views

#### 3.6.3.3 Scene Management and Preloading

The `SceneManager` utility class implements a **scene caching mechanism** that preloads all FXML views during the splash screen initialization phase. This design ensures:

1. Zero latency when navigating between modules after initial loading.
2. Controller instances are cached and reusable, maintaining state across navigation events.
3. FXML parsing (which is CPU-intensive due to reflection) occurs only once during startup.

The `DialogManager` similarly preloads all dialog FXML files, storing them in a named registry for instant retrieval.

### 3.6.4 Algorithm Design

#### 3.6.4.1 Vulnerability Scoring Algorithm (HMIS-Based)

The Household Multivariate Indicator Scoring (HMIS) algorithm computes a composite vulnerability score for each beneficiary household across 16 dimensions. The algorithm operates as follows:

**Step 1: Data Retrieval and Decryption**
```
For each beneficiary:
    1. Retrieve encrypted profile from database
    2. Decrypt all fields using AES-128-GCM
    3. Retrieve all encrypted family member profiles
    4. Decrypt family member fields
```

**Step 2: Age Score Calculation**
```
beneficiary_age_score = f(birth_date)  // Based on age brackets
for each family_member:
    family_member_age_score = f(birth_date)
average_age_score = sum(all_age_scores) / total_members
```

Age scoring brackets:
- 0–4 years (infant/toddler): 1.00
- 5–12 years (child): 0.70
- 13–17 years (adolescent): 0.30
- 18–59 years (adult): 0.00
- 60+ years (elderly): 1.00

**Step 3: Averaged Household Scores**
For dimensions where family members contribute (gender, marital status, disability, health condition, employment, education):
```
For each dimension D:
    total_score_D = beneficiary_score_D
    for each family_member:
        total_score_D += family_member_score_D
    average_score_D = total_score_D / total_members
```

**Step 4: Beneficiary-Only Scores**
For dimensions that apply only to the household head (solo parent status, clean water access, sanitation facility, house type, ownership status, monthly income, digital access):
```
score_D = lookup(beneficiary_value, vulnerability_indicator_weights)
```

**Step 5: Dependency Ratio Calculation**
```
vulnerable_count = 0
able_bodied_count = 0

For beneficiary and each family_member:
    IF age_score ∈ {1.0, 0.7} OR disability ≠ "None" OR health ∈ {chronic, immuno, terminal, medical}:
        vulnerable_count++
    ELSE:
        able_bodied_count++

IF able_bodied_count == 0:
    dependency_ratio = 999.0 (extreme vulnerability)
ELSE:
    dependency_ratio = vulnerable_count / able_bodied_count

vulnerability_score = {
    ≥ 2.00 → 1.00 (Severe)
    ≥ 1.00 → 0.67 (High)
    ≥ 0.50 → 0.33 (Moderate)
    < 0.50 → 0.00 (Low)
}
```

**Step 6: Disaster Damage Score**
```
For the specific disaster:
    damage_record = lookup(beneficiary_id, disaster_id)
    IF damage_record EXISTS:
        damage_severity_score = lookup(decrypted_severity, vulnerability_weights)
    ELSE:
        damage_severity_score = 0.0
```

**Step 7: Composite Score Computation**
All 16 dimensional scores are stored in the `HouseholdScoreModel` object and persisted to the `household_score` table. The composite score (final_score) used for clustering is computed as a weighted sum of all 16 dimensions.

#### 3.6.4.2 Fuzzy C-Means (FCM) Clustering Algorithm

The FCM algorithm is implemented in `FCMAidDistribution.java` (421 lines) and performs soft clustering of beneficiaries based on their composite vulnerability scores. The mathematical formulation is as follows:

**Objective Function:**
```
J_m = Σᵢ₌₁ⁿ Σⱼ₌₁ᶜ (μᵢⱼ)ᵐ ‖xᵢ - cⱼ‖²
```
Where:
- n = number of beneficiaries
- c = number of clusters (default: 3)
- μᵢⱼ = membership degree of beneficiary i in cluster j
- m = fuzziness coefficient (default: 2.0)
- xᵢ = final vulnerability score of beneficiary i
- cⱼ = centroid of cluster j

**Algorithm Steps:**

```
FUNCTION performFCM(beneficiaries, c):
    1. INITIALIZE membership matrix U[n][c] with random values, normalized so Σⱼ μᵢⱼ = 1
    2. SET iteration = 0
    3. WHILE iteration < maxIterations (300):
        a. SAVE prevU = deep_copy(U)
        
        b. COMPUTE cluster centres:
           FOR each cluster j:
               cⱼ = Σᵢ (μᵢⱼ)ᵐ · xᵢ / Σᵢ (μᵢⱼ)ᵐ
        
        c. UPDATE membership matrix:
           FOR each beneficiary i, cluster j:
               IF xᵢ == cⱼ (within 1e-12):
                   μᵢⱼ = 1.0, all other μᵢₖ = 0.0
               ELSE:
                   μᵢⱼ = 1 / Σₖ₌₁ᶜ (|xᵢ - cⱼ| / |xᵢ - cₖ|)^(2/(m-1))
        
        d. CHECK convergence:
           IF max(|U - prevU|) < threshold (1e-5):
               BREAK
        
        e. iteration++
    
    4. HARD-ASSIGN clusters:
       FOR each beneficiary i:
           cluster_i = argmax_j(μᵢⱼ)
           store membership_values[i] = U[i]
    
    5. ASSIGN priority labels:
       COMPUTE average score per cluster
       SORT clusters by average score (descending)
       REMAP: highest avg → "High Priority" (Cluster 0)
              middle avg → "Moderate Priority" (Cluster 1)
              lowest avg → "Low Priority" (Cluster 2)
       REMAP membership arrays to match new cluster order
```

**Key Implementation Details:**
- **Random initialization** with small positive values (1e-9 offset to avoid exact zeros), normalized per row to sum to 1.0.
- **Convergence check** compares element-wise absolute differences between consecutive membership matrices against threshold 1e-5.
- **Cluster priority assignment** uses cluster-average vulnerability scores to deterministically map clusters to priority labels, ensuring the highest-scoring cluster is always labeled "High Priority" regardless of random initialization.
- **Membership value preservation** — each beneficiary retains its full membership vector, enabling administrators to assess the degree of belonging to each cluster.

#### 3.6.4.3 K-Means Clustering Algorithm

The K-Means algorithm is implemented in `KMeansAidDistribution.java` (391 lines) as an alternative hard clustering method:

**K-Means++ Initialization:**
```
FUNCTION initializeCentroidsKMeansPlusPlus(beneficiaries, k):
    1. centroids[0] = random beneficiary score
    2. FOR i = 1 to k-1:
        a. FOR each beneficiary j:
           distances[j] = min_c(|score_j - centroid_c|²)
        b. totalDistance = Σ distances
        c. threshold = random() × totalDistance
        d. sum = 0
        e. FOR each beneficiary j:
           sum += distances[j]
           IF sum ≥ threshold:
               centroids[i] = score_j
               BREAK
    3. RETURN centroids
```

**Main K-Means Loop:**
```
FUNCTION performKMeansClustering(beneficiaries, k):
    1. centroids = initializeCentroidsKMeansPlusPlus(beneficiaries, k)
    2. REPEAT:
        a. ASSIGN each beneficiary to nearest centroid
        b. COMPUTE new centroids as mean of cluster members
        c. CHECK convergence (centroid movement < 0.001)
    3. UNTIL converged OR maxIterations (100)
```

**Priority Label Assignment** follows the same logic as FCM: clusters are sorted by average score, remapped to priority ranks, and labeled accordingly.

#### 3.6.4.4 Aid Prioritization Strategy

The aid prioritization combines clustering results with available quantity constraints:

```
FUNCTION getPrioritizedBeneficiaries(beneficiaries, availableQuantity, numberOfClusters):
    1. RUN clustering (FCM or K-Means) on all beneficiaries
    2. IDENTIFY highest priority cluster (Cluster 0 after remapping)
    3. SELECT from highest priority cluster first (sorted by score descending)
    4. IF selected_count < availableQuantity:
        FILL remaining from other clusters (sorted by score descending)
    5. RETURN top availableQuantity beneficiaries
```

#### 3.6.4.5 Haversine Distance Calculation

Two implementations of the Haversine formula provide geographic distance computation:

1. **`GeographicUtils.calculateDistance()`** — Returns distance in **meters**, used for beneficiary-in-circle detection:
```
a = sin²(Δlat/2) + cos(lat1) · cos(lat2) · sin²(Δlon/2)
c = 2 · atan2(√a, √(1-a))
distance = 6,371,000 × c  (meters)
```

2. **`GeoDistanceCalculator.calculateDistance()`** — Returns distance in **kilometers**, used for evacuation site proximity:
```
distance = 6,371.0 × c  (kilometers)
```

Both implementations include input validation for NaN and out-of-range coordinates.

### 3.6.5 Communication Module Design

#### 3.6.5.1 GSM Hardware Interface

The GSM modem interface is implemented through the `SMSSender` class, which encapsulates the `GSMDongle` inner class for serial communication with the SIM800C module:

**Connection Protocol:**
```
1. Set serial parameters: 9600 baud, 8 data bits, 1 stop bit, No parity
2. Set timeouts: Read semi-blocking, 5000ms read, 5000ms write
3. Open serial port
4. Send AT command: "ATE0" (echo off)
5. Send AT command: "AT+CMGF=1" (text mode)
6. Send AT command: "AT+CNMI=2,2,0,0,0" (new message indication)
```

**SMS Send Protocol:**
```
1. Send AT command: AT+CMGS="<phone_number>"
2. Wait for ">" prompt (2000ms timeout)
3. Write message body followed by Ctrl+Z (0x1A)
4. Wait for "OK" or "+CMGS" response (15000ms timeout)
5. Detect success/failure
```

**Bulk Send Architecture:**
- Uses a `volatile boolean cancelRequested` flag for thread-safe cancellation
- Implements a `CopyOnWriteArrayList<Consumer<SendResult>>` for thread-safe listener notification
- Notifies listeners via `Platform.runLater()` for safe JavaFX UI updates
- Tracks consecutive failures for modem health monitoring

#### 3.6.5.2 API-Based SMS Interface

The `SMSApi` class provides internet-based SMS capability through the SkySMS API:

```
HTTP Method: POST
URL: Configured via Outlet.properties (skysms.api.url)
Authentication: X-API-Key header (loaded from SMS_API_KEY environment variable)
Content-Type: application/json
Payload: { "phone_number": "...", "message": "...", "use_subscription": true }
Timeout: 15 seconds per request, 10 seconds connection timeout
HTTP Version: HTTP/2
Success: Status 200 or 201
Bulk delay: 300ms between messages
```

### 3.6.6 Geospatial Mapping Design

#### 3.6.6.1 Tile Rendering Engine

The mapping module (`Mapping.java`, 547 lines) implements a custom OpenStreetMap tile rendering engine on the JavaFX Canvas:

**Coordinate System:**
- **Tile coordinates** follow the OSM Slippy Map convention: zoom level z → 2^z tiles per axis, each 256×256 pixels.
- **Latitude/Longitude to Pixel conversion:**
```
x_pixel = ((lon + 180) / 360) × 2^zoom × 256
y_pixel = ((1 - log(tan(lat_rad) + sec(lat_rad)) / π) / 2) × 2^zoom × 256
```

**Rendering Pipeline:**
```
1. Clear canvas with background color RGB(63, 91, 156) — ocean blue
2. Calculate visible tile range based on current offset and zoom
3. For each visible tile position (x, y):
    a. Find best available tile (fallback to lower zoom if not available)
    b. Calculate source region (for overzoom interpolation)
    c. Draw tile image with appropriate scaling
4. Draw map marker at current position (if set and not dragging)
5. Execute afterRedraw callback (for overlay drawing)
```

**Tile Caching:**
- In-memory tile cache (`HashMap<String, Image>`) keyed by "zoom/x/y"
- Fallback tile resolution: if tile at requested zoom is unavailable, recursively search lower zoom levels and scale the appropriate quadrant

**Geographic Bounds (Municipality of Banate):**
```
North: 11.116584029742963
South: 10.984159872049194
West:  122.65846664428710
East:  122.93484146118163
```

**Boundary Polygon:**
The municipality boundary is defined as a 30-vertex polygon, rendered as a dashed stroke overlay on the map canvas.

#### 3.6.6.2 Marker System

Two types of markers are rendered on the map:

1. **Person Markers** — 32×32 pixel icons centered horizontally and anchored at the bottom, representing beneficiary locations. Loaded from `/images/person_marker.png`.

2. **Evacuation Site Markers** — 32×32 pixel icons representing evacuation sites. Loaded from `/images/location-pin.png`.

Markers are rendered only when `zoom ≥ 16.0` to prevent visual clutter at lower zoom levels.

**Hit Detection:**
Marker click detection uses bounding-box intersection testing:
```
FOR each marker:
    screen_x = latLonToPixel(marker.lat, marker.lon).x
    screen_y = latLonToPixel(marker.lat, marker.lon).y
    IF |click_x - screen_x| < MARKER_WIDTH/2 AND
       |click_y - screen_y| < MARKER_HEIGHT:
        RETURN marker  // Hit detected
```

---

## 3.7 Phase 4: Implementation and Coding

### 3.7.1 Development Environment Setup

The development environment was configured as follows:

- **IDE:** IntelliJ IDEA (JetBrains)
- **JDK:** OpenJDK 17 (Long-Term Support)
- **Build Tool:** Apache Maven 3.x with `pom.xml` configuration
- **Version Control:** Git
- **Database Server:** MySQL Server (local instance)
- **Hardware Testing:** SIM800C GSM module connected via USB-to-Serial adapter

### 3.7.2 Application Bootstrap Implementation

The application entry point is `RESPONDPH.java`, which extends `javafx.application.Application`:

**Startup Sequence:**
```
1. main() → launch(args) → start(Stage primaryStage)
2. Load SplashScreen.fxml
3. Create Scene (1200×800, minimum 1600×800, maximized)
4. Apply splashscreen.css stylesheet
5. Show primary stage
```

**Splash Screen Loading Sequence (SplashScreenController):**
```
Task<Void> loadingTask (5 steps):
    Step 1: "Initializing utilities..."
            → AppLoader.initializeUtilities()
            → ConfigLoader.get("secretKey")
            → CryptographyManager.getInstance()
            → SessionManager.getInstance()
    
    Step 2: "Connecting to database..."
            → AppLoader.connectDatabase()
            → DBConnection.getInstance()
    
    Step 3: "Loading services..."
            → AppLoader.loadServices()
            → Initialize 12 service singletons:
                LoginService, AdminService, BeneficiaryService,
                FamilyMemberService, DisasterService, DisasterDamageService,
                AidTypeService, EvacSiteService, DashBoardService,
                VulnerabilityIndicatorService, DisasterMappingService,
                EvacSiteMappingService
    
    Step 4: "Preparing interface..."
            → AppLoader.prepareUI()
            → Preload 14 scene FXML files
            → Preload 20+ dialog FXML files
    
    Step 5: "Opening application..."
            → Thread.sleep(200) // Brief pause for visual completion
```

Upon successful loading, the splash stage closes and the Login screen is displayed via `SceneManager.showStage()`.

### 3.7.3 Module Implementation Details

#### 3.7.3.1 Cryptography Module Implementation

The `Cryptography` class provides the core encryption/decryption engine:

**Constructor:** Accepts a Base64-encoded AES key, decodes it, and creates a `SecretKeySpec`.

**Encryption Methods:**
1. `encryptWithOneParameter(String)` — Encrypts a single string value
2. `encrypt(username, firstname, middlename, lastname, regDate)` — Batch encryption of 5 admin fields
3. `encryptDouble(double)` — Converts double to string, then encrypts
4. `encryptId(String)` — Encrypts identifier strings
5. `encryptUpdate(username, firstname, middlename, lastname)` — Batch encryption for update operations

**Decryption Methods:**
1. `decryptWithOneParameter(String)` — Decrypts a single encrypted string
2. `decrypt(List<String>)` — Batch decryption of multiple values
3. `decryptDouble(String)` — Decrypts and parses as double

**Implementation Pattern (for each encryption):**
```java
byte[] iv = new byte[12];
new SecureRandom().nextBytes(iv);              // Random IV per operation
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
GCMParameterSpec spec = new GCMParameterSpec(128, iv);
cipher.init(Cipher.ENCRYPT_MODE, key, spec);
byte[] ciphertext = cipher.doFinal(input.getBytes());
return Base64.encode(iv) + ":" + Base64.encode(ciphertext);
```

**Security Properties Achieved:**
- **Confidentiality** — AES-128 encryption protects data at rest
- **Integrity** — GCM authentication tag (128-bit) detects tampering
- **Semantic Security** — Random IV per operation prevents ciphertext analysis
- **Key Isolation** — Keys loaded from configuration, not embedded in source

#### 3.7.3.2 Household Score Calculation Engine Implementation

The `HouseholdScoreCalculate` class (788 lines) is the core vulnerability assessment engine. Its implementation involves:

**Disaster-Specific Score Calculation (`calculateScoresWithDisaster`):**

This method computes all 16 vulnerability dimensions for a specific disaster context:

1. **Retrieve vulnerability indicator weights** from the database via `dao.getVulnerabilityScores()`
2. **Retrieve beneficiary profile** and decrypt all 13 encrypted fields
3. **Retrieve family members** and decrypt their 6 encrypted fields each
4. **Compute individual dimension scores** using lookup methods that map decrypted categorical values to configured numeric weights:
   - `getGenderScore()` — Maps "Male"/"Female" to configured weights
   - `getMaritalStatusScore()` — Maps "Single"/"Married"/"Widowed"/"Separated"
   - `getSoloParentScore()` — Maps 3 solo parent categories
   - `getDisabilityScore()` — Maps 9 disability types
   - `getHealthScore()` — Maps 6 health conditions
   - `getCleanWaterScore()` — Maps 3 water access levels
   - `getSanitationScore()` — Maps 4 sanitation facility types
   - `getHouseTypeScore()` — Maps 4 house construction types
   - `getOwnershipScore()` — Maps 5 ownership statuses
   - `getEmploymentScore()` — Maps 5 employment statuses
   - `getIncomeScore()` — Maps 7 income brackets
   - `getEducationScore()` — Maps 6 education levels
   - `getDigitalAccessScore()` — Maps 4 digital access levels
   - `getDisasterDamageScore()` — Maps 5 damage severity levels
5. **Compute household averages** for 6 dimensions across all members
6. **Compute dependency ratio** using the vulnerable/able-bodied classification
7. **Look up disaster-specific damage** by matching disaster_id in the damage records
8. **Persist results** to the `household_score` table via `dao.saveHouseholdScoreWithDisaster()`

**Cascade Recalculation:**
The `recalculateAllHouseholdScoresWithAidCascade()` method triggers a full system recalculation:
```
FOR each beneficiary with existing household scores:
    1. Recalculate household score
    2. Trigger UpdateTrigger.triggerCascadeUpdate(beneficiaryId)
    3. Trigger AidHouseholdScoreCascadeUpdater.recalculateAidScoresForBeneficiary(beneficiaryId)
```

#### 3.7.3.3 Beneficiary Profiling Module Implementation

The beneficiary module follows the standard MVC+Service+DAO pattern:

**BeneficiaryModel (281 lines):**
- 28 data fields capturing the complete beneficiary profile
- Multiple constructors (full profile, location-only, default)
- Standard getters/setters for all fields

**BeneficiaryDAO → BeneficiaryDAOImpl:**
- CRUD operations using parameterized SQL queries via JDBC PreparedStatement
- All write operations encrypt data before insertion
- All read operations return encrypted data (decryption occurs at service/controller layer)

**BeneficiaryServiceImpl:**
- Orchestrates DAO operations with encryption/decryption
- Validates input data before persistence
- Triggers household score calculation upon beneficiary creation/update
- Implements search with in-memory filtering on decrypted data

**BeneficiaryController:**
- Binds FXML UI components to service operations
- Manages TableView population with decrypted display data
- Handles dialog opening for Add/Edit operations
- Implements search functionality with case-insensitive matching

#### 3.7.3.4 Aid Distribution Module Implementation

The aid module integrates with both clustering algorithms:

**AidController (305 lines):**
- Manages the aid distribution table with columns: beneficiary ID, beneficiary name, disaster, aid name, date, quantity, cost, provider, notes
- Supports search across all columns
- Provides print functionality via `AidPrintService`
- Loads aid type and disaster comboboxes from DAOs

**AidPrintService (903 lines):**
- Generates printable reports using JavaFX Printing API
- Supports two report types:
  1. **Beneficiary List** — Tabular list of aid recipients with quantities and costs
  2. **Distribution Summary** — Aggregated statistics by disaster and aid type
- Configurable paper size, orientation, margins, headers/footers, page numbers
- Formats currency values with Philippine Peso (₱) symbol

**FCMAidDistribution Integration:**
```
1. Load all beneficiary scores for a disaster
2. Create BeneficiaryCluster objects with (id, finalScore, scoreCategory)
3. Call fcm.getPrioritizedBeneficiaries(beneficiaries, availableQuantity, 3)
4. Display clustered and prioritized results in the distribution dialog
5. Insert aid records for selected beneficiaries
```

#### 3.7.3.5 SMS Communication Module Implementation

**SendSMSController (1086 lines):**

The SMS controller is the most complex UI controller, integrating:

1. **Dual-mode SMS** — Radio button toggle between GSM and API modes
2. **Port management** — Auto-detection, selection, connection, and disconnection of serial ports
3. **Beneficiary selection** — ComboBox-based selection with options for all beneficiaries, by barangay, by disaster zone, or manual selection through BeneficiarySelectionDialogController
4. **AI news generation** — Integration with `NewsGeneratorService` for generating SMS-ready news content:
   - Topic selection via ComboBox
   - 5 radio button slots for generated news items
   - Streaming progress indicators with ETA estimation
   - Cancellation support
5. **Custom evacuation messages** — Persistent message templates managed through `CustomEvacMessageManager`
6. **SMS log table** — Historical record of all sent messages with status tracking
7. **Network status check** — Real-time internet connectivity verification for API mode
8. **Character counting** — Real-time 0/320 character limit display

**NewsGeneratorService (676 lines):**

This service integrates the Anthropic Claude AI for contextual news generation:

1. **RSS Feed Aggregation:**
   - Local feeds: Panay News, Daily Guardian
   - National feeds: GMA Network, PhilStar, ABC News
   - Weather feeds: GMA Weather, weather.gov
2. **Category Resolution:**
   - Local categories → Iloilo City scope, local RSS feeds
   - National categories → Philippines scope, national RSS feeds
   - Weather categories → Weather-specific RSS feeds
3. **Article Parsing:** Regex-based extraction of title, link, and description from RSS XML
4. **Prompt Engineering:** Constructs detailed prompts instructing Claude to:
   - Generate exactly 10 SMS-formatted news items (280-320 characters)
   - Ground content in real RSS articles when available
   - Include ArticleIndex references for traceability
   - Target the specified geographic scope
5. **Streaming Response Processing:** Uses `StreamResponse<RawMessageStreamEvent>` for real-time progress tracking
6. **Progress Reporting:** Real-time ETA estimation with scheduled ticker thread

#### 3.7.3.6 Evacuation Planning Module Implementation

**EvacuationPlanServiceImpl (193 lines):**

The `allocateEvacSite()` method implements the core evacuation allocation logic:

```
1. RETRIEVE evacuation site details (capacity, location)
2. CALCULATE remaining capacity = capacity - already_occupied
3. IF remaining_capacity ≤ 0: RETURN (site full)
4. RETRIEVE ranked beneficiaries for disaster (sorted by vulnerability score descending)
5. FOR each ranked beneficiary:
    a. CHECK if already assigned to this site for this disaster
    b. GET household size (beneficiary + family members)
    c. IF occupied + household_size ≤ capacity:
        INSERT evac_plan record
        Update running total
    d. ELSE: SKIP (household doesn't fit)
6. RETURN list of assigned beneficiaries
```

**GeoDistanceCalculator:**
Used to compute proximity between beneficiary locations and evacuation sites, enabling distance-aware evacuation assignments.

**RankedBeneficiaryModel:**
Extends the standard beneficiary model with vulnerability score, category, and household size for the evacuation allocation algorithm.

#### 3.7.3.7 Dashboard Module Implementation

**DashboardController (873 lines):**

The dashboard is the central hub after login, providing:

1. **Statistics Cards** — Click-navigable cards showing counts of beneficiaries, disasters, aids, and evacuation sites
2. **Interactive Map** — Full implementation of the Mapping engine with:
   - Municipality boundary polygon rendering (30 vertices)
   - Beneficiary person markers (loaded from database, decrypted coordinates)
   - Evacuation site markers
   - Double-click marker inspection with info panels
   - Search-by-name with map centering
   - Scroll zoom and drag panning
3. **Real-time Clock** — 1-second update timeline for date and time display
4. **Admin Identity** — Session-aware display of logged-in administrator name
5. **Cross-Module Navigation** — One-click navigation via `MainFrameController` to Beneficiary, Disaster, Aid, and Evacuation modules

### 3.7.4 Cross-Cutting Concerns Implementation

#### 3.7.4.1 Error Handling Framework

A structured exception hierarchy is implemented in the `exception` package:

```
DomainException (abstract base)
    ├── ValidationException — Input validation failures
    ├── DuplicateEntityException — Unique constraint violations
    └── EntityOperationException — CRUD operation failures
            with Operation enum: CREATE, UPDATE, DELETE, FIND
```

`ExceptionFactory` provides a fluent API for exception creation:
```java
ExceptionFactory.passwordMismatch()
ExceptionFactory.missingField("Email")
ExceptionFactory.duplicate("Admin", "john_doe")
ExceptionFactory.failedToCreate("Beneficiary")
ExceptionFactory.entityNotFound("Disaster #42")
```

#### 3.7.4.2 Alert Dialog Management

`AlertDialogManager` provides a centralized, CSS-styled dialog system for:
- Information alerts
- Warning alerts
- Error alerts
- Confirmation dialogs
- Custom dialog layouts

All alerts are styled via `alertdialogmanager.css` for consistent visual design.

#### 3.7.4.3 Utility Classes

The 28 utility classes in the `util` package provide:

| Utility | Purpose |
|---------|---------|
| `AlertDialogManager` | Standardized dialog alerts |
| `AlertUtils` | Additional alert utilities |
| `AppContext` | Service registry (12 services) |
| `AppLoader` | Application bootstrap orchestrator |
| `AppPreferences` | User preference management |
| `ComboKeyHandler` | Keyboard navigation for combo boxes |
| `ConfigLoader` | Properties file reader |
| `Cryptography` | AES-128-GCM encryption/decryption |
| `CryptographyManager` | Singleton encryption instance |
| `CustomEvacMessageManager` | Persistent evacuation message templates |
| `DashboardRefresher` | Cross-module UI refresh triggers |
| `DialogManager` | FXML dialog preloading and caching |
| `DisasterDamageUpdateHandler` | Cascade update trigger for damage changes |
| `GeographicUtils` | Haversine distance calculation (meters) |
| `InternetConnectionChecker` | Network connectivity verification |
| `Mapping` | OpenStreetMap tile rendering engine |
| `NameDecryptionUtils` | Batch name decryption utilities |
| `ObjectEncDec` | Object-level encryption/decryption |
| `PhoneNumberValidator` | Philippine phone number validation |
| `QuickSearchUtil` | Optimized search utilities |
| `ResourceUtils` | Classpath resource loading |
| `SceneManager` | FXML scene caching and display |
| `SessionManager` | Authentication session tracking |
| `SMSApi` | SkySMS API integration |
| `SMSSender` | SIM800C GSM modem controller |
| `TextFieldUtils` | Input field formatting utilities |
| `UpdateTrigger` | Cascade score update orchestrator |
| `ValidationUtils` | Input validation (blank, numeric, phone) |

---

## 3.8 Phase 5: Testing and Quality Assurance

### 3.8.1 Testing Framework

The project uses **JUnit Jupiter 5.10.3** as the testing framework, with three dependencies:
- `junit-jupiter-api` — Test annotation and assertion APIs
- `junit-jupiter-params` — Parameterized test support
- `junit-jupiter-engine` — Test execution engine

### 3.8.2 Unit Testing

Unit tests were developed for the following critical components:

#### 3.8.2.1 Cryptography Module Tests
- **Encryption/Decryption Symmetry** — Verify that `decrypt(encrypt(plaintext)) == plaintext` for all encryption methods.
- **IV Uniqueness** — Verify that multiple encryptions of the same plaintext produce different ciphertexts.
- **GCM Authentication** — Verify that tampered ciphertext throws `AEABadTagException`.
- **Double Encryption** — Verify that `decryptDouble(encryptDouble(value)) == value` for edge cases (0, negative, MAX_VALUE, NaN).

#### 3.8.2.2 FCM Clustering Tests
- **Convergence** — Verify convergence within maxIterations for various dataset sizes.
- **Cluster Correctness** — Verify that high-score beneficiaries are assigned to Cluster 0 (High Priority).
- **Membership Sum** — Verify that Σⱼ μᵢⱼ ≈ 1.0 for each beneficiary.
- **Priority Label Assignment** — Verify correct label mapping based on cluster average scores.
- **Edge Cases** — Single beneficiary, two beneficiaries, all identical scores.

#### 3.8.2.3 K-Means Clustering Tests
- **K-Means++ Initialization** — Verify centroid diversity (no duplicate initial centroids).
- **Convergence** — Verify convergence for various k values and dataset distributions.
- **Consistency with FCM** — Verify that both algorithms produce compatible priority rankings for well-separated clusters.

#### 3.8.2.4 Household Score Calculation Tests
- **Dimension Score Mapping** — Verify correct lookup for all 16 dimensions across all categorical values.
- **Dependency Ratio** — Verify correct ratio computation for edge cases (no able-bodied adults, all able-bodied, mixed households).
- **Average Computation** — Verify correct averaging across household members.
- **Disaster-Specific Scoring** — Verify correct damage lookup by disaster ID.

#### 3.8.2.5 Geographic Calculation Tests
- **Haversine Accuracy** — Verify distance calculations against known geodesic distances.
- **Circle Intersection** — Verify beneficiary-in-circle detection accuracy.
- **Coordinate Validation** — Verify boundary checks for out-of-range coordinates.

### 3.8.3 Integration Testing

Integration tests verified the following cross-module interactions:

1. **Beneficiary → Household Score → Aid Cascade:**
   - Create beneficiary → Verify household score is computed → Modify vulnerability indicator → Verify cascade recalculation → Verify aid scores are updated.

2. **Disaster → Damage → Score → Clustering:**
   - Create disaster → Add damage record → Verify household score includes damage → Run clustering → Verify prioritization reflects damage severity.

3. **Database → Encryption → Decryption:**
   - Insert encrypted record → Retrieve from database → Decrypt → Verify original data integrity.

4. **GSM → SMS → Log:**
   - Connect to serial port → Send test SMS → Verify SMS log entry created with correct status.

### 3.8.4 System Testing

System-level tests validated end-to-end workflows:

1. **Complete Aid Distribution Workflow:**
   ```
   Login → Create beneficiaries → Add family members → Configure vulnerability indicators →
   Create disaster → Record damage → Compute household scores → Run FCM clustering →
   Distribute aid → Print report
   ```

2. **Emergency Communication Workflow:**
   ```
   Login → Select disaster → Identify affected beneficiaries via mapping →
   Connect GSM modem → Compose/generate message → Send bulk SMS → Verify log
   ```

3. **Evacuation Planning Workflow:**
   ```
   Login → Create evacuation sites → Create disaster → Compute beneficiary scores →
   Run allocation algorithm → Verify capacity constraints → Verify priority ordering
   ```

### 3.8.5 User Acceptance Testing (UAT)

User acceptance testing was conducted with MDRRMO personnel who evaluated:

1. **Functional Completeness** — All specified features are present and operational.
2. **Usability** — Interface is intuitive for non-technical disaster response personnel.
3. **Performance** — System responds within acceptable time frames for all operations.
4. **Data Accuracy** — Vulnerability scores and clustering results align with domain expert expectations.
5. **Communication Reliability** — SMS messages are delivered successfully via both GSM and API modes.

---

## 3.9 Phase 6: Deployment and Maintenance

### 3.9.1 Deployment Architecture

RESPOND-PH is deployed as a standalone desktop application with the following infrastructure requirements:

**Client Machine:**
- Operating System: Windows 10/11 (64-bit)
- Java Runtime: JDK 17 or later
- RAM: Minimum 4 GB (8 GB recommended)
- Storage: Minimum 500 MB for application + map tiles
- USB Port: For SIM800C GSM module connectivity

**Database Server:**
- MySQL Server (local or LAN)
- Database: RESPOND-PH schema with all tables created
- Configuration: Specified in `config/Outlet.properties`

**Hardware Peripherals:**
- SIM800C GSM Module with active SIM card
- USB-to-Serial adapter (if module doesn't include USB interface)

### 3.9.2 Installation Procedure

```
Step 1: Install JDK 17 and configure JAVA_HOME environment variable
Step 2: Install MySQL Server and create the RESPOND-PH database schema
Step 3: Import initial data (vulnerability indicator weights, admin account)
Step 4: Configure Outlet.properties with database credentials and tile directory
Step 5: Set environment variables:
        - ANTHROPIC_API_KEY (for AI news generation)
        - SMS_API_KEY (for SkySMS API)
Step 6: Download OpenStreetMap tile set for Municipality of Banate
Step 7: Connect SIM800C GSM module via USB
Step 8: Run the application: mvn javafx:run
```

### 3.9.3 Configuration Management

All runtime configuration is centralized in `config/Outlet.properties`:

```
# Database Configuration
driver=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://localhost:3306/respondph
user=<db_username>
pass=<db_password>

# Encryption Key
secretKey=<Base64-encoded-AES-key>

# Map Tile Directory
map.tile.directory=<path-to-tile-files>

# SMS API
skysms.api.url=<api-endpoint>
```

### 3.9.4 Maintenance Considerations

1. **Vulnerability Indicator Updates** — LGU personnel can update scoring weights through the VulnerabilityIndicator module without developer intervention.
2. **Database Backups** — MySQL dump scripts should be scheduled for regular backups, especially before and after disaster events.
3. **Map Tile Updates** — OSM tiles can be updated by downloading newer tile sets from OSM tile servers.
4. **GSM Module Maintenance** — SIM card balance monitoring and module health checks should be performed periodically.
5. **API Key Rotation** — Anthropic and SkySMS API keys should be rotated per security best practices.

---

## 3.10 Phase 7: Evaluation

### 3.10.1 Evaluation Framework

The system was evaluated using the **ISO 25010 Software Quality Model**, which defines eight quality characteristics:

| Quality Characteristic | Evaluation Criteria | Evaluation Method |
|----------------------|--------------------|--------------------|
| Functional Suitability | Completeness, correctness, appropriateness | Feature checklist against requirements |
| Performance Efficiency | Time behavior, resource utilization | Benchmark testing |
| Compatibility | Coexistence with other software | Environmental testing |
| Usability | Learnability, operability, user error protection | UAT questionnaire |
| Reliability | Maturity, availability, fault tolerance | Stress testing |
| Security | Confidentiality, integrity, authenticity | Encryption verification, penetration testing |
| Maintainability | Modularity, reusability, modifiability | Code metrics analysis |
| Portability | Adaptability, installability | Cross-environment deployment |

### 3.10.2 Performance Benchmarks

The following performance benchmarks were measured:

| Operation | Target | Measured |
|-----------|--------|----------|
| Application startup (splash to login) | ≤ 10 seconds | Measured during testing |
| Household score computation (single) | ≤ 1 second | Measured during testing |
| FCM clustering (1000 beneficiaries) | ≤ 5 seconds | Measured during testing |
| K-Means clustering (1000 beneficiaries) | ≤ 3 seconds | Measured during testing |
| Map tile rendering (visible area) | ≤ 100ms | Measured during testing |
| Single SMS send (GSM) | ≤ 20 seconds | Measured during testing |
| Beneficiary encryption (single record) | ≤ 50ms | Measured during testing |
| Dashboard data load | ≤ 3 seconds | Measured during testing |

### 3.10.3 Security Evaluation

The security implementation was evaluated against the following criteria:

1. **Data at Rest** — All PII is encrypted with AES-128-GCM. Database dumps contain only ciphertext.
2. **Password Security** — BCrypt hashing with automatic salt generation prevents rainbow table attacks.
3. **IV Randomness** — Each encryption operation uses a fresh 12-byte random IV, preventing ciphertext reuse attacks.
4. **GCM Authentication** — The 128-bit authentication tag ensures data integrity and detects tampering.
5. **Key Management** — Encryption keys are externalized in configuration files, not hardcoded in source code.
6. **Session Security** — The SessionManager singleton tracks authenticated sessions with logout/clear capability.

---

## 3.11 Tools and Instruments Used

### 3.11.1 Development Tools

| Tool | Purpose |
|------|---------|
| IntelliJ IDEA | Primary Integrated Development Environment |
| Apache Maven | Build automation and dependency management |
| Git | Version control system |
| MySQL Workbench | Database design and administration |
| Scene Builder | FXML visual layout editor |
| Postman | API testing (SkySMS, Anthropic) |

### 3.11.2 Hardware Tools

| Hardware | Specification | Purpose |
|----------|--------------|---------|
| SIM800C GSM Module | Quad-band 850/900/1800/1900 MHz | Offline SMS communication |
| USB-to-Serial Adapter | CP2102 or similar | GSM module connectivity |
| SIM Card | Philippine network (Globe/Smart) | SMS transmission |
| Development Computer | Intel i5+, 8GB RAM, Windows 10/11 | Development and testing |

### 3.11.3 Libraries and Frameworks Summary

A total of **22 external dependencies** are managed through Maven, as documented in Section 3.4.3.

---

## 3.12 Data Collection Procedures

### 3.12.1 Beneficiary Data Collection

Beneficiary data is collected through the system's profiling module, which captures:

1. **Personal Information** — Name, birth date, gender, marital status, solo parent status, mobile number
2. **Health Information** — Disability type (9 categories), health condition (6 categories)
3. **Socioeconomic Information** — Employment status (5 categories), monthly income (7 brackets), educational level (6 categories)
4. **Housing Information** — House construction type (4 categories), ownership status (5 categories), clean water access (3 levels), sanitation facility (4 types)
5. **Digital Access** — Connectivity status (4 levels)
6. **Geographic Location** — Latitude and longitude captured via interactive map

All collected data is encrypted before storage using AES-128-GCM to comply with the Philippine Data Privacy Act of 2012.

### 3.12.2 Disaster Data Collection

Disaster events are recorded with:
1. **Disaster Classification** — Type (typhoon, flood, earthquake, etc.) and name
2. **Geographic Extent** — Circular zones defined by center coordinates and radius
3. **Damage Assessment** — Per-beneficiary damage severity recording (5 levels)

### 3.12.3 Vulnerability Indicator Weights

Vulnerability scoring weights are derived from:
1. Philippine Statistical Authority (PSA) poverty and vulnerability data
2. DSWD (Department of Social Welfare and Development) assessment frameworks
3. NDRRMC (National Disaster Risk Reduction and Management Council) vulnerability guidelines
4. WHO health vulnerability classification standards
5. Domain expert consultation with MDRRMO personnel

---

## 3.13 Ethical Considerations

### 3.13.1 Data Privacy Compliance

The system implements the following measures to comply with the Philippine Data Privacy Act of 2012 (R.A. 10173):

1. **Encryption at Rest** — All PII is encrypted using AES-128-GCM before database storage.
2. **Access Control** — Only authenticated administrators can access beneficiary data.
3. **Purpose Limitation** — Data is collected and processed exclusively for disaster response prioritization.
4. **Data Minimization** — Only data necessary for vulnerability assessment is collected.
5. **Accountability** — All system actions are logged with administrator identity tracking.

### 3.13.2 Informed Consent

Beneficiary data collection follows informed consent protocols established by the MDRRMO, with beneficiaries informed about:
- The purpose of data collection (disaster response prioritization)
- The types of data being collected
- How their data will be stored (encrypted) and used
- Their right to access, correct, or request deletion of their data

### 3.13.3 Algorithmic Fairness

The vulnerability scoring framework was designed with fairness considerations:
- Scoring weights are configurable and transparent, allowing review by domain experts
- The FCM soft clustering approach explicitly avoids hard binary classification, acknowledging that vulnerability exists on a continuum
- Both clustering algorithms were validated against expert-classified datasets to ensure alignment with domain expectations

---

## 3.14 Limitations of the Methodology

The following methodological limitations are acknowledged:

1. **Single Municipality Scope** — The system was developed and tested for the Municipality of Banate, Iloilo. Generalizability to other municipalities requires adaptation of geographic bounds, map tiles, and potentially vulnerability indicator weights.

2. **Offline Map Dependency** — The OpenStreetMap tile rendering relies on pre-downloaded tile sets. Coverage beyond the configured bounds requires additional tile downloads.

3. **GSM Hardware Dependency** — The offline SMS capability requires physical possession of and connection to a SIM800C GSM module, which may represent a single point of failure.

4. **Encryption Key Management** — The current implementation stores the AES encryption key in a configuration file. In production environments, a dedicated key management service (KMS) would provide stronger key protection.

5. **Single-User Concurrency** — The desktop application architecture supports single-user access. Multi-user concurrent access would require architectural evolution to a client-server or web-based model.

6. **AI Dependency** — The news generation feature requires internet connectivity and a valid Anthropic API key, representing an external dependency that may be unavailable during disaster scenarios.

---

## 3.15 Summary

This chapter has presented the comprehensive methodology employed in the design, development, and evaluation of the RESPOND-PH system. The Agile-Iterative SDLC model guided the project through 12 development sprints, producing a feature-complete geospatial desktop application comprising **191 Java source files**, **41 FXML view files**, and **42 CSS stylesheets**, organized into **23 packages** within a layered MVC architecture.

The system implements two machine learning clustering algorithms (Fuzzy C-Means and K-Means) for vulnerability-based beneficiary prioritization, a comprehensive 16-dimension vulnerability scoring framework, AES-128-GCM encryption for data privacy compliance, offline SMS communication via SIM800C GSM hardware, interactive OpenStreetMap-based geospatial visualization, AI-powered contextual news generation via Anthropic Claude, and automated evacuation site allocation.

The methodology demonstrates the systematic application of software engineering principles — including established design patterns (Singleton, DAO, MVC, Observer, Factory, Strategy), robust error handling, comprehensive testing (unit, integration, system, and UAT), and security best practices — to the construction of a domain-specific information system for disaster response operations in the Philippine context.

---

*End of Chapter III: Methodology*

---

**Word Count: Approximately 12,500 words**

**Page Estimate: Approximately 45-55 pages (double-spaced, 12pt font, APA formatting)**

---

## APPENDIX A: Complete Source File Inventory

### A.1 Java Source Files (191 files)

#### Root Package
| File | Lines | Description |
|------|-------|-------------|
| RESPONDPH.java | 38 | Application entry point |

#### admin Package (7+ files)
| File | Description |
|------|-------------|
| AdminController.java | Admin management UI controller |
| AdminDAO.java | Admin data access interface |
| AdminDAOImpl.java | Admin data access implementation |
| AdminModel.java | Admin data model |
| AdminService.java | Admin business logic interface |
| AdminServiceImpl.java | Admin business logic implementation |
| dialogs_controller/ | Add/Edit admin dialog controllers |

#### admin.login Package (5 files)
| File | Description |
|------|-------------|
| LoginController.java | Authentication UI controller |
| LoginDAO.java | Login data access interface |
| LoginDAOImpl.java | Login data access implementation |
| LoginService.java | Login business logic interface |
| LoginServiceImpl.java | Login business logic implementation |

#### aid Package (10 files)
| File | Lines | Description |
|------|-------|-------------|
| AidController.java | 305 | Aid management UI controller |
| AidDAO.java | — | Aid data access interface |
| AidDAOImpl.java | — | Aid data access implementation |
| AidModel.java | — | Aid data model |
| AidPrintService.java | 903 | Report printing engine |
| AidService.java | — | Aid business logic interface |
| AidServiceImpl.java | — | Aid business logic implementation |
| FCMAidDistribution.java | 421 | Fuzzy C-Means clustering algorithm |
| KMeansAidDistribution.java | 391 | K-Means clustering algorithm |
| dialogs_controller/ | — | Add aid and print dialog controllers |

#### beneficiary Package (7+ files)
| File | Lines | Description |
|------|-------|-------------|
| BeneficiaryController.java | — | Beneficiary management UI controller |
| BeneficiaryDAO.java | — | Beneficiary data access interface |
| BeneficiaryDAOImpl.java | — | Beneficiary data access implementation |
| BeneficiaryModel.java | 281 | Beneficiary data model (28 fields) |
| BeneficiaryService.java | — | Beneficiary business logic interface |
| BeneficiaryServiceImpl.java | — | Beneficiary business logic implementation |
| AgeScoreCalculate.java | — | Age-based vulnerability score calculator |

#### household_score Package (4 files)
| File | Lines | Description |
|------|-------|-------------|
| HouseholdScoreCalculate.java | 788 | Core vulnerability scoring engine |
| HouseholdScoreDAO.java | — | Score data access interface |
| HouseholdScoreDAOServiceImpl.java | — | Score data access implementation |
| HouseholdScoreModel.java | 184 | Score data model (16 dimensions) |

#### vulnerability_indicator Package (6 files)
| File | Lines | Description |
|------|-------|-------------|
| VulnerabilityIndicatorController.java | — | UI controller |
| VulnerabilityIndicatorDAOService.java | — | DAO interface |
| VulnerabilityIndicatorDAOServiceImpl.java | — | DAO implementation |
| VulnerabilityIndicatorScoreModel.java | 470 | Score model (65+ weight fields) |
| VulnerabilityIndicatorService.java | — | Service interface |
| VulnerabilityIndicatorServiceImpl.java | 56 | Service implementation |

#### util Package (28 files)
| File | Lines | Description |
|------|-------|-------------|
| Cryptography.java | 178 | AES-128-GCM encryption engine |
| CryptographyManager.java | 53 | Singleton encryption manager |
| Mapping.java | 547 | OpenStreetMap tile renderer |
| SMSSender.java | 349 | SIM800C GSM modem controller |
| SMSApi.java | 75 | SkySMS API client |
| GeographicUtils.java | 58 | Haversine distance calculator |
| SessionManager.java | 66 | Authentication session manager |
| AppContext.java | 39 | Service registry |
| AppLoader.java | 125 | Application bootstrap |
| ConfigLoader.java | 33 | Configuration reader |
| ValidationUtils.java | 60 | Input validation utilities |
| (+ 17 more utility classes) | — | Various cross-cutting utilities |

#### common Package (14 files)
| File | Description |
|------|-------------|
| services/NewsGeneratorService.java (676 lines) | Anthropic Claude AI news generation |
| services/NewsItem.java | News data record |
| services/NewsETAEstimator.java | Progress estimation |
| model/BeneficiaryMarker.java | Map marker model |
| model/EvacSiteMarker.java | Evac site marker model |
| model/DisasterCircleInfo.java | Disaster zone model |
| model/BeneficiaryEncrypted.java | Encrypted beneficiary DTO |
| model/DisasterCircleEncrypted.java | Encrypted circle DTO |
| interfaces/BulkProgressListener.java | Progress callback interface |
| controller/MappingDialogController.java | Map dialog controller |

#### exception Package (5 files)
| File | Description |
|------|-------------|
| DomainException.java | Abstract base exception |
| ValidationException.java | Input validation exception |
| DuplicateEntityException.java | Duplicate entity exception |
| EntityOperationException.java | CRUD operation exception |
| ExceptionFactory.java | Exception creation factory |

### A.2 FXML View Files (41 files)

Listed by module in Section 3.6.3.1.

### A.3 CSS Stylesheet Files (42 files)

Listed by module in Section 3.6.3.2.

### A.4 Resource Files

| File | Purpose |
|------|---------|
| config/Outlet.properties | Application configuration |
| images/location-pin.png | Evacuation site map marker |
| images/person_marker.png | Beneficiary map marker |
| images/placeholder.png | Default map marker |
| images/respondph_logo_.png | Application logo |

---

## APPENDIX B: Vulnerability Indicator Scoring Dimensions

### B.1 Complete Scoring Category Reference

**Dimension 1: Gender (2 categories)**
| Category | Description |
|----------|-------------|
| Male | Male gender |
| Female | Female gender |

**Dimension 2: Marital Status (4 categories)**
| Category | Description |
|----------|-------------|
| Single | Unmarried |
| Married | Legally married |
| Widowed | Spouse deceased |
| Separated | Legally or de facto separated |

**Dimension 3: Solo Parent Status (3 categories)**
| Category | Description |
|----------|-------------|
| Not a Solo Parent | Has partner/co-parent |
| Solo Parent (with Support Network) | Solo parent with family/community support |
| Solo Parent (without Support) | Solo parent without support network |

**Dimension 4: Disability Type (9 categories)**
| Category | Description |
|----------|-------------|
| None | No disability |
| Physical | Physical/mobility disability |
| Visual | Visual impairment |
| Hearing | Hearing impairment |
| Speech | Speech disability |
| Intellectual | Intellectual disability |
| Mental/Psychosocial | Mental health condition |
| Due to Chronic Illness | Disability from chronic disease |
| Multiple Disabilities | Two or more disabilities |

**Dimension 5: Health Condition (6 categories)**
| Category | Description |
|----------|-------------|
| Healthy | No health concerns |
| Temporarily Ill | Acute/temporary illness |
| Chronically Ill | Chronic disease condition |
| Immunocompromised | Weakened immune system |
| Terminally Ill | Terminal diagnosis |
| With Medical Equipment Dependence | Dependent on medical devices |

**Dimension 6: Clean Water Access (3 categories)**
| Category | Description |
|----------|-------------|
| Daily Access | Reliable daily water supply |
| No Access | No access to clean water |
| Irregular | Intermittent water access |

**Dimension 7: Sanitation Facilities (4 categories)**
| Category | Description |
|----------|-------------|
| Safely Managed | Proper sanitation facilities |
| Shared | Shared sanitation facilities |
| Unimproved | Substandard sanitation |
| No Sanitation | No sanitation facilities |

**Dimension 8: House Construction Type (4 categories)**
| Category | Description |
|----------|-------------|
| Concrete/Masonry | Strong permanent structure |
| Semi-Concrete | Mixed material construction |
| Light Materials | Wood/bamboo construction |
| Makeshift | Temporary/improvised shelter |

**Dimension 9: Ownership Status (5 categories)**
| Category | Description |
|----------|-------------|
| Owned with Formal Title | Legal property ownership |
| Owned without Formal Title | Informal ownership |
| Rented | Rental arrangement |
| Informal Settler | Informal/squatter occupation |
| Evicted/Displaced | Recently displaced |

**Dimension 10: Damage Severity (5 categories)**
| Category | Description |
|----------|-------------|
| No Visible Damage | Structure intact |
| Minor Damage | Cosmetic/superficial damage |
| Moderate Damage | Structural damage, habitable |
| Severe Damage | Major structural damage |
| Destruction/Collapse | Complete structural failure |

**Dimension 11: Employment Status (5 categories)**
| Category | Description |
|----------|-------------|
| Regular Employment | Stable, regular employment |
| Self-Employed (Stable Income) | Self-employed with reliable income |
| Self-Employed (Unstable Income) | Self-employed with variable income |
| Irregular Employment | Day labor/seasonal work |
| Unemployed | No employment |

**Dimension 12: Monthly Income (7 brackets)**
| Category | Description |
|----------|-------------|
| Poor | Below poverty threshold |
| Low-Income | Near poverty threshold |
| Lower Middle Income | Lower middle class |
| Middle Class | Middle income |
| Upper Middle Class | Upper middle income |
| Upper Income | High income |
| Rich | Wealthy |

**Dimension 13: Education Level (6 categories)**
| Category | Description |
|----------|-------------|
| No Formal Education | Never attended school |
| Elementary | Elementary school level |
| High School | High school level |
| Vocational | Technical/vocational training |
| College | College/university level |
| Graduate Studies | Post-graduate education |

**Dimension 14: Digital Access (4 categories)**
| Category | Description |
|----------|-------------|
| Reliable Internet | Consistent internet access |
| Intermittent Access | Occasional connectivity |
| Limited Access | Device-only, minimal connectivity |
| No Digital Access | No devices or connectivity |

**Dimension 15: Age Score (5 brackets)**
| Age Range | Score | Rationale |
|-----------|-------|-----------|
| 0–4 years | 1.00 | Infant/toddler — highest vulnerability |
| 5–12 years | 0.70 | Child — high vulnerability |
| 13–17 years | 0.30 | Adolescent — moderate vulnerability |
| 18–59 years | 0.00 | Adult — lowest vulnerability |
| 60+ years | 1.00 | Elderly — highest vulnerability |

**Dimension 16: Dependency Ratio (4 ranges)**
| Ratio Range | Score | Classification |
|-------------|-------|----------------|
| ≥ 2.00 | 1.00 | Severe dependency |
| 1.00 – 1.99 | 0.67 | High dependency |
| 0.50 – 0.99 | 0.33 | Moderate dependency |
| < 0.50 | 0.00 | Low dependency |

---

## APPENDIX C: Fuzzy C-Means Mathematical Foundation

### C.1 Formal Definition

The Fuzzy C-Means (FCM) algorithm, introduced by Dunn (1973) and improved by Bezdek (1981), is a soft clustering method that allows each data point to belong to multiple clusters with varying degrees of membership.

### C.2 Objective Function

The FCM algorithm minimizes the following objective function:

$$J_m = \sum_{i=1}^{n} \sum_{j=1}^{c} \mu_{ij}^m \|x_i - c_j\|^2$$

Where:
- $n$ = number of data points (beneficiaries)
- $c$ = number of clusters (3 in RESPOND-PH: High, Moderate, Low Priority)
- $\mu_{ij}$ = degree of membership of data point $x_i$ in cluster $j$
- $m$ = fuzziness coefficient (m > 1; set to 2.0 in RESPOND-PH)
- $x_i$ = data point (beneficiary's composite vulnerability score)
- $c_j$ = centroid of cluster $j$
- $\|.\|$ = Euclidean distance

### C.3 Constraints

$$\sum_{j=1}^{c} \mu_{ij} = 1, \quad \forall i = 1, ..., n$$
$$0 \leq \mu_{ij} \leq 1, \quad \forall i, j$$

### C.4 Update Equations

**Membership Update:**
$$\mu_{ij} = \frac{1}{\sum_{k=1}^{c} \left(\frac{\|x_i - c_j\|}{\|x_i - c_k\|}\right)^{\frac{2}{m-1}}}$$

**Centroid Update:**
$$c_j = \frac{\sum_{i=1}^{n} \mu_{ij}^m \cdot x_i}{\sum_{i=1}^{n} \mu_{ij}^m}$$

### C.5 Implementation Parameters in RESPOND-PH

| Parameter | Value | Justification |
|-----------|-------|---------------|
| Fuzziness (m) | 2.0 | Standard choice; higher values increase fuzziness, lower values approach K-Means |
| Convergence Threshold | 1×10⁻⁵ | Ensures precision while avoiding unnecessary iterations |
| Max Iterations | 300 | Sufficient for convergence on vulnerability score datasets |
| Number of Clusters (c) | 3 | Maps to three priority levels: High, Moderate, Low |

### C.6 Convergence Criterion

The algorithm terminates when:
$$\max_{i,j} |\mu_{ij}^{(t+1)} - \mu_{ij}^{(t)}| < \epsilon = 10^{-5}$$

Or when the iteration count reaches 300.

---

## APPENDIX D: K-Means++ Initialization Algorithm

### D.1 Standard K-Means Limitation

Standard K-Means is sensitive to initial centroid placement, potentially converging to suboptimal local minima. K-Means++ (Arthur & Vassilvitskii, 2007) addresses this through probabilistic centroid initialization.

### D.2 K-Means++ Steps (as implemented)

```
1. Select first centroid uniformly at random from data points
2. For each subsequent centroid i = 2, ..., k:
    a. For each data point x, compute D(x) = distance to nearest existing centroid
    b. Select new centroid with probability proportional to D(x)²
3. Proceed with standard K-Means iterations
```

### D.3 Implementation Parameters in RESPOND-PH

| Parameter | Value | Justification |
|-----------|-------|---------------|
| Max Iterations | 100 | Sufficient for 1D vulnerability score clustering |
| Convergence Threshold | 0.001 | Balances precision with computational efficiency |
| Number of Clusters (k) | 3 | Maps to three priority levels: High, Moderate, Low |

---

*This methodology chapter provides a complete, reproducible account of the RESPOND-PH system development process, enabling future researchers and practitioners to understand, replicate, and extend the work presented in this dissertation.*

