# rfid-java

RFID reader java modules

## Structure

```markdown
rfid-java/                                (root of the repository)
├── .gitignore
├── README.md
├── pom.xml                               (parent POM - single version for all modules)
├── mvnw
├── mvnw.cmd
├── .mvn/
│   └── wrapper/
│       └── ...                            (maven wrapper files)
├── scripts/
│   └── install-local-sdks.sh              (optional helper to install all libs/sdk.jar locally)
├── rfid-core/                             (shared interface + utilities)
│   ├── pom.xml
│   ├── README.md
│   └── src/
│       ├── main/
│       │   └── java/
│       │       └── com/
│       │           └── contare/
│       │               └── rfid/
│       │                   ├── RfidDevice.java
│       │                   └── NativeLoader.java
│       └── test/
│           └── java/
│               └── com/
│                   └── contare/
│                       └── rfid/
│                           └── RfidDeviceTest.java
├── rfid-chainway/                         (manufacturer module example)
│   ├── pom.xml
│   ├── README.md
│   ├── libs/
│   │   ├── sdk.jar                        (vendor Java SDK jar - checked-in)
│   │   └── native/
│   │       ├── linux-x86_64/
│   │       │   └── libchainway.so         (native binary alternative A)
│   │       └── windows-x86_64/
│   │           └── chainway.dll           (native binary alternative A)
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/
│       │   │       └── contare/
│       │   │           └── rfid/
│       │   │               └── chainway/
│       │   │                   └── ChainwayDevice.java
│       │   └── resources/
│       │       └── native/                 (native binary alternative B - packaged directly)
│       │           ├── linux-x86_64/
│       │           │   └── libchainway.so
│       │           └── windows-x86_64/
│       │               └── chainway.dll
│       └── test/
│           └── java/...
├── rfid-urovo/                            (another manufacturer example)
│   ├── pom.xml
│   ├── README.md
│   ├── libs/
│   │   ├── sdk.jar
│   │   └── native/
│   │       └── linux-x86_64/
│   │           └── liburovo.so
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/company/rfid/urovo/UrovoDevice.java
│       │   └── resources/
│       │       └── native/linux-x86_64/liburovo.so
│       └── test/...
├── manufacturer-template/                  (scaffold to add new vendor modules quickly)
│   ├── pom.xml
│   ├── README.md
│   ├── libs/
│   │   └── sdk.jar                         (placeholder - drop vendor SDK here)
│   └── src/
│       ├── main/
│       │   ├── java/com/company/rfid/<vendor>/<VendorDevice.java>
│       │   └── resources/native/<os>-<arch>/
│       │       └── <native-binaries>
│       └── test/...
└── tools/
    └── verify-natives.sh                   (optional: checks each module for native artifacts)
```

## Usage

### Import

```xml
<dependencies>
    <dependency>
        <groupId>com.contare</groupId>
        <artifactId>rfid-core</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>com.contare</groupId>
        <artifactId>rfid-chainway</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>com.contare</groupId>
        <artifactId>rfid-urovo</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

## Frequency

| Region       |  Frequency  | Chainway Mask |
|:-------------|:-----------:|:-------------:|
| China        |  840 ~ 845  |     0x01      |
| China        |  920 ~ 925  |     0x02      |
| Europe       |  865 ~ 868  |     0x04      |
| USA          |  902 ~ 928  |     0x08      |
| Korea        |  917 ~ 923  |     0x16      |
| Japan        |  952 ~ 953  |     0x32      |
| South Africa |  915 ~ 919  |     0x33      |
| Taiwan       |  920 ~ 928  |     0x34      |
| Peru         |  915 ~928   |     0x36      |
| Russia       | 860 ~ 867.6 |     0x37      |
| Brazil       | 905 ~ 907.5 |     0x3C      |
