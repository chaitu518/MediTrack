# JVM Report

How the JVM loads, stores, and executes this project, illustrated with concrete examples from
the MediTrack codebase.

## 1. Class Loader

The class loader reads compiled `.class` files and turns them into `Class` objects the JVM can use.
It works in three phases, and does them **lazily** — a class is loaded the first time it's
actually needed, not all up front.

- **Loading** — finds and reads the bytecode. In this project that's the *Bootstrap* loader
  pulling in `java.lang.*`, `java.util.*`, etc. from the JDK itself, and the *Application*
  (system) loader pulling in everything under `com.airtribe.meditrack.*` from `out/` (or the
  classpath). Each package's classes stay bundled together on disk (`entity/`, `service/`,
  `util/`, ...) but the loader doesn't care about folders — it resolves by fully-qualified
  class name, e.g. `com.airtribe.meditrack.entity.Doctor`.
- **Linking**, in three sub-steps:
  - *Verification* — checks the bytecode is well-formed and doesn't violate the JVM's safety
    rules (e.g. that `Appointment.clone()` really does return an `Appointment`, matching its
    declared signature).
  - *Preparation* — allocates storage for static fields and zeroes them. This is why
    `MedicalEntity.entityCount` (a `static final AtomicInteger`) exists in memory as soon as
    the class is prepared, before any constructor has run.
  - *Resolution* — symbolic references (e.g. `Appointment`'s reference to `AppointmentStatus`)
    are resolved to direct references.
- **Initialization** — runs static initializers and static blocks, top to bottom, the first
  time the class is actively used. `AppointmentService` has an explicit example:

  ```java
  private static final Map<AppointmentStatus, Set<AppointmentStatus>> VALID_TRANSITIONS = new EnumMap<>(...);
  static {
      VALID_TRANSITIONS.put(AppointmentStatus.PENDING, EnumSet.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED));
      VALID_TRANSITIONS.put(AppointmentStatus.CONFIRMED, EnumSet.of(AppointmentStatus.CANCELLED));
      VALID_TRANSITIONS.put(AppointmentStatus.CANCELLED, EnumSet.noneOf(AppointmentStatus.class));
  }
  ```

  This block runs exactly once, the first time `AppointmentService` is touched by the JVM (in
  practice, when `Main` constructs the first `AppointmentService` instance) — not once per
  `AppointmentService` object created afterward.

## 2. Runtime Data Areas

Once a class is loaded, running the program uses several distinct memory regions:

- **Heap** — where every object lives: every `Doctor`, `Patient`, `Appointment`, `Bill`, and the
  `LinkedHashMap` backing each `DataStore<T>`. Heap objects are shared across the whole
  application and reclaimed by the garbage collector once nothing references them anymore —
  e.g. after `DoctorService.deleteDoctor(id)` removes the last reference to a `Doctor` from the
  store, that object becomes eligible for collection.
- **Stack** — each thread gets its own stack of *frames*, one per active method call, holding
  local variables and the operand stack for that call. Calling
  `Main.generateBill() → AppointmentService.generateBillForAppointment() → BillFactory.createBill() → Appointment.generateBill()`
  pushes four frames deep; each frame's local variables (`billId`, `baseFee`, `total`, ...) live
  only as long as that frame does, and disappear the instant the method returns. This project is
  single-threaded (one `main` thread reading from `Scanner`), so there's exactly one call stack
  at any time.
- **Method Area** (part of the shared, per-JVM "metaspace" in modern HotSpot) — holds
  per-class data: the bytecode for every method, constant pool entries, and static fields.
  `MedicalEntity.entityCount` and `AppointmentService.VALID_TRANSITIONS` are stored here, not
  in any individual object's heap slot — there is exactly one copy shared by every instance.
- **PC (Program Counter) Register** — each thread has its own PC register holding the address
  of the JVM bytecode instruction it's currently executing within the current frame. It's how
  a thread resumes at the right spot after a method call returns.

```
┌─────────────── Method Area (shared) ───────────────┐
│ Doctor.class, Patient.class, ...  static fields,    │
│ bytecode, constant pool                             │
└──────────────────────────────────────────────────────┘
┌────────────── Heap (shared) ───────────────┐
│ Doctor/Patient/Appointment/Bill instances,  │
│ DataStore's LinkedHashMap, Scanner, ...     │
└──────────────────────────────────────────────┘
┌── Thread: main ──┐
│ PC register       │
│ Stack:             │
│  frame: generateBill()      │
│  frame: generateBillForAppointment() │
│  frame: createBill()        │
│  frame: Appointment.generateBill()   │
└────────────────────┘
```

## 3. Execution Engine

The execution engine is what actually runs the bytecode the class loader produced. It's made
of:

- **The interpreter** — reads bytecode instructions one at a time and executes them directly.
  This is how every method starts out running.
- **The JIT (Just-In-Time) compiler** — watches which methods run often ("hot" methods) and
  compiles *those specific methods* straight to native machine code, so later calls skip
  interpretation entirely. In this codebase, `DataStore.findById`/`findAll`,
  `Validator.validatePhone`, and `AppointmentService.updateStatus` — called repeatedly across
  every menu loop iteration in `Main` — are exactly the kind of small, frequently-called
  methods the JIT targets first.
- **Garbage collector** — part of the execution engine's supporting machinery, reclaiming heap
  objects (like `Doctor`s removed via `deleteDoctor`) once they're unreachable.

### Interpreter vs. JIT compiler

| | Interpreter | JIT compiler |
|---|---|---|
| Starts running | Immediately, no compile delay | Only after a method is seen running often enough |
| Speed once running | Slower — re-decodes bytecode every call | Fast — runs as native machine code |
| Where it applies | Every method, at least once | Only "hot" methods it chooses to compile |
| Trade-off | Fast startup, slower steady-state | Slower warm-up, faster steady-state |

HotSpot (the JVM this project runs on — confirmed by `java -version` reporting
`Java HotSpot(TM) 64-Bit Server VM`) uses both together: interpret first, profile as it goes,
then JIT-compile the methods that turn out to matter. A short-lived console session like
running `MediTrack` interactively barely gives the JIT anything to warm up on; running
`TestRunner`'s nine tests back-to-back is a better example of code getting hot enough for the
JIT to notice.

## 4. "Write Once, Run Anywhere"

`javac` compiles `.java` source into `.class` files containing **JVM bytecode** — an
instruction set that targets the *Java Virtual Machine*, not any specific CPU or OS. Any
machine with a compatible JVM installed (Windows, macOS, Linux, ARM or x86) can run the exact
same `.class` files unmodified, because the JVM implementation for that platform is the piece
that knows how to translate bytecode into whatever that machine actually needs.

Concretely for this project: the `out/` directory produced by `javac` on this Windows/JDK 21
machine could be copied as-is to a Linux server with a JDK 21 (or later) JVM installed, and
`java -cp out com.airtribe.meditrack.Main` would run identically — no recompilation needed.
That's the guarantee "write once, run anywhere" refers to: portability of compiled bytecode,
not of source code.