package com.digitaltwin.assistant.data.local

import androidx.room.TypeConverter
import com.digitaltwin.assistant.data.model.Category
import com.digitaltwin.assistant.data.model.ItemStatus
import com.digitaltwin.assistant.data.model.ItemType
import com.digitaltwin.assistant.data.model.Priority
import com.digitaltwin.assistant.data.model.Source

/** Stores enums as their stable [Enum.name] string so reordering the enum never corrupts data. */
class Converters {
    @TypeConverter fun itemTypeToString(v: ItemType): String = v.name
    @TypeConverter fun stringToItemType(v: String): ItemType = ItemType.valueOf(v)

    @TypeConverter fun sourceToString(v: Source): String = v.name
    @TypeConverter fun stringToSource(v: String): Source = Source.valueOf(v)

    @TypeConverter fun statusToString(v: ItemStatus): String = v.name
    @TypeConverter fun stringToStatus(v: String): ItemStatus = ItemStatus.valueOf(v)

    @TypeConverter fun categoryToString(v: Category): String = v.name
    @TypeConverter fun stringToCategory(v: String): Category = Category.valueOf(v)

    @TypeConverter fun priorityToString(v: Priority): String = v.name
    @TypeConverter fun stringToPriority(v: String): Priority = Priority.valueOf(v)
}
