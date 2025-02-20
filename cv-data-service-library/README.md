# CV Data Service Library
This library provides helpers, models and services to facilitate the management of Connected Vehicle data. These components are heavily used by the various modules in the WyoCV Suite. If an operation is expected to be performed in multiple modules, it is likely that the operation is implemented in this library.

## Table of Contents
- [Testing](#testing)
- [Usage](#usage)

## Testing
### Unit Testing
To run the unit tests, follow these steps:
1. Reopen the project in the provided dev container by clicking on the blue button in the bottom left corner of the window and selecting "Reopen in Container"
1. Open a terminal in the dev container
1. Run the following command to execute the tests:
    ```
    mvn clean test -pl cv-data-service-library
    ```

## Usage
Modules in the WyoCV Suite can use the components in this library to simplify their code. The following sections describe the components in a high-level overview.

### Helpers
Helpers are utility classes that provide common functionality. They are designed to be used in multiple modules, and are intended to simplify the code in those modules. For example, the `CreateBaseTimUtil` class is used to create a base TIM object, which is a common operation in the WyoCV Suite.

### Models
Models for the WyoCV Suite are concentrated in this library. These models are used to represent data that is passed between modules. For example, the `ActiveTim` class is used to represent an active TIM in the WyoCV Suite. By having a single source of truth for these models, we can ensure that all modules are using the same data structures.

### Services
Services provide a number of endpoints that correspond to endpoints in the CV Data Controller module. This way, modules can interact with the data service without needing to know the specifics of the data controller. The services are designed to be as simple as possible, with the goal of abstracting the complexity of the data controller.

### Other Components
Other components include a factory to create Kafka producers & consumers, and some database classes to make it easier to interact with the database.