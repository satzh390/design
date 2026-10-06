# Design Patterns — Interview Recall Cheat Sheet

A compact interview-oriented reference for the high-value GoF design patterns.

## 1. Creational Patterns

| Pattern | Core idea | Recall |
|---|---|---|
| **Singleton** | Control creation so a class has one instance within the intended scope | **One instance** |
| **Factory** | Encapsulate/delegate object creation | **Which object should I create?** |
| **Abstract Factory** | Create a family of related objects | **Related product family** |
| **Prototype** | Create an object by copying an existing object | **Copy an existing object** |
| **Builder** | Construct a complex object step-by-step | **Many optional fields / readable construction** |

### Factory vs Abstract Factory
- **Factory** → one product/type of object.
- **Abstract Factory** → multiple related products that belong to the same family.
- Do not add extra factories mechanically; use them when creation complexity or independent variation justifies them.

### Builder
Useful when an object has many optional fields or construction rules.

    User user = new User.Builder("Sathish")
        .age(32)
        .email("sathish@example.com")
        .address("Bangalore")
        .build();

Mandatory values can be supplied to the builder constructor; optional values use fluent methods.

---

## 2. Structural Patterns

| Pattern | Core idea | Recall |
|---|---|---|
| **Adapter** | Make incompatible interfaces work together | **Convert interface** |
| **Decorator** | Add responsibilities by wrapping an object | **Add behavior** |
| **Proxy** | Stand in front of an object and control access | **Control access** |
| **Facade** | Provide a simple interface over a complex subsystem | **Simplify workflow** |
| **Composite** | Treat individual objects and groups uniformly | **Tree: leaf + group** |
| **Bridge** | Separate independently varying dimensions | **Avoid combination explosion** |

### Adapter
The key is **interface compatibility**, not merely parameter conversion.

    Client -> ExpectedInterface
                  |
               Adapter
                  |
            Legacy/Other API

### Decorator vs Proxy
Both can wrap another object, but the **intent** differs:

- **Decorator** → add responsibilities/behavior.
- **Proxy** → control access to the real object.

Typical Proxy uses: authorization, lazy initialization, caching, remote access, transactions, logging.

### Facade
A Facade hides a complex subsystem behind a simpler API.

    OrderFacade.placeOrder()
        -> InventoryService
        -> PaymentService
        -> ShippingService
        -> NotificationService

### Composite
Useful for hierarchical/tree structures where a single object and a group of objects should be handled through the same interface.

    Employee
      ├── Developer       (leaf)
      └── Department      (composite)
            ├── Developer
            └── Developer

---

## 3. Behavioral Patterns

| Pattern | Core idea | Recall |
|---|---|---|
| **Strategy** | Encapsulate interchangeable algorithms/implementations | **Which algorithm?** |
| **Observer** | Notify interested subscribers when something changes | **Publish/subscribe** |
| **Chain of Responsibility** | Pass a request through a sequence of handlers | **Handler chain** |
| **Template Method** | Define a fixed algorithm skeleton with variable steps | **Fixed flow, variable steps** |
| **Command** | Encapsulate an operation/request as an object | **Carry an operation** |
| **State** | Encapsulate behavior that varies with the current state | **Behavior depends on state** |

### Strategy
Use when several implementations solve the same operation and the implementation can vary.

    interface PaymentStrategy {
        void pay(PaymentRequest request);
    }

    class CardPayment implements PaymentStrategy { ... }
    class UpiPayment implements PaymentStrategy { ... }

Typical selection:

    paymentType -> Strategy -> execute()

### Strategy vs Command
A useful interview distinction:

- **Strategy** → *How should I perform this kind of operation?*
- **Command** → *What operation/request am I carrying?*

Strategy is often ideal for synchronous runtime selection.

Command becomes especially useful when the operation needs a lifecycle such as:
- queueing
- asynchronous execution
- retry
- scheduling
- logging/auditing
- persistence
- undo

Do not use Command just because Strategy could technically be modeled as a command.

### Observer
One publisher notifies multiple interested observers without tightly coupling the publisher to their concrete implementations.

Spring application events are a practical conceptual example. Kafka is **conceptually similar** to pub/sub, but is not literally the GoF Observer implementation.

### Chain of Responsibility
A request moves through handlers; each handler can process/reject it or delegate to the next handler.

    HTTP Request
        -> Authentication
        -> Authorization
        -> Validation
        -> Logging
        -> Controller

Spring filters/interceptors are useful real-world analogies.

### Template Method
The parent defines the algorithm skeleton; subclasses customize selected steps.

    ETL:
      extract()
      transform()
      load()

**Template Method = inheritance + fixed skeleton.**

### Strategy vs Template Method
- **Strategy** → composition; swap the algorithm/implementation.
- **Template Method** → inheritance; keep the overall algorithm fixed and vary selected steps.

### State
Encapsulates behavior that changes according to the object's current state.

    Payment
      CREATED
      PROCESSING
      SUCCESS
      FAILED

State objects may handle state-dependent validation/behavior. They **do not have to own the state transitions**.

For a small state machine, an enum plus if/switch can be clearer. State becomes valuable when there are many states and many operations whose behavior varies by state.

---

## 4. High-Value Interview Comparisons

| Comparison | Key distinction |
|---|---|
| **Factory vs Abstract Factory** | One product vs family of related products |
| **Adapter vs Facade** | Convert an interface vs simplify a subsystem |
| **Decorator vs Proxy** | Add responsibility vs control access |
| **Strategy vs State** | Selected algorithm vs current-state-dependent behavior |
| **Strategy vs Command** | How to perform vs operation/request being represented |
| **Template Method vs Strategy** | Inheritance/fixed skeleton vs composition/interchangeable algorithm |

---

## 5. Quick Mental Map

    CREATIONAL
      Singleton        -> one instance
      Factory          -> create an object
      Abstract Factory -> create a product family
      Prototype        -> copy an object
      Builder          -> build a complex object

    STRUCTURAL
      Adapter          -> make interfaces compatible
      Decorator        -> add responsibility
      Proxy            -> control access
      Facade            -> simplify subsystem
      Composite        -> leaf + group uniformly
      Bridge           -> separate dimensions

    BEHAVIORAL
      Strategy         -> interchangeable algorithm
      Observer         -> notify subscribers
      Chain            -> pass through handlers
      Template Method  -> fixed algorithm, variable steps
      Command          -> encapsulate an operation
      State            -> state-dependent behavior

## Interview Rule

**Do not apply a pattern mechanically.**

First identify the design problem:

1. **How should objects be created?** → Creational
2. **How should objects/interfaces be composed?** → Structural
3. **How should behavior vary or collaborate?** → Behavioral
4. Prefer the simplest design that solves the problem; introduce a pattern when it makes variation, coupling, or complexity easier to manage.
