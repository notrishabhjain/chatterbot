package com.digitaltwin.assistant.data.model

/**
 * The four kinds of work the assistant tracks. The whole product thesis is that a PM doesn't only
 * have their own tasks — they delegate, they wait on others, and they log status. All four flow
 * through the same capture → approve → track loop.
 */
enum class ItemType {
    /** Something the user needs to do. */
    MY_TASK,

    /** Something the user asked someone else to do; tracked until that person confirms. */
    DELEGATED,

    /** The user is blocked waiting on someone else's reply/decision. */
    FOLLOW_UP,

    /** An update on an existing item (e.g. "report sent") — usually closes a DELEGATED/FOLLOW_UP. */
    STATUS_UPDATE,
}

/** Where a captured item originated. Drives confidence heuristics and context display. */
enum class Source {
    WHATSAPP,
    SMS,
    CALL_RECORDING,
    EMAIL_NOTIF,
    MANUAL,
    OTHER_NOTIF,
}

/** Lifecycle of a work item from capture to resolution. */
enum class ItemStatus {
    /** Just captured, awaiting the user's approve/discard decision. */
    QUEUED,

    /** Approved and live, not yet on the calendar. */
    ACTIVE,

    /** Approved and written to Google Calendar. */
    SCHEDULED,

    /** Blocked on someone else (delegated/follow-up). */
    WAITING,

    /** Completed / closed. */
    RESOLVED,

    /** User rejected it from the queue. */
    DISCARDED,
}

/** Coarse classification used for prioritisation and the morning briefing. */
enum class Category {
    CLIENT,
    TEAM,
    ADMIN,
    PERSONAL,
    UNCATEGORIZED,
}

enum class Priority {
    HIGH,
    MEDIUM,
    LOW,
}

/** The kind of Google Calendar artifact a work item maps to. */
enum class CalendarTargetType {
    EVENT,
    ALL_DAY,
    REMINDER,
    TIME_BLOCK,
}
