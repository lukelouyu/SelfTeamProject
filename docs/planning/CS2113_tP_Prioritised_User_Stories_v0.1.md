# CS2113 tP Prioritised User Stories v0.1

**Status:** Working deliverable containing accepted user stories only.  
**Related baseline:** CS2113 tP Requirements Baseline v0.2.

## 1. Priority Legend

- `***` - Must have.
- `**` - Should have.
- `*` - Nice to have.

## 2. Writing Principles

- Each story describes a user-visible need rather than an implementation.
- Stories remain small enough to trace to features, use cases, tests, and documentation.
- v1.0 contains the smallest complete and usable workflow.
- v2.0 adds the intended final enhancements.
- Sam and Jordan remain equally important primary personas.

## 3. v1.0 User Stories

| ID | Priority | User story |
|---|---|---|
| US-01 | `***` | As a student managing an unfamiliar routine, I can add an activity with its essential scheduling information so that I can include it in my daily itinerary. |
| US-02 | `***` | As a student managing multiple commitments, I can view activities in input, time, or chronological order and select my preferred default order so that I can review my activities in the sequence that best matches my planning needs. |
| US-03 | `***` | As a student managing multiple commitments, I can find activities using keywords and narrow the results by category, topic, or date so that I can retrieve relevant activities without scanning my entire itinerary. |
| US-04 | `***` | As a student, I can change one or more details of an existing activity while keeping all other details unchanged so that I can update my itinerary with less typing. |
| US-05 | `***` | As a student, I can remove an activity whenever I consider it obsolete so that I can reduce unnecessary schedule information and cognitive load. |
| US-06 | `***` | As a student, I can mark an activity as completed or change it back to incomplete so that I can distinguish finished activities from unfinished ones and maintain an accurate progress record. |
| US-07 | `***` | As a student, I can organise each activity under a fixed category so that I can maintain a consistent structure for my commitments and reduce cognitive load. |
| US-08 | `***` | As a student, I can view my next relevant activity and its essential preparation information in a concise format so that I know what to do next without reading my entire itinerary. |
| US-09 | `***` | As a student, I can record an activity's expected energy demand, sensory load, and optional notes so that I can understand its demands and allow the application to use my information when recommending a suitable schedule. |
| US-10 | `***` | As a student who uses a wheelchair, I can view pre-recorded accessibility information for known facilities so that I can prepare for possible barriers. |
| US-16 | `***` | As a student who uses a wheelchair, I can view pre-recorded accessibility types and distances for connections between known facilities so that I can prepare for travel using locally maintained reference data. |
| US-17 | `**` | As a student, I can create and manage optional topics within a fixed category and assign activities to them so that I can organise related commitments at a level that is meaningful to me. |

## 4. v2.0 User Stories

| ID | Priority | User story |
|---|---|---|
| US-11 | `**` | As a student who uses a wheelchair, I can request a confirmed shortest accessible route between two known facilities so that I can avoid locally recorded barriers and understand the route's total distance. |
| US-12 | `**` | As a student managing energy or sensory demands, I can request a recommended daily schedule based on my fixed commitments, flexible activities, accessibility requirements, and planning preferences so that I can create a feasible plan with less cognitive effort. |
| US-13 | `**` | As a student, I can view a selected day as a text-based timetable showing activities, free periods, energy demands, and sensory loads so that I can understand the structure and demands of my day at a glance. |
| US-14 | `***` | As a student, I can view a text-based summary of my completion, scheduled time, energy demand, and sensory load over a selected period so that I can recognise demanding periods and plan my commitments more clearly. |
| US-15 | `**` | As a student, when a schedule recommendation or accessible-route request lacks required information, I can be guided directly to the missing fields and resume my original request after correcting them so that I can complete the task without searching through unrelated records. |

## 5. Notes for Later Requirements Work

- The exact essential scheduling fields will be defined separately.
- Input order means the order in which activities were added.
- Time order means sorting by start time within a selected day.
- Chronological order means sorting by date and then start time across multiple days.
- Activities with identical dates and times require a deterministic tie-breaking rule.
- Keyword matching rules and whether multiple search filters can be combined remain to be defined.
- Editing acceptance criteria will require atomic updates: if a requested value is invalid, the original activity remains unchanged.
- The deletion safety and recovery policy remains to be defined in the acceptance criteria.
- Completion data will support later summaries without being treated as a performance judgement.
- Fixed categories are `Academic`, `CCA`, `Work/Internship`, and `Others`.
- Topic-management acceptance criteria will cover creation, renaming, deletion, and activity reassignment.
- Essential preparation information may include time, energy demand, sensory load, and notes.
- The exact rule for selecting the next relevant activity remains to be defined.
- Energy and sensory values are user-entered planning information rather than medical assessments.
- Activity keyword searches are case-insensitive and cover descriptions, topics, and notes. Activity results may be narrowed by category, topic, or date. Facility and route information is searched separately.
- A pre-populated sample dataset provides facility and connection information for immediate viewing. Facility information may include step-free entrances, lifts, accessible toilets, rest points, and free-text notes.
- Supported accessibility-feature types initially include `LIFT`, `RAMP`, `SHELTERED_RAMP`, `ACCESSIBLE_WASHROOM`, `STEP_FREE_ENTRANCE`, `REST_POINT`, `AUTOMATIC_DOOR`, and `OTHER`.
- Facility commands operate on venues and their features; connection commands inspect direct graph edges; route commands calculate a complete path from several eligible connections.
- The sample data is local reference information, not verified real-time information or a guarantee of actual conditions.
- Accessibility-data CRUD commands are out of scope. The external `facilities.txt` and `connections.txt` files are the single source of accessibility data; manual edits are validated and loaded on the next application launch.
- An accessible route is confirmed only when every required route connection has known accessibility information and satisfies the applicable wheelchair-accessibility requirements. Otherwise, the application must report that no confirmed accessible route was found rather than claiming that the route is accessible.
- Schedule-recommendation acceptance criteria will distinguish mandatory feasibility constraints from softer energy, sensory, buffer-time, and preferred-time rules.
- The text-based timetable is an alternative to the essential chronological list. It must show exact times and text labels, must not rely on colour, and should support simple day selection or navigation.
- Progress and workload summaries must use only user-entered data, show exact numerical values, and avoid medical conclusions or performance judgements.
- If guided correction is unavailable or incomplete, the application must still identify the missing record and field clearly and return the user safely to the normal command flow.
- Facility and route-connection records remain useful as reference information even if automatic Dijkstra-based route suggestion is not delivered.
- Acceptance criteria will be added after the user stories are confirmed.
- Command syntax and implementation details do not belong inside the user-story wording.
