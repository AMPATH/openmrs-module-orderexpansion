This repository is AMPATH's standalone continuation of https://github.com/mekomsolutions/openmrs-module-orderexpansion, itself a fork of https://github.com/palladiumkenya/openmrs-module-orderexpansion/. It does not track either upstream.

Builds run on every push and pull request to `main`. Pushes to `main` and GitHub releases publish the module to GitHub Packages (`https://maven.pkg.github.com/AMPATH/openmrs-module-orderexpansion`).

# openmrs-module-orderexpansion

## Overview

The Order Expansion module adds order types to OpenMRS that core does not provide. AMPATH uses it for:

1. Medical Supply Orders (`medicalsupplyorder`): consumables and supplies ordered with a quantity and quantity units.

Release 2.0.0 removed the procedure order, procedure and medical supply dispense features, which AMPATH does not use. Procedure and radiology orders at AMPATH are plain OpenMRS orders created by the O3 order basket.

Order types are not created by this module. They are defined in AMPATH's Initializer configuration (`ordertypes.csv`).

## Features

- `MedicalSupplyOrder` order subclass, stored in `medical_supplies_order`, exposed over REST as type `medicalsupplyorder`
- Requires OpenMRS Platform 2.6.1 or later (2.x)

## Prerequisites

- Java 1.8 or higher
- Maven 3.x or higher
- OpenMRS Platform 2.x (specific version compatibility to be verified)

## Building from Source

To build the module from source:

1. Clone the repository:
   ```
   git clone https://github.com/your-repo/openmrs-module-orderexpansion.git
   cd openmrs-module-orderexpansion
   mvn clean install
   ```

2. Compile and package the module:
   ```
   mvn clean package
   ```

3. The compiled OMOD file will be available in the `omod/target` folder.

4. Configure the module settings as needed. With relevant metadata. 
   See [here](https://github.com/palladiumkenya/openmrs-config-kenyaemr/blob/main/configuration/ordertypes/ordertypes.csv) for extended order types.

## Installation

### Option 1: Web Upload (Recommended)

1. Log in to your OpenMRS implementation as an administrator.
2. Navigate to `Administration` > `Manage Modules`.
3. Click on `Add or Upgrade Module`.
4. Choose the OMOD file and upload it.

### Option 2: Manual Installation

1. Copy the OMOD file to the OpenMRS application data directory:
   ```
   ~/.OpenMRS/modules/
   ```
   Note: The actual path may vary depending on your OpenMRS configuration.

2. Restart the OpenMRS application.
