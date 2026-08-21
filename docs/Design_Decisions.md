# Design Decisions

Why the MediTrack codebase is structured the way it is - the patterns used, the alternatives
considered, and the trade-offs accepted.

## 1. Layering: entity / interfaces / service / util

- **`entity`** - plain data-holders (`Doctor`, `Patient`, `Appointment`, `Bill`, `BillSummary`)
  plus the inheritance chain `MedicalEntity -> Person -> {Doctor, Patient}`. They own their own
  validation (setters call into `Validator`) so an entity can never be mutated into an invalid
  state, no matter which service touches it.
- **`interfaces`** - contracts shared across entities/services (`Payable`, `PaymentStrategy`,
  `Searchable<T>`, `AppointmentObserver`'s console implementation). Kept separate from `service`
  so a service package can depend on a contract without depending on another service's
  implementation.
- **`service`** - orchestration: `DoctorService`, `PatientService`, `AppointmentService`. Each
  wraps a `DataStore<T>` and is where cross-entity rules live (e.g. "a bill needs both the
  appointment's doctor and patient to still exist").
- **`util`** - stateless or singleton infrastructure (`DataStore`, `DateUtil`, `Validator`,
  `IdGenerator`) with no domain rules of its own.

**Why:** keeps validation next to the data it protects, keeps orchestration/business rules out of
entities, and keeps infrastructure reusable across every entity type instead of duplicated per
service.

## 2. `DataStore<T>` instead of one collection per service

Every service (`DoctorService`, `PatientService`, `AppointmentService`) holds a
`DataStore<T> store = new DataStore<>(T::getId)` - a generic wrapper around a
`LinkedHashMap<String, T>` - rather than each rolling its own `Map`/`List` bookkeeping.

**Why:** `save`/`findById`/`deleteById`/`findAll`/`exists` are identical logic for doctors,
patients, and appointments; writing them once generically avoids three near-duplicate
implementations drifting apart over time. `LinkedHashMap` was picked over `HashMap` specifically
so `findAll()` (used by every `list*()` menu option) returns records in insertion order, which
reads naturally in the console UI.

**Trade-off accepted:** everything is in-memory - the app has no persistence layer, so all data
is lost on exit. That's intentional for this project's scope (see `Main`'s `--loadData` flag,
currently a no-op placeholder) rather than an oversight.

## 3. Checked exceptions for domain errors, not runtime exceptions

`InvalidDataException`, `AppointmentNotFoundException`, and `PaymentFailedException` all extend
`Exception` (checked), not `RuntimeException`.

**Why:** these are exactly the errors a console UI (`Main`) needs to catch and turn into a
friendly message instead of crashing - a doctor ID that doesn't exist, a negative fee, a payment
that fails validation. Making them checked forces every call site to make a decision about
handling them at compile time, which matches a menu-driven app where "operation failed, print why,
loop back to the menu" is the correct behavior almost everywhere.

## 4. Factory pattern for bill creation (`BillFactory`)

`AppointmentService.generateBillForAppointment` doesn't do `new Bill(...)` itself or branch on
patient type inline. It resolves a `BillFactory.BillType` (`STANDARD` / `SENIOR_CITIZEN` /
`INSURANCE`) from the patient's age/insurance flag, then hands off to `BillFactory.createBill(...)`,
which knows the discount/coverage math for each type.

**Why:** the billing *rules* (10% senior discount, insurance covers 70%) are a separate concern
from *when* a bill should be generated (payment succeeding) and *who* is eligible for which type
(the patient's age/insurance). Centralizing bill construction in one factory means the tax rate
and discount math can't drift between call sites, and adding a fourth `BillType` later only
touches `BillFactory`.

## 5. Strategy pattern for payment (`PaymentStrategy`, `CardPayment`, `UpiPayment`)

Paying for an appointment is modeled as a `PaymentStrategy` interface (`pay(amount)`,
`getPaymentMethod()`), with `CardPayment` and `UpiPayment` as interchangeable implementations.
`Main` asks the user which method they want, builds the corresponding strategy from the details
they enter, and passes it into
`AppointmentService.generateBillForAppointment(appointmentId, paymentStrategy)`.

**Why Strategy over an `if (method == CARD) ... else ...` branch in the service:**
- `AppointmentService` shouldn't need to know card-number/CVV or UPI-ID validation rules -
  those belong to the payment method itself.
- Adding a third method (e.g. net banking) means adding one new class, not editing
  `AppointmentService`.
- It composes cleanly with the billing flow described below rather than fighting it.

**Why payment gates bill generation, not the other way round:** `generateBillForAppointment`
computes the amount from `BillFactory` first, then calls `paymentStrategy.pay(amount)` *before*
returning anything or touching appointment status. If `pay(...)` throws `PaymentFailedException`,
the method propagates that exception and:
- no `Bill` is ever returned to the caller,
- the appointment's status is left exactly as it was (a `PENDING` appointment is only promoted to
  `CONFIRMED` *after* `pay(...)` returns normally).

This mirrors how billing actually works: you don't hand over a receipt (`Bill`) for a payment that
never cleared, and an appointment isn't "confirmed" on the strength of an unpaid invoice.

## 6. Observer pattern for appointment lifecycle events (`AppointmentObserver`)

`AppointmentService` keeps a `List<AppointmentObserver>` and fires `onAppointmentCreated` /
`onAppointmentCancelled` after a `store.save(...)`/status change succeeds; `Main` registers a
`ConsoleReminderObserver` at startup.

**Why:** appointment creation/cancellation naturally has interested parties beyond
`AppointmentService` itself (a console reminder today; email/SMS notification, an audit log, etc.
could be added later) without `AppointmentService` needing to know what those parties do. Adding
a new observer never requires touching `AppointmentService`.

## 7. Scheduling conflict checks live in `AppointmentService`, not `Appointment`

`createAppointment(doctorId, patientId, dateTime)` rejects a booking if any existing
non-cancelled appointment for the *same doctor* or the *same patient* falls within
`Constants.APPOINTMENT_SLOT_MINUTES` (30) of the requested time, before the appointment is ever
constructed or saved.

**Why here and not on `Appointment` itself:** a single `Appointment` instance has no way to see
its siblings - detecting a double-booking is inherently a query over *all* stored appointments,
which only the service (holding the `DataStore`) can answer. Cancelled appointments are excluded
from the check so a freed-up slot can be rebooked.

**Why a time window instead of exact-match:** two appointments at 10:00 and 10:05 for the same
doctor are just as much a scheduling conflict as two at the exact same minute; a real clinic slot
has duration, even though `Appointment` itself doesn't model an explicit end time.

## 8. Singleton for `IdGenerator`, not for the services

`IdGenerator` is a lazy, thread-safe (double-checked locking) singleton handing out sequential,
prefixed IDs (`DOC-0001`, `PAT-0001`, `APT-0001`). `DoctorService`/`PatientService`/
`AppointmentService`, by contrast, are ordinary objects `Main` constructs once and wires together
- they are *not* singletons.

**Why the difference:** ID uniqueness is a genuinely global invariant (two doctors must never get
the same ID, no matter which service or test created them), so a single shared counter is
correct. The services themselves have no such global-uniqueness requirement - `TestRunner`
deliberately constructs fresh `DoctorService`/`PatientService`/`AppointmentService` instances per
test so tests don't leak state into each other. Making the services singletons would have made
that isolation impossible.

## 9. Immutable `BillSummary` vs. mutable `Bill`

`Bill` has setters (`setBaseAmount`, `setTotalAmount`) guarded by `Validator`; calling
`bill.finalizeBill()` snapshots it into a `BillSummary`, which has no setters at all and is
`final`.

**Why two classes instead of one:** a `Bill` is legitimately being assembled/adjusted while
`BillFactory` computes discounts, but once it's handed back to the user as a receipt
(`Main.generateBill()` calls `finalizeBill()` before printing), nothing should be able to mutate
the numbers the patient was shown. Splitting "in-progress" from "final" into two types makes that
guarantee a compile-time fact instead of a convention someone could accidentally violate.

## 10. Manual `TestRunner` instead of a JUnit dependency

Tests live in `src/com/airtribe/meditrack/test/TestRunner.java` as plain `testXxx()` methods run
by a small hand-rolled harness (`run`, `assertTrue`, `assertEquals`, `assertThrows`), executed via
`java -cp out com.airtribe.meditrack.test.TestRunner` - no build tool, no external dependency.

**Why:** the project intentionally has zero third-party dependencies (see `Setup_Instructions.md`
- just a JDK and `javac`), so pulling in JUnit would mean introducing a build tool (Maven/Gradle)
purely to manage that one dependency. A manual harness keeps `javac`/`java` sufficient for both
running the app and verifying it.